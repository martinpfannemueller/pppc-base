package info.pppc.base.demo.spotlight;

import info.pppc.base.demo.spotlight.service.ILedService;
import info.pppc.base.demo.spotlight.service.LedService;
import info.pppc.base.demo.spotlight.service.LedServiceSkeleton;
import info.pppc.base.service.ServiceProperties;
import info.pppc.base.service.ServiceRegistry;
import info.pppc.base.service.ServiceRegistryException;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginManager;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.routing.ProactiveRouting;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.serializer.ObjectSerializer;
import info.pppc.basex.plugin.transceiver.MxSpotTransceiver;

import javax.microedition.midlet.MIDlet;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;


/**
 * The spot server exports an led service via 802.15.4.
 * 
 * System properties must be set via the ant build script.
 * ant set-system-property -Dkey=info.pppc.name -Dvalue=Spotserver
 * ant set-system-property -Dkey=info.pppc.id   -Dvalue=1
 * ant set-system-property -Dkey=info.pppc.type -Dvalue=7
 * 
 * @author Marcus Handte
 */
public class Spotserver extends MIDlet {

	/**
	 * Called when the reset button is pressed on the spot.
	 * 
	 * @param force I don't know what this means on the spot.
	 */
	protected void destroyApp(boolean force) {

	}

	/**
	 * This method is apparently not called on the sun spots.
	 */
	protected void pauseApp() {

	}

	/**
	 * Called when the midlet is started.
	 */
	protected void startApp() {
		// register plug-ins
		InvocationBroker broker = InvocationBroker.getInstance();
		PluginManager manager = broker.getPluginManager();
		manager.addPlugin(new ObjectSerializer());
		manager.addPlugin(new RmiSemantic());
		manager.addPlugin(new ProactiveDiscovery());
		manager.addPlugin(new ProactiveRouting());
		manager.addPlugin(new MxSpotTransceiver());
		// register service (use led 0 for status)
		ITriColorLED[] boardLeds = EDemoBoard.getInstance().getLEDs();
		ITriColorLED[] serviceLeds = new ITriColorLED[boardLeds.length - 1];
		for (int i = 0; i < boardLeds.length; i++) { 
			boardLeds[i].setOff();
			boardLeds[i].setRGB(0, 0, 0);
		}
		System.arraycopy(boardLeds, 1, serviceLeds, 0, serviceLeds.length);
		LedService service = new LedService(serviceLeds);
		LedServiceSkeleton skeleton = new LedServiceSkeleton();
		skeleton.setImplementation(service);
		ServiceRegistry registry = ServiceRegistry.getInstance(); 
		try {
			registry.export("SunSpot", new String[] { ILedService.class.getName() }, new ServiceProperties(), skeleton, service);
			boardLeds[0].setRGB(0, 255, 0);
		} catch (ServiceRegistryException e) {
			boardLeds[0].setRGB(255, 0, 0);
		}
		// enable status led (green is ok, red is export failure)
		boardLeds[0].setOn();
	}

}
