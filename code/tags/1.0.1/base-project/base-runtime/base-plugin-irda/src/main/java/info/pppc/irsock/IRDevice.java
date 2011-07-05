package info.pppc.irsock;

import java.io.IOException;

/**
 * This class holds dicovery informations for devices. Note that
 * this class is referenced by native code. Do not edit it.
 * 
 * @author bator
 */
public class IRDevice{
		
	/**
	 * The address of the ir device represented by this object.
	 */
	private byte[] address = new byte[]{0,0,0,0};

	/**
	 * The name of the ir device (human readable).
	 */
	private byte name[] = new byte[22];

	/**
	 * Creates a new uninitialized ir device. This constructor
	 * should not be called from user code.
	 */
	public IRDevice(){
		super();
	}
			
	/**
	 * Returns the device address of the ir device.
	 * 
	 * @return A four byte ip address of the device.
	 */
	public byte[] getAddress(){
		byte[] bytes = new byte[address.length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = address[i];
		}
		return bytes;
	}
			
	/**
	 * Returns the device name.
	 * 
	 * @return A human readable name.
	 */
	public String getName(){
		return new String(name);
	}
	
	/**
	 * Returns a human readable representation of this ir device.
	 * 
	 * @return A human readable string.
	 */
	public String toString() {
		return new String(name) + " " + (address[0] & 0xFF) + "." +
			(address[1] & 0xFF) + "." +(address[2] & 0xFF) + "." +
			(address[3] & 0xFF);
	}
	
	/**
	 * Determines whether two device information objects are equal.
	 * This match is performed on value equality.
	 * 
	 * @return True if the passed object equals this object.
	 */
	public boolean equals(Object o) {
		if (o != null && o.getClass() == getClass()) {
			IRDevice d = (IRDevice)o;
			byte[] a = d.getAddress();
			return (a[0] == address[0] && a[1] == address[1] &&
				a[2] == address[2] && a[3] == address[3]);
		}
		return false;
	}

	/**
	 * Returns a content-based hashcode for this device info.
	 *
	 * @return A content-based hashcode.
	 */
	public int hashCode() {
		return ((((int)(address[0] & 0xFF)) << 24) |
			(((int)(address[1] & 0xFF)) << 16) |
			(((int)(address[2] & 0xFF)) << 8) |
			(((int)(address[3] & 0xFF))));
	}

	/**
	 * Makes a device discovery and returns a list of discovered devices.
	 * 
	 * @return The list of discovered devices.
	 * @throws IOException Thrown if the discovery fails.
	 */
	public static IRDevice[] discover() throws IOException {
		return IRSocketImpl.discover();
	}

}