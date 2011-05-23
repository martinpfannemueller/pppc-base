package info.pppc.basex.plugin.routing.server;

import java.io.IOException;

import info.pppc.base.system.SystemID;
import info.pppc.base.system.plugin.IStreamConnector;

/**
 * The stream connector provider is used to establish connections with
 * another device through a gateway.
 * 
 * @author Mac
 */
public interface IStreamConnectorProvider {

	/**
	 * Returns a connector to the gateway that knows this device.
	 * 
	 * @param target The device that shall be connected.
	 * @return A connector that is connected.
	 * @throws IOException Thrown if the connection cannot be established.
	 */
	public IStreamConnector getConnector(SystemID target) throws IOException;
	
}
