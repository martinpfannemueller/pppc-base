package info.pppc.base.system.plugin;

/**
 * The interface of a discovery plug-in. Discovery plug-ins are responsible
 * for distributing device and plug-in descriptions in such a way that they
 * are available before the remote interaction is initiated. In order to
 * enable device discovery, discovery plug-ins need to distribute (at least)
 * the device description in a proactive manner. Plug-in descriptions may
 * be distributed reactively. To do this, discovery plug-ins may use the 
 * packet connectors from transceivers. To get a handle on them, they can 
 * use the discovery manager interface. Device and plug-in descriptions can 
 * be registered locally via the discovery manager. 
 * 
 * @author Marcus Handte
 */
public interface IDiscovery extends IPlugin {

	/**
	 * Sets the plug-in manager of this plug-in. The plug-in manager enables 
	 * a plug-in to request a communication stack and to perform operations. 
	 * This method is guaranteed to be called before the start method is
	 * invoked for the first time.
	 * 
	 * @param manager The manager of the plug-in.
	 */
	public void setDiscoveryManager(IDiscoveryManager manager);

}
