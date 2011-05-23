/*
 * Revision: $Revision: 1.15 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2007/08/29 13:52:51 $ 
 */
package info.pppc.base.lease;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.io.ObjectStreamTranslator;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;

/**
 * The lease registry can be used to observe objects on another system
 * using local listeners. The lease registry tries to bundle lease 
 * update requests and thus it is more efficient than to establish single
 * update protocols that work independently. In order to leverage the
 * registries, the server system will create a lease with a specified
 * timeout and it will hook up a listener to this lease. The registry
 * will return a lease that can be passed to a remote system. The remote
 * system in turn will hook up a listener to the lease which will start
 * the update protocol between the lease hosting and the lease observing
 * system. If the last observing listener is removed, the system can
 * immediately notify the remote system that the lease is no longer needed
 * on this system. If no more listeners are registered or if no update
 * has been issued by a system for the specified timeout period all 
 * listeners (on the lease hosting as well as on the lease observing
 * system) will be removed and they will receive a lease expired event.
 * 
 * Note that it is perfectly allowed to register multiple observing 
 * listeners on the same system for the same lease and it is also possible
 * to observe the same lease from multiple systems. In this case the
 * hosting listner will not receive an expired event before all listners
 * have been removed. 
 * 
 * Note that the lease registry does not perform any cycle checks, thus
 * it is possible to create cyclic referencing leases which will eventually
 * lead to a memory hole. Thus users of the registry should ensure that
 * they always maintain DAGs if they use the registry to maintain remote
 * state. 
 * 
 * @author Mac
 */
public class LeaseRegistry implements IOperation, ILeaseRegistry {

	/**
	 * Registers the serializable classes used by the BASE lease
	 * registry.
	 */
	static {
		ObjectStreamTranslator.register(Lease.class.getName(), Lease.ABBREVIATION);
	}
	
	/**
	 * The storage is a storage for a single lease. It maintains
	 * the listeners for a certain lease.
	 * 
	 * @author Mac
	 */
	private class Storage {
	
		/**
		 * The storage for listeners that listen to the stored lease. 
		 */
		private Vector listeners = new Vector(1);
	
		/**
		 * The lease that is maintained by this storage.
		 */
		private Lease lease = null;
	
		/**
		 * Creates a new lease storage for the specified lease. 
		 * 
		 * @param lease The lease of the storage. The lease must
		 * 	not be null.
		 * @throws NullPointerException Thrown if the lease is null.
		 */
		public Storage(Lease lease) {
			if (lease == null) 
				throw new NullPointerException("Lease must not be null.");
			this.lease = lease;
		}
	
		/**
		 * Returns the lease of the storage.
		 * 
		 * @return The lease of the storage.
		 */
		public Lease getLease() {
			return lease;
		}
	
		/**
		 * Adds a listener to the storage.
		 * 
		 * @param listener The listener to register.
		 */
		public void addListener(IListener listener) {
			listeners.addElement(listener);
		}
	
		/**
		 * Removes a listener from the storage.
		 *
		 * @param listener The listener that should be removed.
		 * @return True if the listener has been removed.
		 */
		public boolean removeListener(IListener listener) {
			return listeners.removeElement(listener);
		}

		/**
		 * Determines whether there are any listeners registered
		 * at this storage.
		 * 
		 * @return True if the storage has any listeners registered.
		 */
		public boolean hasListeners() {
			return (! listeners.isEmpty());
		}
	
		/**
		 * Notifies all registered listeners about the occurance
		 * of a certain event.
		 * 
		 * @param event The event that should be sent to all
		 * 	listners.
		 */
		public void notifyListeners(Event event) {
			for (int i = 0, s = listeners.size(); i < s; i++) {
				IListener listener = (IListener)listeners.elementAt(i);
				listener.handleEvent(event);	
			}
		}

		/**
		 * Determines whether the object equals the lease storage.
		 * This comparison is based solely on the lease that is 
		 * maintained by this lease storage.
		 * 
		 * @param o The object to compare to.
		 * @return True if the lease storage equals the object.
		 */
		public boolean equals(Object o) {
			if (o != null && o.getClass() == getClass()) {
				Storage s = (Storage)o;
				return lease.equals(s.lease);			
			}
			return false;
		}


		/**
		 * Returns the content-based (lease-based) hashcode.
		 * 
		 * @return The hashcode of the storage.
		 */
		public int hashCode() {
			return lease.hashCode();
		}
	}

	/**
	 * The local storage is used to store locally created leases.
	 * It maintains a list of systems that must be notified if
	 * the lease is removed.
	 * 
	 * @author Mac
	 */
	private final class LeaseStorage extends Storage {
	
		/**
		 * This vector contains object arrays of length 2. The first
		 * index denotes the system id of a certain system that has
		 * sent an update and the second index denotes an integer that
		 * denotes the last point in time when the system has been
		 * updated. Each system is inserted exactly once in the vector
		 * and the vector is ordered by timestamps in an descending
		 * manner.
		 */
		private Vector systems = new Vector();
	
		/**
		 * This is the point in time when the lease bound to the
		 * storage will expire.
		 */
		private long expiration = 0;
	
		/**
		 * The local storage represents a locally created lease that
		 * is refreshed by some remote system.
		 * 
		 * @param lease The lease that is observed by the registry.
		 * @param expiration The expiration time when this lease will
		 * 	expire (as local millis).
		 */
		public LeaseStorage(Lease lease, long expiration) {
			super(lease);
			this.expiration = expiration;
		}

		/**
		 * Returns the expiration time (as local time).
		 * 
		 * @return The expiration time of the lease.
		 */
		public long getExpiration() {
			return expiration;
		}
	
		/**
		 * Updates the expiration date of the specified system to
		 * and sets it to the new expiration date based on the timeout
		 * and the current system time. This method will also recalculate
		 * the expiration date of the whole storage entry.
		 * 
		 * @param system The system that should be updated.
		 */
		public void updateSystem(SystemID system) {
			// calculate the new expiration date
			long timeout = getLease().getTimeout();
			long now = System.currentTimeMillis();
			expiration = now + timeout;
			// try to find the system in the existing entries
			for (int i = systems.size() - 1; i >= 0; i--) {
				Object[] o = (Object[])systems.elementAt(i);
				SystemID sys = (SystemID)o[0];
				if (sys.equals(system)) {
					systems.removeElementAt(i);
					o[1] = new Long(expiration);
					systems.insertElementAt(o, 0);
					return;
				} 
			}
			// add the system as it is not part of the queue 
			Object[] o = new Object[2];
			o[0] = system;
			o[1] = new Long(expiration);
			systems.insertElementAt(o, 0);
		}
	
		/**
		 * Removes a certain system from the chain of sytems.
		 * 
		 * @param system The system that should be removed.
		 */
		public void removeSystem(SystemID system) {
			// find the corresponding system and remove it
			for (int i = systems.size() - 1 ; i >= 0; i--) {
				Object[] o = (Object[])systems.elementAt(i);
				SystemID sys = (SystemID)o[0];
				if (sys.equals(system)) {
					systems.removeElementAt(i);
					return;
				}
			}
		}
	
		/**
		 * Returns the systems that have not been expired so far.
		 * All systems that have been expired will be removed.
		 * 
		 * @return The systems that have not expired so far.
		 */
		public SystemID[] getSystems() {
			long now = System.currentTimeMillis();
			// fill in the result vector 
			Vector result = new Vector();
			// run through systems and prune 
			for (int i = systems.size() - 1; i >= 0; i--) {
				Object[] o = (Object[])systems.elementAt(i);
				Long exp = (Long)o[1];
				if (exp.longValue() < now) {
					// prune expired system
					systems.removeElementAt(i);	
				} else {
					// add non-expired system
					result.insertElementAt(o[0], 0);	
				}
			}
			// wrap results
			SystemID[] ids = new SystemID[result.size()];
			for (int i = result.size() - 1; i >= 0; i--) {
				ids[i] = (SystemID)result.elementAt(i);
			}
			return ids;
		}
	
	}

	/**
	 * The observer storage represents a lease on a remote system
	 * that is observed by this system.
	 * 
	 * @author Mac
	 */
	private final class QueueStorage extends Storage {

		/**
		 * The time at which the next update should take place.
		 */
		private long expiration = 0;

		/**
		 * Creates a new observer storage for the specified lease
		 * with the specified initial update length.
		 * 
		 * @param lease The lease that is observed.
		 * @param expiration The next expiration time.
		 */
		public QueueStorage(Lease lease, long expiration) {
			super(lease);
			this.expiration = expiration;
		}
	
		/**
		 * Returns the expiration time of the observer (as 
		 * local time millis).
		 * 
		 * @return The expiraton time of the observer.
		 */
		public long getExpiration() {
			return expiration;
		}
	
		/**
		 * Sets the expiration time of the observer storage to
		 * the specified point in time.
		 * 
		 * @param expiration The expiration of the storage.
		 */
		public void setExpiration(long expiration) {
			this.expiration = expiration;
		}
	}



	/**
	 * The lease queue is a queue that is used to refresh the leases
	 * of a certain system.
	 * 
	 * @author Mac
	 */
	private final class LeaseQueue implements IOperation {
	
		/**
		 * The monitor used to control the queue's signaling operation.
		 */
		private NullMonitor monitor = new NullMonitor();
	
		/**
		 * The system represented by the queue.
		 */
		private SystemID system;
	
		/**
		 * The registry proxy that points to the remote
		 * system of the queue.
		 */
		private ILeaseRegistry registry;
	
		/**
		 * The vector that stores the entries of the queue.
		 */
		private Vector queue = new Vector();
	
		/**
		 * Creates a new lease queue for the specified system.
		 * 
		 * @param system The system of the queue.
		 */
		public LeaseQueue(SystemID system) {
			this.system = system;
			LeaseRegistryProxy proxy = new LeaseRegistryProxy();
			proxy.setTargetID(new ReferenceID(system, ILeaseRegistry.REGISTRY_ID));
			proxy.setSourceID(new ReferenceID(SystemID.SYSTEM, ILeaseRegistry.REGISTRY_ID));
			proxy.setGateway(true);
			registry = proxy; 
		}
			
		/**
		 * Returns the monitor for the lease queue.
		 * 
		 * @return The monitor of the queue.
		 */
		public IMonitor getMonitor() {
			return monitor;
		}
			
		/**
		 * Inserts a queue storage entry ordered into the queue.
		 * 
		 * @param storage The storage that should be inserted.
		 */
		public void insert(QueueStorage storage) {
			synchronized (queue) {
				long expireNew = storage.getExpiration();
				for (int i = 0, s = queue.size(); i < s; i++) {
					QueueStorage store = (QueueStorage)queue.elementAt(i);
					long expireOld = store.getExpiration();
					if (expireNew < expireOld) {
						queue.insertElementAt(storage, i);
						queue.notify();
						return;
					}
				}
				queue.addElement(storage);
				queue.notify();				
			}
		}
		
		/**
		 * Removes the storage for a certain lease from the queue.
		 * If the lease does not have a storage, the method will
		 * return null, otherwise it will return the storage.
		 * 
		 * @param lease The lease whose storage should be looked up.
		 * @return The queue storage for the lease or null if there
		 * 	is no such storage.
		 */
		public QueueStorage remove(Lease lease) {
			synchronized (queue) {
				for (int i = 0, s = queue.size(); i < s; i++) {
					QueueStorage store = (QueueStorage)queue.elementAt(i);
					if (store.getLease().equals(lease)) {
						queue.removeElementAt(i);
						return store;	
					}
				}
				return null;				
			}
		}

	
		/**
		 * Performs the event notification and the 
		 * remote calls for a specific system. 
		 * 
		 * @param monitor The monitor that is used to 
		 * 	cancel the operation.
		 */
		public void perform(IMonitor monitor) {
			while (! monitor.isCanceled()) {
				// determine whether queue can shutdown
				synchronized (remote) {
					if (queue.isEmpty()) {
						Logging.debug(getClass(), "Stopping empty lease queue for system " + system + ".");
						remote.remove(system);
						monitor.cancel();
						continue;
					}
				}
				// collect storage ids for refresh or wait for next refresh
				Vector leases = new Vector();				
				synchronized (queue) {
					QueueStorage storage = (QueueStorage)queue.elementAt(0);
					long now = System.currentTimeMillis();
					long refresh = now + PERIOD_REFRESH;
					if (storage.getExpiration() > refresh) {
						try {
							queue.wait(storage.getExpiration() - refresh);	
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Lease refresh got interrupted.", e);
						}
						continue;
					} else {
						Lease lease = storage.getLease();
						long period = lease.getTimeout();
						leases.addElement(lease);
						for (int i = 1, s = queue.size(); i < s; i++) {
							storage = (QueueStorage)queue.elementAt(i);
							lease = storage.getLease();
							if (storage.getExpiration() <= refresh) {
								// adjust newly inserted leases
								if (storage.getExpiration() == 0) {
									storage.setExpiration(now + lease.getTimeout());								
								}
								leases.addElement(lease);
							} else if (lease.getTimeout() <= period + PERIOD_REFRESH &&
								lease.getTimeout() >= period - PERIOD_REFRESH) { 
								leases.addElement(lease);
							}
						}
					}
				}
				long now = System.currentTimeMillis();
				// perform refresh and update storage entries
				try {
					//Logging.debug(getClass(), "Performing lease refresh on " + system 
					//	+ " with " + leases.size() + " leases.");
					Vector remove = registry.update(SystemID.SYSTEM, leases);
					// remove storages whose lease has timed out remotely
					for (int i = remove.size() - 1; i >= 0; i--) {
						Lease lease = (Lease)remove.elementAt(i);
						synchronized (queue) {
							QueueStorage storage = remove(lease);
							if (storage != null) {
								Event e = new Event
									(EVENT_LEASE_EXPIRED, LeaseRegistry.this, 
										lease, false);
								storage.notifyListeners(e);								
							}
						}
					}
					// perform lease refresh on updated leases
					for (int i = 0, s = leases.size(); i < s; i++) {
						Lease lease = (Lease)leases.elementAt(i);
						synchronized (queue) {
							QueueStorage storage = remove(lease);
							if (storage != null) {
								storage.setExpiration(now + lease.getTimeout());
								insert(storage);
							}
						}
					}
				} catch (InvocationException e) {
					//Logging.error(getClass(), "Lease extension failed on " + system + ".", e);
					Logging.debug(getClass(), "Lease extension failed on " + system + ".");
					try {
						synchronized (queue) {
							queue.wait(PERIOD_WAIT);	
						}
					} catch (InterruptedException ix) {
						Logging.error(getClass(), "Thread got interrupted.", e);
					}
				} catch (Throwable t) {
					Logging.error(getClass(), "Caught unexpected runtime exception.", t);
				}
				// expire expired leases that are not initial (0) but timed out
				synchronized (queue) {
					for (int i = 0; i < queue.size(); i++) {
						QueueStorage storage = (QueueStorage)queue.elementAt(i);
						long timeout = storage.getExpiration();
						if (timeout == 0) {
							continue;
						} else if (timeout < now) {
							Lease lease = storage.getLease();
							queue.removeElementAt(i);
							i -= 1;
							Logging.debug(getClass(), "Removing remote lease " + lease 
								+ " on " + system + " due to timeout.");
							Event e = new Event(EVENT_LEASE_EXPIRED, 
									LeaseRegistry.this, lease, false);
							storage.notifyListeners(e);
						}
					}
				}
			}
			// cleanup on shutdown
			synchronized (queue) {
				while (!queue.isEmpty()) {
					QueueStorage storage = (QueueStorage)queue.elementAt(0);
					Lease lease = storage.getLease();
					queue.removeElementAt(0);
					Logging.debug(getClass(), "Removing remote lease " + lease 
						+ " on " + system + " due to forced shutdown.");
					Event e = new Event(EVENT_LEASE_EXPIRED, LeaseRegistry.this,
						lease, false);
					storage.notifyListeners(e);
				}
			}
		}
	}
	
	/**
	 * The minimum timeout period. If an observer or lease is registered
	 * with a smaller timeout period, the timeout period is automatically
	 * set to the minimum period.
	 */
	public static final long PERIOD_MINIMUM = 5000;
		
	/**
	 * The default timeout period. If an observer or lease is registered
	 * without a timeout period, this period is automatically taken.
	 */
	public static final long PERIOD_DEFAULT = 15000;
		
	/**
	 * The maximum timeout period. If an observer or lease is registered
	 * with a timeout period that is larger than the maximum period, the
	 * maximum period is automatically taken. 
	 */
	public static final long PERIOD_MAXIMUM = 60000;
	
	/**
	 * This is the grace period that is automatically added to the timeout
	 * period whenever lease is registered for the first time. This will
	 * enable a remote system to register its
	 */
	public static final long PERIOD_GRACE = 5000;
	
	/**
	 * The estimated period required to perform a refresh call. This
	 * is used to determine the set of lease updates that need to be
	 * performed during the next call.
	 */
	public static final long PERIOD_REFRESH = 5000;

	/**
	 * This is the time that lies between two calls to a system when
	 * the first call produced an invocation exception. This should
	 * be smaller than the refresh period if multiple refresh calls
	 * should be emittet whenever a call fails.
	 */
	public static final long PERIOD_WAIT = 1000;

	/**
	 * This event is fired whenever the lease that is observed by a
	 * listener is expired. This might either occur if the remove
	 * method is called and the corresponding registered listeners
	 * are notified or if the timeout for a certain lease is expired.
	 * After this event has been issued, the listener will no longer
	 * be registered at the registry. The source of the event will
	 * be the lease registry. The data object will be the lease that
	 * has expired.
	 */
	public static final int EVENT_LEASE_EXPIRED = 1;

	/**
	 * The local instance of the lease registry.
	 */
	protected static LeaseRegistry instance;

	/**
	 * The invocation broker of the lease registry.
	 */
	private InvocationBroker broker;

	/**
	 * The remote lease queues hashed by system id.
	 */
	private Hashtable remote = new Hashtable();
	
	/**
	 * The local lease storages ordered by the time they will 
	 * expire.
	 */
	private Vector local = new Vector();

	/**
	 * Creates a new lease registry for the specified invocation broker.
	 * 
	 * @param ibroker The invocation broker of this lease registry.
	 */	
	protected LeaseRegistry(InvocationBroker ibroker) {
		broker = ibroker;
		final NullMonitor monitor = new NullMonitor();
		broker.addBrokerListener
			(InvocationBroker.EVENT_BROKER_SHUTDOWN, 
				new IListener() {
					public void handleEvent(Event event) {
						Logging.debug(getClass(), "Removing lease registry due to broker shutdown.");
						synchronized (local) {
							monitor.cancel();
							local.notifyAll();
						}
						try {
							monitor.join();	
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Thread got interrupted.", e);	
						}
						broker.getObjectRegistry().removeObject(ILeaseRegistry.REGISTRY_ID);
						instance = null;
					}
				});
		LeaseRegistrySkeleton skeleton = new LeaseRegistrySkeleton();
		skeleton.setImplementation(this);
		broker.getObjectRegistry().registerObject(ILeaseRegistry.REGISTRY_ID, skeleton, this);
		broker.performOperation(this, monitor);
	}

	/**
	 * Creates, registers and returns the local instance of the lease 
	 * registry.
	 * 
	 * @return The instance of the local lease registry.
	 */
	public static LeaseRegistry getInstance() {
		if (instance == null) {
			instance = new LeaseRegistry(InvocationBroker.getInstance());
		}
		return instance;
	}


	/**
	 * Creates a new local lease with the default timeout period.
	 * When the lease expires, the listener will be informed by
	 * the corresponding event. If the lease is removed by a call
	 * to the remove method, the listener will be removed silently.
	 * After the lease has been expired, it is no longer registered
	 * at the registry.
	 * 
	 * @param listener The listener that is notified whenever the
	 * 	lease is expired. The listener must not be null.
	 * @return The lease that has been created. This lease can be 
	 * 	passed to remote systems so that they can hook an observing
	 * 	listener.
	 */
	public Lease create(IListener listener) {
		return create(listener, PERIOD_DEFAULT);
	}

	/**
	 * Creates a new local lease with the specified timeout period.
	 * If the period lies not between the maximum and minimum period,
	 * it will be adjusted automatically. Whenever the lease expires,
	 * the listener will receive a corresponding event. If the lease
	 * is manually removed by the remove method, the listener will be
	 * removed silently.
	 * 
	 * @param listener The listener that is called whenever the lease
	 * 	is expired. The listener must not be null.
	 * @param timeout The timeout of the lease.
	 * @return The lease that has been created. This lease can be 
	 * 	passed to remote systems so that they can hook up an observing
	 * 	listener.
	 */
	public Lease create(IListener listener, long timeout) {
		// adjust timeout
		if (timeout > PERIOD_MAXIMUM) timeout = PERIOD_MAXIMUM;
		else if (timeout < PERIOD_MINIMUM) timeout = PERIOD_MINIMUM;
		// create lease and storage
		long now = System.currentTimeMillis();
		Lease lease = Lease.create(timeout);
		LeaseStorage storage = new LeaseStorage
			(lease, now + timeout + PERIOD_GRACE);
		storage.addListener(listener);
		synchronized (local) {
			// insert lease and storage at the right place
			boolean added = false;
			for (int i = 0, s = local.size(); i < s; i++)  {
				LeaseStorage store = (LeaseStorage)local.elementAt(i);
				if (store.getExpiration() > storage.getExpiration()) {
					local.insertElementAt(storage, i);
					added = true;
					break;	
				}
			}
			if (! added) {
				local.addElement(storage);
			}
			local.notify();
		}
		return lease;
	}
	
	/**
	 * Removes a specified lease that has been created locally and informs
	 * all remote listeners about the removal. The remote listeners will
	 * immediately receive a lease expired event. Note that the local
	 * listener that is registered for the lease will not be notified of
	 * the removal.
	 * 
	 * @param lease The lease that should be removed. The lease must not	
	 * 	be null.
	 * @return True if the lease has been removed, false if the lease
	 * 	has not been created by this registry or if it was not registered.
	 */
	public boolean remove(Lease lease) {
		return remove(lease, true);
	}
	
	/**
	 * Removes a specified lease that has been created locally.
	 * If the notify flag is set to true, all remote listeners that
	 * have observed this lease will be informed about the removal.
	 * If the notify flag is set to false, the lease will be removed
	 * silently for remote listeners.
	 * 
	 * @param lease The lease that should be removed. The lease must
	 * 	not be null.
	 * @param notify True to indicate that all remote listeners should
	 * 	be notified, false to indicate that the lease should be 
	 * 	removed silently for remote listeners. 
	 * @return True if the lease has been removed, false if the lease
	 * 	has not been created by the registry or if it was not registered.
	 */
	public boolean remove(final Lease lease, boolean notify) {
		LeaseStorage removed = null;
		synchronized (local) {
			for (int i = 0, s = local.size(); i < s; i++) {
				LeaseStorage storage = (LeaseStorage)local.elementAt(i);
				if (storage.getLease().equals(lease)) {
					local.removeElementAt(i);
					removed = storage;
					break;
				}
			}
		}
		if (removed != null) {
			final SystemID[] systems = removed.getSystems();
			if (notify && systems.length > 0) {
				for (int i = 0; i < systems.length; i++) {
					final int id = i; 
					IOperation notification = new IOperation() {
						public void perform(IMonitor monitor) {
							LeaseRegistryProxy proxy = new LeaseRegistryProxy();
							proxy.setGateway(true);
							proxy.setSourceID(new ReferenceID(SystemID.SYSTEM, ILeaseRegistry.REGISTRY_ID));
							proxy.setTargetID(new ReferenceID(systems[id], ILeaseRegistry.REGISTRY_ID));
							try {
								proxy.remove(SystemID.SYSTEM, lease);
							} catch (InvocationException e) {
								Logging.debug(getClass(), "Lease remove notification failed for " 
									+ lease + " on system " + systems[id] + ".");
							}
						}
					};
					broker.performOperation(notification);
				}
			}
			return true;
		} else {
			return false;
		}	
	}
	
	/**
	 * Hooks up a listener to the specified lease that maintains
	 * the lease. If the lease is removed or if the remote system
	 * that hosts the lease can no longer be contacted, the lease
	 * registry will call the specified listener with an expired event
	 * and it will remove the listener. Note that it is possible to
	 * register a number of listeners for the same lease. They will
	 * be informed about the status of the lease.
	 * 
	 * @param lease The lease that should be registered. The lease
	 * 	must not be null. 
	 * @param listener The listener that will listen to lease 
	 * 	expiration events. The listener must not be null.
	 */
	public void hook(Lease lease, IListener listener) {
		synchronized (remote) {
			LeaseQueue queue = (LeaseQueue)remote.get(lease.getCreator());
			if (queue == null) {
				queue = new LeaseQueue(lease.getCreator());
				remote.put(lease.getCreator(), queue);
				broker.performOperation(queue, queue.getMonitor());
			}
			QueueStorage storage = queue.remove(lease);
			if (storage == null) {
				storage = new QueueStorage(lease, 0);
			}
			storage.addListener(listener);
			queue.insert(storage);
		}
	}
	
	/**
	 * Unhooks a previously registered listener for a specified lease
	 * from the registry. If the listener is the last listner for the
	 * specified lease, this method will inform the system that hosts
	 * the lease that the last member on this system has been removed.
	 * 
	 * @param lease The lease for which the listener has been created.
	 * 	The lease must not be null.
	 * @param listener The listener that should be removed. The listener
	 * 	must not be null.
	 * @return True if the listener has been unhooked, false if the 
	 * 	listener was not registered at the registry.
	 */
	public boolean unhook(Lease lease, IListener listener) {
		return unhook(lease, listener, true);
	}
	
	/**
	 * Unhooks a previously registered listener for a specified lease
	 * from the registry. If the listener is the last listner for the
	 * specified lease and the notify flag is set to true, this method 
	 * will inform the system that hosts the lease that the last member 
	 * on this system has been removed. If the notify flag is set to
	 * false, the listener will be removed silently.
	 * 
	 * @param lease The lease for which the listener has been created.
	 * 	The lease must not be null.
	 * @param listener The listener that should be removed. The listener
	 * 	must not be null.
	 * @param notify A flag that indicates whether the remote system 
	 * 	that hosts the lease should be notified of the removal. If the
	 * 	flag is set to true, it will be notified if the lease is removed
	 * 	completely, otherwise the lease will be removed silently.
	 * @return True if the listener has been unhooked, false if the 
	 * 	listener was not registered at the registry. 
	 */	
	public boolean unhook(final Lease lease, IListener listener, boolean notify) {
		synchronized (remote) {
			LeaseQueue queue = (LeaseQueue)remote.get(lease.getCreator());
			if (queue != null) {
				QueueStorage storage = queue.remove(lease);
				if (storage != null) {
					storage.removeListener(listener);
					if (storage.hasListeners()) {
						queue.insert(storage);
						return true;	
					} 
					// fall through at else and perform remote notification
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		if (notify) {
			// perform remote notification for unhook operation
			IOperation notification = new IOperation() {
				public void perform(IMonitor monitor) {
					LeaseRegistryProxy proxy = new LeaseRegistryProxy();
					proxy.setSourceID(new ReferenceID(SystemID.SYSTEM, ILeaseRegistry.REGISTRY_ID));
					proxy.setTargetID(new ReferenceID(lease.getCreator(), ILeaseRegistry.REGISTRY_ID));
					proxy.setGateway(true);
					try {
						proxy.unhook(SystemID.SYSTEM, lease);
					} catch (InvocationException e) {
						Logging.debug(getClass(), "Lease unhook notification failed for " 
							+ lease + ".");
					}
				}
			};
			broker.performOperation(notification);
		}
		return true;
	}
	
// local operation that removes locally created leases
	
	
	/**
	 * This operation checks whether locally created leases are no
	 * longer of interest to any remote system. If this happens
	 * it removes the leases and notifies the corresponding listeners.
	 * 
	 * @param monitor The monitor used to stop the operation.
	 * @throws Exception Should never be thrown.
	 */
	public void perform(IMonitor monitor) throws Exception {
		monitor: while (true) {
			LeaseStorage removed = null;
			synchronized (local) {
				// wait until queue contains entries
				while (local.isEmpty()) {
					if (monitor.isCanceled()) break monitor;
					try {
						local.wait();
					} catch (InterruptedException e) {
						Logging.error(getClass(), "Thread got interrupted.", e);
					}
				}
				// wait upon oldest entry expired
				LeaseStorage storage = (LeaseStorage)local.elementAt(0);
				long now = System.currentTimeMillis();
				if (storage.getExpiration() <= now) {
					// remove storage entry and schedule signaling
					local.removeElementAt(0);
					removed = storage;
				} else {
					try {
						local.wait(storage.getExpiration() - now);
					} catch (InterruptedException e) {
						continue monitor;
					}
				}
			}
			if (removed != null) {
				Lease lease = removed.getLease();
				Logging.debug(getClass(), "Removing local lease " + lease + " due to timeout.");							
				// perform local timeout notification
				Event e = new Event(EVENT_LEASE_EXPIRED, this, lease, false);
				removed.notifyListeners(e);
			}
		}
		// remove all running local queue entries
		synchronized (local) {
			for (int i = 0, s = local.size(); i < s; i++) {
				LeaseStorage storage = (LeaseStorage)local.elementAt(0);
				Lease lease = storage.getLease();
				local.removeElementAt(0);
				// perform local removal notification
				Event e = new Event(EVENT_LEASE_EXPIRED, this, lease, false);
				storage.notifyListeners(e);
			}
		}
		Vector queues = new Vector();
		// shutdown all running remote queues
		synchronized (remote) {
			Enumeration e = remote.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				LeaseQueue queue = (LeaseQueue)remote.get(key);
				IMonitor queueMonitor = queue.getMonitor();
				queueMonitor.cancel();
				queues.addElement(queueMonitor);
			}
		}
		for (int i = 0; i < queues.size(); i++) {
			IMonitor queueMonitor = (IMonitor)queues.elementAt(i);
			try {
				queueMonitor.join();
			} catch (InterruptedException ie) {
				Logging.error(getClass(), "Queue cleanup interrupted.", ie);
			}
		}
	}

	
// remote interface that is called by other systems

	/**
	 * Called by a remote lease registry whenever the remote system is no
	 * longer interested in a local lease.
	 * 
	 * @param system The remote system that is no longer observing a certain
	 * 	lease.
	 * @param lease The lease that is no longer observed.
	 */
	public void unhook(SystemID system, Lease lease) {
		synchronized (local) {
			// find local lease that has been removed
			for (int i = 0, s = local.size(); i < s; i++) {
				LeaseStorage storage = (LeaseStorage)local.elementAt(i);
				if (storage.getLease().equals(lease)) {
					// remove system and check whether there are any other systems
					storage.removeSystem(system);
					if (storage.getSystems().length == 0) {
						Event e = new Event(EVENT_LEASE_EXPIRED, this, lease, false);
						storage.notifyListeners(e);	
						// if no more systems are available, expire lease immediately
						local.removeElement(storage);
					}
					return;
				}
			}
		}			
	}

	/**
	 * Called by a remote system that is interested on a number of leases
	 * created by this lease registry.
	 * 
	 * @param system The system that is interested in the local leases.
	 * @param leases The leases of interest stored in a vector.
	 * @return A vector of leases that is no longer available in this registry.
	 */
	public Vector update(SystemID system, Vector leases) {
		Vector result = new Vector();
		// update storages and notify listeners
		synchronized (local) {
			leases: for (int i = 0, s = leases.size(); i < s; i++) {
				Lease lease = (Lease)leases.elementAt(i);
				for (int j = 0, sl = local.size(); j < sl; j++) {
					LeaseStorage storage = (LeaseStorage)local.elementAt(j);
					if (storage.getLease().equals(lease)) {
						local.removeElementAt(j);
						// perform local refresh notification
						//Logging.debug(getClass(), "Local lease refreshed for " + lease + ".");
						// update timeout to new timeout period
						storage.updateSystem(system);
						// insert storage ordered by timeout
						for (int k = local.size() - 1; k >= 0; k--) {
							LeaseStorage store = (LeaseStorage)local.elementAt(k);							
							if (store.getExpiration() <= storage.getExpiration()) {
								local.insertElementAt(storage , k + 1);
								continue leases;
							}
						}
						// not inserted yet, append as last entry
						local.insertElementAt(storage, 0);
						continue leases;
					}
				}
				Logging.debug(getClass(), "Local lease refresh failed for " + lease + ".");
				result.addElement(lease);
			}
			local.notify();
		}
		return result;
	}

	/**
	 * Called by a remote lease registry to signal that a certain lease created
	 * by this registry has been removed due to a call to the remove method. 
	 * 
	 * @param system The system that has removed a lease.
	 * @param lease The lease that has been removed.
	 */
	public void remove(SystemID system, Lease lease) {
		LeaseQueue queue = null;
		synchronized (remote) {
			queue = (LeaseQueue)remote.get(system);
		}
		if (queue != null) {
			QueueStorage s = (QueueStorage)queue.remove(lease);
			if (s != null) {
				Event e = new Event(EVENT_LEASE_EXPIRED, this, lease, false);
				s.notifyListeners(e);	
			}
		}
	}

}
