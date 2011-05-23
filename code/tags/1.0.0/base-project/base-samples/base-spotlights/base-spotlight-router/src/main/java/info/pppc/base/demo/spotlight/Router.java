package info.pppc.base.demo.spotlight;

import info.pppc.base.demo.spotlight.util.DeviceListener;
import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginManager;
import info.pppc.base.system.event.Event;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.routing.ProactiveRouting;
import info.pppc.basex.plugin.transceiver.MxIPMulticastTransceiver;
import info.pppc.basex.plugin.transceiver.MxSerialTransceiver;

/**
 * This class is the startup class for BASE on the
 * wireless router.
 * 
 * @author Marcus Handte
 */
public class Router {

	/**
	 * The identifier of the com port to which the sun spot will be 
	 * attached. 
	 */
	private static final String COM_PORT =  "/dev/ttyS0"; //"COM10";
	
	/**
	 * The ip address to bind to.
	 */
	private static final byte[] IP_ADDRESS = new byte[] {
		(byte)192, (byte)168, (byte)1, (byte)1
	};
	
	
	/**
	 * Starts the program.
	 * 
	 * @param args Command line arguments, just ignored.
	 */
	public static void main(String[] args) {
		System.setProperty("info.pppc.name", "Router");
		System.setProperty("info.pppc.type", Integer.toString(DeviceDescription.TYPE_WRT));
		System.setProperty("info.pppc.id", "3");
		InvocationBroker broker = InvocationBroker.getInstance();
		PluginManager manager = broker.getPluginManager();
		manager.addPlugin(new ProactiveRouting());
		manager.addPlugin(new ProactiveDiscovery());
		manager.addPlugin(new MxSerialTransceiver(COM_PORT, true));
		manager.addPlugin(new MxIPMulticastTransceiver(IP_ADDRESS));
		DeviceRegistry registry = broker.getDeviceRegistry();
		registry.addDeviceListener(Event.EVENT_EVERYTHING, new DeviceListener());
	}

}
