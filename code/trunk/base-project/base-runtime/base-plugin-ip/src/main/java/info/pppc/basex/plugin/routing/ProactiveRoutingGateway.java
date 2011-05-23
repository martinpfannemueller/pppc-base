package info.pppc.basex.plugin.routing;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.ISession;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;
import info.pppc.base.system.util.Static;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * The proactive routing gateway plug-in implements proactive routing with
 * ip-based gateway functionality. This version of the plug-in is simplified in
 * that it only connects through the router but does not try to connect directly
 * to a remote device.
 * 
 * @author Mac
 */
public class ProactiveRoutingGateway extends ProactiveRouting implements
		IMultiplexPlugin {

	/**
	 * Default router address.
	 */
	public static final byte[] ROUTER_ADDRESS = new byte[] { (byte) 134,
		(byte) 91, (byte) 76, (byte) 7 };
	/**
	 * Default router port.
	 */
	public static final short ROUTER_PORT = 20000;

	/**
	 * The retry interval in milliseconds. This is the pause interval
	 * between connection attempts to the gateway server.
	 */
	public static final long CONNECT_RETRY = 10000;

	/**
	 * The maximum amount of time that the plug-in will try to
	 * open a direct connection to a remote system. Once the timeout
	 * has passed, the system will create an indirect connection
	 * via the gateway.
	 */
	public static final long TIMEOUT_CONNECT = 1000;
	
	/**
	 * The timeout after which the plug-in will attempt to reestablish
	 * direct connections to a client that is known to not support
	 * direct connections due to a previous attempt.
	 */
	public static final long TIMEOUT_RECONNECT = 60000;
	
	/**
	 * The ip address of the router that should be used to find/connect to other
	 * gateways.
	 */
	private byte[] routerAddress;

	/**
	 * The port of the router.
	 */
	private short routerPort;

	/**
	 * The multiplex factory that connects to the router.
	 */
	private MultiplexFactory routerFactory;

	/**
	 * A hash table of systems that are known not to be reachable directly.
	 * The hash table hashes the system id to a long that represents the
	 * time when the next direct connection attempt shall be performed. 
	 */
	private Hashtable indirect = new Hashtable();
	
	/**
	 * The router monitor that is bound to the operation that connects to the
	 * router and updates the gateway registry.
	 */
	private NullMonitor routerMonitor = new NullMonitor();

	/**
	 * Writes the plugin description of the transceivers to a give OutputStream.
	 * 
	 * @param out
	 *            The OutputStream to write to.
	 * */
	private void writePluginsToStream(ObjectOutputStream out)
			throws IOException {
		Vector announcement = new Vector();
		PluginDescription[] plugins = manager
				.getPluginDescriptions(SystemID.SYSTEM);
		for (int i = 0; i < plugins.length; i++) {
			PluginDescription pd = plugins[i];
			// only announce transceivers
			if (pd.getExtension() == EXTENSION_TRANSCEIVER) {
				announcement.addElement(pd);
			}
		}
		// write number of transceivers
		out.writeInt(announcement.size());
		for (int i = 0; i < announcement.size(); i++) {
			// write transceivers
			out.writeObject((PluginDescription) announcement.elementAt(i));
		}
		out.flush();
	}

	/**
	 * Reads the plugin description of the transceivers from a give InputStream
	 * and registers them for the corresponding device.
	 * 
	 * @param next
	 *            The OutputStream to write to.
	 * @param in
	 *            The InputStream to read from.
	 * @return Plugin descriptions
	 * */
	private PluginDescription[] readPluginsFromStream(SystemID next,
			ObjectInputStream in) throws IOException {
		// read number of transceivers
		int x = in.readInt();
		Logging.debug(getClass(), "Received " + x + " transceivers to contact " + next + ".");
		DeviceRegistry registry = InvocationBroker.getInstance()
				.getDeviceRegistry();
		PluginDescription[] pd = new PluginDescription[x];
		// read and register transceivers
		for (int i = 0; i < x; i++) {
			pd[i] = (PluginDescription) in.readObject();
			registry.registerPlugin(next, pd[i]);
		}
		return pd;
	}

	/**
	 * Removes the plugin descriptions from the registry.
	 * 
	 * @param next
	 *            The SystemId of the system.
	 * @param pd
	 *            The plugin descriptions to be removed.
	 */
	private void removePluginsFromRegistry(SystemID next, PluginDescription[] pd) {
		DeviceRegistry registry = InvocationBroker.getInstance()
				.getDeviceRegistry();
		for (int i = 0; i < pd.length; i++) {
			registry.removePlugin(next, pd[i]);
		}
	}

	/**
	 * Opens a direct link to the next hop.
	 * 
	 * @param nextHop
	 *            The next hop to contact.
	 * @param reqs
	 *            The requirements for the session.
	 * @param numberOfHops
	 *            Number of hops on the route.
	 * @return IStreamConnector for the link or null if failed
	 */
	private IStreamConnector openDirectLink(final SystemID nextHop, final NFCollection reqs, final int numberOfHops) {
		// prevent possible loops upon initial discovery
		if (numberOfHops > (MAXIMUM_HOPS & 0xff) << 1) return null;
		// determine whether direct connections are known to fail
		Long timeout = (Long)indirect.get(nextHop);
		if (timeout != null) {
			if (timeout.longValue() < System.currentTimeMillis()) {
				indirect.remove(nextHop);
			} else {
				Logging.debug(getClass(), "Direct connection attempt is known to fail.");
				return null;
			}
		}
		final ISession directSession = manager.prepareSession
			(manager.createSession(nextHop, PLUGIN_ABILITY), reqs);
		if (null != directSession) {
			final IStreamConnector[] result = new IStreamConnector[1];
			IMonitor monitor = new NullMonitor();
			manager.performOperation(new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					try {	
						Logging.debug(getClass(), "Opening direct connection to " + nextHop + ".");
						IStreamConnector directConnector = manager.openSession(directSession);
						try {
							ObjectOutputStream outputStream = new ObjectOutputStream(
									directConnector.getOutputStream());
							outputStream.writeObject(reqs);
							outputStream.writeInt(numberOfHops);
							outputStream.flush();
							synchronized (monitor) {
								if (! monitor.isCanceled()) {
									Logging.debug(getClass(), "Direct connection attempt successful.");
									indirect.remove(nextHop);
									result[0] = directConnector;
									monitor.done();
								} else {
									Logging.debug(getClass(), "Direct connection attempt successful but timeout passed already.");
									directConnector.release();
								}	
							}
						} catch (IOException e) {
							Logging.debug(getClass(), "Failed to transmit route request via direct connection.");
							directConnector.release();
						}															
					} catch (IOException e) {
						Logging.debug(getClass(), "Could not establish direct connection.");
					}
				}
			}, monitor);
			synchronized (monitor) {
				long now = System.currentTimeMillis();
				long end = now + TIMEOUT_CONNECT;
				while (! monitor.isDone()) {
					now = System.currentTimeMillis();
					if (now >= end) {
						Logging.debug(getClass(), "Direct connection attempt timed out.");
						indirect.put(nextHop, new Long(now + TIMEOUT_RECONNECT));
						monitor.cancel();
						return null;
					} else {
						try {
							Logging.debug(getClass(), "Waiting for direct connection attempt for " + (end - now) + " ms.");
							monitor.wait(end - now);	
						} catch (InterruptedException e) {
							Logging.debug(getClass(), "Thread got interrupted.");
						}
					}
				}
				return result[0];
			}
			
		}
		return null;
	}

	/**
	 * The device listener that updates the device list according to changes in
	 * the device registry.
	 */
	private IListener deviceListener = new IListener() {
		public void handleEvent(Event event) {
			switch (event.getType()) {
			case DeviceRegistry.EVENT_DEVICE_REMOVED: {
				DeviceDescription d = (DeviceDescription) event.getData();
				SystemID rid = d.getSystemID();
				synchronized (routerMonitor) {
					for (int i = 1; i < devices.size(); i += 2) {
						SystemID sid = (SystemID) devices.elementAt(i);
						if (sid.equals(rid)) {
							devices.removeElementAt(i - 1);
							devices.insertElementAt(Static.FALSE, i - 1);
							routerMonitor.notify();
							break;
						}
					}
				}
				break;
			}
			default:
				break;
			}
		}
	};

	/**
	 * Creates a new gateway that binds at any local address and that uses the
	 * default router configuration.
	 * 
	 * @param retransmit
	 *            Determines whether packets are retransmitted over the same
	 *            plug-in.
	 */
	public ProactiveRoutingGateway(boolean retransmit) {
		this(retransmit, ROUTER_ADDRESS, ROUTER_PORT);
	}

	/**
	 * Creates a new gateway that uses the specified router configuration.
	 * 
	 * @param retransmit
	 *            Determines whether packets are retransmitted over the same
	 *            plug-in.
	 * @param routerAddress
	 *            The ip address of the router.
	 * @param routerPort
	 *            The port number of the router.
	 */
	public ProactiveRoutingGateway(boolean retransmit, byte[] routerAddress,
			short routerPort) {
		super(retransmit);
		this.routerAddress = routerAddress;
		this.routerPort = routerPort;
		routerMonitor.isDone();
	}
	
	/**
	 * Creates a new gateway that uses the router configuration specified in a configuration file.
	 * 
	 * @param retransmit
	 *            Determines whether packets are retransmitted over the same
	 *            plug-in.
	 * @param fileName
	 *            Name of the configuration file.
	 */
	public ProactiveRoutingGateway(boolean retransmit, String fileName) {
		super(retransmit);
		byte[] routerAddress = ROUTER_ADDRESS;
		short routerPort = ROUTER_PORT;
		Properties prop = new Properties();
		try {
			InputStream is = new FileInputStream(fileName);
			prop.load(is);
			String ra = prop.getProperty("routerAddress");
			String rp = prop.getProperty("routerPort");
			if (ra != null) {
				InetAddress ip = Inet4Address.getByName(ra.trim());
				routerAddress = ip.getAddress();
			}
			if (rp != null) {
				routerPort = Short.parseShort(rp.trim());
			}
		} catch (Exception e) {
		}
		this.routerAddress = routerAddress;
		this.routerPort = routerPort;
		routerMonitor.isDone();
	}

	/**
	 * Called to open a connection using the specified session. This method
	 * first determines whether the connection can be established locally. If
	 * not, the remote gateway server is used.
	 * 
	 * @param session
	 *            The session that defines the target.
	 * @return The stream connector to connect to the target.
	 * @throws IOException
	 *             Thrown if the connection cannot be established.
	 */
	public IStreamConnector openSession(ISession session) throws IOException {
		try {
			return super.openSession(session);
		} catch (IOException e) {
			Object[] local = (Object[]) session.getLocal();
			Vector route = (Vector) local[0];
			SystemID nextHop = (SystemID) route.elementAt(0);

			if (route.size() == 1) {
				// try the gateway server
				MultiplexFactory f = routerFactory;
				if (f != null) {
					NFCollection source = (NFCollection) local[1];
					NFCollection target = new NFCollection();
					NFDimension[] dimensions = source
							.getDimensions(EXTENSION_TRANSCEIVER);
					for (int i = dimensions.length - 1; i >= 0; i--) {
						target.addDimension(EXTENSION_TRANSCEIVER,
								dimensions[i]);
					}
					IStreamConnector c = f.openConnector();
					try {
						ObjectOutputStream out = new ObjectOutputStream(
								c.getOutputStream());
						out.writeObject(target);
						out.writeObject(nextHop);
						out.flush();
						// Request for the route has been send to the server
						// server will answer with a possible direct route
						PluginDescription[] pd = readPluginsFromStream(nextHop,
								new ObjectInputStream(c.getInputStream()));

						IStreamConnector directConnector = null;
						directConnector = openDirectLink(nextHop, target, 0);

						removePluginsFromRegistry(nextHop, pd);
						if (null != directConnector) {
							// direct link was established
							// tell server that no forwarding is needed
							out.writeBoolean(false);
							out.flush();
							c.release();
							Logging.debug(getClass(), "Establishing direct connection to " + nextHop + ".");
							return directConnector;

						} else {
							// direct link could not be established
							// tell server that no forwarding is needed
							out.writeBoolean(true);
							out.flush();
							Logging.debug(getClass(), "Establishing indirect connection to " + nextHop + ".");
							return c;
						}

					} catch (IOException ie) {
						c.release();
						throw ie;
					}
				} else {
					throw new IOException("Not connected to gateway server.");
				}
			} else {
				throw e;
			}
		}
	}

	/**
	 * Called when a remote device connects through a local plugin. The method
	 * decides whether the incoming request is delivered locally, forwarded
	 * locally or transfered through the router.
	 * 
	 * @param connector
	 *            The connector that is bound to the incomming communication.
	 * @param session
	 *            The session object for the communication.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session) {
		try {
			ObjectInputStream input = new ObjectInputStream(
					connector.getInputStream());
			NFCollection reqs = (NFCollection) input.readObject();
			int hops = input.readInt();
			// TODO: check this - prevent loops
			if (hops > (MAXIMUM_HOPS & 0xff) << 1) {
				connector.release();
				return;
			}
			if (hops == 0) {
				manager.acceptSession(connector);
			} else {
				SystemID next = (SystemID) input.readObject();
				ISession s = manager.prepareSession(
						manager.createSession(next, PLUGIN_ABILITY), reqs);
				if (s != null) {
					try {
						IStreamConnector c = manager.openSession(s);
						try {
							ObjectOutputStream output = new ObjectOutputStream(
									c.getOutputStream());
							output.writeObject(reqs);
							output.flush();
							NullMonitor monitor1 = new NullMonitor();
							NullMonitor monitor2 = new NullMonitor();
							manager.performOperation(new RoutingOperation(c,
									connector, monitor2), monitor1);
							manager.performOperation(new RoutingOperation(
									connector, c, monitor1), monitor2);
						} catch (IOException e) {
							//Logging.error(getClass(), "Could not establish forwarding link.", e);
							Logging.debug(getClass(), "Could not establish forwarding link.");
							c.release();
							connector.release();
						}
					} catch (IOException e) {
						//Logging.error(getClass(), "Could not create forward connector.", e);
						Logging.debug(getClass(), "Could not create forward connector.");
						connector.release();
					}
				} else if (hops == 1) {
					MultiplexFactory f = routerFactory;
					if (f != null) {
						try {
							IStreamConnector c = f.openConnector();
							ObjectOutputStream output = new ObjectOutputStream(
									c.getOutputStream());
							output.writeObject(reqs);
							output.writeObject(next);
							output.flush();
							// Request for the route has been send to the server
							// server will answer with a possible direct route
							PluginDescription[] pd = readPluginsFromStream(
									next,
									new ObjectInputStream(c.getInputStream()));
							IStreamConnector directConnector = null;
							directConnector = openDirectLink(next, reqs, 0);
							removePluginsFromRegistry(next, pd);

							IStreamConnector nextHopConnector = null;

							if (null != directConnector) {
								// direct link was established
								// tell server that no forwarding is needed
								output.writeBoolean(false);
								Logging.debug(getClass(), "Using direct link to connecto to " + next + ".");
								nextHopConnector = directConnector;

							} else {
								// direct link could not be established
								// tell server that no forwarding is needed
								output.writeBoolean(true);
								Logging.debug(getClass(), "Using indirect link to connect to " + next + ".");
								nextHopConnector = c;
							}
							output.flush();
							NullMonitor monitor1 = new NullMonitor();
							NullMonitor monitor2 = new NullMonitor();
							manager.performOperation(new RoutingOperation(
									nextHopConnector, connector, monitor2),
									monitor1);
							manager.performOperation(new RoutingOperation(
									connector, nextHopConnector, monitor1),
									monitor2);
						} catch (IOException e) {
							connector.release();
						}
					} else {
						Logging.debug(getClass(), "Command connection not available.");
						connector.release();
					}
				} else {
					Logging.debug(getClass(), "Could not create session for forwarder.");
					connector.release();
				}
			}
		} catch (IOException e) {
			//Logging.error(getClass(), "Could deserialize incoming route request.", e);
			Logging.debug(getClass(), "Could deserialize incoming route request.");
			connector.release();
		}
	}

	/**
	 * Called whenever the router is used to connect to the gateway plugin. This
	 * will only happen in non-server mode.
	 * 
	 * @param source
	 *            The source factory which is simply the factory connecting to
	 *            the router.
	 * @param connector
	 *            The connector created by the factory.
	 */
	public void acceptConnector(final MultiplexFactory source,
			final IStreamConnector connector) {
		manager.performOperation(new IOperation() {
			/**
			 * Performs the connection establishment.
			 */
			public void perform(IMonitor monitor) throws Exception {
				try {
					ObjectInputStream input = new ObjectInputStream(connector
							.getInputStream());
					NFCollection reqs = (NFCollection) input.readObject();
					SystemID next = (SystemID) input.readObject();
					if (next.equals(SystemID.SYSTEM)) {
						manager.acceptSession(connector);
					} else {
						Vector route = getRoute(next);
						if (route != null) {
							next = (SystemID) route.elementAt(0);
							route.removeElementAt(0);
							ISession s = manager.prepareSession(
									manager.createSession(next, PLUGIN_ABILITY),
									reqs);
							if (s != null) {
								try {
									IStreamConnector c = manager.openSession(s);
									try {
										ObjectOutputStream output = new ObjectOutputStream(
												c.getOutputStream());
										output.writeObject(reqs);
										output.writeInt(route.size());
										for (int i = 0; i < route.size(); i++) {
											output.writeObject(route
													.elementAt(i));
										}
										output.flush();
										NullMonitor monitor1 = new NullMonitor();
										NullMonitor monitor2 = new NullMonitor();
										manager.performOperation(
												new RoutingOperation(c,
														connector, monitor2),
												monitor1);
										manager.performOperation(
												new RoutingOperation(connector,
														c, monitor1), monitor2);
									} catch (IOException e) {
										//Logging.error(getClass(), "Could not establish forwarding link.", e);
										Logging.debug(getClass(), "Could not establish forwarding link.");
										c.release();
										connector.release();
									}
								} catch (IOException e) {
									//Logging.error(getClass(), "Could not create forward connector.", e);
									Logging.debug(getClass(), "Could not create forward connector.");
									connector.release();
								}
							} else {
								Logging.debug(getClass(), "Could not create session.");
								connector.release();
							}
						} else {
							Logging.debug(getClass(), "No route to target.");
							connector.release();
						}
					}
				} catch (IOException e) {
					//Logging.error(getClass(), "Could deserialize incoming route request.", e);
					Logging.debug(getClass(), "Could deserialize incoming route request.");
					connector.release();
				}
			}
		});
	}

	/**
	 * Called whenever a device shall be registered at the device registry.
	 * 
	 * @param device
	 *            The device that shall be registered.
	 */
	protected void registerDevice(DeviceDescription device) {
		super.registerDevice(device);
		synchronized (routerMonitor) {
			SystemID system = device.getSystemID();
			int index = devices.indexOf(system);
			if (index == -1) {
				devices.addElement(Static.TRUE);
				devices.addElement(system);
				routerMonitor.notify();
			} else {
				devices.removeElementAt(index - 1);
				devices.insertElementAt(Static.TRUE, index - 1);
			}
		}
	}

	private Vector devices = new Vector();

	/**
	 * Called whenever the multiplexer is closed.
	 * 
	 * @param multiplexer
	 *            The multiplexer that has been closed.
	 */
	public void closeMultiplexer(MultiplexFactory multiplexer) {
		routerFactory = null;
	}

	/**
	 * Called to start the plug-in.
	 */
	public synchronized void start() {
		if (!started) {
			DeviceRegistry registry = InvocationBroker.getInstance()
					.getDeviceRegistry();
			registry.addDeviceListener(DeviceRegistry.EVENT_DEVICE_REMOVED,
					deviceListener);
			super.start();
			routerMonitor = new NullMonitor();
			manager.performOperation(new IOperation() {

				/**
				 * A hashtable that contains a boolean to determine whether a
				 * device has been announced already.
				 */
				private Hashtable announced = new Hashtable();

				/**
				 * Called whenever the plugin is started. This method runs as
				 * long as the plug-in is running. it will periodically try to
				 * connect to the registry.
				 */
				public void perform(IMonitor monitor) throws Exception {
					while (!monitor.isCanceled()) {
						indirect.clear();
						String address = (routerAddress[0] & 0xff) + "."
								+ (routerAddress[1] & 0xff) + "."
								+ (routerAddress[2] & 0xff) + "."
								+ (routerAddress[3] & 0xff);
						try {
							Logging.debug(getClass(),
									"Opening connection to gateway server.");
							Socket s = new Socket(address, routerPort);
							getPluginDescription().setProperty(
									PROPERTY_GATEWAY, new Boolean(true), true);
							Logging.debug(getClass(),
									"Connection to gateway server established.");
							MultiplexFactory f = new MultiplexFactory(
									ProactiveRoutingGateway.this, s
											.getInputStream(), s
											.getOutputStream());
							try {
								Logging.debug(getClass(), "Opening command connection.");
								IStreamConnector c = f.openConnector();
								ObjectOutputStream out = new ObjectOutputStream(
										c.getOutputStream());
								InputStream in = c.getInputStream();
								out.writeObject(SystemID.SYSTEM);
								// announce all transceiver plugins to
								// server
								// for direct connections
								writePluginsToStream(out);

								// announce connection to this system
								out.writeObject(Static.TRUE);
								out.writeObject(SystemID.SYSTEM);

								out.flush();
								routerFactory = f;
								synchronized (monitor) {
									while (!monitor.isCanceled()) {

										for (int i = devices.size() - 1; i >= 0; i -= 2) {
											Boolean b = (Boolean) devices
													.elementAt(i - 1);
											SystemID sid = (SystemID) devices
													.elementAt(i);
											if (b.booleanValue()) {
												if (!announced.containsKey(sid)) {
													announced.put(sid,
															Static.TRUE);
													out.writeObject(Static.TRUE);
													out.writeObject(sid);
												}
											} else {
												if (announced.containsKey(sid)) {
													announced.remove(sid);
													out.writeObject(Static.FALSE);
													out.writeObject(sid);
												}
												devices.removeElementAt(i);
												devices.removeElementAt(i - 1);
											}
										}
										out.flush();
										// periodically test whether the stream
										// is available
										monitor.wait(CONNECT_RETRY);
										in.available();
									}
								}
							} catch (IOException e) {
							}
							announced.clear();
							routerFactory = null;
							f.close();
							getPluginDescription().setProperty(
									PROPERTY_GATEWAY, new Boolean(false), true);
						} catch (IOException e) {
							getPluginDescription().setProperty(
									PROPERTY_GATEWAY, new Boolean(false), true);
							try {
								synchronized (monitor) {
									if (!monitor.isCanceled()) {
										monitor.wait(CONNECT_RETRY);
									}
								}
							} catch (InterruptedException ie) {
							}
						}
					}

				}
			}, routerMonitor);
		}
	}

	/**
	 * Called to stop the plug-in.
	 */
	public synchronized void stop() {
		if (started) {
			super.stop();
			DeviceRegistry registry = InvocationBroker.getInstance()
					.getDeviceRegistry();
			registry.addDeviceListener(DeviceRegistry.EVENT_DEVICE_REMOVED,
					deviceListener);
			devices.removeAllElements();
			try {
				routerMonitor.cancel();
				synchronized (routerMonitor) {
					while (!routerMonitor.isDone())
						routerMonitor.wait();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Returns the plug-in manager.
	 * 
	 * @return The plug-in manager.
	 */
	public IPluginManager getPluginManager() {
		return manager;
	}
}
