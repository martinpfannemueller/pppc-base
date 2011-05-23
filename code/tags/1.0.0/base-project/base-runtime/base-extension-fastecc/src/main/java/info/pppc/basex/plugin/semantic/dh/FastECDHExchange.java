package info.pppc.basex.plugin.semantic.dh;

import java.io.IOException;

import com.sun.spotx.crypto.implementation.ECDHKeyAgreement;

import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.ecc.FastECCCertificate;
import info.pppc.base.system.security.ecc.FastECCPrivateKey;
import info.pppc.base.system.security.ecc.FastECCPublicKey;

/**
 * Creates a shared secret between two parties that possess a ECCCertificate on the curve SECP160R1.
 * Uses fast ECDH (created by SUN for the SUNSpots), is usually a lot faster than the usual ECDH.
 * In fast ECDH, it is not necessary to transmit an integer to the other partner,
 * but calling {@link #createLocalSecret()} is still mandatory!
 * @author Wolfgang Apolinarski
 *
 */
public final class FastECDHExchange extends AbstractExchange {

	private FastECCCertificate cert;
	private ECDHKeyAgreement agreement=null;
	private FastECCPublicKey pubKey=null;
	
	/**
	 * Creates a new fast ecdh, initialize must be called first.
	 */
	public FastECDHExchange() { }
	
	public void initialize(AbstractCertificate certificate) throws IOException {
		if (certificate == null || ! (certificate instanceof AbstractCertificate))
			throw new IOException("Illegal certificate.");
		cert = (FastECCCertificate)certificate;
		if(!cert.hasPrivateKey()) throw new IOException("ECDH certificate has no private key!");
		if(!(cert.getPrivateKey() instanceof FastECCPrivateKey)) throw new IOException("Private key for ECDH is not an instance of FastECCPrivateKey!");
	}
	
	/**
	 * Creates the local secret (created as private value in this class).
	 * @return An empty byte array, no need to transfer to the other communication endpoint.
	 */
	public byte[] createLocalSecret() throws IOException
	{
		FastECCPrivateKey priv=(FastECCPrivateKey)cert.getPrivateKey();
		agreement=priv.createLocalSecret();
		return new byte[0];
	}

	/*
	 * (non-Javadoc)
	 * @see info.pppc.basex.plugin.semantic.dh.AbstractDHKeyExchange#getSharedSecret(byte[], info.pppc.base.system.security.AbstractCertificate)
	 */
	public byte[] getSharedSecret(byte[] nothing, AbstractCertificate cert) throws IOException
	{
		if(cert==null) throw new IOException("No foreign certificate given!");
		if(!(cert.getPublicKey() instanceof FastECCPublicKey)) throw new IOException("Public key for ECDH is not an instance of FastECCPublicKey!");
		pubKey=(FastECCPublicKey)cert.getPublicKey();
		return this.createHash(pubKey.getSharedSecret(agreement));
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
