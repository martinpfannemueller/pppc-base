package info.pppc.base.system.security.ecc;

import info.pppc.base.system.security.IPrivateKey;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.util.Logging;

import java.io.IOException;

import com.sun.spot.security.InvalidKeyException;
import com.sun.spot.security.NoSuchAlgorithmException;
import com.sun.spot.security.Signature;
import com.sun.spot.security.SignatureException;
import com.sun.spot.security.implementation.ECPrivateKeyImpl;
import com.sun.spotx.crypto.implementation.ECDHKeyAgreement;

/**
 * This class implements an elliptic curve cryptography private key, which can be used for creating signatures.
 * It uses the fast ECC implementation from SUN (created for the SUNSpots) that can only be used with the curve SECP160R1.
 * @author WA
 *
 */
public class FastECCPrivateKey implements IPrivateKey {

	private final ECPrivateKeyImpl privateKey;
	
	
	/**
	 * Creates a new fast ECC private key and initializes it for signing.
	 * @param privateKey The private key parameters which are used for signing. This FastECCKeyParser-object is created using the SUN Sunspot SSL library.
	 * @throws IOException If the key could not be initialized for signing, an IOException is thrown.
	 */
	public FastECCPrivateKey(FastECCKeyParser ECCKey) throws IOException
	{
		privateKey=ECCKey.getPrivateKey();
	}
	
	/*
	 * (non-Javadoc)
	 * @see info.pppc.base.system.security.IPrivateKey#createSignature(byte[])
	 */
	public byte[] createSignature(byte[] message)
	{
		synchronized (KeyStore.class) {
			try
			{
				Signature signature = Signature.getInstance(FastECCPublicKey.SIGNATURE_ALGORITHM);;
				signature.initSign(privateKey);
				signature.update(message, 0, message.length);
				byte[] result=new byte[signature.getLength()];
				int countedBytes=signature.sign(result);
				if(result.length!=countedBytes)
				{
					byte[] secondResult=new byte[countedBytes];
					System.arraycopy(result, 0, secondResult, 0, countedBytes);
					result=secondResult;
				}
				return result;
			}
			catch (SignatureException e)
			{
				Logging.error(this.getClass(),"Signature could not be created!",e);
				return null;
			}
			catch (NoSuchAlgorithmException e)
			{
				Logging.error(this.getClass(),"No such algorithm "+FastECCPublicKey.SIGNATURE_ALGORITHM+"! This message should never occur since the used library supports this algorithm!",e);
				return null;
			}
			catch (InvalidKeyException e)
			{
				Logging.error(this.getClass(), "The private ECC key is invalid!", e);
				return null;
			}			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.base.system.security.IPrivateKey#decryptMessage(byte[])
	 */
	public byte[] decryptMessage(byte[] message) {
		Logging.debug(this.getClass(),"Decryption not supported, returning null!");
		return null;
	}
	
	/**
	 * Creates the local secret that is used in the first step of the fast ECDH. This secret is contained in the returned ECDHKeyAgreement.
	 * @return An ECDHBasicAgreement that contains the local secret as private value.
	 * @throws IOException If this key is invalid, an exception is thrown.
	 */
	public ECDHKeyAgreement createLocalSecret() throws IOException
	{
		ECDHKeyAgreement agreement=new ECDHKeyAgreement();
		try
		{
			agreement.init(privateKey);
		}
		catch (InvalidKeyException e)
		{
			throw new IOException(e.getMessage());
		}
		return agreement;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || !(obj instanceof FastECCPrivateKey)) return false;
		ECPrivateKeyImpl privKey=(((FastECCPrivateKey)obj).privateKey);
		if(this.privateKey.getCurve()!=privKey.getCurve()) return false;
		int[] otherKeyData=privKey.getKeyData();
		int[] myKeyData=this.privateKey.getKeyData();
		if(myKeyData.length!=otherKeyData.length) return false;
		for(int i=0;i<myKeyData.length;i++)
		{
			if(myKeyData[i]!=otherKeyData[i]) return false;
		}
		return true;
	}

}
