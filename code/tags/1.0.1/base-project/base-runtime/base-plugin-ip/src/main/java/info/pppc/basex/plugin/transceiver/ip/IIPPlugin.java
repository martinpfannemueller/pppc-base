package info.pppc.basex.plugin.transceiver.ip;

import info.pppc.base.system.plugin.IPlugin;

/**
 * The ip plug-in interface is implemented by ip plug-ins to enable the
 * reuse of the ip-based connectors.
 * 
 * @author Marcus Handte
 */
public interface IIPPlugin extends IPlugin {

	/**
	 * Called by the stream connector to signal that it has been 
	 * removed.
	 * 
	 * @param connector The removed stream connector.
	 */
	public void release(IPStreamConnector connector);
	
	/**
	 * Called by the packet connector to signal that it has been
	 * removed.
	 * 
	 * @param connector The removed packet connector.
	 */
	public void release(IPPacketConnector connector);
	
}
