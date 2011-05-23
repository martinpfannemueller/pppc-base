package info.pppc.base.tutorial.rmi;

import info.pppc.base.system.InvocationException;

/**
 * The interface of the rmi tutorial service. 
 * 
 * @author Marcus Handte
 */
public interface IRmi {

	/**
	 * Prints the specified string onto the standard output.
	 * 
	 * @param string The string to print.
	 * @throws InvocationException Thrown by base if the call fails.
	 */
	public void println(String string) throws InvocationException;

}
