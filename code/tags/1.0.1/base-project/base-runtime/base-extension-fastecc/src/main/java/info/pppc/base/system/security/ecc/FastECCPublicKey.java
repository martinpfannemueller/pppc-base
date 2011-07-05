package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.util.Logging;

import java.io.IOException;

import com.sun.spot.security.InvalidKeyException;
import com.sun.spot.security.NoSuchAlgorithmException;
import com.sun.spot.security.PublicKey;
import com.sun.spot.security.Signature;
import com.sun.spot.security.SignatureException;
import com.sun.spot.security.implementation.ECPublicKeyImpl;
import com.sun.spot.security.implementation.ecc.ECPoint;
import com.sun.spotx.crypto.implementation.ECDHKeyAgreement;

/**
 * This class implements a fast elliptic curve cryptography public key, which can be used for verifying signatures.
 * @author WA
 */
public class FastECCPublicKey implements IPublicKey {
	
	private final ECPublicKeyImpl sunspotPublicKey;
	public static final String SIGNATURE_ALGORITHM="SHA1WITHECDSA";

	/**
	 * Creates a new fast ECC public key and initializes it for the creation of signatures.
	 * @param publicKey The public key parameters which are used for verifying signatures. This PublicKey-object is created using the SUNSpot SSL library.
	 * @throws IOException If it could not be successfully initialized for the creation of signatures.
	 */
	public FastECCPublicKey(PublicKey publicKey) throws IOException
	{
		if(!(publicKey instanceof ECPublicKeyImpl))
		{
			throw new IOException("This is no ECC public key!");
		}
		sunspotPublicKey=(ECPublicKeyImpl) publicKey;
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.base.system.security.IPublicKey#encryptMessage(byte[])
	 */
	public byte[] encryptMessage(byte[] input) throws IOException {
		// Encryption is not supported
		// Use DH/ECDH and AES instead
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.base.system.security.IPublicKey#verifySignature(byte[], byte[])
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		synchronized (KeyStore.class) {
			try
			{
				Signature v = Signature.getInstance(FastECCPublicKey.SIGNATURE_ALGORITHM);
				v.initVerify(sunspotPublicKey);
				v.update(message, 0, message.length);
				return v.verify(signature);
			}
			catch (SignatureException e)
			{
				Logging.error(this.getClass(),"Signature could not validated!",e);
				return false;
			} catch (NoSuchAlgorithmException e) {
				Logging.error(this.getClass(), "The ECC algorithm is not available!", e);
				return false;
			} catch (InvalidKeyException e) {
				Logging.error(this.getClass(), "The public ECC key is invalid!", e);
				return false;
			}			
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || !(obj instanceof FastECCPublicKey)) return false;
		ECPublicKeyImpl publicKey=(((FastECCPublicKey)obj).sunspotPublicKey);
		if(publicKey.getCurve()!=this.sunspotPublicKey.getCurve()) return false;
		ECPoint myECPoint=this.sunspotPublicKey.getECPoint();
		ECPoint otherECPoint=publicKey.getECPoint();
		if(myECPoint.x.length!=otherECPoint.x.length || myECPoint.y.length!=otherECPoint.y.length || myECPoint.z.length!=otherECPoint.z.length)
		{
			return false;
		}
		for(int i=0;i<myECPoint.x.length;i++)
		{
			if(myECPoint.x[i]!=otherECPoint.x[i]) return false;
		}
		for(int i=0;i<myECPoint.y.length;i++)
		{
			if(myECPoint.y[i]!=otherECPoint.y[i]) return false;
		}
		for(int i=0;i<myECPoint.z.length;i++)
		{
			if(myECPoint.z[i]!=otherECPoint.z[i]) return false;
		}
		return(true);
	}

	/**
	 * Get the shared secret from a fast ECDH agreement. This is the second step in the fast ECDH.
	 * @param agreement The agreement that is already initialized with the private key of the foreign device
	 * @return A common big integer that can be used to derive keys
	 * @throws IOException Thrown if the key could not create the common secret
	 */
	public byte[] getSharedSecret(ECDHKeyAgreement agreement) throws IOException {
		try
		{
			byte[] buffer=new byte[100]; //This is also done by SUN in their SSL implementation
			byte[] secret=new byte[100]; //This is also done by SUN in their SSL implementation
			int length=sunspotPublicKey.getW(buffer, 0);
			length=agreement.generateSecret(buffer, 0, length, secret, 0);
			return secret;
		}
		catch(Exception e)
		{
			throw new IOException(e.getMessage());
		}
	}

}
