package info.pppc.irsock;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a OutputStream for the IRDA Socket.
 * 
 * @author bator
 */
public class IROutputStream extends OutputStream {

	/**
	 * The socket implementation.
	 */
	private IRSocketImpl implementation = null;

	/**
	 * Creates a new output stream for the specified
	 * ir socket implementation.
	 * 
	 * @param impl Implementation of a socket.
	 */
	public IROutputStream(IRSocketImpl impl) {
		implementation = impl;
	}

	/**
	 * Close the stream.
	 */
	public void close() throws IOException {
		// nothing to do here
	}

	/**
	 * Write b values to the stream.
	 * 
	 * @param b The buffer to read from.
	 * @param off Offset in buffer.
	 * @param len Lenth to write from offset.
	 */
	public void write(byte[] b, int off, int len) throws IOException, NullPointerException, IndexOutOfBoundsException {
		if (b == null)
			throw new NullPointerException();
		if (off < 0 || len < 0)
			throw new IndexOutOfBoundsException();
		if (b.length - off < len)
			throw new IndexOutOfBoundsException();
		implementation.write(b, off, len);
	}

	/**
	 * Write a single byte to stream.
	 * 
	 * @param b value to write
	 */
	public void write(int b) throws IOException {
		implementation.write(b);
	}
}
