package info.pppc.base.tutorial.serial;

import java.io.IOException;
import java.util.Vector;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

/**
 * All objects that are transmitted to a remote system must
 * implement serializable. For some objects like java.util.Vector
 * and java.util.Hashtable the system provides automatic 
 * serialization. The same holds true for primitive types and
 * one-dimensional object arrays. A more detailed explanation
 * can be found in the description of the ObjectInputStream
 * and ObjectOutputStream classes.
 * 
 * Note that due to limitations of the JVM on small devices,
 * it is not (and never will be) possible to deserialize 
 * objects without public default constructor.
 * 
 * @author Marcus Handte
 */
public class SerialObject implements ISerializable {

	/**
	 * A simple int.
	 */
	public int anInt = 1;
	
	/**
	 * A simple string.
	 */
	public String aString = "Hello";
	
	/**
	 * A boolean.
	 */
	public Boolean aBoolean = new Boolean(false);;
	
	/**
	 * A vector.
	 */
	public Vector aVector = new Vector();
	
	/**
	 * An object array.
	 */
	public Object[] anObjectArray = new Object[0];
		
	/**
	 * Every serializable object MUST have a public default
	 * constructor. Other constructors cannot be called via
	 * reflection on cldc devices. 
	 */
	public SerialObject() {
		// needs to be here
	}
	
	/**
	 * This method reads the internals of the object from
	 * an input stream. This method must always be implemented
	 * and the sequence of reading must be identical to the
	 * sequence of writing in the writeObject method.
	 * 
	 * @param input The input stream to read from.
	 * @throws IOException Thrown if the deserialization fails.
	 */
	public void readObject(IObjectInput input) throws IOException {
		anInt = input.readInt();
		aString = input.readUTF();
		// unchecked casting is ok here
		aBoolean = (Boolean)input.readObject();
		aVector = (Vector)input.readObject();
		anObjectArray = (Object[])input.readObject();
	}

	/**
	 * This method writes the internals of the object to
	 * an output stream. This method must always be implemented
	 * and the sequence of writing must be identical to the
	 * sequence of reading in the readObject method.
	 * 
	 * @param output The output stream to write to.
	 * @throws IOException Thrown if the serialization fails.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeInt(anInt);
		output.writeUTF(aString);
		output.writeObject(aBoolean);
		output.writeObject(aVector);
		output.writeObject(anObjectArray);
	}

	/**
	 * Used to show the results.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("Integer: " + anInt + "\n");
		b.append("String: " + aString + "\n");
		b.append("Boolean: " + aBoolean + "\n");
		b.append("Vector (size): " + aVector.size() + "\n");
		b.append("ObjectArray (size): " + anObjectArray.length + "\n");
		return b.toString();
	}
	
}
