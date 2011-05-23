package info.pppc.basex.plugin.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.IOperator;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IStreamConnector;

/**
 * The timeout connector implements a simple protocol to establish a connection 
 * between two entities that are using unbuffered output streams that do not
 * provide a connection detection. To do this the participating entities
 * will exchange two sync messages before sending data. The synchronization is
 * done in the constructor of the connector. If the synchronization does
 * not take place during the timeout, the connection establishment will 
 * fail with an exception. Similarly, if the entities refrain from 
 * exchanging data for the timeout value, the connection will be considered
 * to be dead.
 * The original purpose of this connector is to establish a connection between
 * sun spots and a base station running on some other device via usb. Since
 * the sun spot and the base station may not be able to reliably detect their
 * presence and their absence (e.g. if a user simply disconnects the sun spot
 * from the usb port), it is necessary to have some simple handshake and
 * keep alive mechanism that must be implemented at the application layer.
 * 
 * @author Marcus Handte
 */
public class TimeoutConnector implements IStreamConnector {

	/**
	 * The contents of the initial handshake message.
	 */
	final static byte[] sync = new byte[] { (byte)5, (byte)2, (byte)3, (byte)4, (byte)4, (byte)3, (byte)2, (byte)5 };
	
	/**
	 * The input stream of the connector that is made 
	 * available to applications.
	 */
	private InputStream input;
	
	/**
	 * The output stream of the connector that is made
	 * available to applications.
	 */
	private OutputStream output;
	
	/**
	 * A flag that indicates whether the connection has
	 * been closed. This can be either due to a call of
	 * the release method or due to a missing keep alive
	 * message.
	 */
	private boolean closed = false;
	
	/**
	 * The time at which the last successful data reception
	 * took place.
	 */
	private long last;
	
	/**
	 * The plug-in that is using the connector.
	 */
	private IPlugin plugin;
	

	/**
	 * The close operation is used to asynchronously close the
	 * input stream during the initial handshake, if the response
	 * is not sent in time.
	 * 
	 * @author Marcus Handte
	 */
	private class CloseOperation implements IOperation {
		
		/**
		 * The input stream that should be closed.
		 */
		private InputStream in;
		
		/**
		 * The timeout after which the handshake should
		 * have taken place.
		 */
		private long timeout;
		
		/**
		 * Creates a new close operation that closes the
		 * input stream after the timeout.
		 * 
		 * @param in The input stream to close.
		 * @param timeout The timeout value.
		 */
		public CloseOperation(InputStream in, long timeout) {
			this.in = in;
			this.timeout = timeout;
		}
		
		/**
		 * Called to execute the operation asynchronously. If
		 * this is called, the calling thread will be blocked
		 * until the timeout has expired. If the monitor has
		 * been canceled, the operation will cease to exist.
		 * Otherwise, the operation will cancel the reception.
		 * 
		 * @param monitor The monitor to cancel the closing.
		 */
		public void perform(IMonitor monitor) throws Exception {
			synchronized (monitor) {
				monitor.wait(timeout);
			}
			if (! monitor.isCanceled()) {
				closed = true;
				in.close();
			}
		}		
	}
	
	/**
	 * Creates a new timeout connector for the specified plug-in. It uses
	 * the specified operator to execute operations asynchronously. Furthermore,
	 * it uses the specified input and output streams to establish a connection.
	 * The mode flag determines which end of the connection will initiate the
	 * handshake. 
	 * 
	 * @param plugin The plug-in that is requesting the connector or null, if none.
	 * @param operator The operator that is used to schedule more threads.
	 * @param in The input stream used to establish a connection.
	 * @param out The output stream used to establish a connection.
	 * @param mode False to initiate the handshake on this side of the connection,
	 * 	true to initiate the handshake on the opposing side.
	 * @param timeout The timeout during the handshake and for the reception of
	 * 	data. The other end of the connection must send at least one byte in a
	 *  timely manner in order to avoid disconnections.
	 * @throws IOException Thrown if the handshake fails.
	 */
	public TimeoutConnector(IPlugin plugin, IOperator operator, final InputStream in, final OutputStream out, boolean mode, final long timeout) throws IOException {
		this.plugin = plugin;
		// run the handshake protocol
		try {
			if (mode) {
				NullMonitor monitor = new NullMonitor();
				operator.performOperation(new CloseOperation(in, timeout), monitor);
				syncRead(sync, in);
				monitor.cancel();
				syncWrite(sync, out);
			} else {
				syncWrite(sync, out);
				NullMonitor monitor = new NullMonitor();
				operator.performOperation(new CloseOperation(in, timeout), monitor);
				syncRead(sync, in);
				monitor.cancel();
			}
			synchronized (this) {
				last = System.currentTimeMillis();	
			}			
		} catch (IOException e) {
			try {
				in.close();
			} catch (IOException ex) { }
			try {
				out.close();
			} catch (IOException ex) { }
			throw e;
		}
		// create input and output streams
		input = new InputStream() {
			public int read() throws IOException {
				try {
					int result = -1;
					while (result == -1 && ! closed) {
						result = in.read();
						synchronized (this) {
							if (System.currentTimeMillis() - last > timeout) {
								input.close();
								output.close();
								throw new IOException("Timeout expired.");
							}							
						}
					}
					synchronized (TimeoutConnector.this) {
						last = System.currentTimeMillis();	
					}
					return result;
				} catch (IOException e) {
					input.close();
					output.close();
					throw e;
				}				
			}
			public void close() throws IOException {
				closed = true;
				try {
					in.close();	
				} catch (IOException e) { }
				
			}
		};
		output = new OutputStream() {
			public void write(int arg0) throws IOException {
				synchronized (TimeoutConnector.this) {
					long now = System.currentTimeMillis();
					if (now - last > timeout) {
						input.close();
						output.close();
					}
				}
				if (closed) {
					throw new IOException("Output stream closed.");
				}
				out.write(arg0);
			}
			public void write(byte[] arg0) throws IOException {
				write(arg0, 0, arg0.length);
			}
			public void write(byte[] data, int off, int len) throws IOException {
				synchronized (TimeoutConnector.this) {
					long now = System.currentTimeMillis();
					if (now - last > timeout) {
						input.close();
						output.close();
					}
				}
				if (closed) {
					throw new IOException("Output stream closed.");
				}
				out.write(data, off, len);
			}
			public void flush() throws IOException {
				if (closed)	throw new IOException("Output stream closed.");
				out.flush();
			}
			public void close() {
				closed = true;
				try {
					out.close();	
				} catch (IOException e) { }
			};
		};
	}
	
	/**
	 * Tries to read the handshake sequence from the specified
	 * input stream.
	 * 
	 * @param sync The handshake sequence that is expected. 
	 * @param in The input stream to read from.
	 * @throws IOException Thrown if the hand shake fails due to 
	 * 	a timeout or if the underlying stream throws an exception.
	 */
	private void syncRead(byte[] sync, InputStream in) throws IOException {
		byte[] data = new byte[sync.length];
		int length = data.length;
		int read = 0;
		read: while (length > read) {
			int value = in.read(data, read, length - read);
			if (value == -1 || value == 0) {
				if (closed) {
					throw new IOException("Handshake timeout expired.");	
				}
			} else {
				read += value;
				int compare = read;
				compare: while (compare > 0) {
					for (int i = 0; i < compare; i++) {
						if (data[i] != sync[i]) {
							byte[] swap = new byte[length];
							System.arraycopy(data, 1, swap, 0, compare - 1);
							compare -= 1;
							continue compare;
						}
					}
					read = compare;
					continue read;
				}
				read = 0;
			}
		}
	}
	
	/**
	 * Writes the specified synchronization sequence to the specified
	 * output stream.
	 * 
	 * @param sync The synchronization sequence to use.
	 * @param out The output stream to write to.
	 * @throws IOException Thrown by the underlying stream.
	 */
	private void syncWrite(byte[] sync, OutputStream out) throws IOException {
		out.write(sync);
		out.flush();	
	}
	
	/**
	 * Returns the input stream of the connector.
	 * 
	 * @return The input stream.
	 */
	public InputStream getInputStream() throws IOException {
		return input;
	}

	/**
	 * Returns the output stream of the connector.
	 * 
	 * @return The output stream.
	 */
	public OutputStream getOutputStream() throws IOException {
		return output;
	}

	/**
	 * Returns the plug-in that has created the connector or
	 * null if none.
	 * 
	 * @return The plug-in that created the connector.
	 */
	public IPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Closes the input and output streams of the
	 * connector.
	 */
	public void release() {
		try {
			input.close();
			output.close();
		} catch (IOException e) { }
	}
}
