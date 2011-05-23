package info.pppc.base.system.security;

import java.io.IOException;

import info.pppc.base.system.security.ICertificateProvider;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.security.ecc.FastECCCertificate;
import info.pppc.basex.plugin.semantic.dh.AbstractExchange;
import info.pppc.basex.plugin.semantic.dh.FastECDHExchange;

/**
 * The fast key store is a drop in replacement for the key store that
 * in addition to bouncycastle-based certificates can also use the
 * highly optimized SunSPOT SSL library.
 * 
 * @author Mac
 */
public class FastKeyStore extends KeyStore {

	
	/**
	 * Initializes the key store using the keys provided by the provider.
	 * This method must be called before the middleware is started. It must
	 * only be called once. If it is called multiple times, it will throw
	 * an illegal state exception. 
	 * 
	 * @param provider The provider of the keys.
	 * @throws IllegalStateException Thrown if the initialization is performed
	 * 	multiple times.
	 */
	public static void initialize(ICertificateProvider provider) throws IllegalStateException {
		if (KeyStore.instance != null) 
			throw new IllegalStateException("Key store has already been initialized.");
		instance = new FastKeyStore(provider);
		AbstractExchange.registerInstance(AbstractExchange.PERFORM_FAST_ECDH, FastECDHExchange.class);
	}	
	
	/**
	 * Creates a new fast key store that is able to create fast ecc certificates
	 * using the fast SunSPOT library.
	 * 
	 * @param provider The provider that contains the certificates.
	 */
	protected FastKeyStore(ICertificateProvider provider) {
		super(provider);
	}
	
	/**
	 * Creates a certificate using the specified byte sequence and optionally using
	 * the specified private key.
	 * 
	 * @param certificate The bytes denoting the certificate.
	 * @param key The bytes denoting the associated private key. 
	 * @return An abstract certificate that represents the byte sequence. 
	 * @throws IOException Thrown if the certificate cannot be generated.
	 */
	protected AbstractCertificate createCertificate(byte[] certificate, byte[] key) throws IOException {
		try
		{
			FastECCCertificate eccCert = new FastECCCertificate(certificate, key);
			return eccCert;
		} catch (Exception e) { }
		return super.createCertificate(certificate, key);
	}
	
}
