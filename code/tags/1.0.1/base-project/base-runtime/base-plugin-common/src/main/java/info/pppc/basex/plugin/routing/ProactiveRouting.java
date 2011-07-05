package info.pppc.basex.plugin.routing;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.IExtension;
import info.pppc.base.system.ISession;
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
import info.pppc.base.system.plugin.GroupConnector;
import info.pppc.base.system.plugin.IDiscoveryManager;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IRouting;
import info.pppc.base.system.plugin.IRoutingManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The proactive implements simple multi-hop routing with proactive route distribution.
 * 
 * @author Marcus Handte
 */
public class ProactiveRouting implements IRouting, IListener, IOperation  {

	/**
	 * A simple tree data structure for computing the
	 * shortest route using breadth first search. 
	 * 
	 * @author Marcus Handte
	 */
	private class Node {
		
		/**
		 * The device represented by this node. 
		 */
		public SystemID system;
		
		/**
		 * The parent node or null if it is the root.
		 */
		public Node parent;
		
	}
	
	/**
	 * The queue entry stores packets 
	 * in the send queue that are supposed
	 * to be forwarded. 
	 * 
	 * @author Marcus Handte
	 */
	private class Entry {
		/**
		 * The source system to remove duplicate
		 * packets from the same source.
		 */
		private SystemID system;
		/**
		 * The source plug-in ability to avoid
		 * retransmissions over the same plug-in.
		 */
		private Short plugin;
		/**
		 * The actual packet that shall be forwarded.
		 */
		private byte[] data;
	}
	
	/**
	 * The maximum hop count for route announcements.
	 */
	public static final byte MAXIMUM_HOPS = 5 & 0xff;
	
	/**
	 * The group id used by the routing plug-in for route announcements.
	 */
	private static final short DISCOVERY_GROUP = 1;
		
	/**
	 * The amount of time that lies between two announcements.
	 */
	private static final int DISCOVERY_PERIOD = 8000;
	
	/**
	 * The amount of time that an announcement stays valid.
	 */
	private static final int REMOVAL_PERIOD = 30000;
	
	/**
	 * The ability of the plug-in [6][0].
	 */
	public static short PLUGIN_ABILITY = 0x0600;
	
	/**
	 * The property in the plug-in description that contains a 
	 * neighbor list.
	 */
	public static String PROPERTY_NEIGHBORS = "NB";
	
	/**
	 * The property in the plug-in description that contains a
	 * system id.
	 */
	public static String PROPERTY_SYSTEM = "ID";
	
	/**
	 * The property in the plug-in description that determines
	 * whether the plug-in has gateway capabilities.
	 */
	public static String PROPERTY_GATEWAY = "GW";
	
	/**
	 * The reference to the semantic manager.
	 */
	protected IRoutingManager manager;

	/**
	 * The packet connectors for route announcements.
	 */
	private Vector announcers = new Vector();
	
	/**
	 * A flag that indicates whether the plug-in is started.
	 */
	protected boolean started = false;
	
	/**
	 * The monitor for the discovery operation.
	 */
	private NullMonitor monitor;
	
	/**
	 * The next packet identifier that will be used (0..255)
	 */
	private int identifier = 0;
	
	/**
	 * A flag that indicates whether the routing plug-in should
	 * retransmit the incoming packets over the same transceiver.
	 * This is only needed in cases where there is no technology
	 * wide routing.
	 */
	private boolean retransmit = false;
	
	/**
	 * The last packet identifiers of different systems. This hashtable
	 * hashes system ids to object arrays of length 2. The first entry
	 * is a long that represents the timeout time, the second entry is
	 * a short that represents the actual identifier that has been
	 * received last. 
	 */
	private Hashtable identifiers = new Hashtable();
	
	/**
	 * The send queue that contains queue entries.
	 */
	private Vector queue = new Vector();
	
	/**
	 * The routing filter used during gateway selection.
	 */
	private IRoutingFilter filter = null;
	
	/**
	 * The plug-in description of the routing plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, (short)(EXTENSION_ROUTING));
	
	/**
	 * Creates a new proactive routing plug-in that does not
	 * retransmit route announcements via the same technology.
	 */
	public ProactiveRouting() {
		this(false);
	}
	
	/**
	 * Creates a new proactive routing plug-in. Depending on
	 * the retransmission flag, this plug-in will either re-
	 * announce incoming packets via the same technology or 
	 * not.
	 * 
	 * @param retransmit A flag that indicates whether route
	 * 	announcements will be retransmitted via the same 
	 * 	technology. Normally, this is only needed for the
	 * 	network emulator.
	 */
	public ProactiveRouting(boolean retransmit) {
		this.retransmit = retransmit;
		description.setProperty(PROPERTY_SYSTEM, SystemID.SYSTEM, false);
		description.setProperty(PROPERTY_NEIGHBORS, new Vector(), true);
		description.setProperty(PROPERTY_GATEWAY, new Boolean(false), true);
	}
	
	
	/**
	 * Sets the routing manager upon startup.
	 * 
	 * @param manager The routing manager upon startup.
	 */
	public void setRoutingManager(IRoutingManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Returns the plug-in description. The plug-in description 
	 * 
	 * @return The plug-in description.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}

	/**
	 * Called to start the plug-in. After startup, the plug-in will start
	 * with the route announcement.
	 */
	public synchronized void start() {
		if (!started) {
			started = true;
			// start the connectors on all plug-ins
			PluginDescription[] descriptions = manager.getPluginDescriptions(SystemID.SYSTEM);
			for (int i = 0; i < descriptions.length; i++) {
				if (descriptions[i].getExtension() == IExtension.EXTENSION_TRANSCEIVER) {
					IPacketConnector connector = manager.openGroup
						(DISCOVERY_GROUP, descriptions[i].getAbility());
					connector.addPacketListener(IPacketConnector.EVENT_PACKET_RECEIVED, this);
					announcers.addElement(connector);
				}
			}
			manager.addPluginListener(IDiscoveryManager.EVENT_PLUGIN_ADDED |
					IDiscoveryManager.EVENT_PLUGIN_REMOVED, this);
			monitor = new NullMonitor();
			manager.performOperation(this, monitor);
		} 
	}

	/**
	 * Called to stop the plug-in. This will also stop the route announcement.
	 */
	public synchronized void stop() {
		if (started) {
			started = false;
			manager.removePluginListener(IDiscoveryManager.EVENT_PLUGIN_ADDED | 
					IDiscoveryManager.EVENT_PLUGIN_REMOVED, this);
			synchronized (monitor) {
				monitor.cancel();
				monitor.notify();
				try {
					monitor.join();	
				} catch (InterruptedException e) { }
			}
			synchronized (announcers) {
				while (! announcers.isEmpty()) {
					IPacketConnector connector = (IPacketConnector)announcers.elementAt(0);
					announcers.removeElementAt(0);
					connector.removePacketListener(IPacketConnector.EVENT_PACKET_RECEIVED, this);
					connector.release();
				}				
			}
			queue.removeAllElements();
			identifiers.clear();
		}
	}

	/**
	 * Called whenever a connection request is incoming.
	 * 
	 * @param connector The stream connector of the request.
	 * @param session The session data of the request.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session) {
		try {
			ObjectInputStream input = new ObjectInputStream(connector.getInputStream());
			NFCollection reqs = (NFCollection)input.readObject();
			int hops = input.readInt();
			if (hops == 0) {
				manager.acceptSession(connector);
			} else {
				SystemID next = (SystemID)input.readObject();
				ISession s = manager.prepareSession(manager.createSession(next, PLUGIN_ABILITY), reqs);
				if (s != null) {
					try {
						IStreamConnector c = manager.openSession(s);
						try {
							ObjectOutputStream output = new ObjectOutputStream(c.getOutputStream());
							output.writeObject(reqs);
							output.writeInt(hops - 1);
							output.flush();
							NullMonitor monitor1 = new NullMonitor();
							NullMonitor monitor2 = new NullMonitor();
							manager.performOperation(new RoutingOperation
									(c, connector, monitor2), monitor1);
							manager.performOperation(new RoutingOperation
									(connector, c, monitor1), monitor2);	
							Logging.debug(getClass(), "Forward link established.");
						} catch (IOException e) {
							Logging.error(getClass(), "Could not establish forwarding link.", e);
							c.release();
							connector.release();
						}
					} catch (IOException e) {
						Logging.error(getClass(), "Could not create forward connector.", e);
						connector.release();
					}					
				} else {
					Logging.debug(getClass(), "Could not create session for forwarder.");
					connector.release();
				}
			}
		} catch (IOException e) {
			Logging.error(getClass(), "Could deserialize incoming route request.", e);
			connector.release();
		}
	}

	/**
	 * Called to open a session with some remote system.
	 * 
	 * @param session The session data object.
	 * @return A stream connector to the remote system.
	 * @throws IOException Thrown if an error occurs.
	 */
	public IStreamConnector openSession(ISession session) throws IOException {
		Object[] local = (Object[])session.getLocal();
		Vector route = (Vector)local[0];
		NFCollection source = (NFCollection)local[1];
		NFCollection target = new NFCollection();
		NFDimension[] dimensions = source.getDimensions(EXTENSION_TRANSCEIVER);
		for (int i = dimensions.length - 1; i >= 0; i--) {
			target.addDimension(EXTENSION_TRANSCEIVER, dimensions[i]);
		}
		ISession prepared = manager.prepareSession
			(manager.createSession((SystemID)route.elementAt(0), PLUGIN_ABILITY), target);
		if (prepared == null) {
			throw new IOException("Cannot connect to next hop.");
		} else {
			IStreamConnector c = manager.openSession(prepared);
			try {
				ObjectOutputStream out = new ObjectOutputStream(c.getOutputStream());
				out.writeObject(target);
				out.writeInt(route.size() - 1);
				for (int i = 1; i < route.size(); i++) {
					out.writeObject(route.elementAt(i));
				}
				out.flush();
				return c;
			} catch (IOException e) {
				c.release();
				throw e;
			}
		}
	}

	/**
	 * Called to prepare a session to the specified remote device 
	 * 
	 * @param d The plugin descripton of the remote device.
	 * @param c The nf collection with the requirements.
	 * @param s The session data used to fill in requests.
	 * @return True if it is possible to contact the device.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		Vector route = getRoute((SystemID)d.getProperty(PROPERTY_SYSTEM));
		// if the route is null and gateway usage is activated, compute the route to the nearest gateway
		if (route == null) {
			NFDimension dimension = c.getDimension(EXTENSION_ROUTING, NFDimension.IDENTIFIER_GATEWAY);
			if (dimension != null && dimension.getHardValue() instanceof Boolean && 
					((Boolean)dimension.getHardValue()).booleanValue()) {
				Vector gateways = getGateways();	
				// find the nearest gateway
				for (int i = 0; i < gateways.size(); i++) {
					SystemID gw = (SystemID)gateways.elementAt(i);
					if (gw.equals(SystemID.SYSTEM)) {
						route = new Vector();
					}  else {
						Vector next = getRoute(gw);
						if (route == null || (next != null && next.size() < route.size())) {
							route = next;
						}						
					}
				}
				if (route != null) {
					// add the actual target extra
					route.addElement((SystemID)d.getProperty(PROPERTY_SYSTEM));
					s.setLocal(new Object[] { route, c });
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else if (route.size() > 1) {
			s.setLocal(new Object[] { route, c });
			return true;
		} else {
			return false;	
		}
	}
	
	/**
	 * Returns the gateways that are currently available.
	 * 
	 * @return The system identifiers of available gateways.
	 */
	protected Vector getGateways() {
		Vector result = new Vector();
		SystemID[] systems = manager.getDevices();
		for (int i = 0; i < systems.length; i++) {
			PluginDescription[] descs = manager.getPluginDescriptions(systems[i]);
			for (int j = 0; j < descs.length; j++) {
				if (descs[j].getAbility() == PLUGIN_ABILITY) {
					Boolean gw = (Boolean)descs[j].getProperty(PROPERTY_GATEWAY);
					if (gw != null && gw.booleanValue()) {
						result.addElement(systems[i]);
					}
				}
			}
		}
		IRoutingFilter f = filter;
		if (f != null) {
			f.getGateways(result);
		}
		return result;
	}
	
	/**
	 * Sets the gateway filter to the specified filter. If the
	 * filter is set to null, no filtering is performed at all.
	 * 
	 * @param filter The gateway filter.
	 */
	public void setFilter(IRoutingFilter filter) {
		this.filter = filter;
	}
	
	/**
	 * Returns the shortest route using breadth first search. This method
	 * will simply construct the connectivity graph as a spanning tree and
	 * return the shortest path that has been found first. 
	 * 
	 * @param target The target system to route to.
	 * @return The route or null if none exists.
	 */
	protected Vector getRoute(SystemID target) {
		Vector found = new Vector();
		Vector nodes = new Vector();
		found.addElement(SystemID.SYSTEM);
		Node root = new Node();
		root.system = SystemID.SYSTEM;
		nodes.addElement(root);
		while (! nodes.isEmpty()) {
			Node node = (Node)nodes.elementAt(0);
			nodes.removeElementAt(0);
			PluginDescription[] descs = manager.getPluginDescriptions(node.system);
			for (int i = descs.length - 1; i >= 0; i--) {
				if (descs[i].getAbility() == PLUGIN_ABILITY) {
					Vector neighbors = (Vector)descs[i].getProperty(PROPERTY_NEIGHBORS);
					for (int j = neighbors.size() - 1; j >= 0; j--) {
						SystemID neighbor = (SystemID)neighbors.elementAt(j);
						if (neighbor.equals(target)) {
							Vector route = new Vector();
							route.addElement(neighbor);
							while (node != root) {
								route.insertElementAt(node.system, 0);
								node = node.parent;
							}
							return route;
						} else if (! found.contains(neighbor)) {
							found.addElement(neighbor);
							Node child = new Node();
							child.system = neighbor;
							child.parent = node;
							nodes.addElement(child);
						}
					}
				}
			}
		}
		return null;
	}
	
	

	/**
	 * Handles incoming route announcements.
	 * 
	 * @param event Remote route announcements.
	 */
	public synchronized void handleEvent(Event event) {
		if (event.getSource() == manager) {
			switch (event.getType()) {
				case IDiscoveryManager.EVENT_PLUGIN_ADDED: {
					PluginDescription add = (PluginDescription)event.getData();
					if (add.getExtension() == EXTENSION_TRANSCEIVER) {
						GroupConnector connector = (GroupConnector)manager.openGroup(DISCOVERY_GROUP, add.getAbility());
						connector.addPacketListener(IPacketConnector.EVENT_PACKET_RECEIVED, this);
						announcers.addElement(connector);
					}
					break;
				}
				case IDiscoveryManager.EVENT_PLUGIN_REMOVED: {
					PluginDescription remove = (PluginDescription)event.getData();
					synchronized (announcers) {
						for (int i = announcers.size() - 1; i >= 0; i--) {
							GroupConnector connector = (GroupConnector)announcers.elementAt(i);
							if (connector.getAbility() != null && connector.getAbility().shortValue() == remove.getAbility()) {
								announcers.removeElementAt(i);
								connector.release();
								connector.removePacketListener
									(IPacketConnector.EVENT_PACKET_RECEIVED, this);
							}
						}
					}
					break;
				}
				default:
					// should not happen
			}
		} else {
			switch (event.getType()) {
				case IPacketConnector.EVENT_PACKET_RECEIVED: {
					try {
						// process received packet
						IPacket packet = (IPacket)event.getData();
						byte[] buffer = packet.getPayload();
						ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
						ObjectInputStream ois = new ObjectInputStream(bis);
						/// read packet
						int sequence = ois.readByte() & 0xff;
						byte hops = ois.readByte();
						DeviceDescription device = (DeviceDescription)ois.readObject();
						if (device != null) { 
							SystemID id = device.getSystemID();
							if (SystemID.SYSTEM.equals(id)) {
								//Logging.log(getClass(), "Reject packet from self.");
								return;
							}
							// check for duplicate packet
							synchronized (identifiers) {
								Object[] value = (Object[])identifiers.get(id);
								if (value != null) {
									int bound1 = ((Integer)value[1]).intValue();
									int bound2 = (bound1 + 128) % 256;
									if ((bound1 < bound2 && sequence > bound1 && sequence < bound2) ||
											((bound1 > bound2) && (sequence > bound1 || sequence >= 0 && sequence < bound2))) {
										value[0] = new Long(System.currentTimeMillis() + REMOVAL_PERIOD);
										value[1] = new Integer(sequence);
										//Logging.log(getClass(), "Accept consequtive packet ("+ id + ", " + sequence + ", " + bound1 + " )");
									} else {
										//Logging.log(getClass(), "Reject out-of-sequence packet (" + id + ", " + sequence + ", " + bound1 + ")");
										return;
									}
								} else {
									//Logging.log(getClass(), "Accept initial packet ("+ id + ", " + sequence + ")");
									value = new Object[] {
										new Long(System.currentTimeMillis() + REMOVAL_PERIOD),
										new Integer(sequence)
									};
									identifiers.put(id, value);
								}
							}
							//Logging.log(getClass(), "Register/update device " + id);
							int plugins = ois.readInt();
							for (int i = 0; i < plugins; i++) {
								PluginDescription plugin = (PluginDescription)ois.readObject();
								registerPlugin(id, plugin);
							}
							registerDevice(device);
							// now decide upon retransmission
							if (hops > 0) {
								// adjust hop count
								buffer[1] = (byte)(hops - 1);
								// retrieve receiver
								GroupConnector c = (GroupConnector)event.getSource();
								Entry e = new Entry();
								e.system = id;
								e.plugin = c.getAbility();
								e.data = buffer;
								synchronized (monitor) {
									for (int i = queue.size() - 1; i >= 0; i--) {
										Entry e2 = (Entry)queue.elementAt(i);
										if (e2.system == e.system) {
											queue.removeElementAt(i);
											queue.insertElementAt(e, i);
											monitor.notify();
											return;
										}
									}
									queue.addElement(e);
									monitor.notify();
								}
								
							}
						}
					} catch (Throwable t) {
						Logging.error(getClass(), "Received malformed packet.", t);							
					}
				}
				default:
					// will never happen
			}
		}
	}
	
	/**
	 * Registers the specified plugin description of the specified system
	 * at the device registry.
	 * 
	 * @param id The id of the system.
	 * @param plugin The plugin description.
	 */
	protected void registerPlugin(SystemID id, PluginDescription plugin) {
		manager.registerPlugin(id, plugin, REMOVAL_PERIOD);
	}
		
	/**
	 * Registers the specified device at the device registry.
	 * 
	 * @param device The device to register.
	 */
	protected void registerDevice(DeviceDescription device) {
		manager.registerDevice(device, REMOVAL_PERIOD);
	}

	/**
	 * Perform continuous route announcements until this thing is deactivated.
	 * 
	 * @param monitor The monitor to cancel.
	 */
	public void perform(IMonitor monitor) {
		while (!monitor.isCanceled()) {
			try {
				// update the plug-in description with devices that are connected directly
				Vector neighbors = new Vector();
				SystemID[] devices = manager.getDevices();
				i: for (int i = 0; i < devices.length; i++) {
					PluginDescription[] descriptions = manager.getPluginDescriptions(devices[i]);
					for (int j = 0; j < descriptions.length; j++) {
						if (descriptions[j].getExtension() == EXTENSION_TRANSCEIVER) {
							neighbors.addElement(devices[i]);
							continue i;
						}
					}
				}
				description.setProperty(PROPERTY_NEIGHBORS, neighbors, true);
				// increase the identifier for the next send operation
				identifier = (identifier + 1) % 256;
				// packet header
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeByte((byte)(identifier & 0xff));
				oos.writeByte(MAXIMUM_HOPS);
				// device description
				oos.writeObject(manager.getDeviceDescription(SystemID.SYSTEM));
				// plug-in descriptions
				Vector announcement = new Vector();
				PluginDescription[] plugins = manager.getPluginDescriptions(SystemID.SYSTEM);
				for (int i = 0; i < plugins.length; i++) {
					PluginDescription pd = plugins[i];
					// only announce non-transceivers					
					if ((pd.getExtension() != EXTENSION_TRANSCEIVER && 
							pd.getExtension() != EXTENSION_DISCOVERY)) {
						announcement.addElement(pd);
					}
				}
				oos.writeInt(announcement.size());
				for (int i = 0; i < announcement.size(); i++) {
					oos.writeObject((PluginDescription)announcement.elementAt(i));
				}
				oos.close();
				byte[] buffer = bos.toByteArray();
				synchronized (announcers) {
					for (int i = 0; i < announcers.size(); i++) {
						IPacketConnector announcer = (IPacketConnector)announcers.elementAt(i);
						try {
							if (announcer.getPacketLength() < buffer.length) {
								Logging.debug(getClass(), "Descriptions exceed maximum packet length.");
							} else {
								IPacket packet = announcer.createPacket();
								packet.setPayload(buffer);
								announcer.sendPacket(packet);
							}	
						} catch (IOException e) {
							Logging.debug(getClass(), "Could not send route announcement packet.");
						}
					}
				}
			} catch (IOException ex) {
				Logging.error(getClass(), "Caught exception while sending.", ex);
				return;
			}				
			// retransmit packets as long as we are waiting 
			long now = System.currentTimeMillis();
			long end = now + DISCOVERY_PERIOD;
			while (true) {
				Entry entry = null;
				synchronized (monitor) {
					while (end > now && ! monitor.isCanceled() && queue.isEmpty()) {		
						try {
							monitor.wait(end - now);
						} catch (InterruptedException e) { };
						now = System.currentTimeMillis();
					}
					if (end <= now || monitor.isCanceled()) {
						break;
					} else if (! queue.isEmpty()) {
						entry = (Entry)queue.elementAt(0);
						queue.removeElementAt(0);
					}
				}
				if (entry != null) {
					for (int i = announcers.size() - 1; i >= 0; i--) {
						GroupConnector c = (GroupConnector)announcers.elementAt(i);
						if (retransmit || c.getAbility() == null || c.getAbility() != entry.plugin) {
							try {
								IPacket p = c.createPacket();
								p.setPayload(entry.data);
								c.sendPacket(p);
							} catch (IOException e) {
								Logging.debug(getClass(), "Releasing connector due to exception.");
								announcers.removeElement(c);
							}							
						} 
					}
					entry = null;
				}
			}
			// clean up the identifiers from disappeared devices
			synchronized (identifiers) {
				Vector clean = new Vector();
				Enumeration e = identifiers.keys();
				while (e.hasMoreElements()) {
					Object key = e.nextElement();
					Object[] value = (Object[])identifiers.get(key);
					if (((Long)value[0]).longValue() < end) {
						clean.addElement(key);
					}
				}
				for (int i = clean.size() - 1; i >= 0; i--) {
					identifiers.remove(clean.elementAt(i));
				}
			}
		}			
	}
	
	
	
}
