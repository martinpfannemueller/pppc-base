package info.pppc.basex.plugin.transceiver.spot;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The shielded output stream provides a wrapper for output streams
 * that shields the user from exceptions that are not declared by 
 * the output stream methods. 
 * 
 * @author Marcus Handte
 */
public class ShieldedOutputStream extends OutputStream {
	
	/**
	 * The actual stream that we want to use.
	 */
	private OutputStream output;
	
	/**
	 * Creates a new output stream that uses the
	 * passed output stream and isolates the application
	 * from undeclared exceptions.
	 * 
	 * @param output The output stream to use.
	 */
	public ShieldedOutputStream(OutputStream output) {
		this.output = output;
	}
	
	/**
	 * Writes the specified byte to the output.
	 * 
	 * @param arg0 The byte to write.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(int arg0) throws IOException {
		try {
			output.write(arg0);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Writes the specified byte array to the output.
	 * 
	 * @param arg0 The byte array to write.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(byte[] arg0) throws IOException {
		try {
			output.write(arg0);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Writes the specified byte array to the output.
	 * 
	 * @param arg0 The byte array to write.
	 * @param arg1 The offset to write from.
	 * @param arg2 The length to write.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		try {
			output.write(arg0, arg1, arg2);
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}
	}
	
	/**
	 * Flushes the underlying output stream.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void flush() throws IOException {
		try {
			output.flush();
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}

	}
	
	/**
	 * Closes the underlying stream.
	 * 
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void close() throws IOException {
		try {
			output.close();
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException("Caught unknown exception.");
		}

	}
}
