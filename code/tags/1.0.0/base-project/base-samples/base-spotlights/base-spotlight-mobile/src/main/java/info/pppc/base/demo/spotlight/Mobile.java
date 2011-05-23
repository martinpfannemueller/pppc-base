package info.pppc.base.demo.spotlight;

import org.eclipse.jface.action.ActionContributionItem;

import info.pppc.base.demo.spotlight.service.ILedService;
import info.pppc.base.demo.spotlight.service.LedService;
import info.pppc.base.demo.spotlight.service.LedServiceProxy;
import info.pppc.base.demo.spotlight.service.LedServiceSkeleton;
import info.pppc.base.demo.spotlight.service.LedState;
import info.pppc.base.demo.spotlight.swtui.action.SunspotAction;
import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.service.ServiceProperties;
import info.pppc.base.service.ServiceRegistry;
import info.pppc.base.service.ServiceRegistryException;
import info.pppc.base.swtui.Application;
import info.pppc.base.swtui.BaseUI;
import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.PluginManager;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.routing.ProactiveRouting;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.serializer.ObjectSerializer;
import info.pppc.basex.plugin.transceiver.MxIPMulticastTransceiver;

/**
 * This class is the startup class for BASE on the windows
 * mobile pda.
 * 
 * @author Marcus Handte
 */
public class Mobile {


	/**
	 * Starts the program.
	 * 
	 * @param args Command line arguments, just ignored.
	 */
	public static void main(String[] args) {
		// device config
		System.setProperty("info.pppc.name", "Mobile");
		System.setProperty("info.pppc.type", Integer.toString(DeviceDescription.TYPE_PDA));
		System.setProperty("info.pppc.id", "4");
		// start the ui 
		BaseUI.registerSystemBrowser();
		final Application a = Application.getInstance();
		a.run(new Runnable() {
			public void run() {
				a.addContribution(new ActionContributionItem(new SunspotAction(a)));
			}
		});
		// start the broker
		InvocationBroker broker = InvocationBroker.getInstance();
		PluginManager manager = broker.getPluginManager();
		manager.addPlugin(new ObjectSerializer());
		manager.addPlugin(new RmiSemantic());
		manager.addPlugin(new ProactiveRouting());
		manager.addPlugin(new ProactiveDiscovery());
		manager.addPlugin(new MxIPMulticastTransceiver());
		// register service for testing
		//doService();
	}

	public static void doService() {
		// register service
		LedService service = new LedService();
		LedServiceSkeleton skeleton = new LedServiceSkeleton();
		skeleton.setImplementation(service);
		ServiceRegistry registry = ServiceRegistry.getInstance(); 
		try {
			registry.export("SunSpot", new String[] { ILedService.class.getName() }, new ServiceProperties(), skeleton, service);
		} catch (ServiceRegistryException e) {
			Logging.error(Mobile.class, "Could not register service.", e);
		}		
	}
	
	public static void doClient() {
		// run a query and set all leds to blue and back
		ServiceRegistry registry = ServiceRegistry.getInstance();
		ServiceDescriptor[] descs = registry.lookup("SunSpot", ServiceRegistry.LOOKUP_BOTH);
		Logging.log(Mobile.class, "Found " + descs.length + " SunSpot services.");
		for (int i = 0; i < descs.length; i++) {
			ServiceDescriptor desc = descs[i];
			LedServiceProxy proxy = new LedServiceProxy();
			proxy.setSourceID(new ReferenceID(SystemID.SYSTEM));
			proxy.setTargetID(desc.getIdentifier());
			try {
				// set all leds to blue
				int leds = proxy.getLedCount();
				for (int j = 0; j < leds; j++) {
					LedState state = new LedState(j);
					state.setBlue(255);
					state.setEnabled(true);
					proxy.setLedState(state);
				}
				// set all leds to off 
				for (int j = 0; j < leds; j++) {
					LedState state = new LedState(j);
					state.setEnabled(false);
					proxy.setLedState(state);
				}
			} catch (InvocationException e) {
				Logging.error(Mobile.class, "Could not run demo.", e);
			}
		}
	}
	
}
