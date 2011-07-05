package info.pppc.base.demo.spotlight;

import info.pppc.base.demo.spotlight.util.DeviceListener;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginManager;
import info.pppc.base.system.event.Event;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.routing.ProactiveRouting;
import info.pppc.basex.plugin.transceiver.MxSerialTransceiver;
import info.pppc.basex.plugin.transceiver.MxSpotTransceiver;

import javax.microedition.midlet.MIDlet;



/**
 * The spot bridge configures a sun spot to act as router
 * that is bridging between a serial and a 802.15.4 
 * connection. 
 * 
 * System properties must be set via the ant build script.
 * ant set-system-property -Dkey=info.pppc.name -Dvalue=Spotbridge
 * ant set-system-property -Dkey=info.pppc.id   -Dvalue=2
 * ant set-system-property -Dkey=info.pppc.type -Dvalue=7
 * 
 * @author Marcus Handte
 */
public class Spotbridge extends MIDlet {

	/**
	 * Called when the reset button is pressed on the spot.
	 * 
	 * @param force I don't know what this means on the spot.
	 */
	protected void destroyApp(boolean force) { 	}

	/**
	 * This method is apparently not called on the sun spots.
	 */
	protected void pauseApp() { }

	/**
	 * Called when the midlet is started.
	 */
	protected void startApp() {
		InvocationBroker broker = InvocationBroker.getInstance();
		PluginManager manager = broker.getPluginManager();
		manager.addPlugin(new ProactiveRouting());
		manager.addPlugin(new ProactiveDiscovery());
		manager.addPlugin(new MxSerialTransceiver());
		manager.addPlugin(new MxSpotTransceiver());
		DeviceRegistry registry = broker.getDeviceRegistry();
		registry.addDeviceListener(Event.EVENT_EVERYTHING, new DeviceListener());
	}
	
}
