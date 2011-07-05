package info.pppc.basex.plugin.transceiver;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.ITransceiver;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.plugin.Packet;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;
import info.pppc.irsock.IRDevice;
import info.pppc.irsock.IRServerSocket;
import info.pppc.irsock.IRSocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The ir transceiver implements IRDA-based communication. The ir 
 * transceiver will continuously perform service discovery and it will 
 * connect to any service that is available immediately in order to mimic 
 * broadcasts over links.
 * 
 * @author Marcus Handte
 */
public class MxIRTransceiver implements ITransceiver, IOperation, IMultiplexPlugin {

	/**
	 * The packet connector that is used to enable broadcast over
	 * multiplexed ir links. This is the packet connector that is 
	 * issued to the plug-in manager. It uses multiple packet connectors
	 * to receive and transfer data, one connector for each link.
	 * 
	 * @author Marcus Handte
	 */
	public class PacketConnector implements IPacketConnector, IListener {

		/**
		 * A flag that indicates whether the packet connector
		 * has been released already. If this flag is true,
		 * all methods will throw an IOException.
		 */
		private boolean released = false;

		/**
		 * A vector that contains the packet connectors that are
		 * used to send and receive packets over streams provided
		 * by the ir socket api.
		 */
		private Vector connectors = new Vector();

		/**
		 * The listeners that receive notifications form this connector.
		 */
		private ListenerBundle listeners = new ListenerBundle(this);
		
		/**
		 * Creates a new packet connector that transfers and
		 * receives data.
		 */
		public PacketConnector() { }

		/**
		 * Adds a packet listener for the specified types.
		 * 
		 * @param type The types to register for.
		 * @param listener The listener to register.
		 */
		public void addPacketListener(int type, IListener listener) {
			listeners.addListener(type, listener);
		}
		
		/**
		 * Removes a packet listener for the specified types.
		 * 
		 * @param type The types to unregister.
		 * @param listener The listener.
		 * @return True if successful, false otherwise.
		 */
		public boolean removePacketListener(int type, IListener listener) {
			return listeners.removeListener(type, listener);
		}
		
		/**
		 * Adds the specified packet connector to the set of
		 * connectors used by this connector.
		 * 
		 * @param c The packet connector to add.
		 */
		public synchronized void addConnector(IPacketConnector c) {
			if (! released) {
				connectors.addElement(c);
				c.addPacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);				
			} else {
				c.release();
			}
		}

		/**
		 * Creates a new packet that can be used to receive and
		 * transfer data from this packet connector.
		 * 
		 * @return A new packet used to receive and transfer data.
		 * @throws IOException Thrown if the connector is closed already.
		 */
		public synchronized IPacket createPacket() throws IOException {
			if (released) {
				throw new IOException("Connector closed.");
			}
			return new Packet(PACKET_LENGTH);
		}


		/**
		 * Transfers a packet through this packet connector. If the
		 * packet connector has been closed already this method will
		 * throw an exception.
		 * 
		 * @param packet The packet that should be transfered.
		 * @throws IOException Thrown if the packet connector has been
		 * 	closed already.
		 */
		public void sendPacket(IPacket packet) throws IOException {
			if (released) {
				throw new IOException("Connector closed.");				
			}
			Vector copy = null;
			synchronized (this) {
				copy = new Vector(connectors.size());
				for (int i = 0, s = connectors.size(); i < s; i++) {
					copy.addElement(connectors.elementAt(i));
				}
			}
			for (int i = 0; i < copy.size(); i++) {
				IPacketConnector c = (IPacketConnector)copy.elementAt(i);
				try {
					IPacket p = c.createPacket();
					p.setPayload(packet.getPayload());
					c.sendPacket(p);											
				} catch (IOException e) {
					Logging.debug(getClass(), "Exception in connector.");
					synchronized (this) {
						connectors.removeElement(c);
						c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);	
						c.release();	
					}
				}
			}
		}

		/**
		 * Releases the specified packet connector and removes all
		 * references within the plug-in.
		 */
		public synchronized void release() {
			this.released = true;
			while (! connectors.isEmpty()) {
				IPacketConnector c = (IPacketConnector)
					connectors.elementAt(0);
				connectors.removeElementAt(0);
				c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
				c.release();
			}
			MxIRTransceiver.this.release(this);
		}

		/**
		 * Returns the plug-in that created the packet connector.
		 * 
		 * @return The plug-in that has created this connector.
		 */
		public IPlugin getPlugin() {
			return MxIRTransceiver.this;
		}
		
		/**
		 * Returns the packet length of the connector.
		 * 
		 * @return The maximum packet length.
		 */
		public int getPacketLength() {
			return PACKET_LENGTH;
		}
		
		/**
		 * Called by the underlying connectors.
		 * 
		 * @param event An event that signals a change.
		 */
		public void handleEvent(Event event) {
			switch (event.getType()) {
				case EVENT_PACKET_CLOSED: {
					IPacketConnector c = (IPacketConnector)event.getSource();
					connectors.removeElement(c);
					c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
					break;
				}
				case EVENT_PACKET_RECEIVED: {
					IPacket data = (IPacket)event.getData();
					Packet p = new Packet(PACKET_LENGTH);
					p.setPayload(data.getPayload());
					listeners.fireEvent(EVENT_PACKET_RECEIVED, p);
					break;
				}
				default: 
					// will never happen
			}	
		}
		
	}

	/**
	 * The discovery process supports device discovery.
	 * As long as the monitor of the operation signals go, 
	 * this operation will open connections to new devices. 
	 * 
	 * @author Marcus Handte
	 */
	public class DiscoveryOperation implements IOperation {

		/**
		 * Creates a new discovery operation.
		 */
		public DiscoveryOperation() {
			super();
		}
		
		/**
		 * Called to perform a continuous device discovery as long as the 
		 * monitor signals running.
		 * 
		 * @param monitor The monitor used to cancel the operation.
		 */
		public void perform(IMonitor monitor) {
			Logging.debug(getClass(), "IR discovery operation started.");
			while (! monitor.isCanceled()) {
				try {
					IRDevice[] devices = IRDevice.discover();	
					if (devices != null) {
						for (int j = 0; j < devices.length; j++) {
							Logging.debug(getClass(), "Connecting to device " + devices[j] + ".");
							try {
								IRSocket socket = new IRSocket(devices[j], SERVICE_NAME);
								try {
									OutputStream os = socket.getOutputStream();
									ObjectOutputStream oos = new ObjectOutputStream(os);
									oos.writeObject(SystemID.SYSTEM);
									InputStream is = socket.getInputStream();
									ObjectInputStream ois = new ObjectInputStream(is);
									SystemID remote = (SystemID)ois.readObject();
									Logging.debug(getClass(), "Outgoing device identified as " + remote + ".");
									// check whether incoming connection is already there
									if (multiplexers.get(remote) == null) {
										// add incoming connection
										synchronized (this) {
											MultiplexFactory f = new MultiplexFactory(MxIRTransceiver.this, new BufferedInputStream(is), new BufferedOutputStream(os));
											multiplexers.put(remote, f);
											connectors.put(f, socket);
											for (int i = 0, s = packets.size(); i < s; i++) {
												PacketConnector c = (PacketConnector)packets.elementAt(i);
												try {
													IPacketConnector ic = f.openConnector(DEFAULT_GROUP);
													c.addConnector(ic);
												} catch (IOException e) {
													Logging.error(getClass(), "Could not open packet connector.", e);	
												}	
											}					
										}
									} else {
										Logging.debug(getClass(), "Outgoing device already connected.");
										// remove incoming connection
										socket.close();
									}
								} catch (IOException e) {
									Logging.error(getClass(), "Exception while connecting.", e);
									socket.close();
								}								
							} catch (IOException e) {
								Logging.error(getClass(), "Could not connect to ir device " + devices[j] + ".", e);								
							}
						}
					}
					synchronized (monitor) {
						monitor.wait(DISCOVERY_PERIOD);
					}
				} catch (InterruptedException e) {
					Logging.error(getClass(), "IR discovery got interrupted, exiting.", e);
					return;
				} catch (IOException e) {
					Logging.error(getClass(), "IR discovery failed, exiting.", e);
				}
			}
		}
	}


	/**
	 * The ability of the plug-in [1][3].
	 */
	private static final short PLUGIN_ABILITY = 0x0103;

	/**
	 * The maximum packet length for packet connectors of this
	 * plug-in.
	 */
	private static final int PACKET_LENGTH = 2048;
	
	/** 
	 * The infra red service name used for this plug-in (this is
	 * comparable to a port number).
	 */
	private static final String SERVICE_NAME = "BaseMxIR";

	/**
	 * This is the period in which the device discovery will be 
	 * performed. Defaults to ten seconds.
	 */
	private static final long DISCOVERY_PERIOD = 20000;

	/**
	 * The property that is used to carry the system id within
	 * the plug-in description.
	 */
	private static final String PROPERTY_ID = "ID";

	/**
	 * The default group for packet connectors.
	 */
	private static final short DEFAULT_GROUP = 0;
	
	/**
	 * The multiplexers hashed by system id.
	 */
	private Hashtable multiplexers = new Hashtable();

	/**
	 * The ir connectors hashed by multiplexers.
	 */
	private Hashtable connectors = new Hashtable();
	
	/**
	 * The packet connectors that are currently opened.
	 */
	private Vector packets = new Vector();

	/**
	 * The transceiver manager of the plug-in.
	 */
	private ITransceiverManager manager = null;
	
	/**
	 * The listeners that listen to state changes of the transceiver.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * A flag that indicates whether the plug-in is currently enabled
	 * or disabled.
	 */
	private boolean enabled = false;

	/**
	 * A flag that indicates whether the plug-in has been started 
	 * already.
	 */
	private boolean started = false;

	/**
	 * The monitor that is used to cancel the operation that performs
	 * service discovery and listens to incoming connections.
	 */
	private NullMonitor monitor = null;

	/**
	 * The plug-in description of this plug-in.
	 */
	private PluginDescription description = null;

	/**
	 * The server socket used to disable the plug-in.
	 */
	private IRServerSocket notifier;

	/**
	 * Creates a new multiplexing ir transceiver.
	 */
	public MxIRTransceiver() {
		super();
	}

	/**
	 * Sets the transceiver manager of the ir plug-in.
	 * 
	 * @param manager The transceiver manager to set.
	 */
	public void setTransceiverManager(ITransceiverManager manager) {
		this.manager = manager;
	}

	/**
	 * Adds a transceiver listener to the plug-in that is registered
	 * for the specified types of events. At the present time the
	 * transceiver plug-ins signals whenever it is enabled or disabled.
	 * 
	 * @param type The types of events to register for.
	 * @param listener The listener to register.
	 */
	public void addTransceiverListener(int type, IListener listener) {
		listeners.addListener(type, listener);		
	}

	/**
	 * Removes the specified transceiver listener from the set of 
	 * listeners that is registered for the specified events.
	 * 
	 * @param type The type of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed, false otherwise.
	 */
	public boolean removeTransceiverListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Called to enable or disable the plug-in. Set true to enable and
	 * false to disable. A call to this method will notify registered
	 * listeners whenever the state of the plug-in changes.
	 * 
	 * @param enabled True to enable the plug-in, false to disable.
	 */
	public synchronized void setEnabled(boolean enabled) {
		if (started & this.enabled != enabled) {
			this.enabled = enabled;
			if (enabled) {
				enablePlugin();
				listeners.fireEvent(EVENT_TRANCEIVER_ENABLED);	
			} else {
				disablePlugin();
				listeners.fireEvent(EVENT_TRANCEIVER_DISABLED);	
			}
		}		
	}

	/**
	 * Determines whether the plug-in is enabled or disabled.
	 * 
	 * @return True if the plug-in is currently enabled, false if it
	 * 	is disabled.
	 */
	public synchronized boolean isEnabled() {
		return enabled;
	}

	/**
	 * Called by the plug-in manager in order to prepare a session
	 * with a remote device.
	 * 
	 * @param d The plug-in description of the remote device.
	 * @param c The collection of requirements towards the communication.
	 * @param s The session to store information about the remote device.
	 * @return True if the session has been prepared, false otherwise
	 */
	public synchronized boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		checkPlugin();
		SystemID remote = (SystemID)d.getProperty(PROPERTY_ID);
		if (remote == null || multiplexers.get(remote) == null) {
			return false;	
		} else {
			s.setLocal(remote);
			return true;
		}
	}

	/**
	 * Called by the plug-in manager to open a stream connector with
	 * the specified session properties.
	 * 
	 * @param session The session properties of the stream connector
	 * 	that should be opened.
	 * @return The stream connector that is used to transfer data
	 * 	within the specified session.
	 * @throws IOException Thrown if the connector could not be opened.
	 */
	public synchronized IStreamConnector openSession(ISession session) throws IOException {
		checkPlugin();
		SystemID id = (SystemID)session.getLocal();
		MultiplexFactory f = (MultiplexFactory)multiplexers.get(id);
		if (f == null) {
			throw new IOException("No multiplexer for target.");
		} else {
			return f.openConnector();
		}
	}

	/**
	 * Called by the plug-in manager to open a packet connector for the
	 * specified group.
	 * 
	 * @return The packet connector that is used to send and receive 
	 * 	packets within that group.
	 * @throws IOException Thrown if the connector could not be opened.
	 */
	public synchronized IPacketConnector openGroup() throws IOException {
		checkPlugin();
		PacketConnector c = new PacketConnector();
		Enumeration e = multiplexers.elements();
		while (e.hasMoreElements()) {
			MultiplexFactory f = (MultiplexFactory)e.nextElement();
			try {
				IPacketConnector pc = f.openConnector(DEFAULT_GROUP);
				c.addConnector(pc);
			} catch (IOException ex) {
				Logging.debug(getClass(), "Could not open packet connector.");
			}
		}
		packets.addElement(c);
		return c;
	}

	/**
	 * Called by the plug-in manager to start the stopped plug-in. A call to 
	 * this method will enable the end point in such a way that it will
	 * enable incoming and outgoing connections.
	 */
	public synchronized void start() {
		if (! started) {
			started = true;	
			setEnabled(true);
		}
	}

	/**
	 * Called by the plug-in manager to stop the started plug-in. A call
	 * to this method will automatically close all incoming and outgoing
	 * connections and it will disable the end point.
	 */
	public synchronized void stop() {
		if (started) {
			setEnabled(false);
			started = false;
		}
	}

	/**
	 * Returns the plug-in description of the ir plug-in.
	 * 
	 * @return The plug-in description of the ir plug-in.
	 */
	public PluginDescription getPluginDescription() {
		if (description == null) {
			description = new PluginDescription
				(PLUGIN_ABILITY, EXTENSION_TRANSCEIVER);
			description.setProperty(PROPERTY_ID, SystemID.SYSTEM, false);
		}
		return description;
	}

	/**
	 * This operation is running as long as the transceiver is enabled
	 * it performs service discovery and accepts incoming connections.
	 * 
	 * @param monitor The monitor that is used to signal that the 
	 * 	operation should be aborted.
	 * @throws Exception Should never occur.
	 */
	public void perform(IMonitor monitor) throws Exception {
		// create a monitor for the discovery thread
		NullMonitor discoveryMonitor = new NullMonitor();
		try {
			notifier = new IRServerSocket(SERVICE_NAME);
			manager.performOperation(new DiscoveryOperation(), discoveryMonitor);		
		} catch (IOException e) {
			Logging.error(getClass(), "Exception in ir setup, thread stopped.", e);
			throw e;
		}
		// at this point, the connection notifier is prepared
		while (!monitor.isCanceled()) {
			IRSocket connection = null;
			try {
				connection = notifier.accept();
				Logging.debug(getClass(), "Incoming device detected.");
				InputStream is = connection.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				SystemID remote = (SystemID)ois.readObject();
				Logging.debug(getClass(), "Incoming device identified as " + remote + ".");
				OutputStream os = connection.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(SystemID.SYSTEM);
				// test whether incoming connection is new
				if (multiplexers.get(remote) == null) {
					// add incoming connection
					synchronized (this) {
						MultiplexFactory f = new MultiplexFactory(this, new BufferedInputStream(is), new BufferedOutputStream(os));
						multiplexers.put(remote, f);
						connectors.put(f, connection);
						for (int i = 0, s = packets.size(); i < s; i++) {
							PacketConnector c = (PacketConnector)packets.elementAt(i);
							try {
								IPacketConnector ic = f.openConnector(DEFAULT_GROUP);
								c.addConnector(ic);
							} catch (IOException e) {
								Logging.error(getClass(), "Could not open packet connector.", e);	
							}	
						}					
					}
				} else {
					Logging.debug(getClass(), "Incoming device already connected.");
					// remove incoming connection
					connection.close();
				}
			} catch (IOException e) {
				continue;	
			}
		}
		// wait until discovery operation finishes
		try {
			discoveryMonitor.cancel();
			discoveryMonitor.join();	
		} catch (InterruptedException e) {
			Logging.error(getClass(), "Joining on ir discovery failed.", e);	
		}
	}

	/**
	 * Called by the multiplexer factory whenever a new stream connector
	 * has been opened due to a remote request.
	 * 
	 * @param source The source factory that has received the open call.
	 * @param connector The stream connector that has been opened within
	 * 	the factory.
	 */
	public void acceptConnector(MultiplexFactory source, IStreamConnector connector) {
		manager.acceptSession(connector);	
	}

	/**
	 * Called by the multiplex factory whenever a multiplexer factory
	 * is closed due to a closed connection.
	 * 
	 * @param multiplexer The multiplexer that has been closed.
	 */
	public synchronized void closeMultiplexer(MultiplexFactory multiplexer) {
		IRSocket c = (IRSocket)connectors.remove(multiplexer);
		if (c != null) {
			try {
				c.close();	
			} catch (IOException e) {
				Logging.debug
					(getClass(), "Exception while closing stream connection.");	
			}
		}
		Enumeration e = multiplexers.keys();
		while (e.hasMoreElements()) {
			Object k = e.nextElement();
			MultiplexFactory f = (MultiplexFactory)multiplexers.get(k);
			if (f == multiplexer) {
				multiplexers.remove(k);
				Logging.debug(getClass(), "Removing connection to " + k);				
			}
		}			
	}

	/**
	 * Returns the plug-in manager of this multiplexer plug-in.
	 * 
	 * @return The plug-in manager of the plug-in.
	 */
	public IPluginManager getPluginManager() {
		return manager;
	}

	/**
	 * Validates whether the plug-in can open a connection and respond to
	 * connection requests. This method throws an exception if the current
	 * state of the plug-in does not allow the initialization or a 
	 * connector.
	 */
	private void checkPlugin() {
		if (manager == null) throw new RuntimeException("Manager not set.");
		if (! started) throw new RuntimeException("Plugin not started.");
		if (! enabled) throw new RuntimeException("Endpoint not enabled.");
	}
	
	/**
	 * Enables the reception of incoming connections.
	 */
	private void enablePlugin() {
		monitor = new NullMonitor();
		manager.performOperation(this, monitor);
	}
	
	/**
	 * Disables the reception of incoming connections and closes all
	 * currently incoming and outgoing connections.
	 */
	private void disablePlugin() {
		// cancel monitor
		monitor.cancel();
		// interrupt reception
		IRServerSocket ss = notifier;
		if (ss != null) {
			try {
				ss.close();
			} catch (Throwable t) {
				Logging.error(getClass(), "Could not close notifier.", t);
			} finally {
				ss = null;	
			}
		}
		// wait for termination
		try {			
			monitor.join();
		} catch (InterruptedException e) {
			Logging.debug(getClass(), "Thread got interrupted.");	
		}
		// remove remaining multiplexers, if any
		Enumeration e = connectors.elements();
		while (e.hasMoreElements()) {
			IRSocket s = (IRSocket)e.nextElement();
			if (s != null) {
				try {
					s.close();	
				} catch (IOException ex) {
					Logging.debug(getClass(), "Could not close ir socket properly.");	
				}
			}
		}
	}
	
	/**
	 * Called by a packet connector whenever it is released.
	 * 
	 * @param connector The packet connector that has been
	 * 	released.
	 */
	private synchronized void release(IPacketConnector connector) {
		packets.removeElement(connector);		
	}

}
