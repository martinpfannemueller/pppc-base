package info.pppc.base.tutorial.swt;

import info.pppc.base.service.ServiceProperties;
import info.pppc.base.service.ServiceRegistry;
import info.pppc.base.service.ServiceRegistryException;
import info.pppc.base.swtui.Application;
import info.pppc.base.swtui.BaseUI;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginManager;
import info.pppc.base.system.util.Logging;
import info.pppc.base.tutorial.rmi.IRmi;
import info.pppc.base.tutorial.rmi.RmiMain;
import info.pppc.base.tutorial.rmi.RmiService;
import info.pppc.base.tutorial.rmi.RmiSkeleton;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.serializer.ObjectSerializer;
import info.pppc.basex.plugin.transceiver.MxIPMulticastTransceiver;

/**
 * Demonstrates a simple swt-based user interface that enables
 * manual construction of service searches and invocations. For
 * more details see the package description.
 * 
 * @author Marcus Handte
 */
public class SwtMain {

	/**
	 * Called to start the application.
	 * 
	 * @param args Command line arguments (ignored in this tutorial).
	 */
	public static void main(String[] args) {
		// set logging verbosity
		Logging.setVerbosity(Logging.MAXIMUM_VERBOSITY);
		// add plugins
		InvocationBroker b = InvocationBroker.getInstance();
		PluginManager m = b.getPluginManager();
		m.addPlugin(new MxIPMulticastTransceiver());
		m.addPlugin(new ProactiveDiscovery());
		m.addPlugin(new RmiSemantic());
		m.addPlugin(new ObjectSerializer());
		// add the system browser
		Application.getInstance();
		BaseUI.registerSystemBrowser();
		// create an example service
		RmiSkeleton skel = new RmiSkeleton();
		RmiService serv = new RmiService();
		skel.setImplementation(serv);
		// register service at local service registry
		ServiceRegistry r = ServiceRegistry.getInstance();
		try {
			r.export(
				"Rmi", 
				new String[] { IRmi.class.getName()},
				new ServiceProperties(),
				skel,
				serv			
			);			
		} catch (ServiceRegistryException e) {
			Logging.error(RmiMain.class, "Could not register service.", e);
			System.exit(1);
		}		
	}
}
