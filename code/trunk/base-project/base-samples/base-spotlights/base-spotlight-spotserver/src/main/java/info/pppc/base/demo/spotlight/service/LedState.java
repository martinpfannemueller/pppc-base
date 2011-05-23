package info.pppc.base.demo.spotlight.service;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;

/**
 * The led state represents the status of an led.
 * 
 * @author Marcus Handte
 */
public class LedState implements ISerializable {

	/**
	 * The number of the led.
	 */
	private int id;
	/**
	 * The red value of the led.
	 */
	private int red;
	/**
	 * The green value of the led.
	 */
	private int green;
	/**
	 * The blue value of the led.
	 */
	private int blue;
	/**
	 * A flag that indicates whether the led is on.
	 */
	private boolean enabled;
	
	/**
	 * Creates a new led state. This constructor
	 * is intended for deserialization purposes.
	 */
	public LedState() {	}
	
	/**
	 * Creates a new led state that represents
	 * the specified led.
	 * 
	 * @param id The number of the led.
	 */
	public LedState(int id) {
		this.id = id;
	}
	
	/**
	 * Reads the object from the input stream.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void readObject(IObjectInput input) throws IOException {
		id = input.readInt();
		red = input.readInt();
		green = input.readInt();
		blue = input.readInt();
		enabled = input.readBoolean();
	}
	
	/**
	 * Writes the object to the output stream.
	 * 
	 * @param output The stream to write to.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeInt(id);
		output.writeInt(red);
		output.writeInt(green);
		output.writeInt(blue);
		output.writeBoolean(enabled);
	}

	/**
	 * Returns the red value of the led.
	 * 
	 * @return The red value.
	 */
	public int getRed() {
		return red;
	}

	/**
	 * Sets the red value of the led.
	 * 
	 * @param red The new red value.
	 */
	public void setRed(int red) {
		this.red = red;
	}

	/**
	 * Returns the green value of the led.
	 * 
	 * @return The green value.
	 */
	public int getGreen() {
		return green;
	}

	/**
	 * Sets the green value of the led.
	 * 
	 * @param green The new green value.
	 */
	public void setGreen(int green) {
		this.green = green;
	}

	/**
	 * Returns the blue value of the led.
	 * 
	 * @return The blue value.
	 */
	public int getBlue() {
		return blue;
	}

	/**
	 * Sets the blue value of the led.
	 * 
	 * @param blue The new blue value.
	 */
	public void setBlue(int blue) {
		this.blue = blue;
	}

	/**
	 * Determines whether the led is on.
	 * 
	 * @return True if on, false otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the led.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the id of the led.
	 * 
	 * @return The id of the led.
	 */
	public int getId() {
		return id;
	}
	
}
