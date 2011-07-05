package info.pppc.basex.plugin.transceiver;

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
import info.pppc.basex.plugin.transceiver.spot.AsyncLoggingStream;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;
import info.pppc.basex.plugin.util.TimeoutConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;

import com.sun.squawk.Isolate;

/**
 * This transceiver provides connectivity with sunspot sensor nodes on
 * a wireless access point via a serial connection over usb.
 * 
 * @author Marcus Handte
 */
public class MxSerialTransceiver implements ITransceiver, IMultiplexPlugin, IOperation {
	
	/**
	 * This event is called whenever the transceiver is opening
	 * the streams. The data object will be null.
	 */
	public static final int EVENT_TRANSCEIVER_OPENING = 4;
	
	/**
	 * This event is called whenever the transceiver is trying to
	 * resync. The data object will be null.
	 */
	public static final int EVENT_TRANSCEIVER_SYNCING = 8;
	
	/**
	 * This event is called whenever the transceiver is waiting. 
	 * The data object will be null.
	 */
	public static final int EVENT_TRANSCEIVER_WAITING = 16;
	
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
	 * The amount of time to sleep between polls on the input stream.
	 */
	private static final int POLLING_PERIOD = 5;

	/**
	 * The maximum number of log entries that are stored in the
	 * logger used by this plug-in.
	 */
	private static final int MAXIMUM_LOG_ENTRIES = 50;
	
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
	 * The reference to the local multiplex factory.
	 */
	private MultiplexFactory factory;
	
	/**
	 * The description of the plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, IExtension.EXTENSION_TRANSCEIVER);
	
	
	/**
	 * Creates a new transceiver plug-in that connects to
	 * the usb port.
	 */
	public MxSerialTransceiver() {	}
	
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
            Logging.debug(getClass(), "Redirecting output streams for current isolate.");
            Isolate currentIsolate = Isolate.currentIsolate();
            currentIsolate.clearOut();
            currentIsolate.clearErr();
            setEnabled(true);
        }

	/**
	 * Called to stop the plug-in.
	 */
	public void stop() {
            setEnabled(false);
            Isolate currentIsolate = Isolate.currentIsolate();
            currentIsolate.addOut("serial://usb");
            currentIsolate.addErr("serial://usb");
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
		manager.acceptSession(connector);
	}
	
	/**
	 * Called whenever the multiplexer is closed.
	 * 
	 * @param multiplexer The multiplexer that has been closed.
	 */
	public void closeMultiplexer(MultiplexFactory multiplexer) {
		synchronized (this) {
			this.factory = null;
			Logging.setOutput(System.out);
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
	 * @param monitor
	 *            The monitor for the operation.
	 */

	public void perform(IMonitor monitor) throws Exception {
		while (enabled) {
			try {
				listeners.fireEvent(EVENT_TRANSCEIVER_OPENING);
				final InputStream input = Connector.openInputStream("serial://usb");
				final OutputStream output = Connector.openOutputStream("serial://usb");
				// create a polling input stream to avoid hangs,
				// this is a work around for the blocking read, very inefficient but works.
				InputStream is = new InputStream() {
					byte[] result = new byte[1];
					private boolean closed = false;
					private static final int MAX_LOOP = RECEIVE_TIMEOUT / POLLING_PERIOD;
					public int read() throws IOException {
						int loop = MAX_LOOP;
						while (input.available() < 1 && !closed) {
							try {
								Thread.sleep(POLLING_PERIOD);
							} catch (InterruptedException e) { }
							if (loop < 0) return -1;
							else loop -= 1;
						}
						synchronized (this) {
							if (closed) 
								throw new IOException();	
							else {
								int amount = 0;
								while (amount == 0) {
									amount = input.read(result, 0, 1);
									if (amount == -1) return -1;
								}
								return (result[0] & 0xff);
							}
						}
					}
					public void close() throws IOException {
						synchronized (this) {
							if (! closed) {
								closed = true;
								input.close();		
								Logging.setOutput(System.out);
							}
						}
					}
				};
				OutputStream os = new OutputStream() {
					private boolean closed = false;
					public void write(int b) throws IOException {
						synchronized (this) {
							if (closed) throw new IOException();
							else output.write(b);	
						}
					}
					public void flush() throws IOException {
						synchronized (this) {
							if (closed) throw new IOException();
							else output.flush();														
						}
					}
					public void close() throws IOException {
						synchronized (this) {
							if (! closed) {
								closed = true;
								output.close();
								Logging.setOutput(System.out);
							}		
						}
					}
				};
				listeners.fireEvent(EVENT_TRANSCEIVER_SYNCING);
				TimeoutConnector c = new TimeoutConnector(this, manager, is, os, false, TIMEOUT_PERIOD);
				// create multiplexer
				MultiplexFactory f = new MultiplexFactory(this, c.getInputStream(), c.getOutputStream(), 
						true, MULTIPLEXER_LENGTH);
				// redirect logging output to host
				AsyncLoggingStream stream = new AsyncLoggingStream(f.openConnector(), MAXIMUM_LOG_ENTRIES);
				manager.performOperation(stream, stream.getMonitor());
				Logging.setOutput(stream);
				factory = f;
				// signal success
				listeners.fireEvent(EVENT_TRANCEIVER_ENABLED);
				// stop this thread
				return;
			} catch (IOException e) {
				Logging.debug(getClass(), "Could not sync.");
			}
			try {
				synchronized (this) {
					if (enabled) {
						listeners.fireEvent(EVENT_TRANSCEIVER_WAITING);
						this.wait(RECONNECT_PERIOD);
					}
				}
			} catch (InterruptedException e) {
				Logging.log(getClass(), "Thread got interrupted.");
			}

		}
	}
}
