package info.pppc.basex.plugin.routing.swing;

import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;

/**
 * An interface to get a reference to a remote gateway server.
 * 
 * @author Mac
 */
public interface IGatewayProvider {

	/**
	 * Returns the remote gateway server or null, if it cannot be accessed.
	 * 
	 * @return The reference to the remote gateway server, or null if this
	 * 	is not possible.
	 */
	public IRemoteGatewayServer getGatewayServer();
	
}
