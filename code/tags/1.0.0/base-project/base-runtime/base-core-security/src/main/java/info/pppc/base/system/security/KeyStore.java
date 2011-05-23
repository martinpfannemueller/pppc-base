package info.pppc.base.system.security;

import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.security.ecc.ECCCertificate;
import info.pppc.base.system.security.rsa.RSACertificate;
import info.pppc.base.system.security.sym.AESSymmetricKey;
import info.pppc.base.system.security.sym.HMACSymmetricKey;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.x509.X509Name;


/**
 * The key store stores the keys and certificates for a device. It
 * allows the on-demand retrieval and the modification of trusted
 * certificates. Before the key store can be used, it must be
 * initialized using a key provider.
 * 
 * @author Marcus Handte
 */
public class KeyStore {
	
	/**
	 * The entry stored in the key store for each certificate that
	 * is added.
	 * 
	 * @author Marcus Handte
	 */
	private static final class CertificateEntry {
		
		/**
		 * The certificate stored in this entry.
		 */
		private AbstractCertificate certificate;
		
		/**
		 * The trust level of the certificate.
		 */
		private int level;
		
		/**
		 * Creates a new entry with the specified certificate and trust level.
		 * 
		 * @param certificate The certificate to store.
		 * @param level The trust level to store.
		 */
		private CertificateEntry(AbstractCertificate certificate, int level) {
			if (certificate == null) throw new RuntimeException("Certificate must not be null.");
			this.certificate = certificate;
			this.level = level;
		}
		
		/**
		 * Returns the certificate of the entry.
		 * 
		 * @return The certificate of the entry.
		 */
		public AbstractCertificate getCertificate() {
			return certificate;
		}
		
		/**
		 * Returns the trust level of the certificate.
		 * 
		 * @return The trust level of the certificate.
		 */
		public int getLevel() {
			return level;
		}
		
		/**
		 * Determines whether the entry equals the passed object.
		 * 
		 * @return True if the entry's certificate and the object's
		 * 	certificate match.
		 */
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != getClass()) return false;
			CertificateEntry e = (CertificateEntry)obj;
			return e.getCertificate().equals(certificate);
		}
		
		/**
		 * Returns a hash code for the entry based on the certificate.
		 * 
		 * @return The hash code for the entry.
		 */
		public int hashCode() {
			return certificate.hashCode();
		}
		
	}
	
	/**
	 * The entry stored in the key store for session keys.
	 * 
	 * @author Marcus Handte
	 */
	private static final class KeyEntry {
		
		/**
		 * The system id of the system that issued the key.
		 */
		private SystemID system;
		/**
		 * The symmetric key used for signatures.
		 */
		private ISymmetricKey signature;
		
		/**
		 * The symmetric key used for encryption.
		 */
		private ISymmetricKey encryption;
		
		/**
		 * A time stamp at which the key has been created.
		 */
		private long timestamp;
		
		/**
		 * The trust level of the key.
		 */
		private int level;
		
		/**
		 * Creates a new session key entry for the specified system with
		 * a particular signature and encryption key as well as a timestamp.
		 * 
		 * @param system The system id of the target.
		 * @param signature The signature key.
		 * @param encryption The encryption key.
		 * @param timestamp The timestamp of the key.
		 */
		private KeyEntry(SystemID system, ISymmetricKey signature, ISymmetricKey encryption, long timestamp, int level) {
			if (system == null) throw new RuntimeException("System ID must not be null.");
			if (encryption == null) throw new RuntimeException("Encryption key must not be null.");
			if (signature == null) throw new RuntimeException("Signature key must not be null.");
			this.level=level;
			this.system = system;
			this.encryption = encryption;
			this.signature = signature;
			this.timestamp = timestamp;
		}
		
		/**
		 * Returns the encryption key.
		 * 
		 * @return The encryption key.
		 */
		public ISymmetricKey getEncryption() {
			return encryption;
		}
		
		/**
		 * Returns the signature key.
		 * 
		 * @return The signature key.
		 */
		public ISymmetricKey getSignature() {
			return signature;
		}
		
		/**
		 * Returns the timestamp of the entry.
		 * 
		 * @return The timestamp of the entry.
		 */
		public long getTimestamp() {
			return timestamp;
		}
		
		/**
		 * Returns the system id of the session key.
		 * 
		 * @return The system id of the key.
		 */
		public SystemID getSystem() {
			return system;
		}
		
		/**
		 * Returns the trust level of the key.
		 * 
		 * @return The trust level of the key.
		 */
		public int getLevel() {
			return level;
		}
		
		/**
		 * Returns a hash code for the entry based on the system.
		 * 
		 * @return The hash code of the entry.
		 */
		public int hashCode() {
			return system.hashCode();
		}
		
		/**
		 * Determines whether the entry has the same id than the object.
		 * 
		 * @param obj The object to compare to.
		 * @return True if the entry has the same system id than the object.
		 */
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != getClass()) return false;
			KeyEntry e = (KeyEntry)obj;
			return e.getSystem().equals(system);
		}
		
	}
	
	/**
	 * The trust level that indicates that there is something invalid.
	 */
	public static final int TRUST_LEVEL_INVALID = -1;
	
	/**
	 * The trust level that indicates that there is no trust at all
	 * on the corresponding issuer of a certificate.
	 */
	public static final int TRUST_LEVEL_NONE = 0;
	
	/**
	 * The trust level that indicates that there is only marginal trust
	 * on the corresponding issuer of a certificate.
	 */
	public static final int TRUST_LEVEL_MARGINAL = 1;
	
	/**
	 * The trust level that indicates that there is full trust on
	 * the corresponding issuer of a certificate.
	 */
	public static final int TRUST_LEVEL_FULL = 2;
	
	/**
	 * The time stamp for manually distributed session keys.
	 */
	public static final long TIMESTAMP_MANUAL = 0;
	
	/**
	 * The time stamp that signals missing session keys.
	 */
	public static final long TIMESTAMP_MISSING = -1;
	
	/**
	 * A list of trust levels for simplified processing.
	 */
	private static final int[] TRUST_LEVELS = 
		new int[] { TRUST_LEVEL_NONE, TRUST_LEVEL_MARGINAL, TRUST_LEVEL_FULL };
	
	/**
	 * The instance of the key store.
	 */
	protected static KeyStore instance = null;

	/**
	 * The vector that contains the key store entries.
	 */
	protected Vector certificates = new Vector();
	
	/**
	 * The session keys that have been negotiated with some devices.
	 */
	protected Vector keys = new Vector();
	
	/**
	 * A vector of systems that are currently establishing a key 
	 * with this device.
	 */
	protected Vector systems = new Vector();
	
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
		instance = new KeyStore(provider);
	}
	
	/**
	 * Returns the global instance of the key store. If the instance has not
	 * been created yet, it will be created. This method will throw an exception
	 * if the provider has not been set before this method is called.
	 * 
	 * @return The global instance of the key store.
	 * @throws IllegalArgumentException Thrown if the provider has not been
	 * 	set before this call using the initialize method.
	 */
	public static KeyStore getInstance() throws IllegalStateException {
		if (instance == null) {
			throw new IllegalStateException("Key store has not been initialized.");
		}
		return instance;
	}
	
	/**
	 * Creates a new key store using the specified provider that
	 * is used to load the initial set of keys.
	 * 
	 * @param provider The key provider.
	 */
	protected KeyStore(ICertificateProvider provider) {
		byte[] dcBytes = provider.getDeviceCertificate();
		byte[] dkBytes = provider.getDeviceKey();
		if (dcBytes == null || dkBytes == null) 
			throw new IllegalArgumentException("Could not find device key pair.");
		try {
			AbstractCertificate device = createCertificate(dcBytes, dkBytes);
			addCertificate(device, TRUST_LEVEL_FULL);
			SystemID.setBytes(device.getFingerprint());
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not load device key pair.");
		}
		for (int i = 0; i < TRUST_LEVELS.length; i++) {
			Enumeration certificates = provider.getTrustCertificates(TRUST_LEVELS[i]);
			while (certificates.hasMoreElements()) {
				byte[] cBytes = (byte[])certificates.nextElement();
				if (cBytes != null) {
					try {
						AbstractCertificate certificate = createCertificate(cBytes);
						addCertificate(certificate, TRUST_LEVELS[i]);
					} catch (IOException e) {
						Logging.debug(getClass(), "Could not load certificate.");
					}
				} else {
					Logging.debug(getClass(), "Found an unreadable certificate.");
				}
				
			}
		}
		// generate a symmetric device key for internal usage
		byte[] bytes = new byte[AESSymmetricKey.STANDARD_AES_KEY_AND_BLOCK_SIZE / 8];
		StaticSecurity.getSecureRandom().nextBytes(bytes);
		AESSymmetricKey encryption = new AESSymmetricKey(bytes);
		bytes = new byte[AESSymmetricKey.STANDARD_AES_KEY_AND_BLOCK_SIZE / 8];
		StaticSecurity.getSecureRandom().nextBytes(bytes);
		HMACSymmetricKey signature = new HMACSymmetricKey(bytes);
		addKey(SystemID.SYSTEM, signature, encryption, TRUST_LEVEL_FULL);
	}
	
	/**
	 * Creates a certificate using the specified byte sequence.
	 * 
	 * @param certificate The bytes of the certificate.
	 * @return An abstract certificate representing the byte sequence.
	 * @throws IOException Thrown if the certificate cannot be generated.
	 */
	public AbstractCertificate createCertificate(byte[] certificate) throws IOException {
		return createCertificate(certificate, null);
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
			ECCCertificate eccCert = new ECCCertificate(certificate, key);
			return eccCert;
		}
		catch(Exception e) { }
		try
		{
			RSACertificate rsaCert = new RSACertificate(certificate, key);
			return rsaCert;
		}
		catch(Exception e) { }

		throw new IOException("Could not create certificate.");
	}
	
	/**
	 * Adds a certificate with a particular trust to the store. The add operation 
	 * will not validate the certificate but it will simply add it. Make sure to 
	 * call the validate method before adding it to ensure that the certificate 
	 * can be trusted. Note that this method will adapt the trust level, if the
	 * certificate is contained already.
	 * 
	 * @param certificate The certificate to add.
	 * @param trust The trust level to set for the certificate.
	 * @throws IllegalArgumentException Thrown if the key store contains already
	 * 	a different certificate for the same subject. If the same certificate is
	 * 	contained already, the trust level is adapted according to the specified
	 * 	trust.
	 */
	public void addCertificate(AbstractCertificate certificate, int trust) throws IllegalArgumentException {
		for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			if (e.getCertificate().getSubject().equals(certificate.getSubject())) {
				if (! e.getCertificate().equals(certificate)) {
					throw new IllegalArgumentException("Duplicate subject name detected.");	
				} else {
					e.level = trust;	
				}
				return;
			}
		}
		certificates.addElement(new CertificateEntry(certificate, trust));
	}
	
	/**
	 * Returns the certificate that has been issued to the specified
	 * subject or null if there is none.
	 * 
	 * @param subject The subject to lookup.
	 * @return The certificate for the subject or null if the key store
	 * 	does not contain a certificate for the specified subject.
	 */
	public AbstractCertificate getCertificate(X509Name subject) {	
		for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			if (subject.equals(e.getCertificate().getSubject())) {
				return e.getCertificate();
			}
		}
		return null;
	}
	
	/**
	 * Returns the certificates that match a particular trust level. This method
	 * will return certificates with the the same trust level or one that is
	 * more trustworthy than that. For example, calling the method with level
	 * equals to trust level none will result in a list of all stored certificates.
	 * 
	 * @param level The trust level used for matching.
	 * @return The certificates that match the trust level.
	 */
	public AbstractCertificate[] getCertificates(int level) {
		Vector certs = new Vector();
		for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			if (e.getLevel() >= level) {
				certs.addElement(e.getCertificate());
			}
		}
		AbstractCertificate[] result = new AbstractCertificate[certs.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (AbstractCertificate)certs.elementAt(i);
		}
		return result;
	}

	/**
	 * Returns the certificate that has the specified system id.
	 * 
	 * @param systemID The system id of the certificate.
	 * @return The certificate with the specified system id or null
	 * 	if there is no such certificate.
	 */
	public AbstractCertificate getCertificate(SystemID systemID) {
		byte[] fingerprint = systemID.getBytes();
		certs: for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			byte[] fp = e.getCertificate().getFingerprint();
			for (int j = 0; j < fp.length; j++) {
				if (fingerprint[j] != fp[j]) continue certs;
			}
			return e.getCertificate();
		}
		return null;
	}
	
	/**
	 * Removes the specified certificate from the store.
	 * 
	 * @param certificate The certificate that shall be removed.
	 * @return True if the certificate has been removed, false if the
	 * 	certificate was not installed at the key store.
	 */
	public boolean removeCertificate(AbstractCertificate certificate) {
		for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			if (e.getCertificate().equals(certificate)) {
				certificates.removeElementAt(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the trust level of the given certificate, or TRUST_LEVEL_INVALID, if the certificate is 
	 * not stored in the KeyStore.
	 * 
	 * @param certificate The certificate which should be checked for the trust level
	 * @return The trust level of the given certificate ({@link #TRUST_LEVELS}), or TRUST_LEVEL_INVALID, if
	 * the certificate is not stored in the KeyStore
	 */
	public int getCertificateLevel(AbstractCertificate certificate)
	{
		if(certificate==null) return TRUST_LEVEL_INVALID;
		
		for (int i = 0; i < certificates.size(); i++) {
			CertificateEntry e = (CertificateEntry)certificates.elementAt(i);
			if (e.getCertificate().equals(certificate)) {
				return e.getLevel();
			}
		}
		return TRUST_LEVEL_INVALID;
	}
	
	/**
	 * Verifies a given certificate and returns the level of trust that can be put
	 * into the certificate.
	 * The level of trust is the first level of trust that can be assigned during the step through
	 * of the certificate chain. So the trust depends of the trust that is put in the parent certificate(s),
	 * if the certificate itself is not stored in the KeyStore.
	 * 
	 * If the certificate chain is broken or any of the certificates is
	 * invalid, then the method will return {@link #TRUST_LEVEL_INVALID}. 
	 * 
	 * @param certificate A chain of certificates that should be verified. The first certificate
	 * 	should point to the actual certificate that shall be validated, the remaining certificates
	 * 	should be the path to the top of the hierarchy. Given that the certificates of the hierarchy
	 *  are installed in the key store, they do not have to be specified as parameter in order to 
	 *  successfully validate the certificate.
	 * @return The level of trust that can be put into the certificate or {@link #TRUST_LEVEL_INVALID}.
	 */
	public int verifyCertificate(AbstractCertificate[] certificate)
	{
		if(certificate==null || certificate.length == 0)
		{
			return TRUST_LEVEL_INVALID;
		}
		for(int i=0;i<certificate.length;i++)
		{
			if((certificate[i]==null) || (!certificate[i].isValidRegardingStartAndEndDate())) return TRUST_LEVEL_INVALID;
			AbstractCertificate currentCert=this.getCertificate(certificate[i].getSubject());
			if((currentCert!=null) && (currentCert.equals(certificate[i])))
			{
				return getCertificateLevel(currentCert);
			}
			if(certificate.length>(i+1))
			{
				//Check signature on certificate
				if(!certificate[i+1].getPublicKey().verifySignature(certificate[i].getSignedPart(), certificate[i].getSignature()))
				{
					Logging.debug(this.getClass(), "Signature on this certificate is invalid, certificate chain is broken!");
					return TRUST_LEVEL_INVALID;
				}
				else
				{
					continue;
				}
			}
			else
			{
				//Must be a CA certificate
				if(certificate[i].getIssuer().equals(certificate[i].getSubject()))
				{
					if(!certificate[i].getPublicKey().verifySignature(certificate[i].getSignedPart(), certificate[i].getSignature()))
					{
						Logging.debug(this.getClass(), "Signature on this certificate is invalid, certificate chain is broken!");
						return TRUST_LEVEL_INVALID;
					}
					else
					{
						return TRUST_LEVEL_NONE;
					}
				}
				else
				{
					Logging.debug(this.getClass(), "The last certificate in the chain is no CA certificate!");
					return TRUST_LEVEL_INVALID;
				}
			}

		}
		Logging.debug(this.getClass(), "The last certificate in the chain is no CA certificate!");
		return TRUST_LEVEL_INVALID;
	}
	
	/**
	 * Verifies a given certificate chain, if it matches a certain fingerprint.
	 * If the first certificate is found that equals one of the given fingerprints, true is returned.<br><br>
	 * 
     * If a certificate that has a certain fingerprint is not found, yet, but the chain is
     * already invalid, false is returned.<br><br>
     * 
     * Note that not all signatures in a chain are verified. Especially note, that a root
     * certificate is never validated against itself. This is done for performance reasons.
     * Assuming that the used hash function for creating the fingerprints is strong enough
     * (i.e. not reversable), this is secure.<br><br>
	 * 
	 * @param certificate A chain of certificates where one certificate fingerprint should match
	 *  one of the fingerprints delivered with the other parameter. The first certificate
	 * 	should point to the actual (context signing) certificate that shall be validated, the remaining
	 *  certificates should be the path to the top of the hierarchy.
	 *  The chain is validated until the first matching fingerprint is found.
	 * @param fingerprints A Vector containing valid fingerprints as byte array which should be matched
	 *  against this certificate chain.
	 * @return True if one of the fingerprints matches one of the certificates and the chain up to this
	 *  certificate is valid. False otherwise
	 */
	public boolean verifyCertChainAgainstFPs(AbstractCertificate[] certificate, Vector fingerprints)
	{
		if(certificate==null || certificate.length == 0 || fingerprints==null || fingerprints.size()==0)
		{
			return false;
		}
		for(int i=0;i<certificate.length;i++)
		{
			if((certificate[i]==null) || (!certificate[i].isValidRegardingStartAndEndDate())) return false;
			//Check for fingerprint:
			fps: for(int j=0;j<fingerprints.size();j++)
			{
				byte[] currentElement=(byte[])fingerprints.elementAt(j);
				byte[] compareFP=certificate[i].getFingerprint();
				if((currentElement==null) || (currentElement.length!=compareFP.length)) continue;
				for(int k=0;k<currentElement.length;k++)
				{
					if(currentElement[k]!=compareFP[k]) continue fps;
				}
				return true;
			}
			if(certificate.length>(i+1))
			{
				//Check signature on certificate
				if(!certificate[i+1].getPublicKey().verifySignature(certificate[i].getSignedPart(), certificate[i].getSignature()))
				{
					Logging.debug(this.getClass(), "Signature on this certificate is invalid, certificate chain is broken!");
					return false;
				}
				else
				{
					continue;
				}
			}
		}
		return false;
	}
	
	/**
	 * Adds a manually configured session key to the key store.
	 * 
	 * @param system The system id of the system that shares the key.
	 * @param signature The signature key.
	 * @param encryption The encryption key.
	 * @param trustLevel The trust level to set for the certificate.
	 */
	public void addKey(SystemID system, ISymmetricKey signature, ISymmetricKey encryption, int trustLevel) {
		addKey(system, signature, encryption, TIMESTAMP_MANUAL, trustLevel);
	}
	
	/**
	 * Adds a negotiated session key to the key store.
	 * 
	 * @param system The system id of the system that shares the key.
	 * @param signature The signature key.
	 * @param encryption The encryption key.
	 * @param timestamp The time stamp of the key. 
	 * @param trustLevel The trust level to set for the certificate.
	 */
	public void addKey(SystemID system, ISymmetricKey signature, ISymmetricKey encryption, long timestamp, int trustLevel) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					if (e.getTimestamp() == TIMESTAMP_MANUAL && timestamp != TIMESTAMP_MANUAL) {
						Logging.debug(getClass(), "Ignoring generated key for system " + system + ".");
						return;
					} else {
						keys.removeElementAt(i);
						break;	
					}
				}
			}
			KeyEntry e = new KeyEntry(system, signature, encryption, timestamp, trustLevel);
			keys.addElement(e);	
			Logging.debug(getClass(), "Added new key for system " + system + ".");
		}
	}
	
	/**
	 * Removes the session key for the specified system from the store.
	 * 
	 * @param system The system to remove.
	 * @return True if a key has been removed, false otherwise.
	 */
	public boolean removeKey(SystemID system) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system) && e.getTimestamp() != TIMESTAMP_MANUAL) {
					keys.removeElementAt(i);
					return true;
				}
			}
			return false;	
		}
	}
	
	/**
	 * Returns the trust level of the session key for a given device. The method
	 * will return invalid trust level if there is no session key for the device.
	 * 
	 * @param system The device to lookup.
	 * @return The trust level of the session key or invalid, if the key is not stored.
	 */
	public int getKeyLevel(SystemID system) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					return e.getLevel();
				}
			}
			return TRUST_LEVEL_INVALID;
	
		}
	}
	
	/**
	 * Creates a new session key for the specified system. Note that
	 * a key will not be established if a manually configured key
	 * is available.
	 * 
	 * @param system The system with which a key should be
	 * 	established.
	 * @return True if successful, false otherwise.
	 */
	public boolean createKey(SystemID system) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					if (e.getTimestamp() == TIMESTAMP_MANUAL) 
						return true;
				}
			}	
		}
		synchronized (systems) {
			boolean waited = false;
			while (systems.contains(system)) {
				waited = true;
				try {
					systems.wait();
				} catch (InterruptedException e) {
					// nothing to be done
				}
			}
			if (waited) 
				return (getTimestamp(system) != TIMESTAMP_MISSING);
			systems.addElement(system);
		}
		Invocation invocation = new Invocation
			(new ReferenceID(SystemID.SYSTEM), new ReferenceID(system), "", new Object[0]);
		invocation.setRequirements(NFCollection.getDefault(NFCollection.TYPE_EXCHANGE, true));
		InvocationBroker broker = InvocationBroker.getInstance();
		broker.invoke(invocation);
		synchronized (systems) {
			systems.removeElement(system);
			systems.notifyAll();
		}
		return (invocation.getException() == null);
	}
	
	/**
	 * Returns the encryption key for the specified system or null
	 * if there is none.
	 * 
	 * @param system The system to search for.
	 * @return The encryption key or null if there is none.
	 */
	public ISymmetricKey getEncryption(SystemID system) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					return e.getEncryption();
				}
			}
			return null;	
		}
	}
	
	/**
	 * Returns the signature key for the specified system or null
	 * if there is none.
	 * 
	 * @param system The system to search for.
	 * @return The signature key or null if there is none.
	 */
	public ISymmetricKey getSignature(SystemID system) {	
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					return e.getSignature();
				}
			}
			return null;
	
		}
	}
	
	/**
	 * Returns the time stamp of a session key or TIMESTAMP_MISSING,
	 * if there is no entry for the key.
	 * 
	 * @param system The system id to retrieve.
	 * @return The time stamp of the key.
	 */ 
	public long getTimestamp(SystemID system) {
		synchronized (keys) {
			for (int i = 0; i < keys.size(); i++) {
				KeyEntry e = (KeyEntry)keys.elementAt(i);
				if (e.getSystem().equals(system)) {
					return e.getTimestamp();
				}
			}
			return TIMESTAMP_MISSING;	
		}
	}
}
