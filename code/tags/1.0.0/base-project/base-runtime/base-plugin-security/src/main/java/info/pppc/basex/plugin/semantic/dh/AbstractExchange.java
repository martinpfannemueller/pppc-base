package info.pppc.basex.plugin.semantic.dh;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Hashtable;

import org.bouncycastle.crypto.digests.SHA256Digest;

import info.pppc.base.system.SystemID;
import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.semantic.ExchangeSemantic;

/**
* The abstract class for all types of DH key-exchanges.
* A DH key-exchange creates a shared secret between two parties. Both parties need to first call 
* the create method, then to send the created integer to the corresponding 
* communication partner. After receiving this message, they call the get secret
* method and obtain a shared secret (a SHA256 hash) that can be directly used 
* for key creation.<br><br>
* Note that not every type of key-exchange needs to send an integer over the
* network connection, nevertheless, to provide interoperability with all types of DH,
* the methods return an integer.
* 
* 
* @author Wolfgang Apolinarski
*/
public abstract class AbstractExchange {

	/**
	 * The identifier that is used to signal that normal DH should be performed.
	 */
	public static final short PERFORM_NORMAL_DH=0;
	
	/**
	 * The identifier that is used to signal that normal ECDH should be performed.
	 */
	public static final short PERFORM_ECDH=1;
	
	/**
	 * The identifier that is used to signal that fast ECDH should be performed.
	 */
	public static final short PERFORM_FAST_ECDH=2;
	
	/**
	 * The exchanges that are registered at this class.
	 */
	private static final Hashtable exchanges = new Hashtable();
	
	/**
	 * Register the default handler classes.
	 */
	static {
		exchanges.put(new Short(PERFORM_ECDH), ECDHExchange.class);
		exchanges.put(new Short(PERFORM_FAST_ECDH), ECDHExchange.class);
		exchanges.put(new Short(PERFORM_NORMAL_DH), DHExchange.class);
	}
	
	/**
	 * Initializes the exchange with the specified certificate.
	 * 
	 * @param cert The certificate used for initialization.
	 * @throws IOException Thrown if the certificate does not match.
	 */
	public abstract void initialize(AbstractCertificate cert) throws IOException;
	
	
	/**
	 * A nonce that is added to the common secret to create the hash.
	 */
	protected byte[] nonce=null;
	
	/**
	 * Creates the the BigInteger of this communication end-point. 
	 * This integer should be transferred to the communication partner.
	 * 
	 * @return A BigInteger (g^a). This message can be read by anybody 
	 * 	without revealing the shared secret.
	 */
	public abstract byte[] createLocalSecret() throws IOException;
	
	/**
	 * Creates the shared secret out of the public BigInteger from the communication 
	 * partner. The returned bytes can directly be transformed into a shared secret key.
	 * 
	 * @param integer The BigInteger that was obtained from the communication partner (only needed for DH)
	 * @param cert The certificate of the other communication partner (only needed for ECDH)
	 * @return 32 bytes that can directly be used to create a shared symmetric key.
	 */
	public abstract byte[] getSharedSecret(byte[] integer, AbstractCertificate cert) throws IOException;
	
	/**
	 * Here an additional nonce can be set to make the output hash random, even if ECDH is used.<br>
	 * DH ignores this method.
	 * @param nonce A nonce of variable size.
	 */
	public abstract void setNonce(byte[] nonce);
		
	/**
	 * Registers a particular exchange class for handling the exchange of
	 * a certain type.
	 * 
	 * @param semantic
	 * @param exchangeClass
	 * @return
	 */
	public static Class registerInstance(short semantic, Class exchangeClass) {
		return (Class)exchanges.put(new Short(semantic), exchangeClass);
	}
	
	/**
	 * Creates a new instance of the DH-Key-Exchange either using the fast ECDH (only with fast ECCCertificates), ECDH or the normal DH.
	 * @param exchangeSemanticType A constant that shows which type of DH should be used, e.g. {@link ExchangeSemantic#PERFORM_NORMAL_DH}
	 * @return An instance of the DH-Key-Exchange that fits the given certificate
	 * @see ExchangeSemantic
	 */
	public static AbstractExchange createInstance(short exchangeSemanticType)
	{
		try
		{
			KeyStore ks=KeyStore.getInstance(); 
			AbstractCertificate cert = ks.getCertificate(SystemID.SYSTEM);
			Class clazz = (Class)exchanges.get(new Short(exchangeSemanticType));
			Object o = clazz.newInstance();
			AbstractExchange ae = (AbstractExchange)o;
			ae.initialize(cert);
			return ae;
		}	
		catch(Throwable e)
		{
			Logging.debug(AbstractExchange.class, "Using normal DH due to exception.");
			return new DHExchange();
		}
	}
	
	/**
	 * Creates a hash from the common secret. Returns a 256-Bit number created with SHA256.
	 * @param agreementInt The common secret
	 * @return A 256-Bit number that can be used to obtain secret keys for signing and encryption
	 * @see #createHash(byte[])
	 */
	byte[] createHash(BigInteger agreementInt)
	{
		return createHash(agreementInt.toByteArray());
	}
	
	/**
	 * Creates a hash from the common secret and the nonce, if this is not null. Returns a 256-Bit number created with SHA256.
	 * @param commonSecretBytes The common secret
	 * @return A 256-Bit number that can be used to obtain secret keys for signing and encryption
	 */
	byte[] createHash(byte[] commonSecretBytes)
	{
		SHA256Digest digest=new SHA256Digest();
		digest.update(commonSecretBytes, 0, commonSecretBytes.length);
		if(nonce!=null)
		{
			digest.update(nonce,0,nonce.length);
		}
		byte[] result=new byte[digest.getDigestSize()]; //Should be 32 bytes == 256 bits in this case
		digest.doFinal(result, 0);
		return result;
	}
	
}
