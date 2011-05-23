package info.pppc.basex.plugin.util;

import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;


/**
 * A multiplexer plug-in is a plug-in that makes use of the multiplexer
 * to multiplex connections.
 * 
 * @author Marcus Handte
 */
public interface IMultiplexPlugin extends IPlugin {

	/**
	 * Called by the multiplexer whenever a connector should be
	 * opened due to remote request.
	 * 
	 * @param source The source multiplexer that created the connector.
	 * @param connector The connector that is requested.
	 */
	public void acceptConnector(MultiplexFactory source, IStreamConnector connector);

	/**
	 * Called by the multiplexer whenever the multiplexer is 
	 * closed.
	 * 
	 * @param multiplexer The multiplexer that is closed.
	 */
	public void closeMultiplexer(MultiplexFactory multiplexer);

	/**
	 * Returns the plug-in manager of the multiplexer plug-in.
	 * 
	 * @return The plug-in manager of the plug-in.
	 */
	public IPluginManager getPluginManager();

}
