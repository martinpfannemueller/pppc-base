package info.pppc.basex.plugin.transceiver.spot;

import java.io.IOException;
import java.io.InputStream;

/**
 * The shielded input stream provides a wrapper for input streams
 * that shields the user from exceptions that are not declared by 
 * the input stream methods. 
 * 
 * @author Marcus Handte
 */
public class ShieldedInputStream extends InputStream {
	
	/**
	 * The input stream to read from.
	 */
	private InputStream input;
	
	/**
	 * Creates a new shielded input stream that uses
	 * the passed stream as underlying input.
	 * 
	 * @param input The input stream to read from.
	 */
	public ShieldedInputStream(InputStream input) {
		this.input = input;
	}

	/**
	 * Reads a byte from the underlying stream.
	 * 
	 * @return The byte that has been read or eof.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int read() throws IOException {
		try {
			return input.read();
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Reads a byte array from the underlying stream.
	 * 
	 * @param arg0 The byte array to fill.
	 * @return The number of bytes read or eof.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int read(byte[] arg0) throws IOException {
		try {
			return input.read(arg0);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Reads a byte array from the underlying stream.
	 * 
	 * @param arg0 The byte array to fill.
	 * @param arg1 The offset.
	 * @param arg2 The length.
	 * @return The number of bytes read or eof.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		try {
			return input.read(arg0, arg1, arg2);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Closes the input stream.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void close() throws IOException {
		try {
			input.close();
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Returns the number of bytes that can be read non-blocking.
	 * 
	 * @return The number of bytes that are available.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public int available() throws IOException {
		try {
			return input.available();
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}

	}
	
}
