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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

/**
 * The bluetooth transceiver implements a JSR-82 compatible transceiver
 * plug-in. The bluetooth transceiver will continuously perform service
 * discovery and it will connect to any service that is available 
 * immediately in order to mimic broadcasts over links.
 * 
 * Note: Although this plug-in works correctly, the continuous discovery
 * performed by this plug-in should probably be controlled via some other
 * entity as it interrupts the bluetooth connections that are currently
 * open for multiple seconds every DISCOVERY_PERIOD amount of time.
 * 
 * @author Marcus Handte
 */
public class MxBluetoothTransceiver implements ITransceiver, IOperation, IMultiplexPlugin {

	/**
	 * The discovery strategy enables the implementation
	 * of different timeouts for the discovery operation. 
	 * 
	 * @author Mac
	 */
	public interface IDiscoveryStrategy {
		
		/**
		 * This method is called when the plug-in is
		 * started. The method may prepare all initial
		 * state but it must return immediately.
		 */
		public void start();
		
		/**
		 * The method is called when discovery operation
		 * is about to be executed. This method may block
		 * the calling thread for an undetermined amount
		 * of time, however, the called must be unblocked
		 * by a call to the stop method.
		 */
		public void execute();
		
		/**
		 * This method is called when the plug-in is
		 * stopped. The method must unblock all threads
		 * that are currently blocked in the strategy.
		 */
		public void stop();
		
	}
	
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
	 * A connector entry that maps connections to their respective
	 * bluetooth device id.
	 * 
	 * @author Mac
	 */
	public class Connection {
		/**
		 * The connector that represents the connection.
		 */
		protected StreamConnection stream;
		/**
		 * The bluetooth device id of the connection.
		 */
		protected String device;
		
		/**
		 * Creates a new connection entry with the given device
		 * and connection.
		 * 
		 * @param device The device id.
		 * @param stream The actual connection.
		 */
		public Connection(String device, StreamConnection stream) {
			this.device = device;
			this.stream = stream;
		}
		
		/**
		 * Returns the stream.
		 * 
		 * @return The stream.
		 */
		public StreamConnection getStream() {
			return stream;
		}
		
		/**
		 * Returns the device id.
		 * 
		 * @return The device id.
		 */
		public String getDevice() {
			return device;
		}
		
	}
	

	/**
	 * The discovery process supports service and device discovery.
	 * As long as the monitor of the operation signals go, this operation
	 * will open connections to new devices. 
	 * 
	 * @author Marcus Handte
	 */
	public class DiscoveryOperation implements DiscoveryListener, IOperation {

		/**
		 * The toggle denotes whether the operation is in the "device
		 * search" step (= false) or whether it is in the "service search"
		 * step (= true).
		 */
		private boolean toggle = false; 

		/**
		 * This vector contains the devices that have been discovered during
		 * device discovery.
		 */
		private Vector devices = new Vector();
		
		/**
		 * This vector contains the transactions for service discovery requests
		 * that are initiated after the device discovery has finished successfully.
		 */
		private Vector transactions = new Vector();
		
		/**
		 * This vector contains the services that have been found during service
		 * discovery.
		 */
		private Vector services = new Vector();

		/**
		 * This field contains the monitor that is used to run the discovery 
		 * operation.
		 */
		private IMonitor monitor;

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
			try {
				Logging.debug(getClass(), "Bluetooth discovery operation started.");
				this.monitor = monitor;
				synchronized (monitor) {
					LocalDevice local = LocalDevice.getLocalDevice();
					DiscoveryAgent agent = local.getDiscoveryAgent();
					while (! monitor.isCanceled()) {
						// wait some time
						strategy.execute();
						if (monitor.isCanceled()) break;
						Logging.debug(getClass(), "Bluetooth inquiry started.");
						// begin device search and wait for toggle
						try {
							toggle = false;
							agent.startInquiry(DiscoveryAgent.GIAC, this);	
						} catch (BluetoothStateException e) {
							Logging.debug(getClass(), "Bluetooth inquiry failed, continue later.");
							devices.removeAllElements();
							continue;
						}
						// wait until canceled or done
						while (! toggle && ! monitor.isCanceled()) {
							monitor.wait();			
						}
						// if canceled, stop inquiry and exit
						if (monitor.isCanceled()) {
							agent.cancelInquiry(this);
							break;
						} 
						// else start service search
						Logging.debug(getClass(), "Bluetooth devices found: " + devices.size());
						if (devices.size() > 0) {
							int[] ats = { ATTRIBUTE_ID };
							UUID[] ids = { PLUGIN_UUID };
							for (int i = 0, s = devices.size(); i < s; i++) {
								try {
									toggle = true;
									RemoteDevice rdev = (RemoteDevice)devices.elementAt(0);
									devices.removeElementAt(0);								
									int tid = agent.searchServices(ats, ids, rdev, this);
									transactions.addElement(new Integer(tid));
									// wait until service search completes or cancel
									while (toggle && ! monitor.isCanceled()) {
										monitor.wait();
									}
									if (monitor.isCanceled()) {
										return;
									}									
								} catch (BluetoothStateException e) {
									Logging.debug(getClass(), "Bluetooth service discovery failed, continue.");
								}
							}
						} else {
							continue; 
						}
						Logging.debug(getClass(), "Bluetooth services found: " + services.size());
						// check whether services need to be opened
						services: for (int i = 0, s = services.size(); i < s; i++) {
							try {
								ServiceRecord service = (ServiceRecord)services.elementAt(0);
								services.removeElementAt(0);						
								String device = service.getHostDevice().getBluetoothAddress().toUpperCase();
								synchronized (MxBluetoothTransceiver.this) {
									Enumeration e = connectors.elements();
									while (e.hasMoreElements()) {
										Connection c = (Connection)e.nextElement();
										if (c != null && device.equals(c.getDevice())) {
											continue services;
										}
									}
								}
								Logging.debug(getClass(), "Opening bluetooth connection to system " + device + ".");
								String url = service.getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, secure);
								StreamConnection connection = (StreamConnection)Connector.open(url);
								InputStream is = connection.openInputStream();
								OutputStream os = connection.openOutputStream();
								ObjectOutputStream oos = new ObjectOutputStream(os);
								oos.writeObject(SystemID.SYSTEM);
								oos.writeUTF(local.getBluetoothAddress().toUpperCase());
								oos.flush();
								// cross validation
								ObjectInputStream vois = new ObjectInputStream(is);
								SystemID id = (SystemID)vois.readObject();
								String remoteDevice = vois.readUTF().toUpperCase();
								if (! remoteDevice.equals(device)) {
									Logging.debug(getClass(), "Missmatch in local and remote device (" + device + " vs. " + remoteDevice + ").");
								}
								Logging.debug(getClass(), "Bluetooth connection to system " + id + " established.");
								synchronized (MxBluetoothTransceiver.this) {
									if (multiplexers.get(id) == null) {
										MultiplexFactory f = new MultiplexFactory(MxBluetoothTransceiver.this, is, os, true);
										multiplexers.put(id, f);
										Connection con = new Connection(device, connection);
										connectors.put(f, con);
										for (int ip = 0, sp = packets.size(); ip < sp; ip++) {
											PacketConnector c = (PacketConnector)packets.elementAt(ip);
											try {
												IPacketConnector ic = f.openConnector(DEFAULT_GROUP);
												c.addConnector(ic);
											} catch (IOException e) {
												Logging.error(getClass(), "Could not open packet connector.", e);	
											}	
										}					
									} else {
										connection.close();
									}
								}
							} catch (Throwable t) {
								Logging.error(getClass(), "Cannot connect to service.", t);
							} 
						}
					}
					Logging.debug(getClass(), "Bluetooth discovery operation stopped.");
				}
			} catch (InterruptedException e) {
				Logging.error(getClass(), "Bluetooth discovery got interrupted, exiting.", e);
			} catch (BluetoothStateException e) {
				Logging.error(getClass(), "Bluetooth discovery failed, exiting.", e);
			}
		}

		/**
		 * Call back for the device discovery.
		 * 
		 * @param arg0 The remote device that has been discovered.
		 * @param arg1 The device class of the remote device.
		 */
		public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {
			synchronized (monitor) {
				if (monitor.isCanceled()) {
					monitor.notify();
				}
			}
			devices.addElement(arg0);
		}
		
		/**
		 * Cal lback for the bluetooth device discovery.
		 * 
		 * @param arg0 The reason for the call.
		 */
		public void inquiryCompleted(int arg0) {
			synchronized (monitor) {
				toggle = true;
				monitor.notify();	
			}
		}
		
		/**
		 * Call back for the service discovery. 
		 * 
		 * @param arg0 The transaction id.
		 * @param arg1 The service record.
		 */
		public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
			for (int i = 0; i < arg1.length; i++) {
				services.addElement(arg1[i]);
			}
		}
		
		/**
		 * Call back for the service discovery.
		 * 
		 * @param arg0 The transaction id.
		 * @param arg1 The reason for the call.
		 */
		public void serviceSearchCompleted(int arg0, int arg1) {
			synchronized (monitor) {
				transactions.removeElement(new Integer(arg0));
				if (transactions.size() == 0) {
					toggle = false;
					monitor.notify();
				}
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
		new UUID("F0E0D0C0B0A000908070605040302011", false);

	/** 
	 * The bluetooth service attribute that contains the system id 
	 * of this system. 
	 */
	private static final int ATTRIBUTE_ID = 256;

	/**
	 * The multiplexers hashed by system id.
	 */
	private Hashtable multiplexers = new Hashtable();

	/**
	 * The bluetooth connectors hashed by multiplexers.
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
	 * The stream connection notifier used to disable the plug-in.
	 */
	private StreamConnectionNotifier notifier;

	/**
	 * The discovery strategy that is used to determine the timing
	 * of the bluetooth discovery operation. 
	 */
	private IDiscoveryStrategy strategy;

	/**
	 * A flag to indicate whether the connection shall be secured.
	 */
	private boolean secure;
	
	/**
	 * Creates a new bluetooth transceiver that uses the specified
	 * strategy for discovery and uses secure connections.
	 * 
	 * @param strategy The strategy that is used to determine the
	 * 	timing of the operation.
	 */
	public MxBluetoothTransceiver(IDiscoveryStrategy strategy) {
		this(strategy, true);
	}
	
	/**
	 * Creates a new bluetooth transceiver that uses the specified
	 * strategy for discovery.
	 * 
	 * @param strategy The strategy that is used to determine the
	 * 	timing of the operation.
	 * @param secure A flag to determine whether connections shall
	 * 	be secured. Note that insecure connections are not compatible 
	 * 	with the android version of this plug-in.
	 */
	public MxBluetoothTransceiver(IDiscoveryStrategy strategy, boolean secure) {
		if (strategy == null) throw new NullPointerException();
		this.strategy = strategy;
		this.secure = secure;
	}

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
		// create a monitor for the discovery thread
		final NullMonitor discoveryMonitor = new NullMonitor();
		final String device;
		try {
			// bring this device into discoverable mode
			final LocalDevice local = LocalDevice.getLocalDevice();
			device = local.getBluetoothAddress().toUpperCase();
			if (! local.setDiscoverable(DiscoveryAgent.GIAC)) {
				Logging.debug(getClass(), "Bluetooth initialization state unknown.");	
			}
			// prepare connection notifier and announce system id
			StringBuffer url = new StringBuffer("btspp://localhost:");
			url.append(PLUGIN_UUID.toString());
			url.append(";name=BASE");
			if (secure) {
				url.append(";authenticate=true;encrypt=true");				
			} else {
				url.append(";authenticate=false;encrypt=false");
			}
			notifier = (StreamConnectionNotifier)Connector.open(url.toString());		
			// start discovery operation		
			manager.performOperation(new DiscoveryOperation(), discoveryMonitor);		
		} catch (IOException e) {
			Logging.error(getClass(), "Exception in bluetooth setup, thread stopped.", e);
			throw e;	
		}
		// at this point, the connection notifier is prepared
		while (!monitor.isCanceled()) {
			StreamConnection connection = null;
			try {
				connection = notifier.acceptAndOpen();
				InputStream is = connection.openInputStream();
				OutputStream os = connection.openOutputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				SystemID remote = (SystemID)ois.readObject();
				String remoteDevice = ois.readUTF().toUpperCase();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(SystemID.SYSTEM);
				oos.writeUTF(device);
				Logging.debug(getClass(), "Incoming bluetooth connection from system " + remote + ".");
				synchronized (this) {
					if (multiplexers.get(remote) == null) {
						MultiplexFactory f = new MultiplexFactory(this, is, os, true);
						multiplexers.put(remote, f);
						connectors.put(f, new Connection(remoteDevice, connection));
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
						connection.close();
					}
				}
			} catch (IOException e) {
				Logging.error(getClass(), "Exception in bluetooth connect.", e);
				continue;	
			}
		}
		// wait until discovery operation finishes
		try {
			discoveryMonitor.cancel();
			discoveryMonitor.join();	
		} catch (InterruptedException e) {
			Logging.error(getClass(), "Joining on discovery failed.", e);	
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
		Connection c = (Connection)connectors.remove(multiplexer);
		if (c != null && c.getStream() != null) {
			try {
				c.getStream().close();	
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
		strategy.start();
		monitor = new NullMonitor();
		manager.performOperation(this, monitor);
	}
	
	/**
	 * Disables the reception of incoming connections and closes all
	 * currently incoming and outgoing connections.
	 */
	private void disablePlugin() {
		strategy.stop();
		// cancel monitor
		monitor.cancel();
		// interrupt reception
		StreamConnectionNotifier cn = notifier;
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
			monitor.join();
		} catch (InterruptedException e) {
			Logging.debug(getClass(), "Thread got interrupted.");	
		}
		// remove remaining multiplexers, if any
		Enumeration e = connectors.elements();
		while (e.hasMoreElements()) {
			Connection s = (Connection)e.nextElement();
			if (s != null && s.getStream() != null) {
				try {
					s.getStream().close();	
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

}
