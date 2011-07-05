package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

/**
 * This class implements an elliptic curve cryptography public key, which can be used for verifying signatures.
 * @author WA
 */
public class ECCPublicKey implements IPublicKey {

	private final ECPublicKeyParameters publicKey;

	/**
	 * Creates a new ECC public key.
	 * @param publicKey The public key parameters which are used for verifying signatures. This ECPublicKeyParameters-object is created using the Bouncycastle library.
	 */
	public ECCPublicKey(ECPublicKeyParameters publicKey) {
		this.publicKey=publicKey;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPublicKey#encryptMessage(byte[])
	 */
	public byte[] encryptMessage(byte[] input) throws IOException {
		// Encryption is not supported
		// Use DH/ECDH and AES instead
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPublicKey#verifySignature(byte[], byte[])
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		SHA1Digest digest=new SHA1Digest();
		ECDSASigner signer=new ECDSASigner();
		signer.init(false, publicKey);
		try
		{
			//Creating SHA1-Hash of the byte array
			digest.update(message, 0, message.length);
			byte[] sha1Hash=new byte[digest.getDigestSize()];
			digest.doFinal(sha1Hash, 0);
		
			//Retrieving the two big integers r and s
			BigInteger[] rAnds=decodeSignature(signature);

			//Verify the signature of the message
			return(signer.verifySignature(sha1Hash, rAnds[0], rAnds[1]));
		}
		catch(Exception e)
		{
			Logging.error(this.getClass(), "Could not verify valid signature (wrong format?)", e);
			return false;
		}
	}

	/**
	 * Decodes the signature format.<br>
	 * This format is the standard ASN1 ECC-Signature format.
	 * Please note, that it is <b>NOT</b> necessary to call this method before verifying a signature with the {@link #verifySignature(byte[], byte[])} method. This method will decode the signature appropriately.<br>
	 * @param signature The two BigIntegers, called "r" and "s" encoded in the ASN1Sequence used for ECC-Signatures.
	 * @return <b>null</b> if the signature could not be decoded.<br>
	 * Otherwise the two BigIntegers "r" (index 0) and "s" (index 1) are returned as BigInteger array.
	 */
	protected static BigInteger[] decodeSignature(byte[] signature)
	{
		if(signature==null) return null;
		
		ASN1Sequence seq;
		try
		{
			ASN1InputStream is = new ASN1InputStream(signature);
			seq = (ASN1Sequence)(is.readObject());
		}
		catch (IOException e)
		{
			Logging.error(ECCPublicKey.class, "Could not decode ECC signature!", e);
			return null;
		}
		
	    if (seq.size() != 2)
	    {
	        throw new IllegalArgumentException("Bad sequence size: "+seq.size());
	    }
	    Enumeration e = seq.getObjects();
	    BigInteger[] result=new BigInteger[2];
	    result[0] = DERInteger.getInstance(e.nextElement()).getPositiveValue();
	    result[1] = DERInteger.getInstance(e.nextElement()).getPositiveValue();
	    return result;
	}
	
	/**
	 * Get the shared secret from a ECDH agreement. This is the second step in the ECDH.
	 * @param agreement The agreement that is already initialized with the private key of the foreign device
	 * @return A common big integer that can be used to derive keys
	 */
	public BigInteger getSharedSecret(ECDHBasicAgreement agreement)
	{
		return agreement.calculateAgreement(publicKey);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || (!(obj instanceof ECCPublicKey))) return false;
		ECPublicKeyParameters compare=((ECCPublicKey)obj).publicKey;
		if(!publicKey.getQ().equals(compare.getQ())) return false;
		ECDomainParameters compareParameters=compare.getParameters();
		ECDomainParameters myParams=publicKey.getParameters();
		if((!myParams.getG().equals(compareParameters.getG())) || !myParams.getH().equals(compareParameters.getH()) || !myParams.getN().equals(compareParameters.getN())) return false;
		return true;
	}
}
