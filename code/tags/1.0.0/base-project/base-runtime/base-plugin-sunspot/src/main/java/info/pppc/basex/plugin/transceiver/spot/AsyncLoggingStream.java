package info.pppc.basex.plugin.transceiver.spot;

import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

/**
 * A logging thread that performs logging tasks
 * asynchronously to avoid hangs in cases where
 * the developer is using logging code within
 * event handlers.
 * 
 * @author Marcus Handte
 */
public class AsyncLoggingStream extends PrintStream implements IOperation {
	
	/**
	 * The monitor to interact with the thread.
	 */
	private NullMonitor monitor = new NullMonitor();
	
	/**
	 * The buffer that contains the log entries.
	 */
	private Vector buffer = new Vector();
	
	/**
	 * The maximum number of entries in the buffer.
	 */
	private int size;
	
	/**
	 * The output stream to write log messages.
	 */
	private DataOutputStream output;
	
	/**
	 * The connector used as output. It will be
	 * closed if the stream fails.
	 */
	private IStreamConnector connector;
	
	/**
	 * Creates a new asynchronous logger that logs the output on the
	 * specified connector (i.e. its output stream). The size configures
	 * the maximum buffer space to avoid running out of memory. If the
	 * maximum is exceeded, the buffer will be purged and a warning 
	 * message will be added.
	 * 
	 * @param connector The connector to use as output.
	 * @param size The maximum number of log entries that should be
	 * 	buffered. This must be larger than 1.
	 * @throws IOException Thrown if the connector breaks during
	 * 	initialization.
	 */
	public AsyncLoggingStream(IStreamConnector connector, int size) throws IOException {
		super(connector.getOutputStream());
		this.output = new DataOutputStream(connector.getOutputStream());
		this.connector = connector;
		this.size = size;
		if (size < 1) throw new IndexOutOfBoundsException();
	}
	
	/**
	 * Returns the monitor that should be used to start the
	 * operation. It can be used to cancel the operation as
	 * well.
	 * 
	 * @return The logger to start the operation.
	 */
	public IMonitor getMonitor() {
		return monitor;
	}
	
	/**
	 * This is the only method that is actually called by the
	 * base logging facility so we do not need to overwrite 
	 * more than that. The method simply adds the string to
	 * the buffer. if the buffer is full, it clears the buffer
	 * and adds a warning message.
	 * 
	 * @param string The string to print.
	 */
	public void print(String string) {
		synchronized (monitor) {
			if (! monitor.isCanceled()) {
				buffer.addElement(string);
				if (buffer.size() > size) {
					buffer.removeAllElements();
					Logging.debug(getClass(), "Log purged.");
				}
				monitor.notify();				
			}
		}
		
	}
	
	/**
	 * Runs the asynchronous log operation. This should be
	 * called with the monitor of the operation.
	 * 
	 * @param monitor The monitor to cancel the operation.
	 */
	public void perform(IMonitor monitor) {
		while (true) {
			String string = null;
			synchronized (monitor) {
				if (buffer.size() > 0) {
					string = (String)buffer.elementAt(0);
					buffer.removeElementAt(0);
				}
			}
			if (string != null) {
				try {
					output.writeUTF(string);
					output.flush();
				} catch (IOException e) {
					connector.release();
					monitor.cancel();
					break;
				}				
			} else {
				synchronized (monitor) {
					if (monitor.isCanceled()) {
						break;
					} else {
						try {
							monitor.wait();
						} catch (InterruptedException e) { }						
					}
				}	
			}
		}
	}

}
