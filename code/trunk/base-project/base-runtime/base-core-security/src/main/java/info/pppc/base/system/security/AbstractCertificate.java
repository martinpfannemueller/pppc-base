package info.pppc.base.system.security;

import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.digests.SHA1Digest;


/**
 * A general abstraction for all different types of certificates (e.g. RSA or ECC).
 * All certificates must inherit from this abstraction.<br>
 * Main methods, like getting the fingerprint or encryption and decryption are already implemented here, only some certificate specific attributes must be added to inherit successfully from this abstraction.<br><br>
 * If you create a new certificate type, don't forget to add the new certificate type also to the CertificateFactory.
 * @author WA
 */
public abstract class AbstractCertificate
{

	/**
	 * The certificate stored in a byte array (in memory!). Can be <b>null</b>, if the certificate is stored in a file.
	 */
	protected final byte[] certificate;
	
	/**
	 * The hash code for the certificate that is computed during load time.
	 */
	final int hashCode;
	
	/**
	 * The finger print for the certificate.
	 */
	private final byte[] fingerprint;
	
	/**
	 * The start date of the certificate
	 */
	private final Date startDate;
	 
	/**
	 * The end date of the certificate
	 */
	private final Date endDate;
	
	/**
	 * Creates a new certificate abstraction, needs the bytes of the certificate.
	 * @param certificateBytes The bytes of the certificate (so it can be accessed properly)
	 */
	public AbstractCertificate(byte[] certificateBytes) throws IOException
	{
		certificate = certificateBytes;
		// compute the finger print for the certificate
		byte[] signature=new byte[20];
		try { 
			ASN1InputStream cert=new ASN1InputStream(new ByteArrayInputStream(certificate));
			ASN1Sequence certificateSequence=(ASN1Sequence)cert.readObject();
			X509CertificateStructure x509cert=X509CertificateStructure.getInstance(certificateSequence);
			
			String date=x509cert.getStartDate().getTime();
			startDate=parse(date);
			if (startDate == null) throw new IllegalArgumentException("Cannot parse date.");
			date=x509cert.getEndDate().getTime();
			endDate=parse(date);
			if (endDate == null) throw new IllegalArgumentException("Cannot parse date.");
			
			SHA1Digest digest=new SHA1Digest();
			byte[] signedPart=x509cert.getDEREncoded();
			digest.update(signedPart, 0, signedPart.length);
			signedPart = null;
			digest.doFinal(signature, 0);
		}
		catch (IOException e) {
			// should never happen since stream is read from byte buffer
			throw new IOException("Could not compute fingerprint.");
		}
		catch(IllegalArgumentException e)
		{
			throw new IOException("Could not parse time for start and end date of the certificate.");
		}
		fingerprint = signature;
		// compute the hash code from the finger print
		int code = 0;
		for (int i = 0; i < fingerprint.length; i += 4) {
			code ^= fingerprint[i] << 24 | fingerprint[i + 1] << 16 | fingerprint[i + 2] << 8 | fingerprint[i + 3];
		}
		hashCode = code;
	}
	
	/**
	 * Parses a data from a certificate (yyyyMMddHHmmssz).
	 * 
	 * @param date The date to parse.
	 * @return The parsed date or null, if not possible.
	 */
	private static Date parse(String date) {
		try {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));                                
            c.set(Calendar.MONTH, Integer.parseInt(date.substring(4, 6)) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(6, 8)));
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(date.substring(8, 10)));
            c.set(Calendar.MINUTE, Integer.parseInt(date.substring(10, 12)));
            c.set(Calendar.SECOND, Integer.parseInt(date.substring(12, 14)));
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        } catch (Throwable t) {
            Logging.error(AbstractCertificate.class, "Cannot convert date.", t);
        }
        return null;
	}
	
	
	/**
	 * Get the public key of the certificate
	 * @return The public key of the certificate or null, if no public key exists
	 */
	public abstract IPublicKey getPublicKey();
	
	/**
	 * Get the private key of the certificate
	 * @return The private key of the certificate or null, if no private key exists
	 */
	protected abstract IPrivateKey getPrivateKey();
	
	/**
	 * Get the subject of the certificate.
	 * 
	 * @return The subject (name) of the certificate as X509Name
	 */
	public abstract X509Name getSubject();
	
	/**
	 * Get the issuer of the certificate
	 * @return The issuer of the certificate as X509Name
	 */
	public abstract X509Name getIssuer();
	
	/**
	 * Verify if the certificate is trusted or issued by a trusted CA.<br>
	 * The result is not cached and reevaluated every time this method is called. You can use the is trusted method instead, to get a cached value.
	 * @return True, if the certificate is trusted, false otherwise.
	 */
	public int verifyCertificate()
	{
		try {
			return KeyStore.getInstance().verifyCertificate(new AbstractCertificate[] { this });	
		} catch (IllegalStateException e) {
			return KeyStore.TRUST_LEVEL_INVALID;
		}
		
	}
	
	/**
	 * Gets the SHA-1 fingerprint of the certificate.
	 * @return The SHA-1 fingerprint of the certificate (160 bit = 20 byte)
	 * @throws IOException This exception is thrown if the certificate is stored in a file and cannot properly accessed.
	 */
	public final byte[] getFingerprint() 
	{
		return fingerprint;
	}
	
	/**
	 * Signs a given byte array. First creates the SHA-1-Hash and then signs it. Returns the signed SHA-1 hash value.<br><br>
	 * This is only possible, if the device also has the private key!
	 * @param toSign The byte array which should be hashed and signed
	 * @return The signature for the SHA-1 hash of the given byte array. Returns null, if the device does not know the private key for this certificate.
	 */
	public byte[] sign(byte[] toSign)
	{
		IPrivateKey signingKey=this.getPrivateKey();
		if(signingKey==null) return null;
		return(signingKey.createSignature(toSign));
	}
	
	/**
	 * Verifies a signature which was created with the private key of this certificate (only needs the public key for verification).
	 * @param message The message which was signed
	 * @param signature The signature of the message
	 * @return True if the signature is valid and was created by the subject of this certificate
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		IPublicKey verificationKey=this.getPublicKey();
		if(verificationKey==null) return false; //TODO Throw exception?
		if(!isValidRegardingStartAndEndDate())
		{
			return false;
		}
		return verificationKey.verifySignature(message, signature);
	}
	
	/**
	 * Encrypts a message, so that it can only be read by the owner of the corresponding private key of this certificate.<br>
	 * Please ensure, that this message contains some "randomness", e.g. at least the current date and time.
	 * @param message The message which should be encrypted
	 * @return A message encrypted with the public key of this certificate, if the certificate contains the public key; null, if not or if an IOException occurred
	 */
	public byte[] encrypt(byte[] message)
	{
		IPublicKey encryptionKey=this.getPublicKey();
		if(encryptionKey==null) return null;
		try
		{
			return encryptionKey.encryptMessage(message);
		}
		catch(IOException e)
		{
			Logging.error(this.getClass(), "Exception occurred while encrypting a message, maybe not enough memory was available! ",e);
			return null;
		}
	}
	
	/**
	 * Decrypts a message, so that the plain text is accessible.
	 * @param encryptedMessage A message which was encrypted using the public key of this certificate
	 * @return The plain text of the message, if the certificate contains the private key; null, if not or if an IOException occurred
	 * @throws IOException 
	 */
	public byte[] decrypt(byte[] encryptedMessage)
	{
		IPrivateKey decryptionKey=this.getPrivateKey();
		if(decryptionKey==null) return null;
		try
		{
			return decryptionKey.decryptMessage(encryptedMessage);
		}
		catch(IOException e)
		{
			Logging.error(this.getClass(), "Exception occured while decrypting a message!",e);
			return null;
		}
	}

	/**
	 * Can be used to check if the device owns the corresponding private key of this certificate.
	 * @return True, if the private key exists, false otherwise
	 */
	public boolean hasPrivateKey()
	{
		if(this.getPrivateKey()==null) return false;
		else return true;
	}

	/**
	 * Convert the bytes of the certificate to a BASE64-encoded string.
	 * @return The certificate as BASE64-encoded string!
	 */
	public String toString()
	{
		return(new String(org.bouncycastle.util.encoders.Base64.encode(this.certificate)));
	}
	
	/**
	 * Get the certificate as byte array (not encoded).
	 * @return The certificate as byte array!
	 */
	public byte[] toByteArray()
	{
		return certificate;
	}

	/**
	 * Every certificate type has to implement a comparison operator. Usually an certificate equals another one, if ALL fields are identical.
	 * This method must returns true, if the certificate equal each other.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @param obj The object of the type CertificateAbstraction that should be compared to the current certificate
	 * @return True, if the current certificate and the given certificate are equal, false otherwise
	 */
	public abstract boolean equals(Object obj);
	
	/**
	 * Returns the part of the certificate, that was signed by the issuer. In combination with the signature (and the certificate of the issuer), the authenticity of this certificate can be checked by anyone.
	 * @return The part of the certificate, that was signed by the certificate issuer.
	 */
	public abstract byte[] getSignedPart();
	
	/**
	 * Get the signature of the issuer of this certificate. This is usually a signature about the signed part of the certificate.
	 * @return The signature of this certificate.
	 * @see #getSignedPart()
	 */
	public abstract byte[] getSignature();

	/**
	 * Verifies a given CA root certificate. Does not verify the current trust status, only the signature.
	 * @param caCertificate A byte array containing a CA root certificate.
	 * @return True, if the given certificate is a valid CA root certificate, false otherwise.
	 */
	public static boolean verifyCACertificateSignature(AbstractCertificate caCertificate)
	{
		if(caCertificate==null) return false;
		return verifySignature(caCertificate.getPublicKey(),caCertificate.getSignedPart(),caCertificate.getSignature());
	}

	/**
	 * Verifies the signature of the given public key on a message.
	 * @param signer The public key of the signer (who established the signature).
	 * @param message A byte array of the message which was signed.
	 * @param signature The signature of the message.
	 * @return True, if the signature of the message is valid, false otherwise.
	 */
	private static boolean verifySignature(IPublicKey signer, byte[] message, byte[] signature)
	{
		if(signer==null) return false;
		return signer.verifySignature(message, signature);
	}

	/**
	 * Returns a hash code for the certificate. The hash code is computed from the
	 * actual byte sequence of the certificate.
	 * 
	 * @return The hash code of the certificate.
	 */
	public final int hashCode() {
		return hashCode;
	}
	
	/**
	 * Returns the time from which the certificate is valid. The certificate is invalid before this time.
	 * 
	 * @return A Date object containing the start date of this certificate
	 * @see #getEndDate()
	 */
	public final Date getStartDate()
	{
		return startDate;
	}

	/**
	 * Returns the time after that the certificate is invalid.
	 * 
	 * @return A Date object containing the end date of this certificate
	 * @see #getStartDate()
	 */
	public final Date getEndDate()
	{
		return endDate;
	}
	
	/**
	 * Validates the start and end date of a certificate.
	 * If the current time is between both of these timestamps, the certificate is valid and true is returned.
	 * 
	 * @return True, if the certificate is valid regarding start and end date, false otherwise.
	 */
	final boolean isValidRegardingStartAndEndDate()
	{
		long currentTime = System.currentTimeMillis();
		if(startDate.getTime() <= currentTime && endDate.getTime() >= currentTime)
		{
			return true;
		}
		return false;
	}
}
