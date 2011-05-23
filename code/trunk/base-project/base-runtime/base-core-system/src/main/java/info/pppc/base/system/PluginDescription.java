package info.pppc.base.system;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Object which describes a plug-in implementation and can be propagated to other systems.
 * The plug-in description contains an ability, an extension and a key. These are read-only
 * values that are set when the description is created for the first time. The ability
 * describes the task performed by the plug-in. For space purposes it is described by a byte
 * array of length two. A plug-in that announces an ability must be compatible with all other
 * plug-ins that announce the same ability. The extension defines the extension points 
 * provided by the plug-in. The possible extension points are defined as constants in the 
 * extension interface.
 * 
 * @author Marcus Handte
 */
public final class PluginDescription implements ISerializable, IExtension {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BP";
	
	/**
	 * Signals that a property has been added to the description.
	 * The data object of the event will contain the name of the property.
	 */
	public static final int EVENT_PROPERTY_ADDED = 1;

	/**
	 * Signals that a property of the description has been changed.
	 * The data object of the event will contain the name of the property.
	 */
	public static final int EVENT_PROPERTY_CHANGED = 2;
	
	/**
	 * Signals that a property is about to be removed. The data object 
	 * of the event will contain the name of the property.
	 */
	public static final int EVENT_PROPERTY_REMOVED = 4;

	/**
	 * The listeners that are registered at this plug-in description.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * The ability of the plug-in described by the description. The ability
	 * is a short that globally identifies the capabilities of the plug-in.
	 * Two plug-ins that have the same ability must also be compatible by
	 * definition.
	 */
	private short ability;

	/**
	 * The extension that is provided by the associated plug-in.
	 */
	private short extension;

	/**
	 * The properties of the plug-in. These may contain additional
	 * information, e.g. compression factor or IP address and port.
	 * The hashtable contains an object array of length 2. The
	 * first index is a boolean to determine whether the property
	 * is dynamic. The second index contains the actual value.
	 */
	private Hashtable properties = null;

	/**
	 * Creates a new plug-in description. This default constructor
	 * is intended for serialization purposes, it must not be called
	 * by user code.
	 */
	public PluginDescription() {
		super();
	}
	
	/**
	 * Creates a new plug-in description with the specified ability and
	 * the specified extension.
	 * 
	 * @param ability The ability of the plu-gin. The ability must be globally 
	 *  unique. The default policy to assign abilities is to use the most
	 *  significant byte as plug-in identifier and the least significant byte as  
	 *  unique id. Note that the ability is used to match plug-ins on different 
	 *  devices, thus if two plug-ins with different tasks have the same id, 
	 *  you might not be able to communicate with a remote device.
	 * @param extension The extension point of the plug-in. The possible extensions
	 * 	are defined as constants in the extension interface.
	 */
	public PluginDescription(short ability, short extension) {
		this.ability = ability;
		this.extension = extension;
		this.properties = new Hashtable();
	}
	
	/**
	 * Registers a listener for the specified types of events. At the
	 * present time, the plug-in description emits EVENT_PROPERTY_ADDED,
	 * EVENT_PROPERTY_REMOVED and EVENT_PROPERTY_CHANGED events whenever a 
	 * property changes. The other values of the plug-in description are read
	 * only values. The data object will contain the name of the property
	 * that has been changed, removed or added. All notifications are
	 * sent in sync and the listeners will hold the lock.
	 * 
	 * @param type The types to register for.
	 * @param listener The listener to register
	 */
	public void addListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}
	
	/**
	 * Unregisters the specified listener from the specified set of event types.
	 * 
	 * @param type The types to unregister for.
	 * @param listener The listener to unregister.
	 * @return True if the listener is no longer registered for any event,
	 * 	false if the listener is still registered or if it has not been
	 * 	registered.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public boolean removeListener(int type, IListener listener) 
			throws NullPointerException {
		return listeners.removeListener(type, listener);
	}
	
	/**
	 * Sets a property to the specified value. If the property 
	 * has been set before, this method will return and replace 
	 * the existing value. The listeners will be informed either
	 * by a EVENT_PROPERTY_ADDED or EVENT_PROPERTY_CHANGED event about
	 * the changed or added property. If the property is set to
	 * the same value as the original property, the listeners will
	 * not be informed.
	 * 
	 * @param key The key of the property to set, this may not be
	 * 	null.
	 * @param property The value of the property.
	 * @param dynamic A boolean that determines whether the property
	 * 	is dynamic. Dynamic properties are only used for local 
	 * 	communication.
	 * @return The value of the property that has been replaced.
	 */
	public synchronized Object setProperty(String key, Object property, boolean dynamic) {
		int type = EVENT_PROPERTY_ADDED;
		if (properties.containsKey(key)) {
			type = EVENT_PROPERTY_CHANGED;
		}
		Object[] replaced = (Object[])properties.put
			(key, new Object[] { new Boolean(dynamic), property});
		if (replaced != null) {
			Boolean b = (Boolean)replaced[0];			
			Object v = replaced[1];
			if ((v == null && property == null || v != null 
					&& v.equals(property)) && dynamic == b.booleanValue()) {
				return replaced[1];
			}
		}
		listeners.fireEvent(type, key);
		return replaced;
	}
	
	/**
	 * Removes a property with the specified key. If the property
	 * has been set before, this method will return and remove
	 * the existing value. If the property was not set, the method
	 * will return null. If the property was set, the listeners will
	 * be informed by a EVENT_PROPERTY_REMOVED event. If the property
	 * was not set, the listeners will not be informed.
	 * 
	 * @param key The key of the property to remove, this may not
	 * 	be null.
	 * @return The value of the property that has been removed or
	 * 	null if the property did not exist.
	 */
	public synchronized Object unsetProperty(String key) {
		if (properties.containsKey(key)) {
			Object[] removed = (Object[])properties.remove(key);
			listeners.fireEvent(EVENT_PROPERTY_REMOVED, key);
			return removed == null?null:removed[1];	
		} else {
			return null;
		}
	}
	
	/**
	 * Determines whether the property at the specified key is
	 * dynamic. If the property does not exist, false will be
	 * returned. Dynamic properties are not used for communication
	 * through a gateway.
	 * 
	 * @param key The key of the property.
	 * @return True if the property is dynamic, false otherwise.
	 */
	public synchronized boolean isPropertyDynamic(String key) {
		Object[] p = (Object[])properties.get(key);	
		if (p != null) {
			Boolean b = (Boolean)p[0];
			return b.booleanValue();
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the property with the specified key or null if it
	 * is not set.
	 * 
	 * @param key The key of the property to retrieve, this may
	 * 	not be null.
	 * @return The value of the property or null if it is not set.
	 */
	public synchronized Object getProperty(String key) {
		Object[] p = (Object[])properties.get(key);
		if (p != null) {
			return p[1];
		} else {
			return null;
		}
	}
	
	/**
	 * Determines whether the specified property is set. Returns
	 * true if the property is set (even if it is set to null)
	 * and false if it is not set.
	 * 
	 * @param key The key of the property to retrieve, this may
	 * 	not be null.
	 * @return Returns true if the specified property is set and
	 * 	false if it is not set.
	 */
	public synchronized boolean hasProperty(String key) {
		return properties.containsKey(key);
	}
	
	/**
	 * Returns the property names that are defined by this plug-in
	 * description.
	 * 
	 * @return The property names defined by the plug-in description.
	 */
	public synchronized String[] getProperties() {
		String[] props = new String[properties.size()];
		Enumeration keys = properties.keys();
		for (int i = 0; keys.hasMoreElements(); i++) {
			props[i] = (String)keys.nextElement();
		}
		return props;
	}

	/**
	 * Returns the ability of the plug-in.
	 * 
	 * @return The ability of the plug-in.
	 */
	public short getAbility() {
		return ability;
	}

	/**
	 * Returns the extension point that is supported by this plug-in.
	 * The value can be one of the three extensions defined by the 
	 * extension constants or a combination of them that is concatenated
	 * using a logical or.
	 * 
	 * @return The extension supported by the plug-in.
	 */
	public short getExtension() {
		return extension;
	}

	/**
	 * Determines whether the passed object equals this plug-in 
	 * description. Two plug-in descriptions are equal if they have
	 * the same key and creator system. If the creator system or
	 * the key of one of the descriptions is not specified the
	 * method will test for reference equality.
	 * 
	 * @param o The object to compare.
	 * @return True if the plug-in descriptions are the same object
	 * 	or if they have the same key generated by the same system.
	 */
	public boolean equals(Object o) {
		if (o != null && getClass() == o.getClass()) {
			PluginDescription d = (PluginDescription)o;
			return (getAbility() == d.getAbility());
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the hash code of the plug-in description. If the key
	 * is set, the hash code will be equal to the key, otherwise
	 * the system generated hash code will be returned.
	 * 
	 * @return The hash code of the plug-in description (typically
	 * 	this will be the value of the key.
	 */
	public int hashCode() {
		return getAbility();
	}

	
	/**
	 * Deserializes the description from the given input stream. The listeners
	 * that were registered at the original description are not deserialized. 
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown if the deserialization fails.
	 */
	public synchronized void readObject(IObjectInput input) throws IOException {
		ability = ((Short)input.readObject()).shortValue();
		extension = ((Short) input.readObject()).shortValue();
		properties = ((Hashtable) input.readObject());
	}

	/**
	 * Serializes the description to the given output stream. The listeners
	 * that are registered at the description are not serialized, they are
	 * transient.
	 * 
	 * @param output The stream to write to.
	 * @throws IOException Thrown if the serialization fails.
	 */
	public synchronized void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(new Short(ability));
		output.writeObject(new Short(extension));
		output.writeObject(properties);
	}

}
