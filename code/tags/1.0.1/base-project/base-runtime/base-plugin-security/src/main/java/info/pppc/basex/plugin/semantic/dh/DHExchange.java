package info.pppc.basex.plugin.semantic.dh;

import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.StaticSecurity;

import java.math.BigInteger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.generators.DHBasicKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;

/**
 * Creates a shared secret between two parties. Both parties need to first call 
 * the create method, then to send the created integer to the corresponding 
 * communication partner. After receiving this message, they call the get secret
 * method and obtain a shared secret (a SHA256 hash) that can be directly used 
 * for key creation.
 * 
 * @author Wolfgang Apolinarski
 */
public final class DHExchange extends AbstractExchange {

	/**
	 * The parameters used for key generation.
	 */
	private final byte[] g=new byte[]{94,-94,-25,-119,-126,-89,-5,93,98,-81,-119,-40,-36,53,-49,-9,28,-4,73,72,75,-17,121,-65,-84,-83,67,-103,-23,90,109,14,-55,47,117,101,118,95,-38,114,-93,-77,-53,-79,73,110,-111,63,49,60,119,-108,-64,-125,104,-4,-113,111,42,23,-64,73,79,119,61,34,-57,-80,28,-118,10,-117,42,98,75,-101,47,-113,-71,-50,-36,-25,-32,-63,59,-95,-44,15,32,107,54,-45,-81,81,91,-31,-87,28,99,-75,-74,-118,-59,55,-15,-126,-123,-39,41,29,-52,-7,-37,40,86,100,114,-7,-113,-61,48,106,21,-37,-105,103,110,82};
	/**
	 * The parameters used for key generation.
	 */
	private final byte[] p=new byte[]{0,-127,-126,-114,96,-91,-117,29,126,111,-117,20,-67,-128,110,66,-56,74,-66,-38,-107,-62,-1,-58,70,94,100,1,-118,-97,66,121,126,74,108,22,9,95,-122,-38,-125,-78,-68,-85,11,-116,-94,47,101,116,76,14,0,91,79,-118,-45,-123,44,-15,54,69,24,-52,-115,-18,-104,-34,-57,-38,22,-39,87,-85,117,51,-41,2,39,20,45,125,-22,108,-2,-62,112,79,-99,35,-122,89,58,-43,95,-41,13,84,-92,99,-117,-57,25,-45,109,-25,-95,63,-30,58,91,124,-62,127,-45,83,40,-78,64,-124,44,-78,83,-99,-63,-26,-55,102,-25};
	
	
	/**
	 * The agreement used to derive a shared secret.
	 */
	private DHBasicAgreement dhAgreement = new DHBasicAgreement();
	
	/**
	 * The parameters used for key generation.
	 */
	private DHParameters dhParams = new DHParameters(new BigInteger(p),new BigInteger(g));
	
	/**
	 * Creates a dh exchange for a certificate.
	 */
	public DHExchange() { }
	
	/**
	 * Initializes the exchange with the certificate.
	 * 
	 * @param cert The certificate.
	 */
	public void initialize(AbstractCertificate cert) {
		
	}
	
	/**
	 * Creates the the BigInteger of this communication end-point. 
	 * This integer should be transferred to the communication partner.
	 * 
	 * @return A BigInteger (g^a). This message can be read by anybody 
	 * 	without revealing the shared secret.
	 */
	public byte[] createLocalSecret()
	{
		DHKeyGenerationParameters dhParameters=new DHKeyGenerationParameters(StaticSecurity.getSecureRandom(), dhParams);
		DHBasicKeyPairGenerator keyPairGenerator=new DHBasicKeyPairGenerator();
		keyPairGenerator.init(dhParameters);
		AsymmetricCipherKeyPair DHKeyPair=keyPairGenerator.generateKeyPair();
		dhAgreement.init(DHKeyPair.getPrivate());
		return ((DHPublicKeyParameters)DHKeyPair.getPublic()).getY().toByteArray();
	}
	
	/**
	 * Creates the shared secret out of the public BigInteger from the communication 
	 * partner. The returned bytes can directly be transformed into a shared secret key.
	 * 
	 * @param remoteParameter The BigInteger that was obtained from the communication partner
	 * @param cert The certificate of the remote device is not used and can be <b>null</b>
	 * @return 32 bytes that can directly be used to create a shared symmetric key.
	 */
	public byte[] getSharedSecret(byte[] remoteParameter, AbstractCertificate cert)
	{
		return createHash(dhAgreement.calculateAgreement(new DHPublicKeyParameters(new BigInteger(remoteParameter),dhParams)));
	}
	
	/*
	 * (non-Javadoc)
	 * @see info.pppc.basex.plugin.semantic.dh.AbstractDHKeyExchange#setNonce(byte[])
	 */
	public void setNonce(byte[] nonce)
	{
		//Do nothing
	}
}
