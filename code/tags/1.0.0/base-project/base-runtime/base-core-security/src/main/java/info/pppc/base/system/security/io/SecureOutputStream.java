package info.pppc.base.system.security.io;

import info.pppc.base.system.security.StaticSecurity;
import info.pppc.base.system.security.sym.AESSymmetricKey;
import info.pppc.base.system.security.sym.HMACSymmetricKey;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.OFBBlockCipher;

/**
 * An AESOutputStream that encrypts and signs a byte stream using the AES algorithm and an HMAC with the SHA1-Hash-Algorithm.
 * Please note that you should use two different keys for signing and for encryption!
 * @author WA
 */
public class SecureOutputStream extends FilterOutputStream{

	private HMac signer=new HMac(new SHA1Digest());
	private BlockCipher ofbCipher;
	private final static short MAXIMUM_DATA_BUFFER_SIZE=256;
	private final short BUFFER_SIZE;
	private final short cipherBlockSize;
	private byte[] buffer;
	private final static short headerByte=1;
	private short usedBuffer=0;
	private final int signatureSize;
	
	/**
	 * Creates a new AESOutputStream that encrypts the given OutputStream with the given key and additionally signs it with the signature key.
	 * The signature key must not be identical to the AES key!<br>
	 * @param out The OutputStream that should be encrypted (and signed)
	 * @param key The AES key that should be used for encryption
	 * @param signKey The symmetric key that is used to create the HMAC (signature)
	 */
	public SecureOutputStream(OutputStream out, AESSymmetricKey key, HMACSymmetricKey signKey)
	{
		super(out);
		AESFastEngine aesEngine=new AESFastEngine();
		cipherBlockSize=(short)aesEngine.getBlockSize();
		ofbCipher=new OFBBlockCipher(aesEngine,cipherBlockSize*8);
		ofbCipher.init(true, key.getKeyParameters());
		signer.init(signKey.getKeyParameters());
		
		signatureSize=signer.getMacSize();
		//Create Buffer:
		int additionalBytes=signatureSize+headerByte;
		additionalBytes=cipherBlockSize-(additionalBytes%cipherBlockSize);
		int tempBuffer=(short)(MAXIMUM_DATA_BUFFER_SIZE+additionalBytes);
		while(tempBuffer>MAXIMUM_DATA_BUFFER_SIZE)
		{
			tempBuffer-=cipherBlockSize;
		}
		BUFFER_SIZE=(short)tempBuffer;
		buffer=new byte[BUFFER_SIZE+headerByte+signatureSize];
		
		buffer[0]=convertLength(BUFFER_SIZE); //Write the header
	}
	
	/**
	 * Write a byte array completely.
	 * 
	 * @param b The byte array to write.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(byte[] b) throws IOException {	
		this.write(b,0,b.length);
	}


	/**
	 * Writes a single byte.
	 * 
	 * @param b The byte to write.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(int b) throws IOException {
		this.write(new byte[]{(byte)b}, 0, 1);
	}

	/**
	 * Writes b bytes to the stream.
	 * 
	 * @param b The buffer of data to write
	 * @param off The offset in buffer
	 * @param len The length of the data in the buffer
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		while (len > 0)
		{
			for(int i=usedBuffer+headerByte;((i<(BUFFER_SIZE+headerByte)) && (len>0));i++)
			{
				buffer[i]=b[off];
				off++;
				len--;
				usedBuffer++;
			}
			if(usedBuffer==BUFFER_SIZE)
			{
				signer.update(buffer,0,BUFFER_SIZE+headerByte); //Add the whole buffer to the MAC
				signer.doFinal(buffer, BUFFER_SIZE+headerByte);
				int processed=0;
				while(processed!=buffer.length) //Process whole buffer
				{
					processed+=ofbCipher.processBlock(buffer, processed, buffer, processed); //Copying it to the same buffer!
				}
				out.write(buffer,0,buffer.length);
				usedBuffer=0;
				buffer[0]=convertLength(BUFFER_SIZE);
			}

		}
	}

	/**
	 * Flushes the stream, creates a signature, if necessary.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void flush() throws IOException
	{
		if(usedBuffer!=0) //Still some bytes to write
		{
			buffer[0]=convertLength((short)usedBuffer);
			int padding=cipherBlockSize-((usedBuffer+signatureSize+headerByte)%cipherBlockSize);
			if(((usedBuffer+signatureSize+headerByte)%cipherBlockSize)==0)
			{
				padding=0; //No padding needed
			}
			byte[] pads=new byte[padding];
			for (int i = 0; i < pads.length; i++) {
				pads[i] = (byte)StaticSecurity.getRandom().nextInt();
			}
			int totalLength=padding+usedBuffer+signatureSize+headerByte; //Must be a multiple of cipherBlockSize
			signer.update(buffer, 0, usedBuffer+headerByte); //Add the whole buffer to the MAC
			signer.doFinal(buffer, usedBuffer+headerByte);
			System.arraycopy(pads, 0, buffer, usedBuffer+headerByte+signatureSize, padding);
			int processed=0;
			while(processed!=totalLength) //Process whole buffer
			{
				processed+=ofbCipher.processBlock(buffer, processed, buffer, processed); //Copying it to the same buffer!
			}
			out.write(buffer,0,totalLength);
			usedBuffer=0;
			buffer[0]=convertLength(BUFFER_SIZE);
		}
		out.flush();
	}

	/**
	 * Closes the stream, first does a {@link #flush()}.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void close() throws IOException {
		flush();
		out.close();
	}
	
	/**
	 * Converts the real length (>0) to a header byte.
	 * The header byte marks the start of the next signature.
	 * @param length The real length of the stream until the next signature comes (from 1 to 256) in bytes
	 * @return The header byte (a real Java byte)
	 */
	private byte convertLength(short length)
	{
		length-=129;
		return (byte)length;
	}
	


}
