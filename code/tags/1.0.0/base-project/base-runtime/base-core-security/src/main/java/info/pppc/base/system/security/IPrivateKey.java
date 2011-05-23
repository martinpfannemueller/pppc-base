package info.pppc.base.system.security;

import java.io.IOException;

/**
 * Describes a general structure for private keys.
 * These are used for creation of signatures (which are normally used for message authentication &amp; integrity)
 * and for the decrypting of encrypted messages. This can only be done, if the reader used the corresponding public key for encryption.
 * @author WA
 */
public interface IPrivateKey
{
	/**
	 * Creates a cryptographic signature (with this private key) of a message.
	 * Normally uses the SHA-1 hash to hash the message, the hash will then be encrypted with the private key (this hash can be decrypted with the public key to verify the message)
	 * @param message The message which should be signed
	 * @return An encrypted SHA-1 hash (usually) of the message 
	 */
	public byte[] createSignature(byte[] message);
	
	/**
	 * Decrypts an encrypted message (with this private key).
	 * @param message The encrypted message, which should be decrypted
	 * @return The decrypted message, if the message could be successfully decrypted or null otherwise
	 */
	public byte[] decryptMessage(byte[] message) throws IOException;
	
	/**
	 * Every private key has to implement a comparison operator. Usually an private key equals another one, if the key is identical.
	 * This method must returns true, if the private keys equal each other.
	 * @see java.lang.Object#equals(java.lang.Object)
	 * @param obj The object of the type {@link AbstractCertificate} that should be compared to the current private key
	 * @return True, if the current private key and the given private key are equal, false otherwise
	 */
	public boolean equals(Object obj);
	
}
