package info.pppc.base.tutorial.serial;

import info.pppc.base.service.Service;
import info.pppc.base.system.util.Logging;

/**
 * A simple service that takes a user-defined serializable
 * object as input.
 * 
 * @author Marcus Handte
 */
public class SerialService extends Service implements ISerial {

	/**
	 * Prints the serializable object.
	 * 
	 * @param object The object to print.
	 */
	public void print(SerialObject object) {
		Logging.log(getClass(), object.toString());
	}

}
