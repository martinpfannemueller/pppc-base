/**
 * 
 */
package info.pppc.base.system.security;

import info.pppc.base.system.util.Logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * The resource certificate provider reads certificates that are contained in
 * the jar together with the source code using the methods provided by the
 * class loader.
 * 
 * @author Mac
 */
public class ResourceCertificateProvider implements ICertificateProvider {

	/**
	 * The buffer size to read from the stream.
	 */
	private static final int BUFFER_SIZE = 1024; 
	
	/**
	 * An enumeration of certificates that can be loaded lazy. 
	 * 
	 * @author Marcus Handte
	 */
	private class ResourceCertificateEnumeration implements Enumeration {
		
		/**
		 * The current position in the file array.
		 */
		private int position = 0;
		
		/**
		 * The file array to be loaded.
		 */
		private String[] files;
		
		/**
		 * The path path.
		 */
		private String path;
		
		/**
		 * Creates a new  certificate enumeration for the
		 * specified set of files.
		 * 
		 * @param files The files to load.
		 * @param path The base path to attach.
		 */
		public ResourceCertificateEnumeration(String path, String[] files) {
			if (files == null)
				this.files = new String[0];
			else 
				this.files = files;
			this.path = path;
		}
		
		/**
		 * Returns true if the end of the file array is not reached.
		 * 
		 * @return True if there are some files.
		 */
		public boolean hasMoreElements() {
			return (position < files.length);
		}
		
		/**
		 * Loads the next element.
		 * 
		 * @return The next element.
		 */
		public Object nextElement() {
			byte[] content = load(path, files[position]);
			position += 1;
			return content;
		}
				
	}	
	
	/**
	 * The base path.
	 */
	private String path;
	/**
	 * The certificate file name.
	 */
	private String certificate;
	/**
	 * The key file name.
	 */
	private String key;
	/**
	 * A list of fully trusted certificates.
	 */
	private String[] full;
	/**
	 * A list of marginal trusted certificates.
	 */
	private String[] marginal;
	/**
	 * A list of none trusted certificates.
	 */
	private String[] none;
	
	
	/**
	 * Initializes the certificate provider to load certificates from the specified path.
	 * 
	 * @param path The path to load the certificates from, relative to the class path.
	 * 	The path should start with a slash and end with a slash.
	 * @param deviceCertificate The file name of the device certificate relative to
	 * 	the specified path. The file name should not start with a slash.
	 * @param deviceKey The file name of the device key relative to the specified path.
	 * 	The file name should not start with a slash.
	 */
	public ResourceCertificateProvider(String path, String deviceCertificate, String deviceKey) {
		this(path, deviceCertificate, deviceKey, new String[0], new String[0], new String[0]);
	}
	
	/**
	 * Initializes the certificate provider to load certificates from the specified path.
	 * 
	 * @param path The path to load the certificates from, relative to the class path.
	 * 	The path should start with a slash and end with a slash.
	 * @param deviceCertificate The file name of the device certificate relative to
	 * 	the specified path. The file name should not start with a slash.
	 * @param deviceKey The file name of the device key relative to the specified path.
	 * 	The file name should not start with a slash.
	 * @param full The file names of the fully trusted certificates. The file names
	 * 	should not start with a slash.
	 */
	public ResourceCertificateProvider(String path, String deviceCertificate, String deviceKey, String[] full) {
		this(path, deviceCertificate, deviceKey, full, new String[0], new String[0]);
	}
	
	/**
	 * Initializes the certificate provider to load certificates from the specified path.
	 * 
	 * @param path The path to load the certificates from, relative to the class path.
	 * 	The path should start with a slash and end with a slash.
	 * @param deviceCertificate The file name of the device certificate relative to
	 * 	the specified path. The file name should not start with a slash.
	 * @param deviceKey The file name of the device key relative to the specified path.
	 * 	The file name should not start with a slash.
	 * @param full The file names of the fully trusted certificates. The file names
	 * 	should not start with a slash.
	 * @param marginal The file names of the marginally trusted certificates. The file names
	 * 	should not start with a slash.
	 */
	public ResourceCertificateProvider(String path, String deviceCertificate, String deviceKey, String[] full, String[] marginal) {
		this(path, deviceCertificate, deviceKey, full, marginal, new String[0]);
	}
	
	
	/**
	 * Initializes the certificate provider to load certificates from the specified path.
	 * 
	 * @param path The path to load the certificates from, relative to the class path.
	 * 	The path should start with a slash and end with a slash.
	 * @param certificate The file name of the device certificate relative to
	 * 	the specified path. The file name should not start with a slash.
	 * @param key The file name of the device key relative to the specified path.
	 * 	The file name should not start with a slash.
	 * @param full The file names of the fully trusted certificates. The file names
	 * 	should not start with a slash.
	 * @param marginal The file names of the marginally trusted certificates. The file names
	 * 	should not start with a slash.
	 * @param none The file names of the none trusted certificates. The file names
	 * 	should not start with a slash.
	 */
	public ResourceCertificateProvider(String path, String certificate, String key, String[] full, String[] marginal, String[] none) {
		this.path = path;
		this.certificate = certificate;
		this.key = key;
		this.full = full;
		this.marginal = marginal;
		this.none = none;
	}
	
	/**
	 * Returns the device certificate.
	 * 
	 * @return The device certificate.
	 */
	public byte[] getDeviceCertificate() {
		return load(path, certificate);
	}

	/**
	 * Returns the device key.
	 * 
	 * @return The device key.
	 */
	public byte[] getDeviceKey() {
		return load(path, key);	
	}

	/**
	 * Returns an enumeration of the specified certificates.
	 * 
	 * @return An enumeration of the specified certificates.
	 */
	public Enumeration getTrustCertificates(int level) {
		switch (level) {
			case KeyStore.TRUST_LEVEL_FULL:
				return new ResourceCertificateEnumeration(path, full);
			case KeyStore.TRUST_LEVEL_MARGINAL:
				return new ResourceCertificateEnumeration(path, marginal);
			case KeyStore.TRUST_LEVEL_NONE:
				return new ResourceCertificateEnumeration(path, none);
			default:
				return new ResourceCertificateEnumeration(null, null);
		}
	}
	
	/**
	 * Loads a resource using the specified path and file name.
	 * 
	 * @param path The path name to prepend.
	 * @param file The file name.
	 * @return The resource as bytes or null.
	 */
	private byte[] load(String path, String file) {
		try {
			if (path == null) path = "";
			if (file == null) return null;
			byte[] buffer = new byte[BUFFER_SIZE];
			InputStream input = getClass().getResourceAsStream(path + file);
			if (input == null) {
				Logging.debug(getClass(), "Cannot load " + path + file);
				return null;
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		} catch (IOException e) {
			Logging.debug(getClass(), "Could not read from resource " + path + file + ".");
			return null;
		}
	}

}
