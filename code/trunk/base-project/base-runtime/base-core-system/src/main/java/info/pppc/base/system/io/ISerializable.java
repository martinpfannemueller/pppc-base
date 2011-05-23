package info.pppc.base.system.io;

import java.io.IOException;

/**
 * Classes that are tagged with this interface are serializable with the 
 * object input and output streams from BASE. Objects that implement this
 * method must have a public default constructor. The public default 
 * constructor is called whenever a serialized object will be deserialized.
 * 
 * @author Marcus Handte
 */
public interface ISerializable {

	/**
	 * This method is called when an implementing object is deserialized.
	 * 
	 * @param input The input stream used to deserialize from.
	 * @throws IOException Thrown if an exception occurs while reading the object.
	 */
	public void readObject(IObjectInput input) throws IOException;

	/**
	 * This method is called when an implementing object is serialized.
	 * 
	 * @param output The output stream used to serialize the object.
	 * @throws IOException Thrown if an exception occurs.
	 */
	public void writeObject(IObjectOutput output) throws IOException;

}
