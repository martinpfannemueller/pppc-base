package info.pppc.base.system.plugin;

/**
 * The transceiver manager is the interface of the plug-in manager exposed to
 * transceiver plug-ins. Transceiver plug-ins use this interface to dispatch
 * incoming connectors to the plug-in manager which takes care of composing
 * the stack.
 * 
 * @author Marcus Handte
 */
public interface ITransceiverManager extends IPluginManager {
	
	/**
	 * Tells the plug-in manager that it should handle an incoming connection using
	 * the specified connector.
	 * 
	 * @param connector The connector that points to a new incoming connection.
	 */
	public void acceptSession(IStreamConnector connector);

}
