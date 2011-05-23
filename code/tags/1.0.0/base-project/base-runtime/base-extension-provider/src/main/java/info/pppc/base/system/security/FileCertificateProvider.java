/**
 * 
 */
package info.pppc.base.system.security;

import info.pppc.base.system.util.Logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Enumeration;

/**
 * The file certificate provider reads the certificates and keys
 * of a device from the local file system. Thereby the provider
 * uses the hierarchy of the file system to determine the trust
 * levels. 
 * 
 * @author Marcus Handte
 */
public class FileCertificateProvider implements ICertificateProvider {

	/**
	 * The trust levels that are supported by this provider.
	 */
	private static final int[] LEVELS = { KeyStore.TRUST_LEVEL_NONE, KeyStore.TRUST_LEVEL_MARGINAL, KeyStore.TRUST_LEVEL_FULL };
	
	/**
	 * The folders in which the certificates for different trust levels can be found.
	 */
	private static final String[] FOLDERS = { "none", "marginal", "full" };
	
	/**
	 * The file extension of key files.
	 */
	private static final String EXTENSION_KEY = ".key";
	
	/**
	 * The file extension of certificate files.
	 */
	private static final String EXTENSION_CERTIFICATE = ".der";
	
	/**
	 * The buffer size used when reading certificates.
	 */
	private static final int BUFFER_SIZE = 1024; 
	
	/**
	 * The device name that is used to get the device certificate and key.
	 */
	private String device;
	
	/**
	 * The folder name that is used to load the keys and certificates.
	 */
	private String folder;
	
	/**
	 * An enumeration of certificates that can be loaded lazy. 
	 * 
	 * @author Marcus Handte
	 */
	private class FileCertificateEnumeration implements Enumeration {
		
		/**
		 * The current position in the file array.
		 */
		private int position = 0;
		
		/**
		 * The file array to be loaded.
		 */
		private File[] files;
		
		/**
		 * Creates a new  certificate enumeration for the
		 * specified set of files.
		 * 
		 * @param files The files to load.
		 */
		public FileCertificateEnumeration(File[] files) {
			this.files = files;
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
			byte[] content = load(files[position]);
			position += 1;
			return content;
		}
				
	}
	
	/**
	 * Creates a new provider that reads the configuration of the specified
	 * device from the specified folder. The provider will look for subfolders
	 * in order to resolve the trust levels and it will search for a file
	 * that corresponds to the device name in order to to load the device
	 * certificate and the device key.
	 * 
	 * @param folder The folder in which to search for.
	 * @param device The name of the device.
	 */
	public FileCertificateProvider(String folder, String device) {
		this.folder = folder;
		this.device = device;
	}
	
	/**
	 * Returns the certificate of the device.
	 * 
	 * @return The device certificate.
	 */
	public byte[] getDeviceCertificate() {
		return load(new File(folder, device + EXTENSION_CERTIFICATE));
	}
	
	
	/**
	 * Returns the key of the device.
	 * 
	 * @return The device key.
	 */
	public byte[] getDeviceKey() {
		return load(new File(folder, device + EXTENSION_KEY));
	}
	
	/**
	 * Returns an enumeration of the certificates for
	 * a particular trust level.
	 * 
	 * @param level The trust level to retrieve.
	 * @return An enumeration of certificates.
	 */
	public Enumeration getTrustCertificates(int level) {	
		for (int i = 0; i < LEVELS.length; i++) {
			if (level == LEVELS[i]) {
				return new FileCertificateEnumeration(list(new File(folder, FOLDERS[i])));
			}
		}
		Logging.debug(getClass(), "Could not find folder definition for trust level " + level + ".");
		return new FileCertificateEnumeration(new File[0]);
	}
	
	
	/**
	 * Loads the contents of a file into memory.
	 * 
	 * @param file The file to read.
	 * @return The sequence of bytes from the file.
	 * @throws RuntimeException Thrown if the certificate cannot be loaded.
	 */
	private byte[] load(File file) {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			FileInputStream input = new FileInputStream(file);
			// a certificate or key file should not be bigger than 4GB, this seems safe
			ByteArrayOutputStream output = new ByteArrayOutputStream((int)file.length());
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		} catch (IOException e) {
			Logging.debug(getClass(), "Could not read from file " + file.getAbsolutePath() + ".");
			return null;
		}
	}
	
	/**
	 * Lists the certificates contained in a directory.
	 * 
	 * @param file The file representing the directory.
	 * @return The list of files or an empty list, if something goes wrong.
	 */
	private File[] list(File file) {
		File[] files = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(EXTENSION_CERTIFICATE); 
			}
		});
		if (files == null) {
			Logging.debug(getClass(), "Could not read contents from directory " + file.getAbsolutePath() + ".");
			return new File[0];
		} else {
			return files;
		}
	}
	
}
