package info.pppc.base.system;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.ObjectStreamTranslator;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.IOperator;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.operation.OperationPool;
import info.pppc.base.system.util.Logging;

import java.util.Date;

/**
 * The invocation broker is responsible for synchronization, threading and performing remote
 * and local calls. The invocation broker currently uses three types of invocations to
 * perform and synchronize on remote invocations. The invoke type to perform a remote call, the
 * result type to retrieve a result and the remove type to remove the result from the table.
 * The invocation broker expects the to occur exactly once (!) for each invocation. Otherwise
 * it will generate error messages and you might end up with some dead entries in the tables
 * of the broker. However, the broker should never crash, even if you pass broken invocations.
 * 
 * With respect to application synchronization, the invocation broker supports three different
 * synchronizations: synchronous, asynchronous and deferred synchronous. For synchronous calls
 * the broker will not return before the call has finished. The result of the call will be 
 * contained in the result or exception variable of the invocation. For asynchronous calls,
 * the broker will return immediately and there is no way of retrieving any results that might
 * have been delivered already. Thus asynchronous calls have a one-way semantic. Deferred
 * synchronous calls will also return immediately. The result object of the invocation will be
 * a future result. If a caller tries to retrieve the exception or the result value of the
 * future result, the result will synchronize with the broker and the call will return as 
 * soon as the initial call has finished.
 * 
 * Note that this implementation has dependencies with the plug-in manager and
 * is especially fragile to broken semantic plug-in implementations. If you are implementing
 * a semantic plug-in, you should test your implementation with the toString method. After
 * performing all calls, this should print a string that says 0 incoming and 0 outgoing 
 * messages otherwise you will end up with some dead entries in the brokers table and you
 * will eventually run out of memory.
 * 
 * @author Marcus Handte
 */
public final class InvocationBroker implements IOperator {

	/**
	 * Registers the serializable types that are implemented and used by the BASE core.
	 */
	static {
		ObjectStreamTranslator.register(DeviceDescription.class.getName(), DeviceDescription.ABBREVIATION);
		ObjectStreamTranslator.register(Invocation.class.getName(), Invocation.ABBREVIATION);
		ObjectStreamTranslator.register(NFCollection.class.getName(), NFCollection.ABBREVIATION);
		ObjectStreamTranslator.register(NFDimension.class.getName(), NFDimension.ABBREVIATION);
		ObjectStreamTranslator.register(ObjectID.class.getName(), ObjectID.ABBREVIATION);
		ObjectStreamTranslator.register(PluginDescription.class.getName(), PluginDescription.ABBREVIATION);
		ObjectStreamTranslator.register(SystemID.class.getName(), SystemID.ABBREVIATION);
		ObjectStreamTranslator.register(ReferenceID.class.getName(), ReferenceID.ABBREVIATION);
		ObjectStreamTranslator.register(InvocationException.class.getName(), InvocationException.ABBREVIATION);
	}

	/**
	 * This property is used to derive the local system id upon startup.
	 * If the property is set to a long value, the local system id is
	 * set to the corresponding value, otherwise the system id is generated
	 * using the random number generator.
	 */
	public static final String PROPERTY_DEVICE_IDENTIFIER = "info.pppc.id";
	
	/**
	 * This is the name of the system property that provides the human readable device
	 * name. If the name contained in this system property exceeds 10 characters it is
	 * simply cut off after 10 chars.
	 */
	public static final String PROPERTY_DEVICE_NAME = "info.pppc.name";
	
	/**
	 * This is the name of the system property that provides the device type. The value
	 * specified in this property should be a short value and it should be one of the
	 * device type constants defined in the device description.
	 */
	public static final String PROPERTY_DEVICE_TYPE = "info.pppc.type";

	/**
	 * This is the name of the system property that describes the maximum number of
	 * threads. The value specified by this property must contain a non-negative
	 * integer. If no value is specified, unlimited (i.e., 0) is assumed.
	 */
	public static final String PROPERTY_THREAD_MAXIMUM = "info.pppc.tmax";
	
	/**
	 * This is the name of the system property that describes the default number of
	 * threads. The value specified by this property must contain a non-negative
	 * integer. If no value is specified 0 is assumed.
	 */
	public static final String PROPERTY_THREAD_DEFAULT = "info.pppc.tdef";
	
	/**
	 * The event constant that denotes that the broker is performing a shutdown.
	 * The source object will be the broker that performs the shutdown and the
	 * data object will be null.
	 */
	public static final int EVENT_BROKER_SHUTDOWN = 1;

	/**
	 * The next free and unused id for invocations. The invocation broker assigns locally
	 * unique identifiers to invocations in order to enable systems to perform matching between
	 * different invocations that might contain different contents. The assigner of an id
	 * is always (!) the system that first sent the message. Other systems send replies and
	 * other status messages using the same (!) id. Thus, the id together with the creator
	 * of the invocation are unique identifiers for one (!) call.
	 * Generation of invocation identifiers is synchronized to the class object of the broker.
	 */
	private static int INVOCATION_ID = Integer.MIN_VALUE;

	/**
	 * The machine's broker. At the present time this singleton is the only way of ensuring
	 * that each proxy generated in application space is capable of accessing the broker
	 * of the vm. If we would use a manual registration of proxies (programmed by the
	 * application developer) we could get rid of this global variable.
	 */
	private static InvocationBroker broker;
	
	/**
	 * Returns the invocation broker of this JVM. This is the preferred way to access the 
	 * internals of base directly. This is for instance used by proxies to access the 
	 * broker and to perform remote calls.
	 * 
	 * @return The only broker on this machine.
	 */
	public static InvocationBroker getInstance() {
		if (broker == null) {
			broker = new InvocationBroker();
		}
		return broker;
	}

	////////////////// broker instance implementation

	/**
	 * The device registry used to send remote requests.
	 */
	private DeviceRegistry deviceRegistry;
	
	/**
	 * The object registry used to send local requests.
	 */
	private ObjectRegistry objectRegistry;

	/**
	 * The plug-in manager that deals with remote calls.
	 */
	private PluginManager pluginManager;	

	/**
	 * The listener bundle that is used to listen for shutdown
	 * events.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * The pool that is used by the broker to schedule operations asynchronously.
	 * This value is initialized in the brokers constructor using the threading
	 * properties.
	 */
	private OperationPool pool;
	
	/**
	 * Creates a new invocation broker instance.
	 */
	private InvocationBroker() {
		Logging.debug(getClass(), "Broker started on system " + SystemID.SYSTEM + " ...");
		// retrieve the system properties for device name and type
		String n = System.getProperty(PROPERTY_DEVICE_NAME);
		if (n == null) {
			n = new Date().toString().substring(11, 19);
		}
		String type = System.getProperty(PROPERTY_DEVICE_TYPE);
		short t = DeviceDescription.TYPE_UNKOWN;
		if (type != null) {
			try {
				t = Short.parseShort(type);
			} catch (NumberFormatException e) {
				t = DeviceDescription.TYPE_UNKOWN;
			}
		}
		// create a new device description
		final DeviceDescription description = new DeviceDescription(SystemID.SYSTEM, n, t);
		// retrieve the system properties for threading
		int td = 0;
		String tDefault = System.getProperty(PROPERTY_THREAD_DEFAULT);
		if (tDefault != null) {
			try {
				td = Integer.parseInt(tDefault);
				if (td < 0) td = 0;
			} catch (NumberFormatException e) {	}			
		}
		int tm = 0;
		String tMaximum = System.getProperty(PROPERTY_THREAD_MAXIMUM);
		if (tMaximum != null) {
			try {
				tm = Integer.parseInt(tMaximum);
				if (tm < 0) tm = 0;
			} catch (NumberFormatException e) { }
		}
		// create a new thread pool using the configuration
		pool = new OperationPool(td, tm);
		// startup the local registries and managers
		objectRegistry = new ObjectRegistry();
		deviceRegistry = new DeviceRegistry(this);
		pluginManager = new PluginManager(this);
		// register device description at the local registry
		getDeviceRegistry().registerDevice(description);
		// create a listener that tracks the object registry for changes
		IListener listener = new IListener() {
			public void handleEvent(Event event) {
				switch (event.getType()) {
					case (ObjectRegistry.EVENT_KNOWN_ADDED):
						ObjectID added = (ObjectID)event.getData();
						description.addService(added);
						break;
					case (ObjectRegistry.EVENT_KNOWN_REMOVED):
						ObjectID removed = (ObjectID)event.getData();
						description.removeService(removed);
						break;
					default:
						// will never happen
				}
			}
		};
		// register already registered well known services
		ObjectID[] ids = getObjectRegistry().getKnownIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			description.addService(ids[i]);
		}
		// register the listener that updates the description
		getObjectRegistry().addObjectListener
			(ObjectRegistry.EVENT_KNOWN_ADDED 
				| ObjectRegistry.EVENT_KNOWN_REMOVED, listener);	
	}
	
	/**
 	 * Returns the device registry of this invocation broker.
 	 * 
 	 * @return The device registry of the broker.
 	 */
 	public DeviceRegistry getDeviceRegistry() {
 		return deviceRegistry;
 	}
 	
 	/**
 	 * Returns the local object registry of this invocation broker.
 	 * 
 	 * @return The object registry of the broker.
 	 */
 	public ObjectRegistry getObjectRegistry() {
  		return objectRegistry;
 	}

	/**
	 * Returns the plugin manager of this invocation broker.
	 * 
	 * @return The plugin manager of the broker.
	 */
	public PluginManager getPluginManager() {
		return pluginManager;
	}
	
	/**
	 * Called to perform a synchronous call. After the method
	 * returns the object stored in the result of the invocation
	 * will be a result.
	 * 
	 * @param invocation The invocation to perform synchronously.
	 */
	public void invoke(final Invocation invocation) {
		if (isValid(invocation)) {
			// update the invocation id
			synchronized (getClass()) {
				invocation.setID(new Integer(INVOCATION_ID));
				if (INVOCATION_ID < Integer.MAX_VALUE) {
					INVOCATION_ID += 1;
				} else {
					INVOCATION_ID = Integer.MIN_VALUE;
				}
			}
			pluginManager.sendSynchronous(invocation);
		}
	}
		
	/**
	 * Determines whether an invocation contains all necessary parts. If not
	 * the method attaches an exception and returns false.
	 * 
	 * @param invocation The invocation to check.
	 * @return True if the invocation can be executed, false otherwise.
	 */
	private boolean isValid(Invocation invocation) {
		ReferenceID source = invocation.getSource();
		ReferenceID target = invocation.getTarget();
		NFCollection requirements = invocation.getRequirements();
		if (source != null && target != null && source.getSystem() != null 
				&& target.getSystem() != null && requirements != null) {
			return true;
		} else {
			invocation.setException(new InvocationException("Received malformed invocation."));
			return false;
		}
	}
	
	/**
	 * Dispatches an invocation synchronously to an object.
	 * 
	 * @param invocation The invocation to dispatch.
	 * @param session The session used to receive the object.
	 */
	protected void dispatchSynchronous(Invocation invocation, ISession session) {
		ReferenceID rid = invocation.getTarget();
		if (rid == null) {
			invocation.setException(new InvocationException("Target reference is null."));
		} else {
			ObjectID id = rid.getObject();
			if (id == null) {
				invocation.setException(new InvocationException("Target object is null."));
			} else {
				IInvocationHandler handler = objectRegistry.getInvocationHandler(id);
				if (handler == null) {
					invocation.setException(new InvocationException("Target object not found."));
				} else {
					handler.invoke(invocation, session);
				}
			}
		}
	}
	
	/**
	 * Called by some plug-in or application to execute an operation. 
	 * The point in time when the operation is executed is defined by the 
	 * invocation broker that defines the global synchronization and threading
	 * policy. A developer should never create his own threads, instead
	 * he should use this method to execute something asynchronously.
	 * 
	 * @param operation The operation to executed by some free thread.
	 * @throws NullPointerException Thrown if the operation is null.
	 */
	public void performOperation(IOperation operation) {
		pool.performOperation(operation, new NullMonitor());
	}

	/**
	 * Called by some plug-in or application to execute an operation. 
	 * The point in time when the operation is executed is defined by the 
	 * invocation broker that defines the global synchronization and threading
	 * policy. A developer should never create his own threads, instead
	 * he should use this method to execute something asynchronously.
	 * A developer can pass a monitor to the method to interface with the
	 * operation and to observe its current status.
	 * 
	 * @param operation The operation to executed by some free thread.
	 * @param monitor The monitor used to interface with the operation. 
	 * @throws NullPointerException Thrown if the operation is null.
	 * 	If the monitor is null, the broker will create a default
	 * 	monitor.
	 */
	public synchronized void performOperation(final IOperation operation, final IMonitor monitor) {
		pool.performOperation(operation, monitor);
	}
	
	/**
	 * Adds a listener that listeners to certain types of events from the 
	 * broker. At the present time the only event that is issued by the 
	 * broker is the EVENT_BROKER_SHUTDOWN event that signals that the
	 * system is shutting down. 
	 * 
	 * @param types The types of events to register for.
	 * @param listener The listener to register.
	 */
	public void addBrokerListener(int types, IListener listener) {
		listeners.addListener(types, listener);	
	}
	
	/**
	 * Removes a previously registered broker listener from the set of
	 * broker listeners for the specified types of events.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed.
	 */
	public boolean removeBrokerListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Performs a shutdown of the broker. This will kill the device 
	 * registry remover thread and it will release all plug-ins of
	 * the plug-in manager.
	 */
	public void shutdown() {
		Logging.debug(getClass(), "Broker shutdown on system " + SystemID.SYSTEM + " ...");
		listeners.fireEvent(EVENT_BROKER_SHUTDOWN, true);
		pool.shutdown();
		Logging.debug(getClass(), "Broker shutdown complete.");
		broker = null;
	}
	

	
}