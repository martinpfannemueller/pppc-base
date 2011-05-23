package info.pppc.base.tutorial.serial;

import info.pppc.base.system.InvocationException;

/**
 * A simple service that receives the serial object.
 * 
 * @author Marcus Handte
 */
public interface ISerial {

	/**
	 * Prints the object.
	 * 
	 * @param object The object to print.
	 * @throws InvocationException Thrown by BASE if a failure occurs.
	 */
	public void print(SerialObject object) throws InvocationException;
	
}
