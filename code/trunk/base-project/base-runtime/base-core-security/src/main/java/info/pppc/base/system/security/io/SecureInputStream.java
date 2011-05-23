package info.pppc.base.system.security.io;

import info.pppc.base.system.security.sym.AESSymmetricKey;
import info.pppc.base.system.security.sym.HMACSymmetricKey;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.OFBBlockCipher;

/**
 * An AESInputStream that reads an encrypted and signed stream, if the right keys are supplied.
 * The stream is encrypted using an AES key and contains a HMAC (created using the SHA-1 Hash-Algorithm).
 * The stream structure can be read using a header byte, that marks the next start of a signature.
 * @author WA
 */
public class SecureInputStream extends FilterInputStream {

	
	private HMac signer=new HMac(new SHA1Digest());
	private BlockCipher ofbCipher;
	private final static short MAXIMUM_DATA_BUFFER_SIZE=256;
	private final short cipherBlockSize;
	private byte[] buffer;
	private byte[] compareSig;
	private byte[] firstBlock;
	private final static short headerByte=1;
	private short usedBuffer=0;
	private final int signatureSize;
	
	/**
	 * Creates a new AESInputStream with the given keys.
	 * Please note that these keys should differ, otherwise it is possible to tamper the message.
	 * @param in The InputStream that should be decrypted
	 * @param key The key that was used for the encryption (symmetric)
	 * @param signKey The key that was used to create the signature (symmetric)
	 */
	public SecureInputStream(InputStream in, AESSymmetricKey key, HMACSymmetricKey signKey)
	{
		super(in);
		AESFastEngine aesEngine=new AESFastEngine();
		cipherBlockSize=(short)aesEngine.getBlockSize();
		ofbCipher=new OFBBlockCipher(aesEngine,cipherBlockSize*8);
		ofbCipher.init(false, key.getKeyParameters());
		signer.init(signKey.getKeyParameters());
		signatureSize=signer.getMacSize();
		
		int additionalBytes=signatureSize+headerByte;
		additionalBytes=cipherBlockSize-(additionalBytes%cipherBlockSize);
		int tempBuffer=(short)(MAXIMUM_DATA_BUFFER_SIZE+additionalBytes);
		while(tempBuffer>MAXIMUM_DATA_BUFFER_SIZE)
		{
			tempBuffer-=cipherBlockSize;
		}
		buffer=new byte[tempBuffer+headerByte+signatureSize];
		compareSig=new byte[signatureSize];
		firstBlock=new byte[cipherBlockSize];
	}
	
	
	/**
	 * Reads a single byte.
	 * 
	 * @return The single byte or EOF if this is read.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int read() throws IOException {
		byte[] b = new byte[1];
		int result = 0;
		while ((result = read(b, 0, 1)) != 1) {
			if (result == -1) return result;
		} 
		return b[0] & 0xFF;
	}
	
	/**
	 * Reads a number of bytes and fills an array.
	 * 
	 * @param b The byte array to fill.
	 * @return The number of bytes read.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	
	/**
	 * Reads an encrypted stream.
	 * 
	 * @param b buffer to write to
	 * @param off offset in buffer
	 * @param len length of buffer
	 * @return The read bytes (can be lower than <b>len</b>)
	 * @throws IOException Thrown by the underlying stream, if the signature is invalid or if the cipher text cannot be decrypted successfully
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (usedBuffer == 0) {
			// read a full block
			int bytesToRead=cipherBlockSize;
			int read = 0;
			int value = 0;
			while (read != bytesToRead)
			{
				value = in.read(buffer, read, bytesToRead - read);
				if (value == -1) return -1;
				else read = read + value;
			}
			//Now read==bytesToRead==cipherBlockSize
			int offset=0;
			while(offset!=bytesToRead)
			{
				try
				{
					offset+=ofbCipher.processBlock(buffer,offset,firstBlock,offset);
				}
				catch (SecurityException e)
				{
					throw new IOException("Invalid cipher text: " + e.getMessage());
				}
			}
			//Now offset==cipherBlockSize
			byte startByte=firstBlock[0];
			short numberOfBytes = convertByte(startByte);

			//Increase the size of bytesToRead
			bytesToRead=headerByte+numberOfBytes+signatureSize+(cipherBlockSize-((headerByte+signatureSize+numberOfBytes)%cipherBlockSize));
			if((headerByte+signatureSize+numberOfBytes)%cipherBlockSize==0)
			{
				bytesToRead-=cipherBlockSize;
			}
			while (read != bytesToRead)
			{
				value = in.read(buffer, read-headerByte, bytesToRead - read);
				if (value == -1) return -1;
				else read = read + value;
			}
			// decrypt the full buffer
			System.arraycopy(firstBlock, headerByte, buffer, 0, cipherBlockSize-headerByte);
			offset-=headerByte;
			bytesToRead-=headerByte;
			while(offset!=bytesToRead)
			{
				try
				{
					offset+=ofbCipher.processBlock(buffer,offset,buffer,offset);
				}
				catch (SecurityException e)
				{
					throw new IOException("Invalid cipher text: " + e.getMessage());
				}
			}
			signer.update(startByte);
			signer.update(buffer, 0, numberOfBytes);
			
			signer.doFinal(compareSig, 0);
			for(int i=0;i<signatureSize;i++) //Padding bytes are ignored in the comparison
			{
				if(buffer[i+numberOfBytes]!=compareSig[i])
				{
					//Signature is not valid
					throw new IOException("Invalid signature!");
					//Logging.log(this.getClass(), "Signature is invalid!");
					//return -1;
				}
			}
			usedBuffer=numberOfBytes;
		}
		if (usedBuffer <= len) {
			System.arraycopy(buffer, 0, b, off, usedBuffer);
			int result = usedBuffer;
			usedBuffer = 0;
			return result;
		} else {
			System.arraycopy(buffer, 0, b, off, len);
			System.arraycopy(buffer, len, buffer, 0, usedBuffer - len);
			usedBuffer -= len;
			return len;
		}
	}
	
	/**
	 * Converts the header byte to a real length (>0).
	 * The header byte marks the start of the next signature.
	 * @param length The header byte
	 * @return The start of the next signature (in bytes), from 1 to 256
	 */
	private short convertByte(byte length)
	{
		short result=length;
		result+=129;
		return result;
	}
}
