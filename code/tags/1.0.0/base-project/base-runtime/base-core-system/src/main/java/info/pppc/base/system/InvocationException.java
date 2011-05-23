package info.pppc.base.system;

import java.io.IOException;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

/**
 * This exception is used to denote communication exceptions
 * that occurred in base. The usage of this exception is similar
 * to the usage of the remote exception in Java RMI. Each method of
 * a remote interface exported by an invocation handler (typically a
 * proxy) must declare an invocation exception to deal with
 * failures that happened during communication. 
 * 
 * @author Marcus Handte
 */

public class InvocationException extends Exception implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BE";
	
	/**
	 * The message that is held during serialization.
	 */
	private String message;
	
    /** 
     * Constructs an exception with no detailed information.
     */
	public InvocationException() {
		super();	
	}

    /** 
     * Constructs an exception with the specified message.
     * 
     * @param message A detailed message.
     */
	public InvocationException(String message) {
		super(message);
		this.message = message;
	}
	
	/**
	 * Called to read the invocation.
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void readObject(IObjectInput input) throws IOException {
		message = input.readUTF();
	}
	
	/**
	 * Called to write the invocation.
	 * 
	 * @param output The output to write to.
	 * @throws IOException Thrown by the stream.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeUTF("SERIALIZED SOURCE(" + SystemID.SYSTEM + ") " + message);
	}
	
	/**
	 * Returns the message.
	 * 
	 * @return The message.
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Returns a string representation.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		return getClass().getName() + ": " + message;
	}
		
}
