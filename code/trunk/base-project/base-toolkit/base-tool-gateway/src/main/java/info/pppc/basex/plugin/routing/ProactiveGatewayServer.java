package info.pppc.basex.plugin.routing;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.ObjectID;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.ObjectStreamTranslator;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.OperationPool;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;
import info.pppc.basex.plugin.routing.server.IGatewayRegistryProvider;
import info.pppc.basex.plugin.routing.server.IStreamConnectorProvider;
import info.pppc.basex.plugin.routing.server.LoggingMessageBuffer;
import info.pppc.basex.plugin.routing.server.MultiplexPluginAdapter;
import info.pppc.basex.plugin.routing.server.PluginManagerAdapter;
import info.pppc.basex.plugin.util.MultiplexFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

/**
 * The proactive gateway server implements the server for the proactive routing
 * gateways. The gateways connect to the server. To do this, they send updates
 * of their reachable devices to the server through a control connection.
 * 
 * @author Mac
 */
public class ProactiveGatewayServer implements IGatewayRegistryProvider, IStreamConnectorProvider, IRemoteGatewayServer {

	/**
	 * Registers the serializable types that are implemented and used by the
	 * BASE core.
	 */
	static {
		ObjectStreamTranslator.register(DeviceDescription.class.getName(),
				DeviceDescription.ABBREVIATION);
		ObjectStreamTranslator.register(Invocation.class.getName(),
				Invocation.ABBREVIATION);
		ObjectStreamTranslator.register(NFCollection.class.getName(),
				NFCollection.ABBREVIATION);
		ObjectStreamTranslator.register(NFDimension.class.getName(),
				NFDimension.ABBREVIATION);
		ObjectStreamTranslator.register(ObjectID.class.getName(),
				ObjectID.ABBREVIATION);
		ObjectStreamTranslator.register(PluginDescription.class.getName(),
				PluginDescription.ABBREVIATION);
		ObjectStreamTranslator.register(SystemID.class.getName(),
				SystemID.ABBREVIATION);
		ObjectStreamTranslator.register(ReferenceID.class.getName(),
				ReferenceID.ABBREVIATION);
		ObjectStreamTranslator.register(InvocationException.class.getName(),
				InvocationException.ABBREVIATION);
	}

	/**
	 * The retry period for establishing the server socket.
	 */
	public static final long CONNECT_RETRY = 10000;

	/**
	 * The backlog of the server.
	 */
	public static final int CONNECT_BACKLOG = 100;

	/**
	 * The local address of the server.
	 */
	private byte[] address = null;

	/**
	 * The local port of the server.
	 */
	private short port = 0;

	/**
	 * The operation pool that is used for threading.
	 */
	private OperationPool pool = new OperationPool(20);

	/**
	 * The plugin manager that is used to create multiplex plugin adapters.
	 */
	private IPluginManager manager = new PluginManagerAdapter(pool);

	/**
	 * A random number generator used to select gateways.
	 */
	private Random random = new Random();

	/**
	 * A log stream to keep the log messages.
	 */
	private LoggingMessageBuffer log = new LoggingMessageBuffer(1000);
	
	/**
	 * Creates a new proactive gateway server that binds to the specified
	 * address and port.
	 * 
	 * @param address
	 *            The address of the interface to bind to.
	 * @param port
	 *            The port to listen to.
	 */
	public ProactiveGatewayServer(byte[] address, short port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * Called to start the server.
	 * 
	 * @param replace A boolean to indicate whether the logging stream
	 * 	should be replaced.
	 */
	public void start(boolean replace) {
		if (replace) {
			Logging.setOutput(log);	
		}
		while (true) {
			try {
				Logging.debug(getClass(), "Starting server ...");
				ServerSocket server = new ServerSocket(port, CONNECT_BACKLOG,
						getInetAddress());
				try {
					while (true) {
						Logging.debug(getClass(),
								"Waiting for incomming connections ...");
						Socket s = server.accept();
						MultiplexPluginAdapter plugin = new MultiplexPluginAdapter(
								manager, this, this);
						plugin.start();
						new MultiplexFactory(plugin, s.getInputStream(),
								s.getOutputStream());
						Logging.debug(getClass(),
								"Multiplexer for incomming connection started.");
					}
				} catch (IOException e) {
					Logging.error(getClass(),
							"Exception while accepting connections.", e);
					server.close();
				}
			} catch (IOException e) {
				Logging.error(getClass(), "Could not start server.", e);
				try {
					Thread.sleep(CONNECT_RETRY);
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	/**
	 * Retrieves the address of the interface that should be used by this
	 * receiver.
	 * 
	 * @return The address that should be used by the receive operation or null
	 *         if no such address can be found. This can happen for instance if
	 *         a wireless network interface is not able to bootstrap.
	 * @throws UnknownHostException
	 *             Thrown if the local host ip address or interface cannot be
	 *             opened.
	 */
	protected InetAddress getInetAddress() throws UnknownHostException {
		// initialize with the required interface
		String hostname = (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "."
				+ (address[2] & 0xFF) + "." + (address[3] & 0xFF);
		InetAddress[] addresses = InetAddress.getAllByName(hostname);
		// remove filtered addresses
		ips: for (int i = 0; i < addresses.length; i++) {
			byte[] ip = addresses[i].getAddress();
			for (int k = 0; k < address.length; k++) {
				if (address[k] != ip[k])
					continue ips;
			}
			return addresses[i];
		}
		throw new UnknownHostException("Cannot find valid IP.");
	}

	/**
	 * The multiplex factories hashed by gateway.
	 */
	private Hashtable<SystemID, MultiplexFactory> factories = new Hashtable<SystemID, MultiplexFactory>();

	/**
	 * A hash table of transceiver plugin descriptions hashed by the gateway.
	 */
	private Hashtable<SystemID, PluginDescription[]> plugins = new Hashtable<SystemID, PluginDescription[]>();

	/**
	 * A hash table of vectors that contains the devices that are reachable via
	 * a particular gateway.
	 */
	private Hashtable<SystemID, Vector<SystemID>> devices = new Hashtable<SystemID, Vector<SystemID>>();

	/**
	 * A hash table of vectors that contains the gateways for a particular
	 * device.
	 */
	private Hashtable<SystemID, Vector<SystemID>> gateways = new Hashtable<SystemID, Vector<SystemID>>();

	/**
	 * Returns a connection to a gateway that is connected to the specified
	 * target.
	 * 
	 * @param target
	 *            The target to connect to.
	 * @return The stream connector or null if not possible.
	 * @throws IOException
	 *             Thrown if an exception occurs during stream open.
	 */
	public IStreamConnector getConnector(SystemID target) throws IOException {
		MultiplexFactory gwf = getFactory(target);
		if (gwf == null)
			return null;
		return gwf.openConnector();
	}

	/**
	 * Returns a multiplex factory that connects to a particular target.
	 * 
	 * @param target
	 *            The target.
	 * @return The factory that connects to a gateway that can reach the target.
	 */
	private synchronized MultiplexFactory getFactory(SystemID target) {
		Vector<SystemID> gws = gateways.get(target);
		if (gws == null || gws.isEmpty())
			return null;
		if (gws.contains(target)) {
			Logging.debug(getClass(), "Using " + target + " as gateway for "
					+ target + ".");
			return (MultiplexFactory) factories.get(target);
		} else {
			SystemID gwid = (SystemID) gws
					.elementAt(random.nextInt(gws.size()));
			Logging.debug(getClass(), "Using " + gwid + " as gateway for "
					+ target + ".");
			return (MultiplexFactory) factories.get(gwid);
		}
	}

	/**
	 * Associates a device with a particular gateway.
	 * 
	 * @param gateway
	 *            The gateway of the device.
	 * @param target
	 *            The target that is reachable.
	 */
	public synchronized void addDevice(SystemID gateway, SystemID target) {
		// add devices to reach ability list for gateway
		Vector<SystemID> devs = devices.get(gateway);
		if (devs == null) {
			devs = new Vector<SystemID>();
			devices.put(gateway, devs);
		}
		if (!devs.contains(target)) {
			devs.addElement(target);
		}

		// add gateway to gateway list for target
		Vector<SystemID> gws = gateways.get(target);
		if (gws == null) {
			gws = new Vector<SystemID>();
			gateways.put(target, gws);
		}
		if (!gws.contains(gateway)) {
			gws.addElement(gateway);
		}
	}

	/**
	 * Adds a particular factory for the gateway.
	 * 
	 * @param gateway
	 *            The gateway that has connected.
	 * @param factory
	 *            The factory of the gateway.
	 */
	public synchronized void addGateway(SystemID gateway,
			MultiplexFactory factory, PluginDescription[] transceivers) {
		MultiplexFactory f = (MultiplexFactory) factories.get(gateway);
		if (f != null) {
			Logging.debug(getClass(), "Detected " + gateway
					+ " as duplicate gateway, closing both connections.");
			f.close();
			factory.close();
		} else {
			factories.put(gateway, factory);
			plugins.put(gateway, transceivers);	
		}
		
	}

	/**
	 * Returns the transceiver plugins for the gateway of the specified
	 * target.
	 * 
	 * @param target The target to connect to.
	 * @return A bunch of plug-ins or null, if the target cannot be 
	 * 	associated with a particular gateway.
	 */
	public PluginDescription[] getGatewayPlugins(SystemID target) {
		if (target == null) return null;
		// translate the target into a gateway
		Vector<SystemID> gws = gateways.get(target);
		SystemID gateway = null;
		if (gws == null || gws.isEmpty()) return null;
		if (gws.contains(target)) {
			Logging.debug(getClass(), "Using " + target + " as gateway for " + target + ".");
			gateway = target;
		} else {
			SystemID gwid = (SystemID) gws
					.elementAt(random.nextInt(gws.size()));
			Logging.debug(getClass(), "Using " + gwid + " as gateway for "
					+ target + ".");
			gateway = gwid;
		}
		// get the plug-in descriptions of the gateway
		return (PluginDescription[]) plugins.get(gateway);
	}

	/**
	 * Unassociates a particular device with a given gateway.
	 * 
	 * @param gateway
	 *            The gateway to remove.
	 * @param target
	 *            The target to remove.
	 */
	public synchronized void removeDevice(SystemID gateway, SystemID target) {
		Vector<SystemID> devs = devices.get(gateway);
		if (devs != null) {
			devs.removeElement(target);
			if (devs.isEmpty()) {
				devices.remove(gateway);
			}
		}
		Vector<SystemID> gws = gateways.get(target);
		if (gws != null) {
			gws.removeElement(gateway);
			if (gws.isEmpty()) {
				gateways.remove(target);
			}
		}
	}

	/**
	 * Removes a complete gateway.
	 * 
	 * @param gateway
	 *            The gateway to remove.
	 * @param factory
	 *            The factory to add.
	 */
	public synchronized void removeGateway(SystemID gateway,
			MultiplexFactory factory) {
		MultiplexFactory f = (MultiplexFactory) factories.get(gateway);
		if (f != factory)
			return;
		factories.remove(gateway);
		plugins.remove(gateway);
		Vector<SystemID> devs = devices.remove(gateway);
		if (devs != null) {
			for (int i = 0; i < devs.size(); i++) {
				SystemID target = (SystemID) devs.elementAt(i);
				Vector<SystemID> gws = gateways.get(target);
				if (gws != null)
					gws.removeElement(gateway);
				if (gws.isEmpty())
					gateways.remove(target);
			}
		}
	}

	/**
	 * Remote interface method for applications. Returns a hashtable of
	 * gateways with their associated devices.
	 * 
	 * @return A hashtable of gateways with associated devices.
	 */
	public Hashtable<String, Vector<String>> getGateways()  {
		Hashtable<SystemID, Vector<SystemID>> gwMap = devices;
		Hashtable<String, Vector<String>> gwStringMap = new Hashtable<String, Vector<String>>();
		for (Entry<SystemID, Vector<SystemID>> entry : gwMap.entrySet()) {
			Vector<String> tmp = new Vector<String>();
			for (SystemID target : entry.getValue()) {
				tmp.add(target.toString());
			}
			gwStringMap.put(entry.getKey().toString(), tmp);
		}
		return gwStringMap;
	}

	/**
	 * Returns the log messages.
	 * 
	 * @return The log messages.
	 */
	public ArrayList<String> getMessages() {
		return log.getMessages();
	}

}
