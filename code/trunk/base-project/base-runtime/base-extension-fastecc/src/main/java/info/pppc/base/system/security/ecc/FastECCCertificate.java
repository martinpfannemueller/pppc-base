package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.security.KeyStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.X509CertificateStructure;

import com.sun.midp.pki.X509Certificate;

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
public class FastECCCertificate extends ECCCertificate {

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
	public FastECCCertificate(byte[] certificate, byte[] privateKey) throws IOException {
		super(certificate);
		ASN1InputStream cert = new ASN1InputStream(new ByteArrayInputStream(
				certificate));
		ASN1Sequence certificateSequence = (ASN1Sequence) cert.readObject();
		X509CertificateStructure x509cert = X509CertificateStructure
				.getInstance(certificateSequence);
		IPublicKey pk=null;
		String curveId=null;
		try
		{
			X509Certificate sunspotCert;
			synchronized(KeyStore.class) {
			//Sunspot modifications:
				sunspotCert=X509Certificate.generateCertificate(certificate,0,certificate.length);
			}
			pk=new FastECCPublicKey(sunspotCert.getPublicKey());
			curveId=SUN_ALGORITHM_ID;

		}
		catch(Exception e)
		{
			throw new IOException("Cannot create fast ecc for key pair.");
		}
		this.publicKey=pk;
		issuer = x509cert.getIssuer();
		subject = x509cert.getSubject();
	
		if (privateKey == null)
		{
			this.privateKey = null;
		}
		else
		{	
			//	Sunspot modifications:
			this.privateKey=new FastECCPrivateKey(FastECCKeyParser.fastECCKeyParser(privateKey));
		}
		this.curveId=curveId;
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
	public FastECCCertificate(byte[] certificate) throws IOException {
		this(certificate, null);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if ((obj == null) || (obj.getClass() != FastECCCertificate.class))
			return false;
		FastECCCertificate compare = (FastECCCertificate) obj;
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
	
	/**
	 * Test if the ECCCertificate uses the fast implementation created from SUN (using the SECP160R1 curve)
	 * @return <b>True</b> if this is a ECCCertificate using the fast SUN implementation (created for the SUNSpots), <b>false</b> otherwise.
	 */
	public boolean isFastKey()
	{
		return true;
	}

}
