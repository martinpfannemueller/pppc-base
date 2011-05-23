package info.pppc.base.swtui.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A ring buffer helper class that provides a generic buffer with input
 * and output streams.
 * 
 * @author Marcus Handte
 */
public class ConsoleBuffer {

	/**
	 * The input stream of the console buffer.
	 */
	private InputStream input;
	
	/**
	 * The output stream of the console buffer.
	 */
	private OutputStream output;

	/**
	 * A flag that indicates whether the stream has been closed.
	 */
	private boolean closed = false;

	/**
	 * The first entry in the buffer to read.
	 */
	private int start = 0;

	/**
	 * The last entry in the buffer to write.
	 */
	private int end = 0;

	/**
	 * The buffer itself.
	 */
	private byte[] buffer;

	/**
	 * Creates a ring buffer with the specified length.
	 * 
	 * @param length The length of the buffer.
	 */
	public ConsoleBuffer(int length) {
		buffer = new byte[length];
	}

	/**
	 * The number of bytes that can be read from the 
	 * buffer.
	 * 
	 * @return The number of bytes that are in the buffer.
	 */
	private int available() {
		return (start > end)?(buffer.length - start + end):(end - start);
	}
	
	/**
	 * Returns the number of bytes that can be written to the
	 * buffer. Note that an empty buffer can only store buffer.length
	 *  - 1 bytes. Since equal start and stop indexes denote an empty 
	 * buffer.
	 * 
	 * @return The amount of free space in the buffer.
	 */
	private int space() {
		return buffer.length - available() - 1;
	}

	/**
	 * Reads a number of bytes from the buffer and puts it into
	 * the specified buffer, starting from the specified offset.
	 * 
	 * @param b The buffer to write the read data.
	 * @param offset The offset to start from.
	 * @param length The number of bytes to read.
	 * @return The number of bytes read.
	 */
	private int read(byte[] b, int offset, int length) {
		if (start > end) {
			int read = Math.min(buffer.length - start, length);
			System.arraycopy(buffer, start, b, offset, read);
			start += read;
			if (start == buffer.length) start = 0;
			if (read == length) {
				return read;
			} else {
				int read2 = Math.min(end, length - read);
				System.arraycopy(buffer, start, b, offset + read, read2);	
				start += read2;
				return read + read2;		
			}
		} else {
			int read = Math.min(end - start, length);
			System.arraycopy(buffer, start, b, offset, read);	
			start += read;
			return read;
		}
	}

	/**
	 * Writes a number of bytes to the buffer starting from the
	 * specified offset up until the specified length.
	 * 
	 * @param b The buffer with the data to write.
	 * @param offset The offset to start from.
	 * @param length The number of bytes to write.
	 * @return The number of bytes written.
	 */
	private int write(byte[] b, int offset, int length) {
		if (start > end) {
			int write = Math.min(start - end - 1, length);
			System.arraycopy(b, offset, buffer, end, write);
			end += write;
			return write;
		} else {
			int sub = (start == 0)?1:0;
			int write = Math.min(buffer.length - end - sub, length);
			System.arraycopy(b, offset, buffer, end, write);
			end += write;
			if (end == buffer.length) end = 0;
			if (write == length || start == 0) {
				return write;
			} else {
				int write2 = Math.min(start - 1, length - write);
				System.arraycopy(b, offset + write, buffer, end, write2);	
				end += write2;
				return write + write2;					
			}
		}
	}

	/**
	 * Returns the input stream for the buffer.
	 * 
	 * @return The input stream for the buffer.
	 */
	public InputStream getInputStream() {
		if (input == null) {
			input = new InputStream() {
				public int read() throws IOException {
					synchronized (ConsoleBuffer.this) {
						while (ConsoleBuffer.this.available() == 0) {
							if (closed) {
								throw new IOException("Stream closed.");
							}
							try {
								ConsoleBuffer.this.wait();
							} catch (InterruptedException e) {
							}
						}
						byte[] b = new byte[1];
						int r = ConsoleBuffer.this.read(b, 0, 1);
						if (r != 1) {
							throw new IOException("Stream error.");
						} else {
							ConsoleBuffer.this.notifyAll();
							return b[0] & 0xFF;
						}
					}
				}
				public int available() throws IOException {
					synchronized (ConsoleBuffer.this) {
						return ConsoleBuffer.this.available();
					}
				}
				public int read(byte[] b, int offset, int length) throws IOException {
					synchronized (ConsoleBuffer.this) {
						while (ConsoleBuffer.this.available() == 0) {
							if (closed) {
								throw new IOException("Stream closed.");
							}
							try {
								ConsoleBuffer.this.wait();
							} catch (InterruptedException e) {
							}
						}
						ConsoleBuffer.this.notifyAll();
						return ConsoleBuffer.this.read(b, offset, length);
					}
				}
			};
		}
		return input;
	}

	/**
	 * Returns the output stream for the buffer.
	 * 
	 * @return The output stream for the buffer.
	 */
	public OutputStream getOutputStream() {
		if (output == null) {
			output = new OutputStream() {
				public void write(int oneByte) throws IOException {
					synchronized (ConsoleBuffer.this) {
						while (ConsoleBuffer.this.space() == 0) {
							try {
								if (closed) {
									throw new IOException("Stream closed.");
								}
								ConsoleBuffer.this.wait();
							} catch (InterruptedException e) {
							}
						}
						byte[] b = new byte[] { (byte)oneByte };
						while (ConsoleBuffer.this.write(b, 0, 1) != 1) {
							if (closed) {
								throw new IOException("Stream closed.");
							}
						}
						ConsoleBuffer.this.notifyAll();
					}
				}
				public void write(byte[] buffer, int offset, int count) throws IOException {
					int length = 0;
					while (length != count) {
						synchronized (ConsoleBuffer.this) {
							while (ConsoleBuffer.this.space() == 0) {
								try {
									if (closed) {
										throw new IOException("Stream closed.");
									}
									ConsoleBuffer.this.wait();
								} catch (InterruptedException e) {
								}																
							}
							length += ConsoleBuffer.this.write(buffer, length + offset, count - length);
							if (closed) {
								throw new IOException("Stream closed");
							}
							ConsoleBuffer.this.notifyAll();
						}
					}
				}						
			};
		}
		return output;
	}
	
	/**
	 * Closes the buffer.
	 */
	public void close() {
		closed = true;
		synchronized (this) {
			notifyAll();	
		}
	}
	
}
