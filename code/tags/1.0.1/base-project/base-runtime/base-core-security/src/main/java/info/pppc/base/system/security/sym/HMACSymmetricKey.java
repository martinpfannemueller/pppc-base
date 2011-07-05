package info.pppc.base.system.security.sym;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import info.pppc.base.system.security.ISymmetricKey;

/**
 * This class is used to create a symmetric key that can be used for signing (using a HMAC)
 * @author WA
 */
public class HMACSymmetricKey implements ISymmetricKey {

	private final byte[] key;
	
	/**
	 * Creates a new symmetric signing key out of a byte array
	 * @param key A secret key as a byte array
	 */
	public HMACSymmetricKey(byte[] key)
	{
		this.key=key;
	}
	
	/**
	 * Since this key is not for encryption, a runtime exception will be thrown.
	 * @param input A runtime exception will be thrown.
	 * @return A runtime exception will be thrown.
	 */
	public byte[] encryptMessage(byte[] input) 
	{
		throw new RuntimeException("Unsupported operation.");
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		if(signature==null || message==null) return false;
		byte[] unknownSignature = signMessage(message);
		if(unknownSignature.length!=signature.length) return false;
		for(int i=0;i<unknownSignature.length;i++)
		{
			if(signature[i]!=unknownSignature[i])
			{
				return false;
			}
		}
		//Logging.debug(this.getClass(), "Successfully verified a signature!");
		return true;
	}

	/**
	 * Creates a HMAC signature
	 * @param message The message that should be signed as byte array
	 * @return The HMAC signature. 
	 */
	public byte[] createSignature(byte[] message)
	{
		return signMessage(message);
	}

	/**
	 * Since this key is not for encryption, a runtime exception will be thrown.
	 * @param message A runtime exception will be thrown.
	 * @return A runtime exception will be thrown.
	 */
	public byte[] decryptMessage(byte[] message)
	{
		throw new RuntimeException("Unsupported operation.");
	}

	/**
	 *  This key cannot be used for encryption or decryption, so this method will return <b>false</b>.<br><br>
	 *  
	 * 	{@inheritDoc}
	 */
	public boolean forEncryption()
	{
		return false;
	}
	
	/**
	 *  This key can be used for signing, so this method will return <b>true</b>.<br><br>
	 * {@inheritDoc}
	 */
	public boolean forSigning()
	{
		return true;
	}

	/**
	 * Signs a message with the given key using a HMAC (symmetric signature)
	 * @param messageToSign The message that should be signed as byte array
	 * 
	 * @return The HMAC signature using SHA-1, it will therefore result in a byte array with the length 20
	 */
	private byte[] signMessage(byte[] messageToSign)
	{
		HMac hMac=new HMac(new SHA1Digest());
		hMac.init(new KeyParameter(key));
		hMac.update(messageToSign, 0, messageToSign.length);
		byte[] signature=new byte[20];
		int digestLength=hMac.doFinal(signature, 0);
		if(digestLength!=signature.length) throw new DataLengthException("HMac signature-buffer is too big!");
		return signature;
	}
	
	public KeyParameter getKeyParameters()
	{
		return new KeyParameter(key);
	}
}
