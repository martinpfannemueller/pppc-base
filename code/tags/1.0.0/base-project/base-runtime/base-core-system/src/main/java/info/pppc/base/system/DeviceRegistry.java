package info.pppc.base.system;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class implements a local registry for locally available plug-ins 
 * and their descriptions. Remotely available plug-in descriptions are tracked as 
 * well. A new description which references an unknown system causes a notification 
 * of all listeners. Descriptions which have not been updated for a while are 
 * removed. All listeners are notified about this event. The methods of the
 * device registry are synchronized additional synchronization for single methods
 * is not required. 
 * 
 * @author Marcus Handte
 */
public final class DeviceRegistry implements IOperation {

	/**
	 * The event constant used to signal that a remote device has been added.
	 * The user object of the event will be the device description of the remote 
	 * device.
	 */
	public static final int EVENT_DEVICE_ADDED = 1;
	
	/**
	 * The event constant used to signal that a remote device has been removed.
	 * The user object of the event will be the device description of the remote 
	 * device.
	 */
	public static final int EVENT_DEVICE_REMOVED = 2;

	/**
	 * The device listeners that have been registered at this registry so far.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * This hash table hashes system ids to vectors. The vectors contain
	 * plug-in descriptions of that device. This table contains permanent
	 * entries at the first index and a reference counter at the second.
	 */
	private Hashtable pPlugins = new Hashtable();
	
	/**
	 * This hash table hashes system ids to device descriptions. This hash table
	 * contains permanent entries at the first entry and a reference counter
	 * at the second.
	 */
	private Hashtable pDevices = new Hashtable();
	
	/**
	 * This hash table hashes system ids to vectors. The vectors contain
	 * plug-in descriptions of that device. This table contains temporary
	 * entries.
	 */
	private Hashtable tPlugins = new Hashtable();

	/**
	 * This hash table hashes system ids to device descriptions. This hash table
	 * contains temporary entries.
	 */
	private Hashtable tDevices = new Hashtable();

	/**
	 * This vector contains the timeouts for the plug-in and device descriptions. For 
	 * device descriptions, it contains an object array of length 2, where the first 
	 * index contains the long at which the description times out and the second index 
	 * contains the system id of the device. For plug-in descriptions, the object
	 * array will have length 3. The first index again denotes the timeout, the second 
	 * index the system and the third index denotes the ability.
	 */
	private Vector entries = new Vector();
	
	/**
	 * The monitor that is used to cancel the registry removal thread.
	 */
	private NullMonitor monitor = new NullMonitor();

	/**
	 * Creates a new device registry. This method will be called when the 
	 * invocation broker is started.
	 * 
	 * @param broker The invocation broker.
	 */
	protected DeviceRegistry(InvocationBroker broker) {
		broker.performOperation(this, monitor);
		broker.addBrokerListener(InvocationBroker.EVENT_BROKER_SHUTDOWN, new IListener() {
			public void handleEvent(Event event) {
				Logging.debug(getClass(), "Removing device registry due to broker shutdown.");
				monitor.cancel();
				try {
					monitor.join();	
				} catch (InterruptedException e) {
					Logging.error(getClass(), "Thread got interrupted.", e);
				}
				
			}
		});
	}
	
	/**
	 * Adds a device listener to this device registry. The device listener
	 * keeps track of changes to devices that are registered at this 
	 * registry. The supported events are EVENT_DEVICE_ADDED and 
	 * EVENT_DEVICE_REMOVED. These events are fired
	 * whenever a device is added or removed.
	 * 
	 * @param type The types of events to register. Multiple types can
	 * 	be added by concatenation.
	 * @param listener The listener to register.
	 */
	public void addDeviceListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}
	
	/**
	 * Removes a device listener from this registry. If the device listener
	 * has been removed completely, this method will return true, otherwise
	 * false.
	 * 
	 * @param type The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed, false otherwise.
	 */
	public boolean removeDeviceListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}
	
    /**
	 * Registers a device description that has been discovered from a remote device.
	 * A call to this method will replace a potentially previously registered 
	 * description with the new one provided by the call. The device description
	 * stays registered as long as the ttl has not run out.
	 * 
	 * @param device The device description of the device that announced the 
	 * 	descriptions.
	 * @param ttl The time to live.
	 */
    public void registerDevice(DeviceDescription device, long ttl) {
		synchronized (monitor) {
	    	boolean exists = false;
			SystemID id = device.getSystemID();
			// check for perm table entry
			if (pDevices.containsKey(id)) {
				exists = true;
			}
			// create the timeout description
			Object[] entry = new Object[2];
			entry[0] = new Long(System.currentTimeMillis() + ttl);
			entry[1] = id; 
			// replace or add the timeout description
			Object replaced = tDevices.put(id, device);
			if (replaced != null) {
				exists = true;
				privateReplaceEntry(entry);
			} else {
				privateAddEntry(entry);
			}
			// if the object has not been there before, fire event
			if (!exists) {
				listeners.fireEvent(EVENT_DEVICE_ADDED, device);
			}			
		}
    }
	
	/**
	 * Registers the specified device description. If the description has been
	 * registered before, the device description is overwritten. The device stays
	 * registered until the remove method is called.
	 * 
	 * @param device The device description to register.
	 */
	public void registerDevice(DeviceDescription device) {
		synchronized (monitor) {
			boolean exists = false;
			SystemID id = device.getSystemID();
			// check for temp table entry
			if (tDevices.containsKey(id)) {
				exists = true;
			}
			// replace perm table entry
			Object[] replaced = (Object[])pDevices.get(id);
			if (replaced != null) {
				exists = true;
				replaced[0] = device;
				Integer count = (Integer)replaced[1];
				replaced[1] = new Integer(count.intValue() + 1);
			} else {
				pDevices.put(id, new Object[] { device, new Integer(1) });
			}
			// if the object has not been there before, fire event
			if (!exists) {
				listeners.fireEvent(EVENT_DEVICE_ADDED, device);
			}					
		}
	}

	/**
	 * Removes the device description of the specified system from the list
	 * of permanently registered device descriptions.
	 * 
	 * @param id The id of the device to remove.
	 */
	public void removeDevice(SystemID id) {
		synchronized (monitor) {
			// remove the device, if registered
			Object[] removed = (Object[])pDevices.get(id);
			if (removed != null) {
				Integer count = (Integer)removed[1];
				if (count.intValue() == 1) {
					pDevices.remove(id);
					// no announcement needed if device was not registered
					if (! tDevices.contains(id)) {
						listeners.fireEvent(EVENT_DEVICE_REMOVED, removed);		
					}
				} else {
					removed[1] = new Integer
						(count.intValue() - 1);
				}
			} 
		}
	}
	
	/**
	 * Determines whether the specified device is currently contained in the
	 * set of devices.
	 * 
	 * @param id The system id of the device to lookup.
	 * @return True if the device is contained in the registry, false otherwise.
	 */
	public boolean containsDevice(SystemID id) {
		synchronized (monitor) {
			return (tDevices.containsKey(id) || pDevices.containsKey(id));	
		}
	}
	
	/**
	 * Returns an array of system id that has been discovered recently.
	 * 
	 * @return The devices that have been discovered.
	 */
	public SystemID[] getDevices() {
		return getDevices(new ObjectID[0]);
	}
	
	/**
	 * Returns an array of system ids that have been discovered recently
	 * and that contain the specified well known service.
	 * 
	 * @param oid The well known object id to lookup.
	 * @return An array of system ids of devices that have the specified
	 * 	well known service.
	 */
	public SystemID[] getDevices(ObjectID oid) {
		return getDevices(new ObjectID[] { oid });		
	}
	
	/**
	 * Returns an array of system ids that consists of systems that contain
	 * the specified well known services.
	 * 
	 * @param ids An array of ids to lookup.
	 * @return The systems that contain the specified services.
	 */
	public SystemID[] getDevices(ObjectID[] ids) {
		Vector unwrapped;
		synchronized (monitor) {
			unwrapped = privateGetDevices(ids);
		}
		return privateWrapDevices(unwrapped);
	}

	/**
	 * Returns an array of remote system id that has been discovered 
	 * recently.
	 * 
	 * @return The remote devices that have been discovered.
	 */
	public SystemID[] getRemoteDevices() {
		return getRemoteDevices(new ObjectID[0]);
	}
	
	/**
	 * Returns an array of remote system ids that have been discovered
	 * recently and that contain the specified well known service.
	 * 
	 * @param oid The well known object id to lookup.
	 * @return An array of remote system ids of devices that have the
	 * 	specified well known service.
	 */
	public SystemID[] getRemoteDevices(ObjectID oid) {
		return getRemoteDevices(new ObjectID[] { oid });		
	}
	
	/**
	 * Returns an array of remote system ids that consists of systems that
	 * contain the specified well known services.
	 * 
	 * @param ids An array of ids to lookup.
	 * @return The remote systems that contain the specified services.
	 */
	public SystemID[] getRemoteDevices(ObjectID[] ids) {
		Vector unwrapped;
		synchronized (monitor) {
			unwrapped = privateGetDevices(ids);
		}
		unwrapped.removeElement(SystemID.SYSTEM);
		return privateWrapDevices(unwrapped);
	}

	/**
	 * Returns the device description of the system with the specified id.
	 * 
	 * @param id The system id of the device description to lookup.
	 * @return The device description of the system with the specified id or
	 * 	null if the device description is not known.
	 */
	public DeviceDescription getDeviceDescription(SystemID id) {
		synchronized (monitor) {
			DeviceDescription result = (DeviceDescription)tDevices.get(id);
			if (result == null) {
				Object[] oid = (Object[])pDevices.get(id);
				if (oid != null) {
					result = (DeviceDescription)oid[0];
				}
			}
			return result;			
		}
	}
	
	/**
	 * Wraps the system ids contained in the passed vector into a system id array.
	 * 
	 * @param unwrapped The vector that contains the unwrapped system ids.
	 * @return An array of system ids that contains the system ids of the vector.
	 */
	private SystemID[] privateWrapDevices(Vector unwrapped) {
		SystemID[] devices = new SystemID[unwrapped.size()];
		for (int i = devices.length - 1; i >= 0; i--) {
			devices[i] = (SystemID)unwrapped.elementAt(i);
		}
		return devices;
	}
	
	/**
	 * Returns the devices that have the specified services as a unwrapped
	 * vector. This method is used internally.
	 * 
	 * @param ids The ids of the services that the device must provide.
	 * @return A vector containing system ids of devices that provide all
	 * 	services.
	 */
	private Vector privateGetDevices(ObjectID[] ids) {
		Vector temp = new Vector();
		// add permanent devices
		Enumeration pe = pDevices.elements();
		systems: while (pe.hasMoreElements()) {
			Object[] entry = (Object[])pe.nextElement();
			if (entry != null) {
				DeviceDescription dd = (DeviceDescription)entry[0];
				for (int i = ids.length - 1; i >= 0; i--) {
					if (! dd.hasService(ids[i])) {
						continue systems;
					}				
				}
				temp.addElement(dd.getSystemID());				
			}
		}
		Enumeration te = tDevices.elements();
		systems: while (te.hasMoreElements()) {
			DeviceDescription dd = (DeviceDescription)te.nextElement();
			SystemID id = dd.getSystemID();
			if (! temp.contains(id)) {
				for (int i = ids.length - 1; i >= 0; i--) {
					if (! dd.hasService(ids[i])) {
						continue systems;
					}				
				}
				temp.addElement(id);				
			}
		}
		return temp;
	}

    /**
 	 * Registers the specified plug-in description of the specified device.
	 * If the plug-in description has been registered before, it will be overwritten.
	 * The plug-in description stays registered as long as the ttl has not run out.
	 * 
	 * @param id The id of the device.
	 * @param plugin The plug-in description to register.
	 * @param ttl The time to live.
	 */
    public void registerPlugin(SystemID id, PluginDescription plugin, long ttl) {
    	synchronized (monitor) {
    		// update temp table, if exists
    		Vector tp = (Vector)tPlugins.get(id);
    		if (tp == null) {
    			tp = new Vector();
    			tPlugins.put(id, tp);
    		}
    		// create entry for later addition to description vector
    		Object[] entry = new Object[3];
    		entry[0] = new Long(System.currentTimeMillis() + ttl);
    		entry[1] = id;
    		entry[2] = new Short(plugin.getAbility());
    		for (int i = tp.size() - 1; i >= 0; i--) {
    			PluginDescription d = (PluginDescription)tp.elementAt(i);
    			if (d.getAbility() == plugin.getAbility()) {
    				tp.removeElementAt(i);
    				tp.addElement(plugin);
    				// replace existing description with new one
    				privateReplaceEntry(entry);
    				return;	
    			}
    		}
    		// add new description as plugin was not there
    		tp.addElement(plugin);
    		privateAddEntry(entry);    		
    	}
	}

	/**
	 * Registers the specified plug-in description of the specified device.
	 * The plug-in stays registered until the remove method is called.
	 * 
	 * @param id The system id of the plug-in to register.
	 * @param plugin The plug-in description to register.
	 */
	public void registerPlugin(SystemID id, PluginDescription plugin) {
		synchronized (monitor) {
			// replace perm table entry, if exists
			Vector pp = (Vector)pPlugins.get(id);
			if (pp == null) {
				pp = new Vector();
				pPlugins.put(id, pp);
			}
			for (int i = pp.size() - 1; i >= 0; i--) {
				Object[] entry = (Object[])pp.elementAt(i);
				PluginDescription d = (PluginDescription)entry[0];
				if (d.getAbility() == plugin.getAbility()) {
					entry[0] = plugin;
					Integer count = (Integer)entry[1];
					entry[1] = new Integer(count.intValue() + 1);
					return;	
				}
			}
			// add to perm table, element not existent
			pp.addElement(new Object[] { plugin, new Integer(1) });					
		}
	}

	/**
	 * Removes the specified plug-in from the set of permanently registered
	 * plug-ins. 
	 * 
	 * @param id The id of the system.
	 * @param plugin The plug-in description to remove.
	 */
	public void removePlugin(SystemID id, PluginDescription plugin) {
		synchronized (monitor) {
			// remove element from perm table
			Vector pp = (Vector)pPlugins.get(id);
			if (pp != null) {
				for (int i = pp.size() - 1; i >= 0; i--) {
					Object[] entry = (Object[])pp.elementAt(i);
					PluginDescription d = (PluginDescription)entry[0];
					if (d.getAbility() == plugin.getAbility()) {
						Integer count = (Integer)entry[1];
						if (count.intValue() == 1) {
							pp.removeElementAt(i);							
						} else {
							entry[1] = new Integer(count.intValue() - 1);
						}
						break;
					}
				}
			}			
		}
	}

	/**
	 * Returns the plug-in descriptions of the target system that are compatible
	 * with the source system.
	 * 
	 * @param source The system id of the source system.
	 * @param target The system id of the target system.
	 * @return The set of plug-in descriptions of the target system that are 
	 * 	compatible with the remote system.
	 */
	public PluginDescription[] getPluginDescriptions(SystemID source, SystemID target) {
		Vector sp;
		Vector tp;
		synchronized (monitor) {
			// get source plug-ins
			sp = privateGetPluginDescriptions(source);
			// get target plug-ins
			tp = privateGetPluginDescriptions(target);			
		}
		// intersect sets
		Vector unwrapped = new Vector(sp.size() + 1);
		for (int i = sp.size() - 1; i >= 0; i--) {
			PluginDescription sd = (PluginDescription)sp.elementAt(i);
			for (int j = tp.size() - 1; j >= 0; j--) {
				PluginDescription td = (PluginDescription)tp.elementAt(j);
				if (sd.getAbility() == td.getAbility()) {
					unwrapped.addElement(td);
					tp.removeElementAt(j);
					break;	
				}
			}
		}
		return privateWrapPluginDescriptions(unwrapped);
	}

	/**
	 * Returns a set of plug-in descriptions which have been registered for
	 * a given system. Note that this method will also work for the local
	 * system.
	 * 
	 * @param systemID The system id of the system to lookup.
	 * @return The plug-in descriptions of the specified system.
	 */
	public PluginDescription[] getPluginDescriptions(SystemID systemID) {
		synchronized (monitor) {
			Vector unwrapped = privateGetPluginDescriptions(systemID);
			return privateWrapPluginDescriptions(unwrapped);
		}
	}
	
	/**
	 * Wraps the plug-in descriptions contained in the vector and returns them as array.
	 * 
	 * @param unwrapped The vector that contains the unwrapped plug-in descriptions.
	 * @return An array of plug-in descriptions that were contained in the vector.
	 */
	private PluginDescription[] privateWrapPluginDescriptions(Vector unwrapped) {
		PluginDescription[] plugins = new PluginDescription[unwrapped.size()];
		for (int i = plugins.length - 1; i >= 0; i--) {
			plugins[i] = (PluginDescription)unwrapped.elementAt(i);
		}
		return plugins;			
	}
	
	/**
	 * Returns the plug-in descriptions of the specified device as
	 * an unwrapped vector.
	 * 
	 * @param systemID The system id of the device whose plug-in
	 * 	descriptions need to be retrieved.
	 * @return A vector of plug-in descriptions of the device
	 */
	private Vector privateGetPluginDescriptions(SystemID systemID) {
		Vector temp = new Vector();
		// add temporary plugin descriptions 
		Vector tp = (Vector)tPlugins.get(systemID);
		if (tp != null) {
			for (int i = tp.size() - 1; i >= 0; i--) {
				temp.addElement(tp.elementAt(i));
			}
		}
		// add permanent plugin descriptions
		Vector pp = (Vector)pPlugins.get(systemID);
		if (pp != null) {
			pps: for (int i = pp.size() - 1; i >= 0; i--) {
				Object[] entry = (Object[])pp.elementAt(i);
				PluginDescription pd = (PluginDescription)entry[0];
				for (int j = 0; j < temp.size(); j++) {
					PluginDescription x = (PluginDescription)temp.elementAt(j);
					if (x.getAbility() == pd.getAbility()) {
						continue pps;
					}
				}
				temp.addElement(pd);
			}
		}
		return temp;
	}
	
	/**
	 * Adds the specified entry to the entry vector without checking whether
	 * an equal entry exists and must be replaced.
	 * 
	 * @param entry The entry to add to the entry vector.
	 */
	private void privateAddEntry(Object[] entry) {
		// step backward through the vector to support fast addition of long-lasting values
		long timeout1 = ((Long)entry[0]).longValue();
		for (int i = entries.size() - 1; i >= 0; i--) {
			Object[] e = (Object[])entries.elementAt(i);
			long timeout2 = ((Long)e[0]).longValue();
			if (timeout1 >= timeout2) {
				// insert after entry and continue to sleep
				entries.insertElementAt(entry, i + 1);
				return;
			}
		}
		// insert as new first element and notify performing queue
		entries.insertElementAt(entry, 0);
		monitor.notifyAll();
	}
	
	/**
	 * Replaces an existing entry in the entry vector with the specified entry.
	 * 
	 * @param entry The entry that should replace an existing entry
	 */
	private void privateReplaceEntry(Object[] entry) {
		// step forward through the vector to support fast updates of soon-to-timeout values
		if (entry.length == 2) {
			SystemID id = (SystemID)entry[1];
			for (int i = 0, s = entries.size(); i < s; i++) {
				Object[] e = (Object[])entries.elementAt(i);
				if (e.length == 2 && e[1].equals(id)) {
					entries.removeElementAt(i);
					break;
				}
			}
		} else {
			SystemID id = (SystemID)entry[1];
			Short ability = (Short)entry[2];
			for (int i = 0, s = entries.size(); i < s; i++) {
				Object[] e = (Object[])entries.elementAt(i);
				if (e.length == 3 && e[1].equals(id) && e[2].equals(ability)) {
					entries.removeElementAt(i);
					break;
				}
			}
		}
		privateAddEntry(entry);
	}

	/**
	 * This operation continuously removes the devices and plug-ins from the
	 * registry whose ttl has been run out.
	 * 
	 * @param monitor The monitor used to cancel the operation.
	 * @throws Exception Should never happen.
	 */
	public void perform(IMonitor monitor) throws Exception {
		synchronized (monitor) {
			while (true) {
				while (! monitor.isCanceled() && entries.size() == 0) {
					monitor.wait();	
				}
				if (monitor.isCanceled()) return;
				long now = System.currentTimeMillis();
				Object[] entry = (Object[])entries.elementAt(0);
				long timeout = ((Long)entry[0]).longValue();
				if (timeout > now) {
					monitor.wait(timeout - now);
				} else {
					entries.removeElementAt(0);
					if (entry.length == 2) {
						Object removed = tDevices.remove(entry[1]);
						if (removed != null && ! pDevices.containsKey(entry[1])) {
							listeners.fireEvent(EVENT_DEVICE_REMOVED, removed);
						}
					} else {
						short ability = ((Short)entry[2]).shortValue();
						Vector tp = (Vector)tPlugins.get(entry[1]);
						if (tp != null) {
							for (int i = tp.size() - 1; i >= 0; i--) {
								PluginDescription pd = (PluginDescription)tp.elementAt(i);
								if (pd.getAbility() == ability) {
									tp.removeElementAt(i);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

}