package info.pppc.base.system.plugin;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.operation.IOperator;

/**
 * Each plug-in gets a reference to a manager. This reference enables the
 * plug-in to execute some operation. Furthermore, it enables plug-ins to
 * retrieve additional information about devices and plug-ins that are
 * available.
 * 
 * @author Marcus Handte
 */
public interface IPluginManager extends IOperator {
	
	/**
	 * Returns all plug-in descriptions of all plug-ins that are currently visible.
	 * The array returned by this call is a working copy. I.e. it can be freely
	 * manipulated. However, the plug-in descriptions are used system-wide. Thus,
	 * changes to a description will be propagated throughout the system. A discovery
	 * plug-in should not manipulate any other plug-in description than its own.
	 * As discovery is used to signal availability and connectivity, a
	 * discovery plug-in should actually never announce this set of plug-in descriptions
	 * as this will lead to false connectivity information. Instead a plug-in should
	 * announce the plug-ins that are compatible with the corresponding communication
	 * technology used during announcement.
	 * 
	 * @param system The system id of the device.
	 * @return A copy of an array that contains all plug-in descriptions of plug-ins
	 * 	that are currently registered at the system.
	 */
	public PluginDescription[] getPluginDescriptions(SystemID system);
	
	/**
	 * Returns the device description of the specified device. Note that the device
	 * description is a global object that should not be changed by discovery 
	 * plug-ins. Changes to the device description are immediately propagated to
	 * other parts of the local system.
	 * 
	 * @param system The system id of the device.
	 * @return The device description of the specified device.
	 */
	public DeviceDescription getDeviceDescription(SystemID system);
	
	/**
	 * Returns the system ids of systems that are currently available in
	 * the environment.
	 * 
	 * @return The available systems.
	 */
	public SystemID[] getDevices();
}
