
package info.pppc.base.demo.spotlight.service;

import info.pppc.base.service.Service;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * 
 * @author Marcus Handte
 */
public class LedService extends Service implements ILedService {

	private Vector leds = new Vector();
	
	public LedService() {
		leds.addElement(new LedState(0));
		leds.addElement(new LedState(1));
		leds.addElement(new LedState(2));
		leds.addElement(new LedState(3));
	}
	
	public int getLedCount() throws InvocationException {
		return leds.size();
	}
	
	public Vector getLedStates() throws InvocationException {
		Vector result = new Vector();
		for (int i = 0; i < leds.size(); i++)
			result.addElement(leds.elementAt(i));
		return result;
	}
	
	public void setLedState(LedState state) throws InvocationException {
		Logging.log(getClass(), "Set state received.");
	}
	
	public void setLedStates(Vector states) throws InvocationException {
		Logging.log(getClass(), "Set states received.");
	}


}
