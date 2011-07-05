package info.pppc.base.system.security.sym;

import info.pppc.base.system.security.ISymmetricKey;
import info.pppc.base.system.security.StaticSecurity;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * This class implements a symmetric AES encryption/decryption.
 * If you also need to create signatures, have a look at the {@link HMACSymmetricKey}.
 * @author WA
 */
public class AESSymmetricKey implements ISymmetricKey {

	/**
	 * Standard key size is 128 bits. The block size is ALWAYS 128 bits, even by using keys with 192 or 256 bits.
	 */
	public static final int STANDARD_AES_KEY_AND_BLOCK_SIZE = 128;

	/**
	 * Possible key sizes are 128, 192 or 256 bits
	 */
	private static final int[] keySizes=new int[]{128,192,256};
	
	private final byte[] aesKey;

	
	/**
	 * Creates a new AES key that can be used for encryption or decryption
	 * @param key A secret key as byte array, only keys of the size described in {@link #keySizes} are allowed
	 * @throws IllegalArgumentException If the key does not conform to the specified key sizes, this exception is thrown
	 */
	public AESSymmetricKey(byte[] key) throws IllegalArgumentException
	{
		checkAESKey(key);
		this.aesKey=key;
	}
	
	/**
	 * Checks if the key supplied could be used as a valid AES key (ie. it conforms to the key sizes)
	 * @param key The key that should be checked as byte array
	 * @throws IllegalArgumentException If the key is not a valid AES key 
	 */
	public static void checkAESKey(byte[] key) throws IllegalArgumentException
	{
		boolean match=false;
		for(int i=0;i<keySizes.length;i++)
		{
			if((key.length*8)==keySizes[i])
			{
				match=true;
				break;
			}
		}
		if(!match)
		{
			StringBuffer errorMessage=new StringBuffer();
			errorMessage.append("Wrong key length, should be one of:");
			for(int i=0;i<keySizes.length;i++)
			{
				errorMessage.append(" ");
				errorMessage.append(keySizes[i]);
				if(i<(keySizes.length-1)) errorMessage.append(",");
			}
			errorMessage.append(" bits.");
			throw new IllegalArgumentException(errorMessage.toString());
		}
	}


	/**
	 * This key can be used for encryption and decryption, so this method will return <b>true</b>.<br><br>
	 * {@inheritDoc}
	 */
	public boolean forEncryption()
	{
		return true;
	}

	/**
	 * This key cannot be used for signing, so this method will return <b>false</b>.<br><br>
	 * {@inheritDoc}
	 */
	public boolean forSigning()
	{
		return false;
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	public byte[] encryptMessage(byte[] input) throws IOException
	{
		byte[] IV=new byte[STANDARD_AES_KEY_AND_BLOCK_SIZE/8];
		StaticSecurity.getSecureRandom().nextBytes(IV);
		
		PaddedBufferedBlockCipher aesCipher=new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
		aesCipher.init(true, new ParametersWithIV(new KeyParameter(aesKey),IV));

		ByteArrayOutputStream aesBlock=new ByteArrayOutputStream(input.length+STANDARD_AES_KEY_AND_BLOCK_SIZE/8);
		aesBlock.write(IV);
		byte[] aesEncryptedBlocks=new byte[aesCipher.getOutputSize(input.length)];
		int outputBytes=aesCipher.processBytes(input, 0, input.length, aesEncryptedBlocks, 0);
		int finalOutputBytes;
		try
		{
			finalOutputBytes = aesCipher.doFinal(aesEncryptedBlocks, outputBytes);
		}
		catch (DataLengthException e)
		{
			Logging.error(this.getClass(), "Not enough space left in the buffer!", e);
			throw new IOException("Not enough space left in the buffer! "+e.getMessage());
		}
		catch (IllegalStateException e)
		{
			Logging.error(this.getClass(), "The cipher is not properly initialized!", e);
			throw new IOException("The cipher is not properly initialized! "+e.getMessage());
		}
		catch (InvalidCipherTextException e)
		{
			Logging.error(this.getClass(), "This error can only occur if we try to decrypt!\nSo this error should not occur now!", e);
			throw new IOException();
		}
		if((outputBytes+finalOutputBytes)<aesEncryptedBlocks.length)
		{
			Logging.log(this.getClass(),"Output Bytes do not coincide with the length of the encrypted aesBlock!");
			byte[] resultBytes=new byte[outputBytes+finalOutputBytes];
			System.arraycopy(aesEncryptedBlocks, 0, resultBytes, 0, outputBytes+finalOutputBytes);
			aesEncryptedBlocks=resultBytes;
		}
		else
		{
			if((outputBytes+finalOutputBytes)>aesEncryptedBlocks.length) throw new IOException("AES Encryption: Number of output bytes is higher than byte array length!");
		}
		aesBlock.write(aesEncryptedBlocks);
		//System.arraycopy(aesEncryptedBlocks, 0, resultBytes, 0, outputBytes+finalOutputBytes); //Only done for safety reasons, aesEncryptedBlocks
		return(aesBlock.toByteArray());
	}

	/**
	 * Since this key is not for signing, a runtime exception will be thrown.
	 * 
	 * @param message A runtime exception will be thrown.
	 * @param signature A runtime exception will be thrown.
	 * @return A runtime exception will be thrown.
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		throw new RuntimeException("Unsupported operation.");
	}

	/**
	 * Since this key is not for signing, a runtime exception will be thrown.
	 * 
	 * @param message A runtime exception will be thrown.
	 * @return A runtime exception will be thrown.
	 */
	public byte[] createSignature(byte[] message)
	{
		throw new RuntimeException("Unsupported operation.");
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	public byte[] decryptMessage(byte[] message) throws IndexOutOfBoundsException
	{
		if(message.length<STANDARD_AES_KEY_AND_BLOCK_SIZE/8)
		{
			throw new IndexOutOfBoundsException("Message is too short.");
		}
		byte[] IV=new byte[STANDARD_AES_KEY_AND_BLOCK_SIZE/8];
		for(int i=0;i<STANDARD_AES_KEY_AND_BLOCK_SIZE/8;i++)
		{
			IV[i]=message[i];
		}
		message=subbyte(message,STANDARD_AES_KEY_AND_BLOCK_SIZE/8, message.length-(STANDARD_AES_KEY_AND_BLOCK_SIZE/8));
		PaddedBufferedBlockCipher aesCipher=new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
		aesCipher.init(false, new ParametersWithIV(new KeyParameter(aesKey),IV));
		byte[] decryptedMessage=new byte[aesCipher.getOutputSize(message.length)];
		int copiedBytes=aesCipher.processBytes(message, 0, message.length, decryptedMessage, 0);
		try
		{
			int finalOutputBytes = aesCipher.doFinal(decryptedMessage, copiedBytes);
			if(finalOutputBytes+copiedBytes<decryptedMessage.length)
			{
				decryptedMessage=subbyte(decryptedMessage,0,finalOutputBytes+copiedBytes);
			}
			else if(finalOutputBytes+copiedBytes>decryptedMessage.length)
			{
				throw new InvalidCipherTextException("Number of output bytes higher than the length of the message");
			}
			// == is ok!
		}
		catch (InvalidCipherTextException e)
		{
			Logging.error(this.getClass(), "Ciphertext can not be decrypted, wrong padding?", e);
			return null;
		}
		return(decryptedMessage);
	}
	
	/**
	 * Cuts a byte array according to the given parameters.
	 * @param input The input byte array which should be cut
	 * @param offset The start of the new, cut byte array
	 * @param length The length of the new, cut byte array
	 * @return The new, cut byte array, its length is equal to the parameter <b>length</b>
	 */
	private byte[] subbyte(byte[] input, int offset, int length)
	{
		if(input.length<offset+length)
		{
			Logging.log(this.getClass(), "Could not create subbyte, because offset+length are smaller than the length of the original byte array!");
			return null;
		}
		byte[] resultByte=new byte[length];
		System.arraycopy(input, offset, resultByte, 0, length);
		return resultByte;
	}
	
	public KeyParameter getKeyParameters()
	{
		return new KeyParameter(aesKey);
	}

}
