package info.pppc.basex.plugin.transceiver.emulator;

/**
 * A simple data structure that stores the scenario information
 * about a single connection.
 * 
 * @author Marcus Handte
 */
public class Connection {
	
	/**
	 * The source device of the connection. This field
	 * references the name field in the device.
	 */
	public String source;
	
	/**
	 * The target device of the connection. This field
	 * references the name field in the device.
	 */
	public String target;
}
