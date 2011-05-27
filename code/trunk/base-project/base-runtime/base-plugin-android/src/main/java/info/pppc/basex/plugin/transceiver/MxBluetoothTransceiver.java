package info.pppc.basex.plugin.transceiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Vector;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

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

/**
 * A Bluetooth transceiver plug-in that is compatible with
 * the Android APIs.
 * 
 * @author Mac
 */
public class MxBluetoothTransceiver implements ITransceiver, IOperation, IMultiplexPlugin {

	/**
	 * The packet connector that is used to enable broadcast over
	 * bluetooth l2cap. This is the packet connector that is issued
	 * to the plug-in manager. It uses multiple packet connectors
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
		 * by the bluetooth api.
		 */
		private Vector<IPacketConnector> connectors = new Vector<IPacketConnector>();

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
		 * @return A new packet used to receive and tranfer data.
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
			Vector<IPacketConnector> copy = null;
			synchronized (this) {
				copy = new Vector<IPacketConnector>(connectors.size());
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
			listeners.fireEvent(EVENT_PACKET_CLOSED);
			MxBluetoothTransceiver.this.release(this);
		}

		/**
		 * Returns the plug-in that created the packet connector.
		 * 
		 * @return The plug-in that has created this connector.
		 */
		public IPlugin getPlugin() {
			return MxBluetoothTransceiver.this;
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
	 * The ability of the plug-in [1][2].
	 */
	private static final short PLUGIN_ABILITY = 0x0102;

	/**
	 * The maximum packet length for packet connectors of this
	 * plug-in.
	 */
	private static final int PACKET_LENGTH = 2048;
	
	/**
	 * The default timeout period for packet connectors of this
	 * plug-in.
	 */
	private static final short DEFAULT_GROUP = 0;

	/**
	 * The property that is used to carry the system id within
	 * the plug-in description.
	 */
	private static final String PROPERTY_ID = "ID";

	/** 
	 * The bluetooth service id of plug-ins that have this type.
	 */
	private static final UUID PLUGIN_UUID =
		UUID.fromString("F0E0D0C0-B0A0-0090-8070-605040302011");

	/**
	 * The multiplexers hashed by system id.
	 */
	private Hashtable<SystemID, MultiplexFactory> multiplexers = new Hashtable<SystemID, MultiplexFactory>();

	/**
	 * The Bluetooth connectors hashed by multiplexers.
	 */
	private Hashtable<MultiplexFactory, BluetoothSocket> connectors = 
		new Hashtable<MultiplexFactory, BluetoothSocket>();
	
	/**
	 * The packet connectors that are currently opened.
	 */
	private Vector<PacketConnector> packets = new Vector<PacketConnector>();

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
	 * The bluetooth server socket to receive connections.
	 */
	private BluetoothServerSocket server = null;
	
	/**
	 * The bluetooth adapter to enable communication.
	 */
	private BluetoothAdapter adapter = null;
	
	/**
	 * Creates a new Bluetooth transceiver.
	 */
	public MxBluetoothTransceiver() { }

	/**
	 * Sets the transceiver manager of the bluetooth plug-in.
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
	 * @return True if the session has been prepared, false otherwise.
	 */
	public synchronized boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		checkPlugin();
		SystemID remote = (SystemID)d.getProperty(PROPERTY_ID);
		if (remote == null) {
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
		Enumeration<MultiplexFactory> e = multiplexers.elements();
		while (e.hasMoreElements()) {
			MultiplexFactory f = e.nextElement();
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
	 * Returns the plug-in description of the bluetooth plug-in.
	 * 
	 * @return The plug-in description of the bluetooth plug-in.
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
	 * @throws Exception Should never happen.
	 */
	public void perform(IMonitor monitor) throws Exception {
		try {
			// create a data element that contains the system id
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			SystemID.SYSTEM.writeObject(oos);
			oos.flush(); 
			oos.close();
			byte[] sysid = bos.toByteArray();
			//DataElement element = new DataElement(DataElement.STRING, new String(sysid));
			// bring this device into discoverable mode
			if (adapter == null) {
				Logging.log(getClass(), "Bluetooth adapter not available.");
				monitor.done();
				setEnabled(false);
				return;
			}
			if (!adapter.isEnabled()) {
			   // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			   // startActivityForResult(enableBtIntent, 2);
				Logging.log(getClass(), "Bluetooth adapter not enabled.");
				monitor.done();
				setEnabled(false);
				return;
			}			
			server = adapter.listenUsingRfcommWithServiceRecord(getID(sysid), PLUGIN_UUID);
			// start discovery operation		
			// manager.performOperation(new DiscoveryOperation(), discoveryMonitor);		
		} catch (IOException e) {
			monitor.done();
			setEnabled(false);
			Logging.error(getClass(), "Exception in bluetooth setup, thread stopped.", e);
			throw e;	
		}
		// at this point, the connection notifier is prepared
		while (!monitor.isCanceled()) {
			BluetoothSocket connection = null;
			try {
				connection = server.accept(10000);
				Logging.debug(getClass(), "Accepted incoming from " + connection.getRemoteDevice().getAddress() + ".");
				InputStream is = connection.getInputStream();
				OutputStream os = connection.getOutputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				SystemID remote = (SystemID)ois.readObject();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(SystemID.SYSTEM);
				synchronized (this) {
					if (multiplexers.get(remote) == null) {
						Logging.debug(getClass(), "Connected to system " + remote + ".");
						MultiplexFactory f = new MultiplexFactory(this, is, os, true);
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
					} else {
						Logging.debug(getClass(), "Disconnecting from system " + remote + ".");
						is.close();
						os.close();
						connection.close();
					}
				}
			} catch (IOException e) {
				//Logging.error(getClass(), "Exception in bluetooth connect.", e);
				if (monitor.isCanceled()) break;
				// try connect
				LinkedList<BluetoothDevice> bonded = new LinkedList<BluetoothDevice>(adapter.getBondedDevices());
				i: for (int j = bonded.size() - 1; j >= 0; j--) {
					BluetoothDevice device = bonded.get(j);
					synchronized (this) {
						Enumeration<BluetoothSocket> connected = connectors.elements();
						while (connected.hasMoreElements()) {
							BluetoothSocket s = connected.nextElement();
							if (s.getRemoteDevice().getAddress().equalsIgnoreCase(device.getAddress())) {
								continue i;
							}
						}
					}
					try {
						BluetoothSocket bs = device.createRfcommSocketToServiceRecord(PLUGIN_UUID);
						bs.connect();
						InputStream is = bs.getInputStream();
						OutputStream os = bs.getOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(os);
						oos.writeObject(SystemID.SYSTEM);
						ObjectInputStream ois = new ObjectInputStream(is);
						SystemID remote = (SystemID)ois.readObject();
						synchronized (this) {
							if (multiplexers.get(remote) == null) {
								MultiplexFactory f = new MultiplexFactory(MxBluetoothTransceiver.this, is, os, true);
								multiplexers.put(remote, f);
								connectors.put(f, bs);
								for (int i = 0, s = packets.size(); i < s; i++) {
									PacketConnector c = (PacketConnector)packets.elementAt(i);
									try {
										IPacketConnector ic = f.openConnector(DEFAULT_GROUP);
										c.addConnector(ic);
									} catch (IOException x) {
										Logging.error(getClass(), "Could not open packet connector.", x);	
									}	
								}
							} else {
								is.close();
								os.close();
								bs.close();
							}
						}							
					} catch (IOException x) {
						//Logging.error(getClass(), "Could not connect to remote bluetooth device.", x);
						if (monitor.isCanceled()) break;
					}							
				}
				continue;	
			}
		}
		Logging.debug(getClass(), "Thread is exiting ...");
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
		BluetoothSocket c = connectors.remove(multiplexer);
		if (c != null) {
			try {
				c.close();	
			} catch (IOException e) {
				Logging.debug
					(getClass(), "Exception while closing stream connection.");	
			}
		}
		Enumeration<SystemID> e = multiplexers.keys();
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
	 * Returns the plug-in manager of this muliplexer plug-in.
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
		adapter = BluetoothAdapter.getDefaultAdapter();
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
		BluetoothServerSocket cn = server;
		if (cn != null) {
			try {
				cn.close();
			} catch (Throwable t) {
				Logging.error(getClass(), "Could not close notifier.", t);
			} finally {
				cn = null;	
			}
		}
		// wait for termination
		try {			
			Logging.debug(getClass(), "Waiting for monitor in bt plugin.");
			monitor.join();
			Logging.debug(getClass(), "Waiting is done.");
		} catch (InterruptedException e) {
			Logging.debug(getClass(), "Thread got interrupted.");	
		}
		// remove remaining multiplexers, if any
		Enumeration<BluetoothSocket> e = connectors.elements();
		while (e.hasMoreElements()) {
			BluetoothSocket s = (BluetoothSocket)e.nextElement();
			if (s != null) {
				try {
					s.close();	
				} catch (IOException ex) {
					Logging.debug(getClass(), "Could not close connection properly.");	
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
	
	/**
	 * This is a dirty helper method that is used to encode the system
	 * id as string.
	 * 
	 * @param id The system id as bytes.
	 * @return The system id as string.
	 */
	private static String getID(byte[] id) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < id.length; i++) {
			byte b = id[i];
			int high = ((b & 0xF0) >> 4);
			int low = (b & 0x0F);
			buffer.append((char)(high + 65));
			buffer.append((char)(low + 65));
		}
		return buffer.toString();
	}

}
