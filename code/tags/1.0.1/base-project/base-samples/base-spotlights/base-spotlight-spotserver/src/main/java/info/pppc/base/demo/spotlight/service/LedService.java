package info.pppc.base.demo.spotlight.service;

import info.pppc.base.service.Service;

import java.util.Vector;

import com.sun.spot.sensorboard.peripheral.ITriColorLED;

/**
 * The led service implements the led service interface and provides
 * a user access to a configurable number of leds on the sun spot
 * demo board.
 *  
 * @author Marcus Handte
 */
public class LedService extends Service implements ILedService {

	/**
	 * The vector that contains references to the leds.
	 */
	private Vector leds = new Vector();
	
	/**
	 * Creates a new led service that enables remote access to
	 * the specified set of leds.
	 * 
	 * @param leds The leds that can be accessed remotely.
	 */
	public LedService(ITriColorLED[] leds) {
		for (int i = 0; i < leds.length; i++) {
			if (leds[i] != null && ! this.leds.contains(leds[i])) {
				this.leds.addElement(leds[i]);
			}
		}
	}
	
	/**
	 * Returns the number of leds that can be accessed.
	 * 
	 * @return The number of available leds.
	 */
	public int getLedCount() {
		return leds.size();
	}

	/**
	 * Returns the led states for all leds that are configured.
	 * 
	 * @return The led states wrapped in a vector.
	 */
	public Vector getLedStates() {
		Vector result = new Vector();
		for (int i = 0; i < leds.size(); i++) {
			ITriColorLED led = (ITriColorLED)leds.elementAt(i);
			LedState state = new LedState(i);
			state.setBlue(led.getBlue());
			state.setGreen(led.getGreen());
			state.setRed(led.getRed());
			state.setEnabled(led.isOn());
			result.addElement(state);
		}
		return result;
	}

	/**
	 * Sets the state of a single led.
	 * 
	 * @param state The led state to set.
	 */
	public void setLedState(LedState state) {
		Vector wrap = new Vector();
		wrap.addElement(state);
		setLedStates(wrap);
	}

	/**
	 * Sets the led states of a number of leds.
	 * 
	 * @param states The states to set. 
	 */
	public void setLedStates(Vector states) {
		for (int i = 0; i < states.size(); i++) {
			Object object = states.elementAt(i);
			if (object != null && object instanceof LedState) {
				LedState state = (LedState)object;
				int index = state.getId();
				if (index >= 0 && index < leds.size()) {
					ITriColorLED led = (ITriColorLED)leds.elementAt(index);
					led.setRGB(state.getRed(), state.getGreen(), state.getBlue());
					led.setOn(state.isEnabled());
				}
			}
		}
	}

}
