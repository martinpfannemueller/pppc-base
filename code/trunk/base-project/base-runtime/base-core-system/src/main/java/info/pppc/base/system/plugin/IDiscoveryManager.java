package info.pppc.base.system.plugin;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.IListener;

/**
 * The discovery manager is the interface of the plug-in manager exposed to
 * discovery plug-ins. This interface enables discovery plug-ins to listen
 * for changes to plug-ins and it enables them to register plug-in and device
 * descriptions at the local device registry.
 * 
 * @author Marcus Handte
 */
public interface IDiscoveryManager extends IPluginManager {

	/**
	 * The event constant that is used to propagate the addition of a plug-in.
	 * The data object of this event will contain the plug-in description of the
	 * plug-in that has been added.
	 */
	public static final int EVENT_PLUGIN_ADDED = 1;
	
	/**
	 * The event constant that is used to propagate the removal of a plug-in.
	 * The data object of this event will contain the plug-in description of the
	 * plug-in that has been removed.
	 */
	public static final int EVENT_PLUGIN_REMOVED = 2;

	/**
	 * Adds a listener to the discovery manager. The listener keeps track of
	 * changes to plug-ins. At the present time a discovery manager issues
	 * EVENT_PLUGIN_ADDED and EVENT_PLUGIN_REMOVED events to signal the addition
	 * and the removal of a plug-in. A discovery plug-in that caches the 
	 * plug-in descriptions should add a listen to keep track of changes and
	 * to modify its cache. The data object of the two events will contain the
	 * plug-in description of the plug-in that has been removed or added 
	 * respectively. The add event will be fired shortly after a plug-in has
	 * been installed. The remove event will be fired shortly before a plug-in
	 * is removed.
	 * 
	 * @param type The type of event to register for. At the present time the
	 * 	discovery manager supports EVENT_PLUGIN_ADDED and EVENT_PLUGIN_REMOVED
	 * 	events.
	 * @param listener The listener to register for the specified events.
	 */
	public void addPluginListener(int type, IListener listener);
	
	/**
	 * Removes the specified listener for the specified set of event types.
	 * 
	 * @param type The types of events to unregister.
	 * @param listener The listener to unregister for the specified types of
	 * 	events.
	 * @return True if the listener is no longer registered for any event,
	 * 	false if the listener is still registered or if it has not been
	 * 	registered.
	 */
	public boolean removePluginListener(int type, IListener listener);

	/**
	 * Opens a group connector using all transceivers that sends and
	 * receives data using the specified group.
	 * 
	 * @param group The group to use.
	 * @return The packet connector that is connected to all transceivers.
	 */
	public IPacketConnector openGroup(short group);
	
	/**
	 * Opens a group connector using the specified transceiver.
	 * 
	 * @param group The group to use.
	 * @param ability The ability of the transceiver to use.
	 * @return The packet connector that is connected to the specified transceiver. 
	 */
	public IPacketConnector openGroup(short group, short ability);
	
	/**
	 * Registers a device description that has been discovered from a remote device.
	 * A call to this method will replace a potentially previously registered 
	 * description with the new one provided by the call. 
	 * 
	 * @param device The device description of the device that announced the 
	 * 	descriptions.
	 * @param ttl The time to live.
	 * @throws NullPointerException Thrown if one of the parameters is null.
	 */
	public void registerDevice(DeviceDescription device, long ttl);
	
	/**
	 * Registers the specified plug-in description of the specified device.
	 * If the plug-in description has been registered before, it will be overwritten.
	 * 
	 * @param id The id of the device.
	 * @param plugin The plug-in description to register.
	 * @param ttl The time to live.
	 * @throws NullPointerException Thrown if one of the parameters is null.
	 */
	public void registerPlugin(SystemID id, PluginDescription plugin, long ttl);
	
}
