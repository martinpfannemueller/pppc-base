package info.pppc.base.demo.spotlight.util;

import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.util.Logging;

/**
 * A simple listener that prints the detected and removed devices
 * when registered to the device registry.
 * 
 * @author Marcus Handte
 */
public class DeviceListener implements IListener {

	/**
	 * Called by the device registry to signal changes.
	 * 
	 * @param event The event that signals changes.
	 */
	public void handleEvent(Event event) {
		switch (event.getType()) {
			case DeviceRegistry.EVENT_DEVICE_ADDED:
				Logging.log(getClass(), "Device added: " + event.getData());
				break;
			case DeviceRegistry.EVENT_DEVICE_REMOVED:
				Logging.log(getClass(), "Device removed: " + event.getData());
				break;
			default:
				break;
		}
	}
	
}
