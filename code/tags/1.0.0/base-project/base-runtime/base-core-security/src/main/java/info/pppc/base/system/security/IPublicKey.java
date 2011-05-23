package info.pppc.base.system.security;

import java.io.IOException;

/**
 * Describes a general structure for public keys.
 * These are used for verification of signatures (which are normally used for message authentication &amp; integrity)
 * and for the creation of encrypted messages, which can only be read by the owner of the corresponding private key. 
 * @author WA
 */
public interface IPublicKey
{
	/**
	 * Use this public key to verify a signature.
	 * @param message The message which was signed
	 * @param signature  The signature of this message
	 * @return True, if the signature is valid, false otherwise
	 */
	public boolean verifySignature(byte[] message, byte[] signature);
	
	
	/**
	 * Encrypts a message with this public key.
	 * The encryption could include a header in the returned message, to use hybrid encryption mechanisms.<br>
	 * A message will be encrypted using both asymmetric and symmetric encryption, if the message size exceeds the block size of the asymmetric encryption.<br>
	 * If this is a symmetric key, then always symmetric encryption is used.
	 * @param input The message which should be encrypted
	 * @return The encrypted message, maybe containing a header, as described above
	 * @throws IOException If not enough memory is available or if a key could not be read, this exception occurs
	 */
	public byte[] encryptMessage(byte[] input) throws IOException;
	
	/**
	 * Every public key has to implement a comparison operator. Usually an public key equals another one, if the key is identical.
	 * This method must returns true, if the public keys equal each other.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @param obj The object of the type {@link AbstractCertificate} that should be compared to the current public key
	 * @return True, if the current public key and the given public key are equal, false otherwise
	 */
	public boolean equals(Object obj);
}
