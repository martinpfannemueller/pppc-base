package info.pppc.base.system.io;

import java.io.IOException;

/**
 * The object input interface is a common interface used to read
 * primitive types and composed objects from some abstract source.
 * 
 * @author Marcus Handte
 */
public interface IObjectInput {

	/**
	 * Reads a boolean.
	 *
	 * @return The next boolean.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public boolean readBoolean() throws IOException;

	/**
	 * Reads a char.
	 *
	 * @return The next char.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public char readChar() throws IOException;

	/**
	 * Reads a 32 bit integer.
	 *
	 * @return The next 32 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public int readInt() throws IOException;

	/**
	 * Reads a 64 bit integer.
	 *
	 * @return The next 64 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public long readLong() throws IOException;

	/**
	 * Reads a 16 bit integer.
	 *
	 * @return The next 16 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public short readShort() throws IOException;

	/**
	 * Reads a utf encoded string.
	 *
	 * @return The next utf encoded string.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public String readUTF() throws IOException;
	
	/**
	 * Reads a byte.
	 *
	 * @return The next byte.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public byte readByte() throws IOException;

	/**
	 * Reads a number of bytes and writes them into the buffer.
	 * The number of bytes read is defined by the lenght of the
	 * buffer.
	 * 
	 * @param buffer The buffer to fill.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public void readBytes(byte[] buffer) throws IOException;
	
	/**
	 * Reads a number of bytes and writes them into the buffer
	 * starting from the specified offset. The number of bytes
	 * read is defined by the length parameter.
	 * 
	 * @param buffer The buffer to fill.
	 * @param offset The offset to start from.
	 * @param length The number of bytes to fill.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public void readBytes(byte[] buffer, int offset, int length) throws IOException;

	/**
	 * Reads an object.
	 * 
	 * @return The object that has been read.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public Object readObject() throws IOException;

}
