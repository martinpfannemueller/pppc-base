/**
 * 
 */
package info.pppc.base.system.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The stream buffer provides input and output streams
 * that are connected to each other through a threadsafe
 * buffer.
 * 
 * @author Mac
 */
public class StreamBuffer {

	/**
	 * The current position for reading.
	 */
	int read = 0;
	
	/**
	 * The number of available bytes.
	 */
	int available = 0;
	
	/**
	 * The current position for writing.
	 */
	int write = 0;
	
	/**
	 * A flag that indicates whether the buffer is closed.
	 */
	boolean closed = false;
	
	/**
	 * The buffer that is read and written.
	 */
	byte[] buffer;
	
	/**
	 * The output stream that writes to the buffer.
	 */
	OutputStream output = new OutputStream() {
		public void write(int b) throws IOException {
			synchronized (StreamBuffer.this) {
				if (closed) 
					throw new IOException("Stream bufffer closed.");
				while (available == buffer.length) {
					try {
						if (closed) 
							throw new IOException("Stream bufffer closed.");
						StreamBuffer.this.wait();
					} catch (InterruptedException e) {
						throw new IOException("Thread got interrupted.");
					}
				}
				buffer[write] = (byte)b;
				write += 1;
				if (write == buffer.length) 
					write = 0;
				available += 1;
				StreamBuffer.this.notify();
			}
		};
		public void close() throws IOException {
			StreamBuffer.this.close();
		};
	};
	
	/**
	 * The input stream that reads from the buffer.
	 */
	InputStream input = new InputStream() {
		public int read() throws IOException {
			synchronized (StreamBuffer.this) {
				while (available == 0) {
					if (closed) 
						throw new IOException("Stream bufffer closed.");
					try {
						StreamBuffer.this.wait();
					} catch (InterruptedException e) {
						throw new IOException("Thread got interrupted.");
					}
				}
				int result = buffer[read] & 0xff;
				read += 1;
				if (read == buffer.length) read = 0;
				available -= 1;
				StreamBuffer.this.notify();
				return result;
			}
		};
		public int available() throws IOException {
			synchronized (StreamBuffer.this) {
				if (available == 0 && closed) 
					throw new IOException("Stream buffer closed.");
				return available;	
			}
		};
		public void close() throws IOException {
			StreamBuffer.this.close();
		};
	};
	
	/**
	 * Creates a new stream buffer with the specified buffer size.
	 * 
	 * @param size The size of the stream buffer.
	 */
	public StreamBuffer(int size) {
		buffer = new byte[size];
	}
	
	/**
	 * Closes the buffer which forces the streams to close.
	 */
	public synchronized void close() {
		closed = true;
		notify();
	}
	
	/**
	 * Returns the input stream.
	 * 
	 * @return The input stream.
	 */
	public InputStream getInputStream() {
		return input;
	}
	
	/**
	 * Returns the output stream.
	 * 
	 * @return The output stream.
	 */
	public OutputStream getOutputStream() {
		return output;
	}
	
	
}
