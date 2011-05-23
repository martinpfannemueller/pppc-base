package info.pppc.base.system.plugin;

/**
 * A connector is a handle that is created by a transport plug-in. This handle
 * can be used to connect to one or more devices. Conceptually, a connector is
 * similar to a socket. It enables the reception or the transfer of data from
 * a sender or to a receiver. A connector supports the querying of properties.
 * To determine the value of a property, a connector will typically request 
 * a number of properties from its children and determine its state by comparing
 * them to some preset defaults. If a plug-in does not know a property, it should
 * return the value of its children. This enables high-level plug-ins to answer 
 * queries to low-level attributes. If a property is not declared, a plug-in
 * should return false to the has property method. Note that the implementation
 * of this method will typically also be recursive.
 * 
 * @author Marcus Handte
 */
public interface IConnector {
	
	/**
	 * Releases the connector and all of its children. Note that this method must
	 * be called by the creator of the connector after all transmissions have been
	 * finished. If the creator of the connector omits this method, the plug-in that
	 * provides the connector might not be able to perform a proper cleanup which 
	 * might result in a considerable waste of resources.
	 */
	public void release();
	
	/**
	 * Returns the plug-in that created the connector or null if there is
	 * no single plug-in that is bound to the connector.
	 * 
	 * @return The plug-in that created the connector or null if there are
	 * multiple plug-ins.
	 */
	public IPlugin getPlugin();

}
