package info.pppc.basex.plugin.transceiver;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import info.pppc.base.system.IExtension;
import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.ITransceiver;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;
import info.pppc.basex.plugin.util.TimeoutConnector;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * This transceiver provides connectivity with suns pot sensor nodes on
 * a wireless access point via a serial connection over usb.
 * 
 * @author Marcus Handte
 */
public class MxSerialTransceiver implements ITransceiver, IMultiplexPlugin, IOperation {

	/**
	 * The logger is used to establish a connection for the
	 * standard output and standard error streams of the 
	 * sun spot. The first incoming connection will be used
	 * to as default log. The logger simply reads strings
	 * using a data input stream.
	 * 
	 * @author Marcus Handte
	 */
	private class Logger implements IOperation {

		/**
		 * The connector to use for the log.
		 */
		private IStreamConnector connector;
		
		/**
		 * Creates a new logger with the specified connector.
		 * 
		 * @param connector The connector to use for the logger.
		 */
		public Logger(IStreamConnector connector) {
			this.connector = connector;
		}

		/**
		 * Continuously reads incoming log messages from the
		 * input stream of the connector and releases the
		 * connector once it experiences an exception.
		 * 
		 * @param monitor The monitor, not used here.
		 */
		public void perform(IMonitor monitor) throws Exception {
			InputStream is = connector.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			try {
				Logging.debug(getClass(), "Log stream opened.");
				while (true) {
					String message = dis.readUTF();
					if (message.endsWith("\n")) {
						Logging.log(getClass(), "< " + message.substring(0, message.length() - 1) + " >");	
					} else {
						Logging.log(getClass(), "< " + message + " >");
					}
				}							
			} catch (IOException e) {
				Logging.debug(getClass(), "Log stream closed.");
				connector.release();
			}
		}
	}
	
	/**
	 * The serial stream listener implements reactive reading on a serial
	 * port. Instead of reading continuously on the port, the readers will
	 * be blocked until new data is available by means of a new data signal
	 * emmited from the serial port. 
	 * 
	 * @author Marcus Handte
	 */
	private class SerialStreamListener extends InputStream implements SerialPortEventListener {
		
		/**
		 * The input stream of the serial port.
		 */
		final private InputStream in;
		
		/**
		 * A flag that indicates whether the 
		 */
		private boolean closed = false;
		
		/**
		 * The pointer into the chunk. The pointer 
		 * points to the next available data byte
		 * in the chunk. If the pointer equals the
		 * chunk length, the chunk has been processed
		 * completely.
		 */
		private int pointer = 0;
		
		/**
		 * The chunk that is currently processed.
		 */
		private byte[] chunk = new byte[0];
		
		/**
		 * The data that has been read so far. This
		 * vector contains a number of chunks that
		 * have not been processed in the order
		 * in which they should be processed.
		 */
		private final Vector data = new Vector();
		
		/**
		 * The buffer is used to read all data 
		 * available on the serial port with one
		 * read. Thus, the buffer length must exceed
		 * the buffer of the serial port.
		 */
		private final byte[] buffer; 
		
		/**
		 * The serial port that is used for reading.
		 */
		private final SerialPort serial;
		
		/**
		 * Creates a new serial stream listener for the
		 * specified serial port that uses the specified
		 * buffer size for reading all available data. The
		 * caller must ensure that the available data never
		 * exceeds this value. In this plug-in this is ensured
		 * by enabling application level flow-control with
		 * a particular multiplexer length.
		 * 
		 * @param serial The serial port.
		 * @param size The size of the buffer. This must be
		 * 	the maximum size that will be delivered at once at
		 * 	all times.
		 * @throws IOException Thrown by the underlying serial
		 * 	port implementation.
		 */
		public SerialStreamListener(SerialPort serial, int size) throws IOException {
			this.serial = serial;
			try {
				serial.addEventListener(this);	
			} catch (Throwable e) {
				throw new IOException("Could not register serial port listener.");
			}
			this.in = serial.getInputStream();
			buffer = new byte[size];
		}
		
		/**
		 * Reads a single byte from the serial port input stream. This method
		 * is implemented by reading bytes from the chunk, if possible and
		 * if not, it blocks until the serial port is closed or until the
		 * next chunk is available. In the first case the method will return
		 * -1 and in the latter, it will return the byte from the new chunk.
		 * 
		 * @return The data that has been read or -1 if the stream has been closed.
		 * @throws IOException Thrown by the underlying stream.
		 */
		public int read() throws IOException {
			if (pointer < chunk.length) {
				byte result = chunk[pointer];
				pointer += 1;
				return result & 0xff;
			}
			while (pointer >= chunk.length) {
				synchronized (data) {
					long now = System.currentTimeMillis();
					long end = now + RECEIVE_TIMEOUT;
					while (data.size() == 0 && ! closed) {
						now = System.currentTimeMillis();
						if (end - now < 0) {
							return - 1;
						} else {
							try {
								data.wait(end - now);	
							} catch (InterruptedException e) {	}							
						}
					}
					if (closed) return -1;
					chunk = (byte[])data.elementAt(0);
					data.removeElementAt(0);
					pointer = 0;
				}
			}
			pointer = 1;
			return chunk[0] & 0xff;
		}
		
		/**
		 * Closes the stream and unblocks a reading thread if 
		 * there is any.
		 * 
		 * @throws IOException Thrown by the underlying stream.
		 */
		public void close() throws IOException {
			synchronized (data) {
				data.notify();
				if (! closed) {
					closed = true;
					try {
						serial.removeEventListener();				
					} catch (Throwable t) { }
					in.close();
					serial.close();
				}
			}
		}
		
		/**
		 * Reads a byte array. Implemented by calling the method
		 * that uses offsets.
		 * 
		 * @param b The byte array to fill with reading.
		 * @return The bytes that have been read.
		 * @throws IOException Thrown by the underlying stream.
		 */
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}
		
		/**
		 * Reads a portion of a byte array. This method will read the
		 * available chunk and return. If no chunk is available, it will
		 * read a single byte and return it which whill reload a chunk
		 * for the next call.
		 * 
		 * @param b The byte array to fill.
		 * @param off The offset to start from.
		 * @param len The length to read at most.
		 * @return The bytes that have been read.
		 * @throws IOException Thrown by the underlying stream.
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (pointer < chunk.length) {
				int available = Math.min(chunk.length - pointer, len);
				System.arraycopy(chunk, pointer, b, off, available);
				pointer += available;
				return available;
			} else {
				int value = read();
				if (value == -1) {
					return -1;
				} else {
					b[off] = (byte)(value & 0xff);
					return 1;
				}
			}
		}
		
		/**
		 * Called whenever new data is available. This method will
		 * read all available data from the stream.
		 * 
		 * @param event The event that indicates the new data.
		 */
		public synchronized void serialEvent(SerialPortEvent event) {
			try {
				if (! closed && event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
					while (in.available() > 0) {
						int value = in.read(buffer);
						if (value != -1 && value != 0) {
							byte[] chunk = new byte[value];
							System.arraycopy(buffer, 0, chunk, 0, value);
							synchronized (data) {
								data.addElement(chunk);
								data.notify();
							}
						}
					}
				}										
			} catch (IOException e) {
				Logging.debug(getClass(), "Failed to read data from serial stream.");
				synchronized (data) {
					closed = true;
					data.notify();
					try {
						serial.removeEventListener();	
					} catch (Throwable t) {
						Logging.debug(getClass(), "Exception while removing serial listener.");
					}
					
				}
			}
		}
				
	}
	
	/**
	 * The period after which a connection establishment will be
	 * repeated, if the connector cannot be enabled.
	 */
	private static final int RECONNECT_PERIOD = 5000;
	
	/**
	 * The period of time after which the connection will be doomed
	 * if no data has been received. Note that this time is also used 
	 * for the timeout of the initial hand shake.
	 */
	private static final int TIMEOUT_PERIOD = 20000;
	
	/**
	 * The receive timeout of the serial connection used to detect
	 * dead connections.
	 */
	private static final int RECEIVE_TIMEOUT = 2000;
	
	/**
	 * The data packet length used within the multiplexer. This
	 * must be smaller than 245 to ensure that the buffers are
	 * not overwritten.
	 */
	private static final int MULTIPLEXER_LENGTH = 50;
	
	/**
	 * The ability of the plug-in [1][7].
	 */
	public static short PLUGIN_ABILITY = 0x0107;

	/**
	 * The transceiver listeners that are registered.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * A flag to indicate whether the transceiver is enabled.
	 */
	private boolean enabled = false;
	
	/**
	 * The reference to the transceiver manager.
	 */
	private ITransceiverManager manager;
	
	/**
	 * The name of the port to connect to.
	 */
	private String port;
	
	/**
	 * The reference to the local multiplex factory.
	 */
	private MultiplexFactory factory;
	
	/**
	 * The reference to the serial port.
	 */
	private SerialPort serial;
	
	/**
	 * A flag that indicates whether the incoming 
	 * connection is the first. If this is the
	 * case, it will be used for logging.
	 */
	private boolean first = true;
	
	/**
	 * The flag that indicates whether the open wrt patch
	 * should be applied.
	 */
	private boolean patch = false;
	
	/**
	 * The description of the plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, IExtension.EXTENSION_TRANSCEIVER);
	
	
	/**
	 * Creates a new transceiver plug-in that connects to
	 * the specified serial port. If the patch flag is set
	 * to true, the input stream will be guarded from 
	 * interrupted system calls. This may happen on an
	 * open wrt device.
	 * 
	 * @param port The serial port to connect to.
	 * @param patch A flag that indicates whether the open
	 * 	wrt patch should be applied.
	 */
	public MxSerialTransceiver(String port, boolean patch) { 
		this.port = port;
		this.patch = patch;
	}
	
	/**
	 * Adds a transceiver listener that is registered for the 
	 * specified types.
	 * 
	 * @param type The types to register for.
	 * @param listener The listener to register.
	 */
	public void addTransceiverListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}

	/**
	 * Returns true if the plug-in is enabled, false otherwise.
	 * 
	 * @return True if the plug-in is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Removes the specified transceiver listener for the specified events.
	 * 
	 * @param type The type of events.
	 * @param listener The listener.
	 */
	public boolean removeTransceiverListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Enables or disables the transceiver.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			if (enabled) {
				// activate
				manager.performOperation(this);
			} else {
				// deactivate
				this.enabled = false;
				synchronized (this) {
					if (factory != null) {
						factory.close();
					} else {
						this.notify();
					}
				}
			}
		}
	}

	/**
	 * Sets the transceiver manager for this plug-in.
	 * 
	 * @param manager The transceiver manager.
	 */
	public void setTransceiverManager(ITransceiverManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the plug-in description of the plug-in.
	 * 
	 * @return The plug-in description.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}

	/**
	 * Called to start the plug-in.
	 */
	public void start() { 
		setEnabled(true);
	}

	/**
	 * Called to stop the plug-in.
	 */
	public void stop() { 
		setEnabled(false);
	}

	/**
	 * Called to open a packet connector.
	 * 
	 * @return The packet connector.
	 * @throws IOException Thrown if a failure occurs.
	 */
	public synchronized IPacketConnector openGroup() throws IOException {
		if (factory != null) {
			return factory.openConnector((short)0);
		} else {
			throw new IOException("Serial connection is not ready.");
		}
	}
	
	
	/**
	 * Called to open a stream connector.
	 * 
	 * @return The session to open.
	 * @throws IOException Thrown if a failure occurs.
	 */
	public synchronized IStreamConnector openSession(ISession session) throws IOException {
		if (factory != null) {
			return factory.openConnector();
		} else {
			throw new IOException("Serial connection is not ready.");
		}
	}

	/**
	 * Called to prepare a new session.
	 * 
	 * @param d The plugin description.
	 * @param c The nf params.
	 * @param s The session.
	 */
	public synchronized boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		return (factory != null);
	}

	/**
	 * Called whenever an incoming connection has been detected by the 
	 * multiplexer.
	 * 
	 * @param source The source of the connection.
	 * @param connector The new incoming connector.
	 */
	public void acceptConnector(MultiplexFactory source, IStreamConnector connector) {
		if (first) {
			first = false;
			try {
				manager.performOperation(new Logger(connector));
			} catch (Throwable e) {
				Logging.error(getClass(), "Could not open logging stream.", e);
			}
		} else {
			manager.acceptSession(connector);	
		}
	}

	/**
	 * Called whenever the multiplexer is closed.
	 * 
	 * @param multiplexer The multiplexer that has been closed.
	 */
	public void closeMultiplexer(MultiplexFactory multiplexer) {
		synchronized (this) {
			this.factory = null;
			try {
				// this is a work-around for the serial
				// library, aparently the close call may hang
				// if called immediately after closing the streams
				Thread.sleep(RECONNECT_PERIOD);
				serial.close();	
			} catch (Throwable t) {}
			serial = null;
		}
		listeners.fireEvent(EVENT_TRANCEIVER_DISABLED);
		if (enabled) {
			manager.performOperation(this);
		}
	}

	/**
	 * Returns the plug-in manager of the plug-in.
	 * 
	 * @return The plug-in manager.
	 */
	public IPluginManager getPluginManager() {
		return manager;
	}
	
	/**
	 * Tries to open the connection on the specified port.
	 * 
	 * @param monitor The monitor for the operation.
	 */
	public void perform(IMonitor monitor) throws Exception {
		while (enabled) {
			try {
				CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(port);
				if (! identifier.isCurrentlyOwned()) {
					CommPort comm = identifier.open(getClass().getName(), RECEIVE_TIMEOUT);
					if (comm instanceof SerialPort) {
						try  {
							first = true;
							serial = (SerialPort)comm;
							InputStream input = null;
							if (patch) {
								// This input stream implements a bug fix for the asus
								// wrt which will throw an interrupted io exception as
								// io exception. 
								serial.disableReceiveTimeout();
								serial.notifyOnDataAvailable(true);
								input = new SerialStreamListener(serial, MULTIPLEXER_LENGTH * 4);
							} else {
								serial.enableReceiveTimeout(RECEIVE_TIMEOUT);
								input = serial.getInputStream();
							}
							Logging.debug(getClass(), "Trying to sync.");
							TimeoutConnector c = new TimeoutConnector(this, manager, 
									input, serial.getOutputStream(), true, TIMEOUT_PERIOD);
							Logging.debug(getClass(), "Synced successfully.");
							factory = new MultiplexFactory(this, c.getInputStream(), c.getOutputStream(), 
									true, MULTIPLEXER_LENGTH);
							listeners.fireEvent(EVENT_TRANCEIVER_ENABLED);
							return;
						} catch (IOException e) {
							Logging.debug(getClass(), "Could not sync.");
							serial = null;
							comm.close();
						}
					} else {
						comm.close();
						Logging.debug(getClass(), port + " is not a serial port, exiting.");
						return;
					}
				} else {
					Logging.debug(getClass(), "Could not open port as it is owned, retrying.");
					doWait(RECONNECT_PERIOD);
				}
			} catch (Throwable e) {
				Logging.debug(getClass(), "Could not open port as it is owned or not existing, retrying.");
				doWait(RECONNECT_PERIOD);
			}
		}
	}
	
	/**
	 * Waits until the time has expired or until the plug-in is
	 * notified.
	 * 
	 * @param time The time to wait.
	 */
	private void doWait(int time) {
		try {
			synchronized (this) {
				if (enabled) {
					this.wait(time);	
				}
			}
		} catch (InterruptedException e) {
			Logging.log(getClass(), "Thread got interrupted.");
		}
	}

}
