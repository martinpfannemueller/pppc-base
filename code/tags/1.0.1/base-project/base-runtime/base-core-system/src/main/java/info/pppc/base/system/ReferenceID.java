package info.pppc.base.system;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;

/**
 * A reference identifier represents a remote object reference. It contains
 * a system identifier that denotes the location and an object identifier
 * that denotes the object.
 * 
 * @author Marcus Handte
 */
public final class ReferenceID implements ISerializable {
	
	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BR";
	
	/**
	 * The identity of the object.
	 */
	private ObjectID object;
	
	/**
	 * The location of the object.
	 */
	private SystemID system;

	/**
	 * Creates a new empty reference. This constructor
	 * should only be used for serialization purposes.
	 */
	public ReferenceID() { }
	
	/**
	 * Creates a new reference that points to the specified system.
	 * 
	 * @param system The location of the registry.
	 */
	public ReferenceID(SystemID system) {
		this.system = system;
	}

	/**
	 * Creates a new reference to the specified object at
	 * the specified location.
	 * 
	 * @param system The location of the object.
	 * @param object The identity of the object.
	 */
	public ReferenceID(SystemID system, ObjectID object) {
		this.system = system;
		this.object = object;
	}

	/**
	 * Determines whether the reference points to anything else
	 * than null.
	 * 
	 * @return True if system or object id are not null, false
	 * 	otherwise.
	 */
	public boolean isEmpty() {
		return (system == null && object == null);
	}

	/**
	 * Returns the location of the referenced object.
	 * 
	 * @return The location of the object.
	 */
	public SystemID getSystem() {
		return system;	
	}
	
	/**
	 * Sets the location of the referenced object.
	 * 
	 * @param system The location of the object.
	 */
	public void setSystem(SystemID system) {
		this.system = system;
	}
	
	/**
	 * Returns the identity of the referenced object.
	 * 
	 * @return The identity of the object.
	 */
	public ObjectID getObject() {
		return object;
	}
	
	/**
	 * Sets the identity of the referenced object.
	 * 
	 * @param object The identity of the object.
	 */
	public void setObject(ObjectID object) {
		this.object = object;
	}
	
	/**
	 * Reads the reference identifier from the stream.
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown if reading caused this exception.
	 */
	public void readObject(IObjectInput input) throws IOException {
		system = (SystemID)input.readObject();
		object = (ObjectID)input.readObject();
	}
	
	/**
	 * Writes the reference identifier to the given stream.
	 * 
	 * @param output The stream to write to.
	 * @throws IOException If writing to the stream caused an exception.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(system);
		output.writeObject(object);
	}

	/**
	 * Returns a string representation.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("System(");
		if (system != null) buffer.append(system.toString());
		else buffer.append("NULL");
		buffer.append(") Object(");
		if (object != null) buffer.append(object.toString());
		else buffer.append("NULL");
		buffer.append(")");
		return buffer.toString();
	}
	
	/**
	 * Returns a hash code for the reference id that is based on its 
	 * system id and object id.
	 * 
	 * @return A hash code for the reference id.
	 */
	public int hashCode() {
		return (system != null ? system.hashCode() : "NULL".hashCode())
			+ (object != null ? object.hashCode() : "NULL".hashCode());
	}
	
	/**
	 * Determines whether two reference identifiers are equal. 
	 * 
	 * @param obj The object to compare to.
	 * @return True if they are equal, false otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ReferenceID) {
			ReferenceID r = (ReferenceID)obj;
			return (system != null ? system.equals(r.system) : r.system == null) &&
				(object != null ? object.equals(r.object) : r.object == null);
		}
		return super.equals(obj);
	}
}
