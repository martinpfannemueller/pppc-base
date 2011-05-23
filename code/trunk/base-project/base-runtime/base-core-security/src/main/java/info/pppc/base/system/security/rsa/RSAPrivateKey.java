package info.pppc.base.system.security.rsa;

import info.pppc.base.system.security.IPrivateKey;
import info.pppc.base.system.util.Logging;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.RSADigestSigner;

/**
 * Implements a RSA private key, which can be used for decryption of messages and the creation of signatures.<br>
 * Decrypts also the hybrid encryption used by the RSAPublicKey class.
 * @author WA
 */
public class RSAPrivateKey implements IPrivateKey
{

	private final RSAKeyParameters privateKey;
	private final static int STANDARD_AES_KEYANDBLOCKSIZE=128;
	private final static int AES_KEYSIZE_LENGTH=3; //128, 192, 256
	
	/**
	 * Creates a new RSAPrivateKey, which can be used for decryption of messages and creation of signatures with the given private key.
	 * @param privateKeyParam The private key which should be used for decryption of messages and the creation of signatures
	 */
	public RSAPrivateKey(RSAKeyParameters privateKeyParam)
	{
		privateKey=privateKeyParam;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPrivateKey#createSignature(byte[])
	 */
	public byte[] createSignature(byte[] message)
	{
		RSADigestSigner createSig=new RSADigestSigner(new SHA1Digest());
		createSig.init(true, privateKey);
		createSig.update(message,0,message.length);
		try
		{
			return(createSig.generateSignature());
		}
		catch (DataLengthException e)
		{
			Logging.error(this.getClass(),"Creating a signature was not possible, DataLengthException occured!",e);
		}
		catch (CryptoException e)
		{
			Logging.error(this.getClass(),"Creating a signature was not possible, CryptoException occured!",e);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPrivateKey#decryptMessage(byte[])
	 */
	public byte[] decryptMessage(byte[] message)
	{
		String recognizeMessageType=new String(message,0,RSAPublicKey.PRE_HEADER_LENGTH);
		Logging.log(this.getClass(), "Decrypting message with type: "+recognizeMessageType);
		RSAEngine rsaEngine=new RSAEngine();
		rsaEngine.init(false, privateKey);
		if(recognizeMessageType.equals("RSA"))
		{
			//Do RSA decryption
			if(rsaEngine.getInputBlockSize()<(message.length-RSAPublicKey.PRE_HEADER_LENGTH)) //because of the string "RSA"
			{
				Logging.log(this.getClass(), "Wrong header string of encrypted message, was RSA, but message is too long!\nCould not decrypt message");
				return null;
			}
			return (decryptRSA(rsaEngine, message,RSAPublicKey.PRE_HEADER_LENGTH,message.length-RSAPublicKey.PRE_HEADER_LENGTH));
		}
		else if(recognizeMessageType.equals("AES"))
		{
			//Do AES decryption
			if(rsaEngine.getInputBlockSize()>(message.length-RSAPublicKey.PRE_HEADER_LENGTH))
			{
				Logging.log(this.getClass(), "Wrong header string of encrypted message, was AES, but message is too short!\nCould not decrypt message");
				return null;
			}
			if((rsaEngine.getInputBlockSize())<128)
			{
				Logging.log(this.getClass(), "Using unsecure secret key, less than 1024 bits!\nMethod will abort decryption!");
				return null;
			}
			byte[] header=decryptRSA(rsaEngine,message,RSAPublicKey.PRE_HEADER_LENGTH,rsaEngine.getInputBlockSize());
			byte[] aesKey;
			byte[] iv;
			try
			{
				String aesKeySize=new String(header,0,AES_KEYSIZE_LENGTH);
				int keySize=Integer.parseInt(aesKeySize);
				aesKey=subbyte(header,AES_KEYSIZE_LENGTH,(keySize/8));
				iv=subbyte(header,(AES_KEYSIZE_LENGTH+(keySize/8)),(STANDARD_AES_KEYANDBLOCKSIZE/8));
				//Cut message, remove RSA-Header:
				int rsaHeaderLength=AES_KEYSIZE_LENGTH+rsaEngine.getInputBlockSize();
				message=subbyte(message,rsaHeaderLength,(message.length-rsaHeaderLength));
			}
			catch(NumberFormatException e)
			{
				Logging.error(this.getClass(),"Could find out the key size of the AES key!",e);
				return null;
			}
			return(decryptAES(message,aesKey,iv));
		}
		else
		{
			Logging.log(this.getClass(), "Wrong header string of encrypted message, was: "+recognizeMessageType);
			return null;
		}
	}
	
	/**
	 * Decrypts the AES part of the hybrid encryption.
	 * This is the actual message, only the key is RSA encrypted.
	 * @param message The AES encrypted part of the message (payload)
	 * @param aesKey The AES key (which was RSA encrypted)
	 * @param iv The initialization vector (IV) which was not RSA encrypted.
	 * @return The decrypted bytes of the message.
	 */
	private byte[] decryptAES(byte[] message, byte[] aesKey, byte[] iv)
	{
		KeyParameter aesKeyParam=new KeyParameter(aesKey);
		ParametersWithIV aesKeyAndIV=new ParametersWithIV(aesKeyParam,iv);
		PaddedBufferedBlockCipher aesCipher=new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
		aesCipher.init(false, aesKeyAndIV);
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
	 * Decrypts the RSA part of the message using this private key.
	 * @param decryptEngine The used RSAEngine, which was initialized before
	 * @param input The RSA encrypted part of the message (can be cut appropriately to the RSA encrypted part using the parameters <b>offset</b> and <b>length</b>
	 * @param offset The offset of the byte array
	 * @param length The length of the RSA encrypted part of the message
	 * @return An byte array containing the decrypted RSA message
	 */
	private byte[] decryptRSA(RSAEngine decryptEngine, byte[] input, int offset, int length)
	{
		return (decryptEngine.processBlock(input, offset, length));
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
		/*int j=0;
		for(int i=offset;i<offset+length;i++)
		{
			resultByte[j]=input[i];
			j++;
		}*/
		return resultByte;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || !(obj instanceof RSAPrivateKey)) return false;
		RSAKeyParameters compare=((RSAPrivateKey)obj).privateKey;
		if(compare.isPrivate()!=privateKey.isPrivate()) return false;
		if(!(privateKey.getExponent().equals(compare.getExponent())) || (!(privateKey.getModulus().equals(compare.getModulus())))) return false;
		return true;
	}
}
