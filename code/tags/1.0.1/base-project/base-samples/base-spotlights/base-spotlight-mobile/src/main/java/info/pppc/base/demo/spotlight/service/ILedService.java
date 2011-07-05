package info.pppc.base.demo.spotlight.service;

import info.pppc.base.system.InvocationException;

import java.util.Vector;

/**
 * The led service lets a user access the leds of a sun spot.
 * 
 * @author Marcus Handte
 */
public interface ILedService {

	/**
	 * Returns the number of available leds.
	 * 
	 * @return The number of available leds.
	 * @throws InvocationException Thrown if the remote call fails.
	 */
	public int getLedCount() throws InvocationException;
	
	/**
	 * Returns a vector with all accessible leds. The vector will
	 * contain led state objects.
	 * 
	 * @return A vector with the led states.
	 * @throws InvocationException Thrown if the remote call fails.
	 */
	public Vector getLedStates() throws InvocationException;
	
	/**
	 * Sets a number of leds to the specified states. The vector must
	 * contain led state objects.
	 * 
	 * @param states The states that shall be set.
	 * @throws InvocationException Thrown if the remote call fails.
	 */
	public void setLedStates(Vector states) throws InvocationException;

	/**
	 * Sets a single state of an led. 
	 * 
	 * @param state The state that shall be set.
	 * @throws InvocationException Thrown if the remote call fails.
	 */
	public void setLedState(LedState state) throws InvocationException;
	
}
