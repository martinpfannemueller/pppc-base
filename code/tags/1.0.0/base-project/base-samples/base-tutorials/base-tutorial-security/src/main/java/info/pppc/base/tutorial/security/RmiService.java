package info.pppc.base.tutorial.security;

import info.pppc.base.service.Service;
import info.pppc.base.system.util.Logging;

/**
 * This service supports printing. It extends the basic service
 * and implements the desired application interface.
 * 
 * @author Marcus Handte
 */
public class RmiService extends Service implements IRmi {

	/**
	 * Creates a new tutorial service.
	 */
	public RmiService() {	}
	
	/**
	 * Prints a string using the system logger.
	 * 
	 * @param string The string to print.
	 */
	public void println(String string) {
		Logging.log(getClass(), string);
	}

}
