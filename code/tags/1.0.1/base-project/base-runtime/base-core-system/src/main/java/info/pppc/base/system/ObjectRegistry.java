package info.pppc.base.system;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * A registry for local objects managed by the invocation broker. The object
 * registry is synchronized, all calls to the registry can be performed without
 * any additional synchronization.
 * 
 * @author Marcus Handte
 */
public final class ObjectRegistry {

	/**
	 * The event constant that is used to signal that a new local 
	 * object has been registered at the object registry. The user object
	 * of the event will carry the object id of the registered object.
	 * Note that this event will be fired for any object id (well-known or 
	 * auto-generated).
	 */
	public static final int EVENT_OBJECT_ADDED = 1;
	
	/**
	 * The event constant that is used to signal that a previously registered
	 * object has been removed from the object registry. The user object
	 * of the event will carry the object id of the unregistered object.
	 * Note that this event will be fired for any object id (well-known or
	 * auto-generated).
	 */
	public static final int EVENT_OBJECT_REMOVED = 2;

	/**
	 * The event constant that is used to signal that a previously unknown
	 * object with a well known object id has been registered at the object
	 * registry. The user object of the event will carry the object id of the
	 * registered object. 
	 */
	public static final int EVENT_KNOWN_ADDED = 4;
	
	/**
	 * The event constant that is used to signal that a previously registered
	 * object with a well known object id has been removed from the registry.
	 * The user object of the vent will carry the object id of the removed 
	 * object.
	 */
	public static final int EVENT_KNOWN_REMOVED = 8;

	/**
	 * The object listeners that have been added.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * Hashes invocation handler by id.
	 */
	private Hashtable handlers = new Hashtable();
	
	/**
	 * Hashes id by invocation handler.
	 */
	private Hashtable identifiers = new Hashtable();

	/**
	 * Hashes local references by id.
	 */
	private Hashtable knownReferences = new Hashtable();

	/**
	 * A vector of the registered well known object ids.
	 */
	private Vector knownIdentifiers = new Vector();
	
	/**
	 * Creates a new object registry. This is called by the
	 * invocation broker upon startup.
	 */
	protected ObjectRegistry() {
		super();
	}

	/**
	 * Determines whether the specified object has been registered.
	 * 
	 * @param id The id of the object to lookup.
	 * @return True if it exists, false otherwise.
	 */
	public boolean isIdentifierRegistered(ObjectID id) {
		synchronized (this) {
			return (handlers.containsKey(id));	
		}
	}
	
	/**
	 * Retrieves an invocation handler for the specified id.
	 *
	 * @param id The id of the object to lookup.
	 * @return An invocation handler if the id has been
	 * 	registered, otherwise null.
	 */
	public IInvocationHandler getInvocationHandler(ObjectID id) {
		synchronized (this) {
			return (IInvocationHandler)handlers.get(id);	
		}
	}
	
	/**
	 * Retrieves the id of a previously registered invocation handler.
	 * 
	 * @param handler The handler to lookup.
	 * @return The id of the invocation handler or null if the
	 * 	handler is no longer registered.
	 */
	public ObjectID getIdentifier(IInvocationHandler handler) {
		synchronized (this) {
			return (ObjectID)identifiers.get(handler);
		}
	}
	
	/**
	 * Retrieves a local reference for the specified id.
	 *
	 * @param id The id of the object to lookup.
	 * @return An invocation handler if a local reference for the 
	 *  id has been registered, otherwise null.
	 */
	public Object getKnownReference(ObjectID id){
		synchronized(this){
			return knownReferences.get(id);
		}
	}
	
	/**
	 * Registers a new object that handles invocations.
	 * 
	 * @param handler The handler used to dispatch requests.
	 * @return The object id assigned to the object.
	 */
	public ObjectID registerObject(IInvocationHandler handler) {
		synchronized (this) {		
			ObjectID id = ObjectID.create();
			handlers.put(id, handler);
			identifiers.put(handler, id);	
			listeners.fireEvent(EVENT_OBJECT_ADDED, id);
			return id;
		}
	}
	
	/**
	 * Registers a well known object with the specified id.
	 * 
	 * @param id The id of the well known object.
	 * @param handler The handler for the object with the specified id.
	 * @param localReference The local reference for the well known object.
	 */
	public void registerObject(ObjectID id, IInvocationHandler handler, Object localReference) {
		synchronized (knownIdentifiers) {
			if (id == null || ! id.isKnown()) {
				throw new RuntimeException("Id is not well known.");
			} else if (knownIdentifiers.contains(id)) {
				throw new RuntimeException("Multiple registration of a well known id.");
			} else if (localReference == null) {
				throw new RuntimeException("Local reference invalid.");
			} 				
		}
		synchronized (this) {
			handlers.put(id, handler);
			identifiers.put(handler, id);
			knownReferences.put(id,localReference);
			synchronized (knownIdentifiers) {
				knownIdentifiers.addElement(id);
				listeners.fireEvent(EVENT_OBJECT_ADDED, id);
				listeners.fireEvent(EVENT_KNOWN_ADDED, id);
			}
		}						
	}	
	
	/**
	 * Removes the invocation handler with the specified identifier.
	 * 
	 * @param id The identifier of the invocation handler.
	 * @return The invocation handler that has been removed or null
	 * 	if the id is not registered.
	 */
	public IInvocationHandler removeObject(ObjectID id) {
		synchronized (this) {
			IInvocationHandler handler = (IInvocationHandler)handlers.remove(id);
			identifiers.remove(handler);
			knownReferences.remove(id);
			listeners.fireEvent(EVENT_OBJECT_REMOVED, id);
			if (id.isKnown()) {
				synchronized (knownIdentifiers) {
					knownIdentifiers.removeElement(id);
					listeners.fireEvent(EVENT_KNOWN_REMOVED, id);			
				}
			}
			return handler;
		}
	}
	
	
	/**
	 * Returns the well known services registered at this registry.
	 * 
	 * @return The identifiers of well known services that are available
	 * 	locally.
	 */
	public ObjectID[] getKnownIdentifiers() {
		synchronized (knownIdentifiers) {
			ObjectID[] objects = new ObjectID[knownIdentifiers.size()];
			for (int i = 0; i < objects.length; i++) {
				objects[i] = (ObjectID)knownIdentifiers.elementAt(i);
			}
			return objects;
		}
	}
	
	/**
	 * Adds an object listener for the specified event types. At the 
	 * present time, object listeners can be registered for 
	 * EVENT_OBJECT_ADDED, EVENT_OBJECT_REMOVED, 
	 * EVENT_KNOWN_REMOVED, EVENT_KNOWN_ADDED events. All events will
	 * carry the corresponding object id as user object.
	 * 
	 * @param type The types of events to register. Multiple types can
	 * 	be concatenated using a logical or.
	 * @param listener The object listener to add.
	 */
	public void addObjectListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}
	
	/**
	 * Removes a previously registered object listener from the set of listeners
	 * that are registered for the specified types.
	 * 
	 * @param type The types of events to register. Multiple types can
	 * 	be concatenated using a logical or.
	 * @param listener The listener to remove.
	 * @return True if the listener is no longer registered for an event and 
	 *  thus it has been removed.
	 */
	public boolean removeObjectListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}
	
	/**
	 * Returns a string representation of the internal data structures
	 * of the object registry.
	 * 
	 * @return An internal string representation.
	 */
	public String toString() {
		synchronized (this) {
			StringBuffer b = new StringBuffer("KNOWN <");
			b.append(knownIdentifiers.size());
			b.append("|");
			for (int i = 0; i < knownIdentifiers.size(); i++) {
				b.append(knownIdentifiers.elementAt(i));
				if (i < knownIdentifiers.size() - 1) {
					b.append(",");
				}
			}
			b.append("> REGISTERED <");
			b.append(handlers.size());
			b.append("|");
			Enumeration e = handlers.keys();
			while (e.hasMoreElements()) {
				b.append(e.nextElement());
				if (e.hasMoreElements()) {
					b.append(",");			
				}
			}
			return b.toString();
		}
	}
}