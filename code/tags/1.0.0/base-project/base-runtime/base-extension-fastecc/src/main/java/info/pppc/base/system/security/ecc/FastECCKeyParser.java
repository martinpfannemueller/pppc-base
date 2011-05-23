package info.pppc.base.system.security.ecc;

import java.io.IOException;

import com.sun.spot.security.GeneralSecurityException;
import com.sun.spot.security.implementation.ECPrivateKeyImpl;
import com.sun.spot.security.implementation.ecc.ECCurve;

/**
 * Supports only the SECP160R1 curve!
 * @author apolinar
 *
 */
public class FastECCKeyParser {
	
	private static final byte sequence=0x30;
	private static final byte integer=0x02;
	private static final byte octetString=0x04;
	private static final byte longLengthBitmask=(byte)0x80;
	private final ECPrivateKeyImpl privateKey;
	
	private FastECCKeyParser(ECPrivateKeyImpl privateKey)
	{
		this.privateKey=privateKey;	
	}
	
	public static FastECCKeyParser fastECCKeyParser(byte[] keyFile) throws IOException
	{
		ECPrivateKeyImpl privateKey=new ECPrivateKeyImpl(ECCurve.SECP160R1);
		
		int index=0;
		//Sequence starts
		if(!(sequence==keyFile[index++])) throw new IOException("This is not a compatible private ECC key; No sequence at this location!");
		//Length field starts (this field is ignored!)
		{
			byte lengthByte=keyFile[index++];
			if((lengthByte & longLengthBitmask)==longLengthBitmask)
			{
				int length=(((byte)(lengthByte^longLengthBitmask))&0xff); //Make a positive integer out of it
				if(length<0) throw new IOException("Length field is negative!"); //0 is allowed in the DER-Standard
				index+=length;
			}
		}
		//Version number starts
		if(!(integer==keyFile[index++])) throw new IOException("This is not a compatible private ECC key; No integer at this (version) location!");
		//Version length
		{
			byte lengthByte=keyFile[index++];
			if((lengthByte & longLengthBitmask)==longLengthBitmask)
			{
				int length=(((byte)(lengthByte^longLengthBitmask))&0xff); //Make a positive integer out of it
				if(length<0) throw new IOException("Length field is negative!"); //0 is allowed in the DER-Standard
				int lengthOfThisField=0;
				for(int i=0;i<length;i++)
				{
					lengthOfThisField=lengthOfThisField << 8;
					lengthOfThisField+=keyFile[index++] & 0xff;
				}
				index+=lengthOfThisField; //Skip this field;
			}
			else
			{
				index+=(lengthByte&0xff); //Skip this field;
			}
		}
		//Private key starts
		if(!(octetString==keyFile[index++])) throw new IOException("This is not a compatible private ECC key; No octet string at this (private key) location!");
		//Private key length
		int keySize;
		{
			byte lengthByte=keyFile[index++];
			if((lengthByte & longLengthBitmask)==longLengthBitmask)
			{
				int length=(((byte)(lengthByte^longLengthBitmask))&0xff); //Make a positive integer out of it
				if(length<0) throw new IOException("Length field is negative!"); //0 is allowed in the DER-Standard
				int lengthOfThisField=0;
				for(int i=0;i<length;i++)
				{
					lengthOfThisField=lengthOfThisField << 8;
					lengthOfThisField+=keyFile[index++] & 0xff;
				}
				keySize=lengthOfThisField;
			}
			else
			{
				keySize=(lengthByte&0xff);
			}
		}
		//Start reading private key:
		if(keyFile[index]==0x00)
		{
			//Skip this sign byte (compare to X509Certificate.java)
			index++;
			keySize--;
		}
		try
		{
			privateKey.setS(keyFile, index, keySize);
		}
		catch (GeneralSecurityException e)
		{
			throw new IOException("Could not read in ECC private key, cause: "+e.getMessage());
		}
		return(new FastECCKeyParser(privateKey));
	}
	
	public ECPrivateKeyImpl getPrivateKey()
	{
		return privateKey;
	}

}
