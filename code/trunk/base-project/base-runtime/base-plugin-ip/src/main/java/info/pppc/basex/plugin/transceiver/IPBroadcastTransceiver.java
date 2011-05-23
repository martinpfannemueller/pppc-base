package info.pppc.basex.plugin.transceiver;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IConnector;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.ITransceiver;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.transceiver.ip.IIPPlugin;
import info.pppc.basex.plugin.transceiver.ip.IPPacketConnector;
import info.pppc.basex.plugin.transceiver.ip.IPStreamConnector;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * The ip broadcast transceiver plug-in delivers a transport end point based on ip. Broadcast
 * channels are implemented with datagram sockets above port 12500 and unicast channels 
 * rely on tcp. The implementation of this plug-in is  intended for all J2ME configurations 
 * starting from CDC with foundation profile. 
 * 
 * @author Marcus Handte
 */
public class IPBroadcastTransceiver implements IIPPlugin, ITransceiver, IOperation {

	/**
	 * The ability of the plug-in. [1][4].
	 */
	private static final short PLUGIN_ABILITY = 0x0104;
	
	/**
	 * The property name of the ip address. This is used to determine
	 * the ip address of a remote system through its plug-in description.
	 */
	private static final String PROPERTY_ADDRESS = "AD";
	
	/**
	 * The property name of the port number. This is used to determine
	 * the port number of a remote system through its plug-in description.
	 */
	private static final String PROPERTY_PORT = "PT";
	
	/**
	 * The reconnection period. Whenever the plug-in is enabled, it tries
	 * to open a server socket on the specified network interface and
	 * port. If the server socket cannot be opened, this value indicates
	 * the sleep time (in milliseconds) between open requests.
	 */
	private static final int RECONNECT_PERIOD = 5000;
	
	/**
	 * The timeout period for incoming connections. When the server 
	 * waits for incoming connections, this is the timeout (in milliseconds)
	 * after which the server will abort waiting.
	 */
	private static final int TIMEOUT_PERIOD = 2000;

	/**
	 * The maximum packet length for datagram packets sent by the
	 * packet connector.
	 */
	private static final int PACKET_LENGTH = 2048;
	
	/**
	 * Raw addresses that are never used to execute the server socket.
	 * The format is an array of byte arrays that contains addresses
	 * or address fragments. The filter algorithm will check whether 
	 * the address of the interface matches the complete sequence of
	 * bytes, ex. 127.0.0.1 will reject the local host interface,
	 * 129.69 will reject all ips starting with 129.69. 
	 */
	private static final byte[][] ADDRESS_FILTER = {
		new byte[]{ 127, 0, 0, 1 },       // do not connect to localhost
		new byte[]{ (byte)169, (byte)254} // do not connect to autoip
	};
	
	/**
	 * The first port of the broadcast socket.
	 */
	private static final int BROADCAST_PORT = 12500;
	
	/**
	 * The end point listeners that keep track of changes to the state
	 * of the end point.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * A list of connectors that is currently opened. This list is 
	 * maintained by the connectors. It ensures that a disable 
	 * operation is performed completely (otherwise the connectors
	 * might not be able to open upon the next enable operation).
	 */
	private Vector connectors = new Vector();

	/**
	 * The plug-in manager used to perform operations and to signal
	 * incoming connections.
	 */
	private ITransceiverManager manager = null;

	/**
	 * A flag that indicates whether the end point is enabled.
	 */
	private boolean enabled = false;
	
	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * A flag that indicates whether the tcp no delay socket option will
	 * be set.
	 */
	private boolean nodelay = true;
	
	/**
	 * The plug-in description of the ip plugin.
	 */
	private PluginDescription description;

	/**
	 * The address to which this plug-in is bound. If the address is set to
	 * null, the first interface on the host that is not loop back is used.
	 * A default address can be enforced using the constructor that receives
	 * an address.
	 */
	private byte[] address = null;
	
	/**
	 * The port that is used to receive incoming connections. Initially,
	 * the port is 0 (= any). After the plug-in has been enabled, the
	 * port is fixed.
	 */
	private int port = 0;
	
	/**
	 * The monitor that is used to monitor and cancel the reception operation.
	 */
	private NullMonitor monitor = null; 
	
	/**
	 * Creates a new instance of the plug-in that binds to any ip address
	 * and port number that is available on the local system. For this
	 * plug-in, the tcp no delay option will be set.
	 */
	public IPBroadcastTransceiver() {
		this(null, true);
	}
	
	/**
	 * Creates a new instance of the plug-in that binds to any ip address
	 * and port number that is available on the local system. The flag 
	 * indicates whether the tcp nodelay option will be set.
	 * 
	 * @param nodelay The flag that indicates whether the tcp no delay
	 * 	option will be set.
	 */
	public IPBroadcastTransceiver(boolean nodelay) {
		this(null, nodelay);
	}
	
	/**
	 * Creates a new instance of the plug-in that binds only to the specified
	 * ip address. Note that the bytes are interpreted in 2 complement. The
	 * first byte is the most significant. 
	 * 
	 * @param address The ip address to bind to or null if any.
	 * @throws IllegalArgumentException Thrown if the address does not
	 * 	indicate a valid ip address or if the address is part of the 
	 * 	filter list.
	 */
	public IPBroadcastTransceiver(byte[] address) {
		this(address, true);
	}
	
	/**
	 * Creates a new instance of the plug-in that binds only to the specified
	 * ip address. Note that the bytes are interpreted in 2 complement. The
	 * first byte is the most significant. 
	 * 
	 * @param address The ip address to bind to or null if any.
	 * @param nodelay The flag that indicates whether the tcp no delay
	 * 	option will be set.
	 * @throws IllegalArgumentException Thrown if the address does not
	 * 	indicate a valid ip address or if the address is part of the 
	 * 	filter list.
	 */
	public IPBroadcastTransceiver(byte[] address, boolean nodelay) {
		this.nodelay = nodelay;
		if (address != null) {
			if (address.length == 4) {
				filters: for (int i = 0; i < ADDRESS_FILTER.length; i++) {
					byte[] filter = ADDRESS_FILTER[i];
					for (int j = 0; j < filter.length; j++) {
						if (filter[j] != address[j]) continue filters; 		
					}
					throw new IllegalArgumentException("The address matches a filter.");
				}
				this.address = address;
			} else {
				throw new IllegalArgumentException("The address length must be 4.");	
			}			
		}
	}
	

	/**
	 * Called by the plug-in manager during initialization of the plug-in.
	 * A call to this method will overwrite the currently stored reference
	 * to a potentially already initialized manager. This should not be
	 * a problem except if the same plug-in instance is installed twice.
	 * 
	 * @param manager The manager of the plug-in used to interface with
	 * 	other plug-ins and to retrieve connection parameters.
	 */
	public void setTransceiverManager(ITransceiverManager manager) {
		this.manager = manager;
	}

	/**
	 * Called when the operation is started. The operation opens a
	 * server socket on an interface and continuously listens to it
	 * for incoming connections. If a connection is created, it
	 * dispatches it is dispatched current plug-in manager. The 
	 * monitor is checked every TIMEOUT_PERIOD milliseconds. If
	 * the monitor is canceled, the operation is finished.
	 * 
	 * @param monitor The monitor that is used to signal changes.
	 * @throws Exception Should never happen.
	 */
	public void perform(IMonitor monitor) throws Exception {
		ServerSocket server = null;
		try {
			server = getServerSocket();
		} catch (IOException e) {
			Logging.error(getClass(), "Error while opening server.", e);
		}
		receive: while (! monitor.isCanceled()) {
			try {
				try {
					while (server == null) {
						synchronized (monitor) {
							monitor.wait(RECONNECT_PERIOD);
						}
						if (monitor.isCanceled()) break receive;
						server = getServerSocket();							
					}
				} catch (InterruptedException e) {
					Logging.error(getClass(), "Error while waiting for server.", e);
					continue;
				} catch (IOException e) {
					Logging.error(getClass(), "Error while opening server.", e);
					continue;
				}
				Socket client = server.accept();
				client.setTcpNoDelay(nodelay);
				IPStreamConnector connector = new IPStreamConnector(this, client);
				synchronized (this) {
					connectors.addElement(connector);	
				}
				manager.acceptSession(connector);
			} catch (InterruptedIOException e) {
				// nothing to be done
			} catch (IOException e) {
				Logging.error(getClass(), "Error while receiving incoming message.", e);
				try {
					if (server != null)	server.close();
				} catch (IOException ioe) {
					Logging.error(getClass(), "Error while closing erronous socket.", ioe);
				}
				server = null;
			} 
		}
		// finally close the server socket used to accept incoming calls. 
		if (server != null) {
			server.close();
		}
	}

	/**
	 * Called in order to prepare a session. Updates the session with local
	 * attributes regarding the address and port of the remote system.
	 * 
	 * @param description The description of the remote system.
	 * @param collection The requirements regarding the communication.
	 * @param session The session that holds the attributes.
	 * @return True if the session can be established, false if the port or
	 * 	the address are not set.
	 */
	public boolean prepareSession(PluginDescription description, NFCollection collection, ISession session) {
		if (description == null) {
			return false;
		}
		byte[] address = (byte[])description.getProperty(PROPERTY_ADDRESS);
		if (address == null || address.length != 4){
			return false;
		}
		Integer port = (Integer)description.getProperty(PROPERTY_PORT);
		if (port == null) {
			return false;
		}
		session.setLocal(new Object[] { address, port });
		return true;
		
	}


	/**
	 * Opens a connection to the specified system. A call to this method
	 * will first determine whether it is possible to open a connection.
	 * If it is not possible due to the state of the plug-in (e.g., if it
	 * is disabled) an exception will be thrown.
	 * 
	 * @param session The session data that has been prepared by the 
	 * 	prepare method.
	 * @return The connector for the session.
	 * @throws IOException Thrown if the connector cannot be opened.
	 */
	public synchronized IStreamConnector openSession(ISession session) throws IOException {
		checkPlugin();
		byte[] address = (byte[])((Object[])session.getLocal())[0];
		Integer port = (Integer)((Object[])session.getLocal())[1];
		String name = (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." +
			(address[2] & 0xFF) + "." + (address[3] & 0xFF);
		InetAddress ip = InetAddress.getByName(name);
		Socket socket = getClientSocket(ip, port.intValue());
		IPStreamConnector connector = new IPStreamConnector(this, socket);
		connectors.addElement(connector);
		return connector;
	}

	/**
	 * Opens a connector that connects to the specified group.
	 * A call to this method will first determine whether it is possible 
	 * to open a connection. If it is not possible due to the state of 
	 * the plug-in (e.g., if it is disabled) an exception will be thrown.
	 * 
	 * @return The connector for the group.
	 * @throws IOException Thrown if the connector cannot be opened.
	 */
	public synchronized IPacketConnector openGroup() throws IOException {
		checkPlugin();
		DatagramSocket socket = getBroadcastSocket();
		InetAddress group = InetAddress.getByName("255.255.255.255");
		IPPacketConnector connector = new IPPacketConnector(this, socket, group, PACKET_LENGTH);
		connectors.addElement(connector);
		manager.performOperation(connector);
		return connector;
	}

	/**
	 * Called by the plug-in manager to start the stopped plug-in. A call to 
	 * this method will enable the end point in such a way that it will
	 * enable incoming and outgoing connections.
	 */
	public void start() {
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
	public void stop() {
		if (started) {
			setEnabled(false);
			started = false;
		}
	}

	/**
	 * Returns the description of the plug-in.
	 * 
	 * @return The plugin's description.
	 */
	public PluginDescription getPluginDescription() {
		if (description == null) {
			description = new PluginDescription
					(PLUGIN_ABILITY, EXTENSION_TRANSCEIVER);			
		}
		return description;
	}

	/**
	 * Adds a listener to the bundle of registered end point listeners. The 
	 * properly registered listeners will be informed if the state of the 
	 * end point changes.
	 * 
	 * @param type The type of event to register for.
	 * @param listener The listener to register.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public void addTransceiverListener(int type, IListener listener) 
			throws NullPointerException {
		listeners.addListener(type, listener);
	}

	/**
	 * Removes a potentially previously registered end point listener for a
	 * certain type of event.
	 * 
	 * @param type The type to unregister from.
	 * @param listener The listener to unregister.
	 * @return True if the listener is no longer registered, false if the
	 * 	listener has not been registered.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public boolean removeTransceiverListener(int type, IListener listener) 
			throws NullPointerException {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Enables or disables the end point provided by the plug-in and
	 * notifies all listeners if the state has changed.
	 * 
	 * @param enabled Set true to enable and false to disable.
	 */
	public void setEnabled(boolean enabled) {
		synchronized (this) {
			if (! started || this.enabled == enabled) {
				return;
			} else {
				this.enabled = enabled;
			}			
		}
		if (enabled) {
			enablePlugin();
			listeners.fireEvent(EVENT_TRANCEIVER_ENABLED);	
		} else {
			disablePlugin();
			listeners.fireEvent(EVENT_TRANCEIVER_DISABLED);	
		}
	}

	/**
	 * Determines whether the end point provided by the plug-in is enabled.
	 * 
	 * @return True if the end point provided by the plug-in is enabled,
	 * 	false otherwise.
	 */
	public synchronized boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns the address of the server socket.
	 * 
	 * @return The address of the server socket.
	 */
	public byte[] getAddress() {
		return address;
	}

	/**
	 * Returns the port of the server socket.
	 * 
	 * @return The port of the server socket.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Releases the specified connector by removing it from the 
	 * list of open connectors.
	 * 
	 * @param connector The connector to remove from the list of
	 * 	connectors.
	 */
	public synchronized void release(IPStreamConnector connector) {
		connectors.removeElement(connector);	
	}

	/**
	 * Releases the specified connector by removing it from the 
	 * list of open connectors.
	 * 
	 * @param connector The connector to remove from the list of
	 * 	connectors.
	 */
	public synchronized void release(IPPacketConnector connector) {
		connectors.removeElement(connector);	
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
		// end reception
		try {
			monitor.cancel();
			monitor.join();
		} catch (InterruptedException e) {
			Logging.error(getClass(), "Thread got interrupted.", e);
		}
		// close all opened connectors, removal is automatic
		for (int i = connectors.size() - 1; i >= 0; i--) {
			IConnector c = (IConnector)connectors.elementAt(i);
			c.release(); 
		}
	}
	
	/**
	 * Retrieves the address of the interface that should be used
	 * by this receiver.
	 * 
	 * @return The address that should be used by the receive operation
	 * 	or null if no such address can be found. This can happen for
	 * 	instance if a wireless network interface is not able to bootstrap.
	 * @throws IOException Thrown if the local host ip address
	 * 	or interface cannot be opened.
	 */
	private InetAddress getInetAddress() throws IOException {
		// initialize with the required interface
		String hostname = null;		
		if (address != null) {
			hostname = (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." +
				(address[2] & 0xFF) + "." + (address[3] & 0xFF);
		} else {
			// this method will fail on openwrt so do not call
			// it if the address is set (bugfix)
			hostname = InetAddress.getLocalHost().getHostName();	
		}
		InetAddress[] addresses = InetAddress.getAllByName(hostname);
		// remove filtered addresses
		ips: for (int i = 0; i < addresses.length; i++) {
			byte[] ip = addresses[i].getAddress();
			filters: for (int j = 0; j < ADDRESS_FILTER.length; j++) {
				byte[] filter = ADDRESS_FILTER[j];
				for (int k = 0; k < filter.length; k++) {
					if (filter[k] != ip[k]) continue filters; 		
				}
				continue ips;
			}
			address = ip;
			return addresses[i];								
		}
		throw new IOException("Cannot find valid IP.");
	}

	/**
	 * Tries to open a server socket on the current port and address without
	 * using the filtered addresses.
	 * 
	 * @return A server socket that is bound to the current address and port.
	 * @throws IOException Thrown if the socket cannot be opened.
	 */
	private ServerSocket getServerSocket() throws IOException {
		InetAddress ip = getInetAddress();
		ServerSocket server = new ServerSocket(port, 50, ip);
		server.setSoTimeout(TIMEOUT_PERIOD);
		port = server.getLocalPort();
		PluginDescription d = getPluginDescription();
		d.setProperty(PROPERTY_PORT, new Integer(port), true);
		d.setProperty(PROPERTY_ADDRESS, address, true);
		Logging.log(getClass(), "Running server on " +
			(address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." +
			(address[2] & 0xFF) + "." + (address[3] & 0xFF) + ":" + 
			port + "."
		);
		return server;				
	}

	/**
	 * Tries to open a client socket on the current port and address that is
	 * connected with the specified port and address.
	 * 
	 * @param target The target system to connect to.
	 * @param port The target port to connect to.
	 * @return The socket connected to the port.
	 * @throws IOException Thrown if the socket cannot be opened.
	 */
	private Socket getClientSocket(InetAddress target, int port) throws IOException {
		InetAddress ip = getInetAddress();
		Socket socket = new Socket(target, port, ip, 0);
		socket.setTcpNoDelay(nodelay);
		return socket;		 		
	}
	
	/**
	 * Tries to open a broadcast socket on the bc port.
	 * 
	 * @return The socket connected to the local system.
	 * @throws IOException Thrown if the socket cannot be opened.
	 */
	private DatagramSocket getBroadcastSocket() throws IOException {
		InetAddress address = getInetAddress();
		return new DatagramSocket(BROADCAST_PORT, address);
	}



}
