package info.pppc.irsock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * This class is used for reading input from an IR socket.
 * 
 * @author bator
 */
public class IRInputStream extends InputStream {

	/**
	 * The implementation used to read data.
	 */
	private IRSocketImpl implementation;

	/**
	 * Constructs a InputStream using the given
	 * native implementation.
	 * 
	 * @param impl The native implementation.
	 */
	protected IRInputStream(IRSocketImpl impl) {
		implementation = impl;
	}

	/**
	 * Closes the stream.
	 */
	public void close() throws IOException {
		// nothing to do here
	}

	/**
	 * Returns the number of available bytes in stream. Note
	 * that this method is not implemented on windows device. 
	 * 
	 * @return Available data or -1
	 */
	public int available() throws IOException {
		return implementation.available();
	}

	/**
	 * Skips a specified number of bytes in the stream.
	 * 
	 * @param count The number of bytes to skip.
	 * @return The number of bytes skipped.
	 */
	public long skip(long count) throws IOException {
		long skipped = 0;
		try {
			while (skipped < count) {
				int ret = read();
				if (ret < 0)
					break;
				else
					skipped++;
			}
		} catch (InterruptedIOException e) {
		}
		return skipped;
	}

	/**
	 * Read a single byte.
	 *
	 * @return A byte or -1.
	 * 
	 */
	public int read() throws IOException {
		return implementation.read();
	}

	/**
	 * Read a block of max. len bytes into the buffer,
	 * beginning at offset off.
	 * 
	 * @param b buffer for reading
	 * @param off offset in buffer to use
	 * @param len max. len of buffer
	 * @return readed bytes
	 */
	public int read(byte[] b, int off, int len) throws IOException, NullPointerException, IndexOutOfBoundsException {
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || len < 0)
			throw new IndexOutOfBoundsException();
		if (b.length - off < len)
			throw new IndexOutOfBoundsException();
		return implementation.read(b, off, len);
	}
}
