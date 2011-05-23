package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.IPrivateKey;
import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.sec.ECPrivateKeyStructure;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ECPoint;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;


/**
 * Implements an elliptic curve cryptography certificate. Uses Bouncycastle for
 * parsing in the parameters from a certificate (and a private key) file.
 * Encryption/decryption are currently not supported.
 * <br><br>
 * Tries first to use the fast ECC implementation that is only compatible to the SECP160R1 curve.
 * If this is not possibles falls back to a slower ECC implementation that allows the usage of more curves.
 * 
 * @author WA
 */
public class ECCCertificate extends AbstractCertificate {

	IPrivateKey privateKey;

	IPublicKey publicKey;

	X509Name issuer;

	X509Name subject;
	
	String curveId;
	
	public static final String SUN_ALGORITHM_ID="SUN SECP160R1";

	/**
	 * Creates a new ECCCertificate from a byte array. Combines the certificate
	 * and the private key, if the parameter <b>privateKey</b> is not
	 * <b>null</b>. Uses code from the PrivateKeyFactory.java from Bouncycastle
	 * to create the private key.
	 * 
	 * @param certificate
	 *            The byte array containing the ECC certificate
	 * @param privateKey
	 *            The byte array containing the corresponding private key of
	 *            this ECC certificate or <b>null</b> if no such key exists
	 * @exception IOException
	 *                If the byte array format is not compatible to the ECC
	 *                format or if another read error occurs, this exception is
	 *                thrown.
	 */
	public ECCCertificate(byte[] certificate, byte[] privateKey) throws IOException {
		super(certificate);
		ASN1InputStream cert = new ASN1InputStream(new ByteArrayInputStream(
				certificate));
		ASN1Sequence certificateSequence = (ASN1Sequence) cert.readObject();
		X509CertificateStructure x509cert = X509CertificateStructure
				.getInstance(certificateSequence);
		
		IPublicKey pk=null;
		String curveId=null;

		SubjectPublicKeyInfo keyInfo = x509cert.getSubjectPublicKeyInfo();
		//Get the algorithm id
		{
			X962Parameters params = new X962Parameters((DERObject)keyInfo.getAlgorithmId().getParameters());
			if (params.isNamedCurve())
			{
				DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
				curveId=oid.getId();
			}
			else
			{
				curveId=null;
			}
		}
		
		// drop in replacement
		AsymmetricKeyParameter publicKey = createPublicKey(keyInfo);
		// 	end drop in replacement
		if (!(publicKey instanceof ECPublicKeyParameters)) {
			throw new IOException(
					"This is no elliptic curve key (ECKeyPublicParameters): "
							+ publicKey.getClass());
		}
		pk = new ECCPublicKey((ECPublicKeyParameters) publicKey);

		this.publicKey=pk;
		
		issuer = x509cert.getIssuer();
		subject = x509cert.getSubject();
	
		if (privateKey == null)
		{
			this.privateKey = null;
		}
		else
		{	
			
				this.privateKey=createStandardPrivateKey(privateKey);
		}
		this.curveId=curveId;
	}
	
	private IPrivateKey createStandardPrivateKey(byte[] privateKey) {
		//2 - Comment this to use sunspot library
		try {
			ASN1InputStream privKey = new ASN1InputStream(
					new ByteArrayInputStream(privateKey));
			ECPrivateKeyStructure privKeyStructure = new ECPrivateKeyStructure(
					(ASN1Sequence) privKey.readObject());
			// Copied from PrivateKeyFactory.java - Bouncycastle:
			X962Parameters params = new X962Parameters(getParameter((ASN1Sequence)privKeyStructure.toASN1Object()));
			//X962Parameters params = new X962Parameters(privKeyStructure.getParameters());
			ECDomainParameters dParams = null;

			if (params.isNamedCurve()) {
				DERObjectIdentifier oid = (DERObjectIdentifier) params.getParameters();
				X9ECParameters ecP = X962NamedCurves.getByOID(oid);

				if (ecP == null) {
					ecP = SECNamedCurves.getByOID(oid);

					if (ecP == null) {
						ecP = NISTNamedCurves.getByOID(oid);

						if (ecP == null) {
							ecP = TeleTrusTNamedCurves.getByOID(oid);
						}
					}
				}

				dParams = new ECDomainParameters(ecP.getCurve(),
						ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
			} else {
				X9ECParameters ecP = new X9ECParameters(
						(ASN1Sequence) params.getParameters());
				dParams = new ECDomainParameters(ecP.getCurve(),
						ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
			}

			// Copying ends!

			return new ECCPrivateKey(
					new ECPrivateKeyParameters(privKeyStructure.getKey(),
							dParams));

		} catch (IOException e) {
			Logging.error(this.getClass(),"Could not load private elliptic curve key, using no private key!",e);
			return null;
		}
	}

	/**
	 * Creates a new ECC certificate without the private key, only consisting of
	 * the public key.
	 * 
	 * @see #ECCCertificate(byte[], byte[])
	 * @param certificate
	 *            The byte array containing the ECC certificate
	 * @throws IOException
	 *             If the byte array format is not compatible to the ECC format
	 *             or if another read error occurs, this exception is thrown.
	 */
	public ECCCertificate(byte[] certificate) throws IOException {
		this(certificate, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getIssuer()
	 */
	public X509Name getIssuer() {
		return issuer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getName()
	 */
	public X509Name getSubject() {
		return subject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getPrivateKey()
	 */
	public IPrivateKey getPrivateKey() {
		return privateKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getPublicKey()
	 */
	public IPublicKey getPublicKey() {
		return publicKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#equals(java.lang.Object
	 * )
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if ((obj == null) || (obj.getClass() != ECCCertificate.class))
			return false;
		ECCCertificate compare = (ECCCertificate) obj;
		if (privateKey != null) {
			if (!privateKey.equals(compare.getPrivateKey()))
				return false;
		} else {
			if (compare.getPrivateKey() != null)
				return false;
		}
		//Public key is part of the certificate bytes:
		if(this.certificate.length!=compare.certificate.length) {
			return false;
		}
		for(int i=0;i<this.certificate.length;i++)
		{
			if(this.certificate[i]!=compare.certificate[i]) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getSignature()
	 */
	public byte[] getSignature() {
		try {
			ASN1InputStream cert = new ASN1InputStream(new ByteArrayInputStream(this.certificate));
			ASN1Sequence certificateSequence = (ASN1Sequence) cert.readObject();
			X509CertificateStructure x509cert = X509CertificateStructure
					.getInstance(certificateSequence);
			ASN1InputStream is = new ASN1InputStream(x509cert.getSignature().getBytes());
			
			ASN1Sequence seq = ((ASN1Sequence) is.readObject());
			// ASN1Sequence seq=(ASN1Sequence)hello.toASN1Object();

			if (seq.size() != 2) {
				throw new IllegalArgumentException("Bad sequence size: " + seq.size());
			}
			return seq.getDEREncoded();
		} catch (IOException e) {
			Logging.error(this.getClass(), "Could not load signature of the certificate.", e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getSignedPart()
	 */
	public byte[] getSignedPart() {
		try {
			ASN1InputStream cert = new ASN1InputStream(new ByteArrayInputStream(this.certificate));
			ASN1Sequence certificateSequence = (ASN1Sequence) cert.readObject();
			X509CertificateStructure x509cert = X509CertificateStructure
					.getInstance(certificateSequence);
			return x509cert.getTBSCertificate().getDEREncoded();
		} catch (IOException e) {
			Logging.error(this.getClass(),	"Could not load signed part of the certificate.!", e);
			return null;
		}
	}
	
	/**
	 * Test if the ECCCertificate uses the fast implementation created from SUN (using the SECP160R1 curve)
	 * @return <b>True</b> if this is a ECCCertificate using the fast SUN implementation (created for the SUNSpots), <b>false</b> otherwise.
	 */
	public boolean isFastKey()
	{
		return false;
	}
	
	/**
	 * Get the curve id of this curve.
	 * @return The curve ID of the used elliptic curve
	 */
	public String getCurveId()
	{
		return curveId;
	}

	/**
	 * Drop in replacement for missing fragment in createKey method in Bouncycastle 1.34
	 * PublicKeyFactory.createKey(SubjectPublicKeyInfo keyInfo).
	 * 
	 * @param keyInfo The key info used to extract the parameter.
	 * @return The parameters extracted from the key info.
	 */
	private ECPublicKeyParameters createPublicKey(SubjectPublicKeyInfo keyInfo) {
		
        X962Parameters      params = new X962Parameters((DERObject)keyInfo.getAlgorithmId().getParameters());
        ECDomainParameters  dParams = null;
        
        if (params.isNamedCurve())
        {
            DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
            X9ECParameters      ecP = X962NamedCurves.getByOID(oid);

            if (ecP == null)
            {
                ecP = SECNamedCurves.getByOID(oid);

                if (ecP == null)
                {
                    ecP = NISTNamedCurves.getByOID(oid);

                    if (ecP == null)
                    {
                        ecP = TeleTrusTNamedCurves.getByOID(oid);
                    }
                }
            }

            dParams = new ECDomainParameters(
                                        ecP.getCurve(),
                                        ecP.getG(),
                                        ecP.getN(),
                                        ecP.getH(),
                                        ecP.getSeed());
        }
        else
        {
            X9ECParameters ecP = new X9ECParameters(
                        (ASN1Sequence)params.getParameters());
            dParams = new ECDomainParameters(
                                        ecP.getCurve(),
                                        ecP.getG(),
                                        ecP.getN(),
                                        ecP.getH(),
                                        ecP.getSeed());
        }

        DERBitString    bits = keyInfo.getPublicKeyData();
        byte[]          data = bits.getBytes();
        ASN1OctetString key = new DEROctetString(data);

        X9ECPoint       derQ = new X9ECPoint(dParams.getCurve(), key);
        return new ECPublicKeyParameters(derQ.getPoint(), dParams);
	}
	
	/**
	 * Drop in replacement for missing getParameters method in Bouncycastle 1.34
	 * ECPrivateKeyStructure.getParameters().
	 * 
	 * @param seq The sequence that is contained within the private key structure.
	 * @return The parameters extracted from the structure.
	 */
    private DERObject getParameter(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            DEREncodable obj = (DEREncodable)e.nextElement();

            if (obj instanceof ASN1TaggedObject)
            {
                ASN1TaggedObject tag = (ASN1TaggedObject)obj;
                if (tag.getTagNo() == 0)
                {
                	return (DERObject)((DEREncodable)tag.getObject()).getDERObject();
                }
            }
        }
        return null;
    }
}
