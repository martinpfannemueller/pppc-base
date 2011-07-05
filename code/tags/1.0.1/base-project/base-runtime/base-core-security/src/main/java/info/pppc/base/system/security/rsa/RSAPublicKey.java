package info.pppc.base.system.security.rsa;

import info.pppc.base.system.security.IPublicKey;
import info.pppc.base.system.security.StaticSecurity;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
 * Here Input and OutputStreams could be used, but a new Thread would be necessary, because the use of the PipeStream (is not available for j2me).
 * Implements a RSA public key, which can be used for encryption of messages and verification of signatures.
 * If the message exceeds the key size (usually 1024 bits), than hybrid encryption is used. First a symmetric AES key is encrypted with this RSA public key and then the message is encrypted using AES-CBC.
 * @author WA
 *
 */
public class RSAPublicKey implements IPublicKey
{

	private final int AES_KEYSIZE;
	public static final int PRE_HEADER_LENGTH=3;
	private final static int STANDARD_AES_KEYANDBLOCKSIZE=128; //Block size stays the same, even if the key size changes!
	private final RSAKeyParameters publicKey;
	
	/**
	 * Creates a new RSAPublicKey with the given rsa key parameters and uses the given key size for the AES key (hybrid encryption).
	 * The key size must be 128, 192 or 256 bits.
	 * @param keyParameters An RSA public key
	 * @param aesKeySize The AES key size, can be 128, 192 or 256 bits
	 */
	public RSAPublicKey(RSAKeyParameters keyParameters,int aesKeySize)
	{
		if(keyParameters.isPrivate()) throw new IllegalArgumentException("This is a private key!");
		publicKey=keyParameters;
		if(aesKeySize==128 || aesKeySize==192 || aesKeySize==256)
		{
			AES_KEYSIZE=aesKeySize;
		}
		else
		{
			throw new IllegalArgumentException("AES key size must be one of: 128, 192 or 256");
		}
	}
	
	/**
	 * Creates a new RSAPublicKey with the given rsa key parameters and uses an AES key of 128 bit size (for hybrid encryption).
	 * @param keyParameters An RSA public key
	 */
	public RSAPublicKey(RSAKeyParameters keyParameters)
	{
		this(keyParameters, STANDARD_AES_KEYANDBLOCKSIZE);
	}
	
	/**
	 * Encrypts a message using this RSA public key.<br>
	 * Uses Hybrid encryption, if the input length exceeds the block size, then AES is used!
	 * Returns a header: The first 3 Byte are RSA or AES. For RSA now the encrypted parts start,
	 * for AES, the key is added, so the AES-header is:<br>
	 * AES&lt;KeySize&gt;&lt;Key&gt;&lt;InitializationVector&gt;&lt;EncryptedPart&gt;<br>
	 * For example: AES128THEAESKEY...THEINITIALIZATIONVECTOR...THEENCRYPTEDPART...
	 * @param input The message which should be encrypted
	 * @return An encrypted message containing the header which is described above
	 * @exception IOException If not enough memory is available or if a key could not be read, this exception occurs
	 */
	public byte[] encryptMessage(byte[] input) throws IOException
	{
		RSAEngine encryption=new RSAEngine();
		encryption.init(true, publicKey);
		ByteArrayOutputStream result=new ByteArrayOutputStream(input.length+PRE_HEADER_LENGTH); //Initialized with minimal length
		if(encryption.getInputBlockSize()<input.length)
		{
			//DO RSA Encryption for the Header
			//Encrypt everything else with AESFast-CBC
			result.write("AES".getBytes());
			result.write(aesEncryption(encryption,input));
		}
		else
		{
			result.write("RSA".getBytes());
			result.write(rsaEncryption(encryption,input));
		}
		return(result.toByteArray());
		
	}
	
	/**
	 * Does a RSA encryption of the given byte array. Make sure the array length is smaller than the block size (normally corresponds to the key size, ie. usually 1024 bits).
	 * A RSA encrypted byte array is created, with the maximal size of one RSA block (ie. key size, 1024 bits) 
	 * @param encryptionEngine An initialized encryption engine, that can be used to encrypt the input
	 * @param input A byte array containing a message which should be encrypted with RSA. Maximal size is the block size of RSA, {@link RSAEngine#getInputBlockSize()}.
	 * @return A byte array containing one RSA block which includes the encrypted message.
	 */
	private byte[] rsaEncryption(RSAEngine encryptionEngine,byte[] input)
	{
		return(encryptionEngine.processBlock(input, 0, input.length));
	}
	
	/**
	 * Does a hybrid encryption, first uses the {@link #rsaEncryption(RSAEngine, byte[])} method to encrypt an AES key and IV,
	 * than encrypts the message with the derived AES key (AES-CBC with padding) and returns both the RSA and the AES encrypted part as one byte array.<br>
	 * The structure is as follows:<br>
	 * AES&lt;AES key bit size&gt;&lt;AES key&gt;&lt;IV&gt;&lt;AES encrypted input&gt;<br>
	 * The first part of this structure (from the key bit size to the IV (including the IV) is encrypted with RSA, the last part is only encrypted with AES,
	 * using the AES key, which was encrypted with RSA.
	 * @param keyEncryptionEngine The initialized RSA key encryption engine
	 * @param input The message which should be encrypted (will use symmetric AES encryption)
	 * @return A byte array which consists of two parts, the first is encrypted with RSA, the second one with AES, for the structure see the general method description.
	 * @throws IOException If the memory size is exceeded, or if the bytes could not be written in memory, this exception occurs
	 */
	private byte[] aesEncryption(RSAEngine keyEncryptionEngine,byte[] input) throws IOException
	{
		ByteArrayOutputStream result=new ByteArrayOutputStream(input.length+(2*STANDARD_AES_KEYANDBLOCKSIZE+AES_KEYSIZE)/8);
		CBCBlockCipher aesCBC=new CBCBlockCipher(new AESFastEngine());
		byte[] IV=new byte[STANDARD_AES_KEYANDBLOCKSIZE/8];
		byte[] aesKey=new byte[AES_KEYSIZE/8];
		StaticSecurity.getSecureRandom().nextBytes(IV);
		StaticSecurity.getSecureRandom().nextBytes(aesKey);
		KeyParameter aesKeyParam=new KeyParameter(aesKey);
		ParametersWithIV aesKeyAndIV=new ParametersWithIV(aesKeyParam,IV);
		PaddedBufferedBlockCipher aesCipher=new PaddedBufferedBlockCipher(aesCBC);
		aesCipher.init(true, aesKeyAndIV);
		if(PRE_HEADER_LENGTH+AES_KEYSIZE/8+STANDARD_AES_KEYANDBLOCKSIZE/8>keyEncryptionEngine.getInputBlockSize())
		{
			throw new IOException("AES header (including key and initialization vector (iv)) is bigger than RSA block size!\nAES header: "+(PRE_HEADER_LENGTH+AES_KEYSIZE/8+STANDARD_AES_KEYANDBLOCKSIZE/8)+" RSA block size: "+keyEncryptionEngine.getInputBlockSize());
		}
		ByteArrayOutputStream rsaBlock=new ByteArrayOutputStream(PRE_HEADER_LENGTH+AES_KEYSIZE/8+STANDARD_AES_KEYANDBLOCKSIZE/8);
		rsaBlock.write(String.valueOf(AES_KEYSIZE).getBytes());
		rsaBlock.write(aesKey);
		rsaBlock.write(IV);
		result.write(rsaEncryption(keyEncryptionEngine,rsaBlock.toByteArray()));
		/*rsaBlock=null;
		aesKey=null;
		IV=null;*/
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
		result.write(aesEncryptedBlocks);
		//System.arraycopy(aesEncryptedBlocks, 0, resultBytes, 0, outputBytes+finalOutputBytes); //Only done for safety reasons, aesEncryptedBlocks
		return(result.toByteArray());
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.uni.bonn.snpc.wa.certs.IPublicKey#verifySignature(byte[], byte[])
	 */
	public boolean verifySignature(byte[] message, byte[] signature)
	{
		RSADigestSigner signer=new RSADigestSigner(new SHA1Digest());
		signer.init(false, publicKey);
		signer.update(message,0,message.length);
		return(signer.verifySignature(signature));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if(obj==this) return true;
		if(obj==null || !(obj instanceof RSAPublicKey)) return false;
		RSAKeyParameters compare=((RSAPublicKey)obj).publicKey;
		if(compare.isPrivate()!=publicKey.isPrivate()) return false;
		if(!(publicKey.getExponent().equals(compare.getExponent())) || (!(publicKey.getModulus().equals(compare.getModulus())))) return false;
		return true;
	}
}
