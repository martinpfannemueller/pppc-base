package info.pppc.base.system;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;

/**
 * The description of the device. The device description entails the well known services 
 * registered at the device. The device description can be observed by listeners and all 
 * calls to it are synchronized. Note that the device description supports serialization, 
 * however, registered listeners will not be serialized. 
 * 
 * @author Marcus Handte
 */
public final class DeviceDescription implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BD";
	
	/**
	 * The device type constant that signals that the device type is unknown.
	 */
	public static final short TYPE_UNKOWN = 0;
	
	/**
	 * The device type constant for notebook personal computers.
	 */
	public static final short TYPE_LAPTOP = 1;
	
	/**
	 * The device type constant for desktop personal computers.
	 */
	public static final short TYPE_DESKTOP = 2;
	
	/**
	 * The device type constant for servers.
	 */
	public static final short TYPE_MAINFRAME = 3;
	
	/**
	 * The device type constant for mobile phones.
	 */
	public static final short TYPE_PHONE = 4;
	
	/**
	 * The device type constant for personal digital assistants.
	 */
	public static final short TYPE_PDA = 5;
	
	/**
	 * The device type constant for tablet personal computers.
	 */
	public static final short TYPE_TABLET = 6;

	/**
	 * The device type constant for embedded sensors.
	 */
	public static final short TYPE_SENSOR = 7;

	/**
	 * The device type constant for access points.
	 */
	public static final short TYPE_WRT = 8;
	
	/**
	 * Event constant that defines that a new known service has been added.
	 * The data object of the event will contain the added id.
	 */
	public static final int EVENT_SERVICE_ADDED = 1;
	
	/**
	 * Event constant that defines that a known service has been removed.
	 * The data object of the event will contain the removed id.
	 */
	public static final int EVENT_SERVICE_REMOVED = 2;

	/**
	 * The object id of the known services of the device.
	 */
	private ObjectID[] services = new ObjectID[0];

	/**
	 * The bundle that manages the listeners of this description.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * The name of the device. This is a human readable string with
	 * a maximum length of 10. The human readable name is only 
	 * used by user interfaces.
	 */
	private String name;

	/**
	 * The type of the device. This should be one of the type constants
	 * defined in this class. The device type is only used by user
	 * interfaces.
	 */
	private short type;

	/**
	 * The system id of the device described by this description.
	 */
	private SystemID systemID;
	
	/**
	 * Creates a new uninitialized device description. This constructor
	 * is solely intended for deserialization purposes, it should not be
	 * called by user code.
	 */
	public DeviceDescription() {
		super();
	}
	
	/**
	 * Creates a new device description for the specified device.
	 * 
	 * @param systemID The system id of the device.
	 * @param name The name of the device used in user interfaces.
	 * @param type The type of the device used in user interfaces.
	 */
	public DeviceDescription(SystemID systemID, String name, short type) {
		// check and set system id
		if (systemID == null) throw new NullPointerException("System id is null.");
		this.systemID = systemID;
		// check and set name
		if (name == null) {
			this.name = "";
		} else if (name.length() > 10) {
			this.name = name.substring(0, 10);	
		} else {
			this.name = name;	
		}
		// check and set type
		if (type > 9 || type < 0) {
			this.type = TYPE_UNKOWN;
		} else {
			this.type = type;	
		}
	}
	
	/**
	 * Returns the device name. This will be a string with the maximum
	 * length of 10 characters.
	 * 
	 * @return The name of the device (human readable).
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the type of the device. This should be one of the type
	 * constants defined in this class.
	 * 
	 * @return The device type as a short.
	 */
	public short getType() {
		return type;
	}

	/**
	 * Returns the system id of the device description.
	 * 
	 * @return The system id of the device description.
	 */
	public SystemID getSystemID() {
		return systemID;
	}
	
	/**
	 * Registers a listener for the specified type of event. The device description 
	 * emits EVENT_SERVICE_ADDED events and EVENT_SERVICE_REMOVED events for changes 
	 * to the set of well known services on the device. If such an event is fired,
	 * the data object will contain the object id of the removed or added well known
	 * service. 
	 * 
	 * @param type The type to register for. At the present time EVENT_SERVICE_REMOVED
	 * and EVENT_SERVICE_ADDED events are emitted for changed object ids.
	 * @param listener The listener to add.
	 */
	public void addListener(int type, IListener listener) {
		listeners.addListener(type, listener);	
	}
	
	/**
	 * Unregisters the specified listener from the specified set of 
	 * event types.
	 * 
	 * @param type The types to unregister for.
	 * @param listener The listener to unregister.
	 * @return True if the listener is no longer registered for any event,
	 * 	false if the listener is still registered or if it has not been
	 * 	registered.
	 */
	public boolean removeListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Returns the well known object ids of well known services of
	 * the device. Note that the array returned by this method will
	 * be a copy of the array used by the description.
	 * 
	 * @return The well known object ids.
	 */
	public synchronized ObjectID[] getServices() {
		ObjectID[] result = new ObjectID[services.length];
		System.arraycopy(services, 0, result, 0, result.length);
		return result;
	}

	/**
	 * Determines whether the service description contains the specified 
	 * well known service.
	 * 
	 * @param id The object id of the well known service to lookup.
	 * @return True if the service description contains the well known
	 * 	service, false otherwise.
	 */
	public synchronized boolean hasService(ObjectID id) {
		if (id != null) {
			for (int i = 0, length = services.length; i < length; i++) {
				if (id.equals(services[i])) {
					return true;
				}
			}
			return false;
		} else {
			throw new NullPointerException("The object id must not be null.");
		}
	}

	/**
	 * Adds the id of a well known service and sends a notification to the
	 * registered listeners. If the id is already contained in the description,
	 * no change is propagated. The change is propagated in sync. Thus, all
	 * listeners will already hold the lock. The change is propagated as a
	 * EVENT_SERVICE_ADDED event that contains the added id as data object. 
	 * 
	 * @param id The id of the object to add.
	 */
	public synchronized void addService(ObjectID id) {
		if (id != null) {
			if (! hasService(id)) {
				ObjectID[] temp = new ObjectID[services.length + 1];
				System.arraycopy(services, 0, temp, 0, services.length);
				temp[services.length] = id;
				services = temp;
				listeners.fireEvent(EVENT_SERVICE_ADDED, id);		
			}
		} else {
			throw new NullPointerException("Object id must not be null.");
		}
	}
	
	/**
	 * Removes the specified id from the description and sends a notification
	 * to all registered listeners. If the id is not contained in the description,
	 * no change is propagated. The change is propagated in sync. Thus, all
	 * listeners will already hold the lock. The event is propagated as 
	 * EVENT_SERVICE_REMOVED event that contains the removed id as data object.
	 * 
	 * @param id The id of the object to remove.
	 */
	public synchronized void removeService(ObjectID id) {
		if (id != null) {
			ObjectID[] temp = new ObjectID[services.length - 1];
			for (int i = 0, length = services.length; i < length; i++) {
				if (id.equals(services[i])) {
					for (int j = i + 1; j < length; j++) {
						temp[j - 1] = services[j];
					}
					services = temp;
					listeners.fireEvent(EVENT_SERVICE_REMOVED, id);
					break;
				} else if (i != length - 1) {
					temp[i] = services[i];
				}
			}
		} else {
			throw new NullPointerException("Object id must not be null.");
		}		
	}

	/**
	 * Reads a description from a stream. Note that the listeners are not
	 * read from the stream, they are transient.
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown by the stream.
	 */
	public synchronized void readObject(IObjectInput input) throws IOException {
		systemID = (SystemID)input.readObject();
		name = input.readUTF();
		type = input.readShort();
		int length = input.readInt();
		services = new ObjectID[length];
		for (int i = 0; i < length; i++) {
			services[i] = (ObjectID)input.readObject();
		}			
	}

	/**
	 * Writes a description to a stream. Note that the listeners are not
	 * written to the stream, they are transient.
	 * 
	 * @param output The stream to write to.
	 * @throws IOException Thrown by the stream.
	 */
	public synchronized void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(systemID);
		output.writeUTF(name);
		output.writeShort(type);
		output.writeInt(services.length);
		for (int i = 0, length = services.length; i < length; i++) {
			output.writeObject(services[i]);
		}
	}
	
	/**
	 * Returns a string representation of the device description.
	 * 
	 * @return A string representation of the description.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("SYSTEMID (");
		b.append(systemID);
		b.append(") NAME (");
		b.append(name);
		b.append(") TYPE (");
		b.append(type);
		b.append(")");
		b.append("SERVICES (");
		for (int i = 0; i < services.length; i++) {
			b.append(services[i]);
			if (i != services.length - 1) {
				b.append(", ");
			}
		}
		b.append(")");
		return b.toString();
	}

}
