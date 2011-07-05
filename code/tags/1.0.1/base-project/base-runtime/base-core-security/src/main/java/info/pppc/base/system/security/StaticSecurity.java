package info.pppc.base.system.security;

import java.security.SecureRandom;
import java.util.Random;

import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;

/**
 * Contains some singletons for the current device, for file handling, random
 * numbers or data handling and the CA root certificates.<br>
 * File names for all used certificates (which must be installed in the directory which is parameterized by the creation of the <b>DataStorage</b>) are:
 * <ul><li>For the own certificate of this device: <b>cert.der</b>
 * <li>For the private key of this certificate: <b>priv.der</b>
 * <li>The name of the certificate of the certificate authority is: <b>ca.der</b>
 * </ul>
 * @author WA
 */
public class StaticSecurity {

	/**
	 * An normal random number generator. 
	 */
	private static final Random random = new Random();
	
	/**
	 * A secure random number generator.
	 */
	private final static SecureRandom secureRandom;
	
	/**
	 * The number of random seed bytes that should be used for seeding the
	 * secure random number generator.
	 */
	private final static int SEED_BYTES=20;
	
	/**
	 * Is true, if the fast seeding algorithm should be used.
	 * False is more secure.
	 */
	private final static boolean FAST_SEEDER=true;
	
	/**
	 * Initializes the secure random number generator.
	 */
	static{
		ThreadedSeedGenerator seeder=new ThreadedSeedGenerator();
		SecureRandom sr=new SecureRandom();
		sr.setSeed(System.currentTimeMillis());
		sr.setSeed(seeder.generateSeed(SEED_BYTES, FAST_SEEDER));
		secureRandom=sr;
	}
	
	/**
	 * A normal pseudo random number generator, used to get "normal" random values.
	 * 
	 * @return Get the current instance of the pseudo random number generator.
	 */
	public static Random getRandom()
	{
		return random;
	}
	
	/**
	 * A secure pseudo random number generator, used to get secure random values
	 * @return The current instance of the <b>secure</b> pseudo random number generator. 
	 */
	public static SecureRandom getSecureRandom()
	{
		return secureRandom;
	}
}
