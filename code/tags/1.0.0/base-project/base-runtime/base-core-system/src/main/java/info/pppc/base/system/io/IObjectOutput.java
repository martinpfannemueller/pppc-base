package info.pppc.base.system.io;

import java.io.IOException;

/**
 * The object output interface is a common interface used to write
 * primitive types and composed objects to some abstract target.
 *  
 * @author Marcus Handte
 */
public interface IObjectOutput {

	/**
	 * Writes a boolean.
	 *
	 * @param val the boolean value to write
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBoolean(boolean val) throws IOException;

	/**
	 * Writes the specified 16-bit character.
	 *
	 * @param val The character to be written
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeChar(char val) throws IOException;

	/**
	 * Writes a 32-bit integer. 
	 *
	 * @param val The integer to be written.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeInt(int val) throws IOException;

	/**
	 * Writes a 64-bit integer.
	 *
	 * @param val The 64-bit integer.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeLong(long val) throws IOException;

	/**
	 * Writes the specified 16-bit integer.
	 *
	 * @param val The 16-bit integer to be written.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeShort(int val) throws IOException;

	/**
	 * Writes the specified String out in UTF format.
	 *
	 * @param str The String to be written in UTF format.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeUTF(String str) throws IOException;

	/**
	 * Writes a 8-bit byte.
	 *
	 * @param val The byte value to write
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeByte(byte val) throws IOException;
	
	/**
	 * Writes a number of bytes from the byte array buffer. The number of 
	 * bytes is defined by the length of the array.
	 *
	 * @param buffer The buffer to be written
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBytes(byte buffer[]) throws IOException;
	
	/**
	 * Writes a number of bytes from the byte array buffer starting at 
	 * the offset index. The number of bytes is defined by the length
	 * parameter.
	 *
	 * @param buffer The buffer to be written.
	 * @param offset The offset in buffer.
	 * @param length The number of bytes to write.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBytes(byte buffer[], int offset, int length) throws IOException;

	/**
	 * Writes an object.
	 * 
	 * @param val The object to write.
	 * @throws IOException Thrown if an problem occurs.
	 */
	public void writeObject(Object val) throws IOException;

}
