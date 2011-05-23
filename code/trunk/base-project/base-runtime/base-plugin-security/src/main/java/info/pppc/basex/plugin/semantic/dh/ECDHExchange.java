package info.pppc.basex.plugin.semantic.dh;

import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;

import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.ecc.ECCCertificate;
import info.pppc.base.system.security.ecc.ECCPrivateKey;
import info.pppc.base.system.security.ecc.ECCPublicKey;

/**
 * Creates a shared secret between two parties that possess a ECCCertificate.
 * Uses ECDH, is usually faster than the normal DHExchange.
 * In ECDH, it is not necessary to transmit an integer to the other partner,
 * but calling {@link #createLocalSecret()} is still mandatory!
 * 
 * @author Wolfgang Apolinarski
 */
public final class ECDHExchange extends AbstractExchange {

	private ECCCertificate cert;
	private ECDHBasicAgreement agreement=null;
	
	/**
	 * Creates a new instance. Initialize must be called
	 * 	first.
	 */
	public ECDHExchange() { }
	
	/**
	 * Initializes the elliptic curve diffie-hellman key-exchange class.
	 * @param cert An ECCCertificate including a private key (needed for the ECDH key-exchange).
	 * @throws IOException If the ECDH has no private key, or the private key is of the wrong format (i.e. use FastECDH instead).
	 */
	public void initialize(AbstractCertificate certificate) throws IOException {
		if (certificate == null || ! (certificate instanceof ECCCertificate))
			throw new IOException("Illegal certificate.");
		cert = (ECCCertificate)certificate;	
		if(!cert.hasPrivateKey()) throw new IOException("ECDH certificate has no private key!");
		if(!(cert.getPrivateKey() instanceof ECCPrivateKey)) throw new IOException("Private key for ECDH is not an instance of ECCPrivateKey!");
	}
	
	/**
	 * Creates the local secret (created as private value in this class).
	 * @return An empty byte array, no need to transfer to the other communication endpoint.
	 */
	public byte[] createLocalSecret()
	{
		ECCPrivateKey priv=(ECCPrivateKey)cert.getPrivateKey();
		agreement=priv.createLocalSecret();
		return new byte[0];
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.basex.plugin.semantic.AbstractDHKeyExchange#getSharedSecret(byte[], info.pppc.base.system.security.AbstractCertificate)
	 */
	public byte[] getSharedSecret(byte[] nothing, AbstractCertificate cert) throws IOException {
		if(cert==null) throw new IOException("No foreign certificate given!");
		if(!(cert.getPublicKey() instanceof ECCPublicKey)) throw new IOException("Public key for ECDH is not an instance of ECCPublicKey!");
		ECCPublicKey pubKey=(ECCPublicKey)cert.getPublicKey();
		BigInteger result=pubKey.getSharedSecret(agreement);
		return this.createHash(result.toByteArray());
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.basex.plugin.semantic.dh.AbstractDHKeyExchange#setNonce(byte[])
	 */
	public void setNonce(byte[] nonce)
	{
		this.nonce=nonce;
	}
}
