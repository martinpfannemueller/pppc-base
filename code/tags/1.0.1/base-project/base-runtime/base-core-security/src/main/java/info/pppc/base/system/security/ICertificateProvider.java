package info.pppc.base.system.security;

import java.util.Enumeration;

/**
 * The certificate provider provides abstract access to the initial certificates
 * and keys that are deployed on the device. The reason for this indirection
 * is the lack of file system support on some devices. Thus, by implementing
 * different device-specific providers, it is possible to get around this 
 * limitation. 
 * 
 * @author Marcus Handte
 */
public interface ICertificateProvider {

	/**
	 * Returns the certificate of the device. If a failure
	 * occurs, this method should return null.
	 * 
	 * @return The certificate of the device.
	 */
	public byte[] getDeviceCertificate();
	
	/**
	 * Returns the private key of the device. If a failure
	 * occurs, this method should return null.
	 * 
	 * @return The private key of the device.
	 */
	public byte[] getDeviceKey();
	
	/**
	 * Returns trusted certificates for the specified trust level
	 * as an enumeration of byte arrays. In order to express failures
	 * the enumeration may return null.
	 * 
	 * @param level The level that shall be retrieved.
	 * @return An enumeration of byte arrays that may be null to
	 * 	signal failures.
	 */
	public Enumeration getTrustCertificates(int level);

}
