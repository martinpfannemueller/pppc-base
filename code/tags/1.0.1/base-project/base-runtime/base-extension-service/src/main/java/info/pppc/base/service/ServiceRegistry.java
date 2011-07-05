package info.pppc.base.service;

import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.IInvocationHandler;
import info.pppc.base.system.ObjectID;
import info.pppc.base.system.ObjectRegistry;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.ObjectStreamTranslator;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * This class is responsible for storing and searching available services.
 * Other system parts can monitor the state of all registered services by
 * adding service listeners. The service registry will signal changes to
 * its internal state (i.e. added and removed services) as well as changes
 * to the state of services (i.e. activated and passivated services).
 * 
 * @author Marcus Handte
 */
public final class ServiceRegistry implements IServiceRegistry { 	

	/**
	 * Registers all serializable types that are used by the
	 * BASE service registry.
	 */
	static {
		ObjectStreamTranslator.register(ServiceDescriptor.class.getName(), ServiceDescriptor.ABBREVIATION);
		ObjectStreamTranslator.register(ServiceProperties.class.getName(), ServiceProperties.ABBREVIATION);
	}
	
	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on the local device only.
	 */
	public static final int LOOKUP_LOCAL_ONLY = 1;

	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on the remote devices only.
	 */
	public static final int LOOKUP_REMOTE_ONLY = 2;
	
	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on both, local and remote devices.
	 */
	public static final int LOOKUP_BOTH = 4;

	/**
	 * This class is used to store all relevant data of a service
	 * that needs to be accessed at some later point in time. This
	 * includes the reference to the service, the service descriptor
	 * and the handler provided by the service.
	 * 
	 * @author Marcus Handte
	 */
	private class ServiceStorage {
		
		/**
		 * The service descriptor of the service.
		 */
		private ServiceDescriptor descriptor;
			
		/**
		 * The reference to the service.
		 */
		private Service service;
		
		/**
		 * Creates a new uninitialized service storage.
		 */
		public ServiceStorage() {
			super();
		}
		
		/**
		 * Returns the descriptor of the service.
		 * 
		 * @return The descriptor of the service.
		 */
		public ServiceDescriptor getDescriptor() {
			return descriptor;
		}

		/**
		 * Returns the reference to the service.
		 * 
		 * @return The reference to the service.
		 */
		public Service getService() {
			return service;
		}

		/**
		 * Sets the descriptor of the service.
		 * 
		 * @param descriptor The descriptor of the service.
		 */
		public void setDescriptor(ServiceDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		/**
		 * Sets the service implementation.
		 * 
		 * @param service The service implementation.
		 */
		public void setService(Service service) {
			this.service = service;
		}

	}
	
	/**
	 * This interceptor detects incoming calls that are dispatched
	 * to services registered by this registry. It will automatically
	 * update the active state of the registry whenever an incoming
	 * call is detected.
	 * 
	 * @author Marcus Handte
	 */
	private class ServiceInterceptor implements IInvocationHandler {

		/**
		 * The real invocation handler provided by the service.
		 */
		private IInvocationHandler handler;

		/**
		 * Creates a new interceptor that dispatches the call
		 * to the specified handler.
		 * 
		 * @param handler The handler that handles the call.
		 */
		public ServiceInterceptor(IInvocationHandler handler) {
			this.handler = handler;
		}

		/**
		 * Called whenever an incoming call should be dispatched.
		 * This method updates the active state of the registry.
		 * 
		 * @param invocation The invocation that needs to be 
		 * 	handled by the invocation handler.
		 * @param session The session used to receive the invocation.
		 */
		public void invoke(Invocation invocation, ISession session) {
			synchronized (activeServices) {
				activeServices[0] += 1;
				if (activeServices[0] == 1) {
					listeners.fireEvent(EVENT_SERVICE_ACTIVATED); 
				}
			}
			handler.invoke(invocation, session);
			synchronized (activeServices) {
				activeServices[0] -= 1;
				if (activeServices[0] == 0) {
					listeners.fireEvent(EVENT_SERVICE_PASSIVATED);
				}
			}
		}
	}

	/**
	 * The event constant that denotes that at least one service is
	 * now activated. The data object will be null and the source
	 * object will be the service registry.
	 */
	public static final int EVENT_SERVICE_ACTIVATED = 1;

	/**
	 * The event constant that denotes that at all services are now
	 * passivated. The data object of the event will be the id of the
	 * service and the source object will denote the registry.
	 */
	public static final int EVENT_SERVICE_PASSIVATED = 2;
	
	/**
	 * The event constant that denotes that a new service has been
	 * added. The data object of the event will be the id of the
	 * service and the source object will denote the registry.
	 */
	public static final int EVENT_SERVICE_ADDED = 4;
	
	/**
	 * The event constant that denotes that a service has been 
	 * removed. The data object of the event will be null and the
	 * source object will denote the registry.
	 */
	public static final int EVENT_SERVICE_REMOVED = 8;

	/**
	 * The constant that is used to create a unique name for anonymous
	 * services.
	 */
	private static final String ANNONYMOUS_SERVICE = "ANNONYMOUS";

	/**
	 * The local instance of the service registry.
	 */
	protected static ServiceRegistry instance;

	/**
	 * The listener bundle that holds the listeners that are registered
	 * for event emitted by the registry. At the present time the 
	 * registry signals changes to installed services and changes to 
	 * their activation state.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * The registry that contains devices and corresponding plug-ins.
	 * This is used to perform remote lookups.
	 */
	private DeviceRegistry deviceRegistry;
	
	/**
	 * The object registry that is used by the invocation broker to dispatch
	 * calls. This is used to register services.
	 */
	private ObjectRegistry objectRegistry;
	
	/**
	 * The vector that contains the service storages for all locally
	 * registered services.
	 */
	private Vector services = new Vector();
	
	/**
	 * The service listener that listens to state changes within the activation
	 * states of services.
	 */
	private IListener serviceListener = new IListener() {
		public void handleEvent(Event event) {
			synchronized (activeServices) {
				switch (event.getType()) {
					case Service.EVENT_SERVICE_ACTIVATED:
						activeServices[0] += 1;
						if (activeServices[0] == 1) {
							listeners.fireEvent(EVENT_SERVICE_ACTIVATED); 
						}
						break;
					case Service.EVENT_SERVICE_PASSIVATED:
						activeServices[0] -= 1;
						if (activeServices[0] == 0) {
							listeners.fireEvent(EVENT_SERVICE_PASSIVATED);
						}
						break;
					default:
						// will never happen
				}
			}
		}
	};
	
	/**
	 * The number of services that is currently activated. This value
	 * is increased and decreased according to the state of the 
	 * services and to the state of incoming calls.
	 */
	private int[] activeServices = new int[1];
	
	/**
	 * The thread limit for lookup threads. All service lookup requests
	 * from all clients will use at most the specified limit of threads.
	 * Increasing this value might decrease the lookup latency at the
	 * cost of a higher memory and thread usage.
	 */
	private int[] lookupThreads = new int[] { 4 };
	
	
	/**
	 * Creates a new registry and registers the registry at the object
	 * registry of the invocation broker.
	 * 
	 * @param broker The invocation broker instance of the registry.
	 */
	protected ServiceRegistry(InvocationBroker broker){
		objectRegistry = broker.getObjectRegistry();
		deviceRegistry = broker.getDeviceRegistry();
		ServiceRegistrySkeleton skeleton = new ServiceRegistrySkeleton();
		skeleton.setImplementation(this);
		objectRegistry.registerObject(IServiceRegistry.REGISTRY_ID, skeleton, this);
		broker.addBrokerListener(InvocationBroker.EVENT_BROKER_SHUTDOWN, new IListener() {
			public void handleEvent(Event event) {
				Logging.debug(getClass(), "Removing service registry due to broker shutdown.");
				synchronized (services) {
					while (! services.isEmpty()) {
						ServiceStorage store = (ServiceStorage)services.elementAt(0);
						ServiceDescriptor desc = store.getDescriptor();
						ObjectID id = desc.getIdentifier().getObject();
						release(id);
					}
				}   			
				objectRegistry.removeObject(IServiceRegistry.REGISTRY_ID);
				instance = null;
			}
		});
	}

	/**
	 * Creates, registers and returns the local instance of the service
	 * registry.
	 * 
	 * @return The local instance of the service registry.
	 */
	public static ServiceRegistry getInstance() {
		if (instance == null) {
			instance = new ServiceRegistry(InvocationBroker.getInstance());
		}
		return instance;
	}

	/**
	 * Determines whether one of the services hosted by this registry
	 * is active.
	 * 
	 * @return True if there exists one active service, false otherwise.
	 */
	public boolean isActive() {
		synchronized (activeServices) {
			return activeServices[0] != 0;
		}
	}

	/**
	 * Adds a service listener to the registry. The service listener
	 * can be added for service additions and removals (EVENT_SERVICE_ADDED,
	 * EVENT_SERVICE_REMOVED) and for state changes within all services
	 * (EVENT_SERVICE_ACTIVATED, EVENT_SERVICE_PASSIVATED). 
	 * 
	 * @param types The types of events to register for.
	 * @param listener The listener to register.
	 */
	public void addServiceListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}

	/**
	 * Removes a previously registered event listener from the set of 
	 * registered listeners for the specified event types.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been unregistered.
	 */
	public boolean removeServiceListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Exports the specified service, if its name is null, the name will be
	 * auto generated. If the handler or the implementation is null a null 
	 * pointer exception is raised. The service interfaces and properties
	 * a checked for compliance and they are adapted to default values if
	 * they are null or contain null values.
	 * 
	 * @param serviceName The name of the service.
	 * @param properties The properties of the service, if null set to an
	 * 	empty set of properties.
	 * @param serviceInterfaces The interfaces exported by the service, if
	 * 	null replaced by an empty array, null values are removed from the 
	 * 	array.
	 * @param service The service implementation (not null).
	 * @param handler The invocation handler of the service (not null).
	 * @return The object id of the exported service.
	 * @throws ServiceRegistryException If a service name is passed to this method,
	 * 	an exception will be thrown if the specified service name is already
	 * 	in use. If the service name is null, this method will never throw a
	 * 	registry exception.
	 */
	public ObjectID export(String serviceName, String[] serviceInterfaces, ServiceProperties properties, 
		IInvocationHandler handler, Service service) throws ServiceRegistryException {			
		// perform compliance check on service handler
		if (handler == null) {
			throw new NullPointerException("Invocation handler must not be null.");
		}
		// perform compliance check on service implementation
		if (service == null) {
			throw new NullPointerException("Service implementation must not be null.");
		}
		// perform compliance check on service properties
		if (properties == null) {
			properties = new ServiceProperties();
		}		
		// perform compliance check on service interfaces
		Vector interfaces = new Vector();
		if (serviceInterfaces != null) {
			for (int i = serviceInterfaces.length - 1; i >= 0; i--) {
				if (serviceInterfaces[i] != null) {
					interfaces.addElement(serviceInterfaces[i]);
				}
			}
		}
		serviceInterfaces = new String[interfaces.size()];
		for (int i = interfaces.size() - 1; i >= 0; i--) {
			serviceInterfaces[i] = (String)interfaces.elementAt(i);
		}
		synchronized (services) {
			// perform compliance check on service name, generate if neccessary
			Vector names = new Vector();
			for (int i = services.size() - 1; i >= 0; i--) {
				ServiceStorage store = (ServiceStorage)services.elementAt(i);
				ServiceDescriptor d = (ServiceDescriptor)store.getDescriptor();
				names.addElement(d.getName());
			}
			if (serviceName == null) {
				long time = System.currentTimeMillis();
				serviceName = ANNONYMOUS_SERVICE + time;
				while (names.contains(serviceName)) {
					time += 1;
					serviceName = ANNONYMOUS_SERVICE + time;
				}
			}
			if (names.contains(serviceName)) {
				throw new ServiceRegistryException("Service name already in use.");
			}
			// add service status listener and update registry state
			synchronized (activeServices) {
				if (service.isActive()) {
					activeServices[0] += 1;
					if (activeServices[0] == 1) {
						listeners.fireEvent(EVENT_SERVICE_ACTIVATED);
					}
				}
				service.addServiceListener(Service.EVENT_SERVICE_ACTIVATED |
					Service.EVENT_SERVICE_PASSIVATED, serviceListener);
			}
			// create activity interceptor 
			ServiceInterceptor interceptor = new ServiceInterceptor(handler);
			// perform registration			
			ObjectID id = objectRegistry.registerObject(interceptor);
			// create service storage entry and descriptor
			ServiceDescriptor desc = new ServiceDescriptor();
			desc.setName(serviceName);
			desc.setInterfaces(serviceInterfaces);
			desc.setProperties(properties);
			desc.setIdentifier(new ReferenceID(SystemID.SYSTEM, id));
			ServiceStorage storage = new ServiceStorage();
			storage.setDescriptor(desc);
			storage.setService(service);
			services.addElement(storage);
			listeners.fireEvent(EVENT_SERVICE_ADDED, id);
			return id;
		}
	}
	
	/**
	 * Exports the specified service as an anonymous service.
	 * 
	 * @param serviceInterface The service interfaces exported by the service.
	 * @param handler The invocation handler of the service.
	 * @param service The service implementation.
	 * @return The object id of the registered service.
	 */
	public ObjectID export(String[] serviceInterface, IInvocationHandler handler, Service service) {
		try {
			return export(null, serviceInterface, null, handler, service);
		} catch (ServiceRegistryException e) {
			// this will never happen
			throw new RuntimeException("Could not register annonymous service.");
		}
	}

	/**
	 * Exports the specified service as an anonymous service.
	 * 
	 * @param serviceInterface The interfaces exported by the service.
	 * @param handler The handler of the service.
	 * @param properties The service properties of the service.
	 * @param service The service implementation.
	 * @return The object id of the registered service.
	 */
	public ObjectID export(String[] serviceInterface, ServiceProperties properties, 
		IInvocationHandler handler, Service service) {
		try {
			return export(null, serviceInterface, properties, handler, service);	
		} catch (ServiceRegistryException e) {
			// this will never happen
			throw new RuntimeException("Could not register annonymous service.");
		}
	}

	/**
	 * Releases a previously registered service from the registry.
	 * 
	 * @param id The id of the service.
	 * @return True if the service has been removed, false if the 
	 * 	service was not registered.
	 */
	public boolean release(ObjectID id) {
		synchronized (services) {
			for (int i = services.size() - 1; i >= 0; i--) {
				ServiceStorage store = (ServiceStorage)services.elementAt(i);
				ServiceDescriptor d = store.getDescriptor();
				ObjectID sid = d.getIdentifier().getObject();
				if (sid.equals(id)) {
					// found service, unregister it
					objectRegistry.removeObject(id);
					services.removeElementAt(i);
					synchronized (activeServices) {
						if (store.getService().isActive()) {
							activeServices[0] -= 1;
							if (activeServices[0] == 0) {
								listeners.fireEvent(EVENT_SERVICE_PASSIVATED);
							}
						}
						store.getService().removeServiceListener
							(Service.EVENT_SERVICE_ACTIVATED | Service.EVENT_SERVICE_PASSIVATED, serviceListener);						
					}
					listeners.fireEvent(EVENT_SERVICE_REMOVED, id);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Returns all services within the specified range, regardless of their
	 * interface, name or properties.
	 * 
	 * @param range The range (remote or local) for the lookup.
	 * @return An array of service descriptors that contains all available
	 * 	services.
	 */
	public ServiceDescriptor[] lookup(int range) {
		return wrap(lookup(null, null, null, range));
	}

	/**
	 * Returns the service descriptors within the specified range that
	 * contain the specified properties.
	 * 
	 * @param properties The properties to lookup.
	 * @param range The range to lookup.
	 * @return The services whose service descriptor matches the 
	 * 	specified properties within the specified range.
	 */
	public ServiceDescriptor[] lookup(ServiceProperties properties, int range) {
		return wrap(lookup(null, null, properties, range));
	}

	/**
	 * Returns an array of service descriptors that contains services 
	 * with the specified name within the specified range.
	 * 
	 * @param serviceName The name of the service to find.
	 * @param range The range to lookup.
	 * @return An array of matching service descriptors.
	 */
	public ServiceDescriptor[] lookup(String serviceName, int range) {
		return wrap(lookup(serviceName, null, null, range));
	}

	/**
	 * Returns an array of service descriptors found within the specified range
	 * whose name and properties match the request.
	 * 
	 * @param serviceName The name of the service.
	 * @param properties The properties of the service.
	 * @param range The range to lookup.
	 * @return An array of matching service descriptors.
	 */
	public ServiceDescriptor[] lookup(String serviceName, ServiceProperties properties, int range) {
		return wrap(lookup(serviceName, null, properties, range));
	}

	/**
	 * Returns an array of service descriptors found within the specified range
	 * whose service interfaces match the request.
	 * 
	 * @param serviceInterfaces The interfaces to lookup.
	 * @param range The range to lookup.
	 * @return An array of descriptor that match the request.
	 */
	public ServiceDescriptor[] lookup(String[] serviceInterfaces, int range) {
		return wrap(lookup(null, serviceInterfaces, null, range));
	}

	/**
	 * Returns an array of service descriptors found within the specified range
	 * that provide the specified interfaces and contain the specified properites.
	 * 
	 * @param serviceInterfaces The service interfaces to find.
	 * @param properties The properties to find.
	 * @param range The search range.
	 * @return The service descriptors that match the request.
	 */
	public ServiceDescriptor[] lookup(String[] serviceInterfaces, ServiceProperties properties, int range) {
		return wrap(lookup(null, serviceInterfaces, properties, range));
	}

	/**
	 * Returns a vector of services found within the specified range.
	 * 
	 * @param name The name of the service, or null if any name should be
	 * 	returned.
	 * @param interfaces The interfaces provided by the service, or null if
	 * 	any interface should be returned.
	 * @param properties The properties specified by the service, or null if
	 * 	any property should be returned.
	 * @param range The range of the lookup, either local, remote or both.
	 * @return A vector that contains all services found within the specified
	 * 	range.
	 */
	public Vector lookup(final String name, final String[] interfaces, final ServiceProperties properties, int range) {
		final Vector result = new Vector();
		// search within local registry
		if (range == LOOKUP_LOCAL_ONLY || range == LOOKUP_BOTH) {
			synchronized (services) {
				descriptors: for (int i = services.size() - 1; i >= 0; i--) {
					ServiceStorage store = (ServiceStorage)services.elementAt(i);
					ServiceDescriptor d = store.getDescriptor();
					// perform name search
					if (name != null && ! name.equals(d.getName())) {
						// name does not match
						continue descriptors;
					}
					// perform interface search
					if (interfaces != null) {
						String[] serviceInterfaces = d.getInterfaces();
						interfaces: for (int j = interfaces.length - 1; j >= 0; j--) {
							for (int k = serviceInterfaces.length - 1; k >= 0; k--) {
								if (interfaces[j].equals(serviceInterfaces[k])) {
									// interface j found as interface k
									continue interfaces;	
								}					
							}
							// interface j not found
							continue descriptors;
						}
					}
					// perform property match
					if (properties != null) {
						ServiceProperties p = d.getProperties();
						if (! p.contains(properties)) {
							// properties not met by service
							continue descriptors;
						}
					}
					// service descriptor matches all query parameters
					result.addElement(d);				
				}
			}			
		}
		// search within remote registries
		if (range == LOOKUP_REMOTE_ONLY || range == LOOKUP_BOTH) {
			// perform asynchronous lookup operations
			final SystemID[] devices = deviceRegistry.getRemoteDevices(IServiceRegistry.REGISTRY_ID);
			final NullMonitor[] monitors = new NullMonitor[devices.length];
			for (int i = devices.length - 1; i >= 0; i--) {	
				final int id = i;
				IOperation lookup = new IOperation() {
					public void perform(IMonitor monitor) throws Exception {
						try {
							ServiceRegistryProxy registry = new ServiceRegistryProxy();
							registry.setSourceID(new ReferenceID(SystemID.SYSTEM, IServiceRegistry.REGISTRY_ID));
							registry.setTargetID(new ReferenceID(devices[id], IServiceRegistry.REGISTRY_ID));
							Vector services = registry.lookup(name, interfaces, properties, LOOKUP_LOCAL_ONLY);
							for (int j = services.size() - 1; j >= 0; j--) {
								result.addElement(services.elementAt(j));
							}
						} catch (InvocationException e) {
							Logging.debug(getClass(), "Could not contact registry on device " + devices[id]);
						} finally {
							synchronized (lookupThreads) {
								lookupThreads[0] += 1;
								lookupThreads.notifyAll();
							}
						}
					}
				};
				monitors[id] = new NullMonitor();
				// acquire semaphore before executing the thread.
				synchronized (lookupThreads) {
					while (lookupThreads[0] <= 0) {
						try {
							lookupThreads.wait();	
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Lookup thread got interrupted.", e);
							return new Vector();	
						}
					}
					lookupThreads[0] -= 1;
				}
				InvocationBroker.getInstance().performOperation(lookup, monitors[id]);
			}
			// barrier for monitors
			for (int i = monitors.length - 1; i >= 0; i--) {
				try {
					monitors[i].join();	
				} catch (InterruptedException e) {
					Logging.error(getClass(), "Thread got interrupted.", e);
				}
			}
		}
		// return result set (never null)
		return result;
	}
	
	/**
	 * Returns an array of service descriptors contained in the vector.
	 * 
	 * @param descriptors The vector that contains the descriptors.
	 * @return An array of descriptors contained in the vector.
	 */
	private ServiceDescriptor[] wrap(Vector descriptors) {
		if (descriptors == null) {
			return new ServiceDescriptor[0];
		} else {
			ServiceDescriptor[] result = new ServiceDescriptor[descriptors.size()];
			for (int i = result.length - 1; i >= 0; i--) {
				result[i] = (ServiceDescriptor)descriptors.elementAt(i);
			}
			return result;
		}
	}
	
}



