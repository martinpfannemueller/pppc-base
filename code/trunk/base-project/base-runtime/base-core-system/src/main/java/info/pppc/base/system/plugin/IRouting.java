package info.pppc.base.system.plugin;

/**
 * The interface of routing plug-ins. This interface is a subset
 * of the functionality provided by a discovery plug-in, a semantic plug-in 
 * and a transceiver plug-in. A routing plug-in is responsible for route
 * discovery and it is responsible for forwarding as well as dispatching.
 * 
 * @author Marcus Handte
 */
public interface IRouting extends IPlugin, IStreamer, IDispatcher {

	/**
	 * Sets the routing manager. The routing manager enables the plug-in
	 * to access the necessary functionality for route discovery and
	 * call forwarding and dispatching.
	 * 
	 * @param manager The routing manager.
	 */
	public void setRoutingManager(IRoutingManager manager);
	
}
