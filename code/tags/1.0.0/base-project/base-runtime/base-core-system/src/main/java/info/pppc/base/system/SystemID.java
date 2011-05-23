package info.pppc.base.system;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.util.Random;

/**
 * The system id identifies a single system. The actual identifier
 * that is assigned to a system can be controlled via a system 
 * property. If this property is not set, the system identifier is
 * generated randomly upon startup. Although, this is quite useful
 * to simplify debugging, random identifiers are dangerous since there
 * is a (small) chance that they might collide. Thus, it is safer to
 * assign a system identifier manually when running actual applications.
 * 
 * @author Marcus Handte
 */
public final class SystemID implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BY";
	
	/**
	 * Digits used to create a string.
	 */
	private static final char digits[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'a', 'b', 'c', 'd', 'e', 'f'
    };

	/**
	 * The id of the local system. Currently, this value is created
	 * randomly. Actually, it should be configured manually outside
	 * of a development environment.
	 */
	public static final SystemID SYSTEM = new SystemID(new Random().nextLong());
	
	/**
	 * The length of the byte array representation of the system id. This
	 * is a global constant. The byte representations of all system ids
	 * will have the same length. At the present time this length is 8
	 * (a single serialized long) however as it is not guaranteed that this
	 * length stays 8, users should always rely on this constant.
	 */
	public static final int LENGTH = 20;

	/**
	 * Initializes the local system id upon startup by parsing the systems
	 * internal identifier from the properties passed to the JVM on startup.
	 */
	static {
		String property = System.getProperty(InvocationBroker.PROPERTY_DEVICE_IDENTIFIER);
		if (property != null) {
			try {
				long id = Long.parseLong(property);
				setBytes(toBytes(id)); 
			} catch (NumberFormatException e) {
				Logging.error(SystemID.class, "Could not restore system id.", e);
			}
		}
	}

	/**
	 * Converts a compact byte representation of a system id
	 * into a system id object.
	 * 
	 * @param bytes The bytes that represent a system id.
	 * @return The system id represented by the byte array.
	 * @throws NumberFormatException Thrown if the bytes do not
	 * 	represent a valid system id.
	 */
	public static SystemID valueOf(byte[] bytes) throws NumberFormatException {
		return new SystemID(bytes);
	}
	
	/**
	 * The identifier for systems.
	 */
	private byte[] systemID;
	
	/**
	 * The hash code for the system id.
	 */
	private int hashCode;

	/**
	 * Creates a new uninitialized system id. This constructor is
	 * solely intended for deserialization purposes. If you want 
	 * to create a new system id, you must call the static create
	 * method.
	 */
	public SystemID() {
		systemID = new byte[LENGTH];
	}

	/**
	 * Creates a system id with the given id.
	 * 
	 * @param id The id of the system id.
	 */
	public SystemID(long id) {
		systemID = toBytes(id);
		rehash();
	}
	
	/**
	 * Creates a system id from a byte sequence. The
	 * byte sequence must have at least the length
	 * of LENGTH.
	 * 
	 * @param id The byte sequence.
	 */
	public SystemID(byte[] id) {
		if (id.length != LENGTH) 
			throw new IllegalArgumentException("Illegal system id.");
		systemID = id;
		rehash();
	}

	/**
	 * Reads the SystemID from a stream.
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown if reading caused this exception.
	 */
	public void readObject(IObjectInput input) throws IOException {
		input.readBytes(systemID);
		rehash();
	}

	/**
	 * Writes the SystemID to the given stream.
	 * 
	 * @param output The stream to write to.
	 * @throws IOException If writing to the stream caused an exception.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeBytes(systemID);
	}

	/**
	 * Determines whether this object equals the given object. For 
	 * SystemIDs this method does not compare object equality, but
	 * id-based equality. 
	 * 
	 * @param object The object used for the comparison.
	 * @return True if the objects are equal or if two SystemIDs
	 * 	denote the same system.
	 */
	public boolean equals(Object object) {
		if (object != null && object.getClass() == getClass()) {
			SystemID sid = (SystemID)object;
			byte[] otherID = sid.systemID;
			for (int i = 0; i < LENGTH; i++) {
				if (otherID[i] != systemID[i]) return false;
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Compares this system id with the specified system id and returns
	 * 0 if this id equals the system id, returns 1 if this id is larger
	 * than the system id and -1 if this id is smaller than the system id.
	 * This method will throw a null pointer exception if the passed system
	 * id is null.
	 * 
	 * @param id The system id to compare this id to.
	 * @return 0 if equal, 1 if this is larger, -1 if this is smaller.
	 */
	public int compareTo(SystemID id) {
		byte[] otherID = id.systemID;
		for (int i = 0; i < LENGTH; i++) {
			if (otherID[i] < systemID[i]) return 1;
			if (otherID[i] > systemID[i]) return -1;
		}
		return 0;
	}

	/**
	 * Returns the hash code of this system id. This hash code solely
	 * depends on the denoted system.
	 * 
	 * @return The hash code of the system.
	 */
	public int hashCode() {
		return hashCode;		
	}
	
	/**
	 * Returns a string representation.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		char[] string = new char[LENGTH * 2];
		for (int i = 0; i < LENGTH; i++) {
			int high = (systemID[i] & 0xff) >> 4;
			int low = (systemID[i] & 0x0f);
			string[i * 2] = digits[high];
			string[(i * 2) + 1] = digits[low];               
		}
		return new String(string);
	}
	
	/**
	 * Returns a compact byte representation of the system id.
	 * The length of this representation is constant for all
	 * system ids.
	 * 
	 * @return A compact byte representation of the system id.
	 */
	public byte[] getBytes() {
		byte[] copy = new byte[LENGTH];
		System.arraycopy(systemID, 0, copy, 0, LENGTH);
		return copy;
	}
	
	/**
	 * (Re-)computes the hash code for the system id.
	 */
	private void rehash() {
		int code = 0;
		for (int i = 0; i < LENGTH; i += 4) {
			code ^= systemID[i] << 24 | systemID[i + 1] << 16 | systemID[i + 2] << 8 | systemID[i + 3];
		}
		hashCode = code;
	}
	
	/**
	 * Casts a long value to a byte array that
	 * has the length of the id array.
	 * 
	 * @param value The value to cast.
	 * @return A byte array with the id array length.
	 */
	public static final byte[] toBytes(long value) {
		byte[] bytes = new byte[LENGTH];
		for (int i = 0; i < 8; i++) {
			bytes[i] = (byte)(value >> (8 * i));
		}
		return bytes;		
	}
	
	/**
	 * Sets the local system id. Warning, this method may only be called
	 * before the micro broker is booting up.
	 * 
	 * @param system The byte representation of the local system id.
	 */
	public static final void setBytes(byte[] system) {
		SYSTEM.systemID = system;
		SYSTEM.rehash();
	}
	
}
