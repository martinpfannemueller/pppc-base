package info.pppc.base.system.security;

/**
 * An interface for symmetric keys, which combine the ability of
 * public and private asymmetric keys.
 * 
 * Since usually one key is used for encryption OR for signing, additional
 * methods are necessary to use the key in the right way. 
 * @author WA
 */
public interface ISymmetricKey extends IPublicKey, IPrivateKey {

	/**
	 * If this symmetric key can be used for encryption, this method will return <b>true</b>.
	 * @return True, if this key can be used for encryption
	 */
	public boolean forEncryption();
	
	/**
	 * If this symmetric key can be used to create signatures, this method will return <b>true</b>.
	 * @return True, if this key can be used to create signatures
	 */
	public boolean forSigning();
}
