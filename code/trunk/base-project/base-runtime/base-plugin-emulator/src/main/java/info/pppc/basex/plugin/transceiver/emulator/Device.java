package info.pppc.basex.plugin.transceiver.emulator;

/**
 * A simple data structure that stores the scenario information 
 * about one device.
 * 
 * @author Marcus Handte
 */
public class Device {
	
	/**
	 * The host name or ip address of the device.
	 */
	public String host;
	
	/**
	 * The port number of the device.
	 */
	public int port;
	
	/**
	 * A human readable name that is referenced in connections.
	 */
	public String name;

}