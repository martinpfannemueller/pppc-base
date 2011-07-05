package info.pppc.base.tutorial.rmi;

import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.service.ServiceProperties;
import info.pppc.base.service.ServiceRegistry;
import info.pppc.base.service.ServiceRegistryException;
import info.pppc.base.system.FutureResult;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.PluginManager;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.serializer.ObjectSerializer;
import info.pppc.basex.plugin.transceiver.MxIPMulticastTransceiver;

/**
 * Performs a base startup, registers a set of plug-ins to perform remote calls via java.net 
 * and registers one service. The service will perform a remote search for services that 
 * have its type and it will call a method on all remote services that have been found.
 *  
 * @author Marcus Handte
 */
public class RmiMain {

	/**
	 * The amount of time that the client thread will wait after
	 * it has been started.
	 */
	public static final long PERIOD_INIT = 10000;
	
	/**
	 * The amount of time that the client thread will wait between
	 * the lookup calls.
	 */
	public static final long PERIOD_CYCLE = 1000;
	
	/**
	 * The total time for this demo to run. After this time, the
	 * broker will be shut down.
	 */
	public static final long PERIOD_TIME = 60000;
	
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
		// create service
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
		// create and execute a client thread
		IOperation client = new IOperation() {	
			/**
			 * Performs a query every x seconds and calls print on the 
			 * remote services that have been found until then.
			 * 
			 * @param monitor The monitor to cancel the operation.
			 * @throws Exception Should not be thrown.
			 */
			public void perform(IMonitor monitor) throws Exception {
				ServiceRegistry registry = ServiceRegistry.getInstance();
				synchronized (monitor) {
					// wait for some time
					try {
						monitor.wait(PERIOD_INIT);
					} catch (InterruptedException e) {
						Logging.error(getClass(), "Thread got interrupted.", e);					
					}
					// now run the loop until the thread is canceled
					while (! monitor.isCanceled()) {
						ServiceDescriptor[] descriptors = registry.lookup
							(new String[] {IRmi.class.getName() }, ServiceRegistry.LOOKUP_REMOTE_ONLY);
						Logging.log(getClass(), "Found " + descriptors.length + " remote services.");
						RmiProxy p = new RmiProxy();
						p.setSourceID(new ReferenceID(SystemID.SYSTEM));
						for (int i = 0; i < descriptors.length; i++) {
							p.setTargetID(descriptors[i].getIdentifier());
							try {
								p.println("Hello World (Synchronous)!");
								p.printlnAsync("Hello World (Async)!");
								FutureResult future = p.printlnDef("Hello World (Deferred)!");
								Logging.log(getClass(), "Syncing ...");
								future.getResult();
								Logging.log(getClass(), "Synced.");
							} catch (InvocationException e) {
								Logging.error(getClass(), "Could not call print.", e);
							} 			
						}
						// wait for some time
						try {
							monitor.wait(PERIOD_CYCLE);
							Logging.log(getClass(), "Thread woke up.");
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Thread got interrupted.", e);					
						}
					}
				}
			}
		};
		NullMonitor monitor = new NullMonitor();
		b.performOperation(client, monitor);
		// sleep for some time and shutdown the system
		try {
			Thread.sleep(PERIOD_TIME);	
		} catch (InterruptedException e) {
			Logging.error(RmiMain.class, "Thread got interrupted.", e);
		}
		System.exit(0);
	}

	
	
	

}
