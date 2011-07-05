package info.pppc.base.system.security.rsa;

import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.IPrivateKey;
import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.params.RSAKeyParameters;

/**
 * Implements a standard x509 RSA certificate. The key length is variable (at least 1024 bit).
 * Only the public key is mandatory.<br>
 * For using the certificate for encryption, decryption, signing and verifying, use the underlying methods of the keys.
 * @author WA
 */
public class RSACertificate extends AbstractCertificate {

	private final RSAPublicKey publicKey;
	private final RSAPrivateKey privateKey;
	
	private final X509Name issuer;
	private final X509Name subject;
	
	/**
	 * Creates a new RSACertificate from a byte array. The public key will be loaded into memory,
	 * so it can be used later on. This constructor needs the certificate as a byte array.<br>
	 * This constructor does not create a private key in the certificate abstraction, therefore decryption and signing are not available for this instance.
	 * @param certificate The byte array containing the certificate
	 * @throws IOException If the certificate could not be created, an exception will be thrown
	 */
	public RSACertificate(byte[] certificate) throws IOException
	{
		this(certificate,null);
	}

	/**
	 * Creates a RSA certificate from a byte array.
	 * Also loads a private key from a byte array.
	 * This can be used to get simple access to a certificate file, the public and private key (if available) will be loaded into memory,
	 * so they can be used later on.
	 * @param certificateBytes The certificate in a byte array
	 * @param privateKey The private key in a byte array or null, if no private key exists
	 * @throws IOException If the certificate could not be created, an exception will be thrown
	 */
	public RSACertificate(byte[] certificateBytes, byte[] privateKey) throws IOException
	{
		super(certificateBytes);
		ByteArrayInputStream cert=new ByteArrayInputStream(certificateBytes);
		ASN1InputStream readIn=new ASN1InputStream(cert);
		ASN1Sequence encodedSeq=(ASN1Sequence)readIn.readObject();
		cert.close();
		readIn.close();
		X509CertificateStructure x509 = X509CertificateStructure.getInstance(encodedSeq); 
		RSAPublicKeyStructure publicKey = RSAPublicKeyStructure.getInstance(x509.getSubjectPublicKeyInfo().getPublicKey());
		issuer=x509.getIssuer();
		subject=x509.getSubject();
		this.publicKey=new RSAPublicKey(new RSAKeyParameters(false, publicKey.getModulus(), publicKey.getPublicExponent()));
		if(privateKey==null)
		{
			this.privateKey=null;
			return;
		}
		RSAPrivateKey thePrivateKey=null;
		try 
		{
			ByteArrayInputStream privateKeyStream=new ByteArrayInputStream(privateKey);
			ASN1InputStream asn1readIn=new ASN1InputStream(privateKeyStream); 
			ASN1Sequence encodedPrivateSeq=(ASN1Sequence)asn1readIn.readObject();
			RSAPrivateKeyStructure privateKeyStructure=RSAPrivateKeyStructure.getInstance(encodedPrivateSeq);
			thePrivateKey=new RSAPrivateKey(new RSAKeyParameters(true, privateKeyStructure.getModulus(), privateKeyStructure.getPrivateExponent()));
		}
		catch(IOException e)
		{
			Logging.error(RSACertificate.class, "Could not load private key!", e);
			this.privateKey = null;
			return;
		}
		this.privateKey = thePrivateKey;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getIssuer()
	 */
	public X509Name getIssuer()
	{
		return issuer;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getName()
	 */
	public X509Name getSubject()
	{
		return subject;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getPrivateKey()
	 */
	public IPrivateKey getPrivateKey()
	{
		return this.privateKey;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#getPublicKey()
	 */
	public IPublicKey getPublicKey()
	{
		return this.publicKey;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.CertificateAbstraction#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if((obj==null) || (!(obj instanceof RSACertificate))) return false;
		RSACertificate compare=(RSACertificate)obj;
		if(privateKey!=null)
		{
			if(!privateKey.equals(compare.getPrivateKey())) return false;
		}
		else
		{
			if(compare.getPrivateKey()!=null) return false;
		}
		if(this.certificate.length!=compare.certificate.length) return false;
		//Public key is part of the certificate bytes:
		for(int i=0;i<this.certificate.length;i++)
		{
			if(this.certificate[i]!=compare.certificate[i]) return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] getSignature()
	{
		try
		{
			ASN1InputStream readIn=new ASN1InputStream(new ByteArrayInputStream(this.certificate));
			ASN1Sequence encodedSeq=(ASN1Sequence)readIn.readObject();
			readIn.close();
			X509CertificateStructure x509 = X509CertificateStructure.getInstance(encodedSeq);
			return x509.getSignature().getBytes(); //ganzes Zertifikat als byte-Array
		}
		catch(IOException e)
		{
			Logging.error(this.getClass(), "Could not load the signature of the certificate!", e);
			return null;
		}
	}

	/**
	 * 
	 * 	{@inheritDoc}
	 */
	public byte[] getSignedPart()
	{
		try
		{
			ASN1InputStream readIn=new ASN1InputStream(new ByteArrayInputStream(this.certificate));
			ASN1Sequence encodedSeq=(ASN1Sequence)readIn.readObject();
			readIn.close();
			X509CertificateStructure x509 = X509CertificateStructure.getInstance(encodedSeq);
			return x509.getTBSCertificate().getDEREncoded(); //ganzes Zertifikat als byte-Array
		}
		catch(IOException e)
		{
			Logging.error(this.getClass(), "Could not load signed part of the certificate!", e);
			return null;
		}
	}
}
