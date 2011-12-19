package info.pppc.basex.plugin.discovery;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.GroupConnector;
import info.pppc.base.system.plugin.IDiscovery;
import info.pppc.base.system.plugin.IDiscoveryManager;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A discovery plug-in that performs proactive announcement of plug-in descriptions. Thereby
 * it assumes a periodic reception of plug-in descriptions from other plug-ins. If a description
 * is not received, it sends a negative acknowledgement which will then be confirmed by the
 * corresponding device. In order to keep the message overhead low, other devices overhear the
 * negative acknowledgements and suppress their own if they would send one too.
 * It listens to state changes within the plug-in manager (additions and removals of plug-ins)
 * and it detects enabled/disabled transceivers and starts or stops discovery operations
 * depending on the state.
 * 
 * @author Marcus Handte
 */
public class TimedDiscovery implements IDiscovery, IListener, IOperation {
		
	/**
	 * This class is used to memorize when the next message
	 * should be received from a known remote device.
	 * 
	 * @author Mac
	 */
	private class Annoucement {
		/**
		 * This is the system that has sent an annoucement.
		 */
		private SystemID system;
		/**
		 * This is the ability over which the annoucement has been received.
		 */
		private Short ability;
		/**
		 * This is the time at which the next annoucement should be received.
		 */
		private long time;
		/**
		 * The number of messages that have been missed.
		 */
		private int missed;
	}
	
	/**
	 * The ability of the plug-in. [0][1].
	 */
	private static final short PLUGIN_ABILITY = 0x0001;
	
	/**
	 * The group id used by discovery plug-ins.
	 */
	private static final short DISCOVERY_GROUP = 3;
		
	/**
	 * The minimum period between two packets.
	 */
	private static final int DISCOVERY_SLACK = 1000;
	
	/**
	 * The normal amount of time that lies between two announcements.
	 */
	private static final int DISCOVERY_PERIOD = 4000;
	
	/**
	 * The duration of the burst mode in number of transmissions.
	 */
	private static final int DISCOVERY_BURST = 3;
	
	/**
	 * The amount of time that a announcement stays valid.
	 */
	private static final int REMOVAL_PERIOD;
	
	/**
	 * Set the removal period depending on the underlying operating
	 * system. Normally, 18 seconds should be fine but on Android,
	 * there are frequent wifi scans which lead to packet loss and
	 * thus, we will just increase the period there.
	 */
	static {
		int period = 18000;
		String vendor = System.getProperty("java.vm.vendor");
		if (vendor != null) {
			vendor = vendor.toLowerCase();
			if (vendor.indexOf("android") != -1) {
				period = 30000;
			}
		}
		REMOVAL_PERIOD = period;
	}
	
	/**
	 * The plug-in description of the ip plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, EXTENSION_DISCOVERY);

	/**
	 * The plug-in manager used to retrieve plug-ins.
	 */
	private IDiscoveryManager manager;

	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * A hash table of connectors hashed by ability.
	 */
	private Hashtable connectors = new Hashtable();

	/**
	 * This is a list of negative acknowledgements that have been
	 * received from certain remote devices. The list contains
	 * the ability of the plug-in that received the nack.
	 */
	private Vector negatives = new Vector();
	
	/**
	 * This is a list of devices and connectors that describe when
	 * to transmit a certain nack over a certain plug-in.
	 */
	private Vector announcements = new Vector();
	
	/**
	 * The monitor of the announcement operation.
	 */
	private IMonitor monitor;
	
	/**
	 * The number of remaining packets to be transmitted in
	 * burst mode.
	 */
	private int burst = 0;
	
	/**
	 * Creates a new simple discovery plug-in.
	 */
	public TimedDiscovery() {
		super();
	}

	/**
	 * Called to start the plug-in. This method initializes the plug-in and
	 * starts a thread that announces plug-ins.
	 */
	public synchronized void start() {
		Logging.debug(getClass(), "Starting proactive discovery with " + DISCOVERY_PERIOD 
				+ "/" + DISCOVERY_SLACK + "/" + REMOVAL_PERIOD + ".");
		if (! started) {
			started = true;
			// register this plug-in manager for plug-in events
			manager.addPluginListener(IDiscoveryManager.EVENT_PLUGIN_ADDED  
					| IDiscoveryManager.EVENT_PLUGIN_REMOVED, this);
			// get all descriptions and start discovery for each transceiver
			PluginDescription[] pds = manager.getPluginDescriptions(SystemID.SYSTEM);
			for (int i = 0; i < pds.length; i++) {
				PluginDescription pd = pds[i];
				if ((pd.getExtension() == EXTENSION_TRANSCEIVER)) {
					IPacketConnector connector = manager.openGroup(DISCOVERY_GROUP, pd.getAbility());
					connectors.put(new Short(pd.getAbility()), connector);
					connector.addPacketListener(IPacketConnector.EVENT_PACKET_CLOSED 
							| IPacketConnector.EVENT_PACKET_RECEIVED, this);
				}				
			}
			monitor = new NullMonitor();
			manager.performOperation(this, monitor);
		}
	}

	/**
	 * Called to stop the plug-in. After this method has been called, all
	 * open calls will fail.
	 */
	public synchronized void stop() {
		if (started) {
			started = false;
			// unregister this plug-in manager for plug-in events
			manager.removePluginListener(IDiscoveryManager.EVENT_PLUGIN_ADDED 
					| IDiscoveryManager.EVENT_PLUGIN_REMOVED, this);
			// cancel all running operations
			monitor.cancel();
		}
	}
	
	/**
	 * Performs discovery and waits for the monitor to be canceled.
	 * 
	 * @param monitor The monitor used to determine whether the 
	 * 	operation should be still performed.
	 */
	public void perform(IMonitor monitor) {
		Annoucement a = new Annoucement();
		a.system = SystemID.SYSTEM;
		a.ability = new Short((short)0);
		Vector keys = new Vector();
		while (! monitor.isCanceled()) {
			// get all transceiver plug-in abilities
			synchronized (this) {
				Enumeration e = connectors.keys();
				while (e.hasMoreElements()) {
					keys.addElement(e.nextElement());
				}
			}
			// send an announcement using all transceivers
			while (! keys.isEmpty()) {
				Short ability = (Short)keys.elementAt(0);
				keys.removeElementAt(0);
				announce(ability);
			}
			// compute the next announcement time and insert a marker
			synchronized (this) {
				if (burst > 0) {
					burst -= 1;
					a.time = System.currentTimeMillis() + DISCOVERY_SLACK;
					Logging.debug(getClass(), "Continuing burst mode for " + burst + " transmissions.");
				} else {
					a.time = System.currentTimeMillis() + DISCOVERY_PERIOD;
				}
				insert(a);
			}
			try {
				wait: while (true) {
					// wait until we need to reply to a nack or the next announcement must be made
					Annoucement next = (Annoucement)announcements.elementAt(0);
					long sleep = System.currentTimeMillis() - next.time;
					while (sleep > 0 && negatives.isEmpty()) {
						synchronized (monitor) {					
							monitor.wait(sleep);
						}
						next = (Annoucement)announcements.elementAt(0);
						sleep = System.currentTimeMillis() - next.time;
					}
					synchronized (this) {
						// respond to nacks
						while (! negatives.isEmpty()) {
							Short ability = (Short)negatives.elementAt(0);
							negatives.removeElementAt(0);
							Logging.debug(getClass(), "Handling negative acknowledge " + ability);
							if (ability != null) announce(ability);
						}
						// send nacks, if necessary or retransmit bc
						next = (Annoucement)announcements.elementAt(0);
						while (next.time <= System.currentTimeMillis()) {
							announcements.removeElement(next);
							if (next.system.equals(SystemID.SYSTEM)) {
								// do a complete announcement
								break wait;
							} else {
								// send a nack and insert packet
								acknowledge(next.ability, next.system);
								next.time = System.currentTimeMillis() + DISCOVERY_SLACK;
								next.missed += 1;
								if (next.missed * DISCOVERY_SLACK < REMOVAL_PERIOD) {
									insert(next);
								}
							}
							next = (Annoucement)announcements.elementAt(0);
						}
					}
				}
			} catch (InterruptedException ex) {
				Logging.debug(getClass(), "Thread got interrupted.");
			}
		}
		Logging.debug(getClass(), "Stopping discovery.");
	}
	
	/**
	 * Annouces the availability of the device using the given plug-in.
	 * 
	 * @param ability The ability of the plug-in.
	 */
	private void announce(Short ability) {
		Logging.log(getClass(), "Sending annouce " + ability);
		IPacketConnector connector = null;
		synchronized (this) {
			connector = (IPacketConnector)connectors.get(ability);
			if (connector == null) return;				
		}
		try {

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			// this is an announcement and not a nack
			oos.writeBoolean(true);
			// write the device description
			oos.writeObject(manager.getDeviceDescription(SystemID.SYSTEM));
			// plug-in descriptions
			Vector announcement = new Vector();
			PluginDescription[] plugins = manager.getPluginDescriptions(SystemID.SYSTEM);
			for (int i = 0; i < plugins.length; i++) {
				PluginDescription pd = plugins[i];
				// only announce non-transceivers and same transceiver					
				if ((pd.getExtension() != EXTENSION_TRANSCEIVER) && 
						pd.getExtension() != EXTENSION_ROUTING &&
							pd.getExtension() != EXTENSION_DISCOVERY || 
							pd.getAbility() == ability.shortValue()) {
					announcement.addElement(pd);
				}
			}
			// write the plug-in descriptions
			oos.writeInt(announcement.size());
			for (int i = 0; i < announcement.size(); i++) {
				oos.writeObject((PluginDescription)announcement.elementAt(i));
			}
			// write the devices that have been received via this transceiver to trigger bursts
			Vector systems = new Vector();
			synchronized (this) {
				for (int i = 0; i < announcements.size(); i++) {
					Annoucement a = (Annoucement)announcements.elementAt(i);
					if (a.ability.equals(ability) && ! SystemID.SYSTEM.equals(a.system)) {
						systems.addElement(a.system);
					}
				}
			}
			oos.writeInt(systems.size());
			for (int i = 0; i < systems.size(); i++) {
				oos.writeObject((SystemID)systems.elementAt(i));
			}
			// create a packet and send it
			oos.close();
			byte[] buffer = bos.toByteArray();
			if (connector.getPacketLength() < buffer.length) {
				Logging.debug(getClass(), "Descriptions exceed maximum packet length.");
			} else {
				IPacket packet = connector.createPacket();
				packet.setPayload(buffer);
				connector.sendPacket(packet);
			}
		} catch (IOException ex) {
			Logging.error(getClass(), "Caught exception while sending.", ex);
		}				
	}
	
	/**
	 * Sends a negative acknowledgment using the specified plug-in to
	 * the specified system.
	 * 
	 * @param ability The ability of the plug-in to use.
	 * @param system The system to target.
	 */
	private void acknowledge(Short ability, SystemID system) {
		Logging.log(getClass(), "Sending negative acknowledge " + ability + " to " + system);
		IPacketConnector connector = null;
		synchronized (this) {
			connector = (IPacketConnector)connectors.get(ability);
			if (connector == null) return;				
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeBoolean(false);
			oos.writeObject(system);
			oos.close();
			byte[] buffer = bos.toByteArray();
			if (connector.getPacketLength() < buffer.length) {
				Logging.debug(getClass(), "Acknowledge exceeds maximum packet length.");
			} else {
				IPacket packet = connector.createPacket();
				packet.setPayload(buffer);
				connector.sendPacket(packet);
			}
		} catch (IOException ex) {
			Logging.error(getClass(), "Caught exception while sending.", ex);
		}				
	}
	
	/**
	 * Inserts an announcement into the announcement
	 * queue and returns true if it is the first. Caller
	 * must hold a lock to this.
	 * 
	 * @param annoucement The announcement to insert.
	 * @return True if this announcement has been inserted
	 * 	as the first element.
	 */
	private boolean insert(Annoucement annoucement) {
		for (int i =  announcements.size() - 1; i >= 0; i--) {
			Annoucement b = (Annoucement)announcements.elementAt(i);
			if (b.time < annoucement.time) {
				announcements.insertElementAt(annoucement, i + 1);
				return false;
			}
		}
		announcements.insertElementAt(annoucement, 0);
		return true;
	}
	

	/**
	 * Sets the plug-in manager that is used to monitor the installation of new
	 * plug-ins.
	 * 
	 * @param manager The plug-in manager.
	 */
	public void setDiscoveryManager(IDiscoveryManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the plug-in description of this plug-in. There will be only one instance
	 * of the plug-in description per instance of this plug-in.
	 * 
	 * @return The plug-in description of this plug-in.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}

	/**
	 * Implementation of the plug-in listener that listens to the discovery listener
	 * to detect the addition of new transceiver plug-ins and an implementation of
	 * a transceiver listener that listens to transceivers that are enabled and
	 * disabled. 
	 * If a transceiver is added to the plug-in manager, a transceiver listener will
	 * be added. If a plug-in is removed, the listener will be removed. Whenever a
	 * transceiver is enabled, a discovery operation is started and whenever it is
	 * disabled, the corresponding discovery operation is aborted.
	 * 
	 * @param event The event that has been thrown by the plug-in manager. At the
	 * 	present time, the implementation registers for addition and removal events
	 * 	of plug-ins.
	 */
	public void handleEvent(Event event) {
		// only consider events while started, the deliver of an event in cases
		// where the plug-in has stopped might be a side-effect of synchronizing
		// the method with the start and stop methods
		if (started) {
			if (event.getSource() == manager) {
				switch (event.getType()) {
					case IDiscoveryManager.EVENT_PLUGIN_ADDED: {
						// add unregistered transceivers to the watch list
						PluginDescription add = (PluginDescription)event.getData();
						Short key = new Short(add.getAbility());
						if (add.getExtension() == EXTENSION_TRANSCEIVER && ! connectors.containsKey(key)) {
							IPacketConnector connector = manager.openGroup(DISCOVERY_GROUP, add.getAbility());
							connector.addPacketListener(IPacketConnector.EVENT_PACKET_CLOSED 
									| IPacketConnector.EVENT_PACKET_RECEIVED, this);
							connectors.put(key, connector);
						}
						break;
					}
					case IDiscoveryManager.EVENT_PLUGIN_REMOVED: {
						PluginDescription remove = (PluginDescription)event.getData();
						Short key = new Short(remove.getAbility());
						if (remove.getExtension() == EXTENSION_TRANSCEIVER && connectors.containsKey(key)) {
							IPacketConnector connector = (IPacketConnector)connectors.remove(key);
							connector.removePacketListener(IPacketConnector.EVENT_PACKET_CLOSED 
									| IPacketConnector.EVENT_PACKET_RECEIVED, this);
							connector.release();
						}
						break;
					}
					default:
						// will never happen
				}							
			} 
			// handle events coming from one of the packet connectors.
			else if (event.getSource() instanceof IPacketConnector) {
				Short ability = null;
				if (event.getSource() instanceof GroupConnector) {
					ability = ((GroupConnector)event.getSource()).getAbility();
				} if (ability == null) {
					try {
						ability = new Short(((IPacketConnector)event.getSource())
								.getPlugin().getPluginDescription().getAbility());
					} catch (NullPointerException e) {
						return;
					}
				}
				switch (event.getType()) {
					case IPacketConnector.EVENT_PACKET_CLOSED: {
						IPacketConnector connector = null;
						synchronized (this) {
							connector = (IPacketConnector)connectors.remove(ability);
							
						}
						if (connector != null) {
							connector.removePacketListener(IPacketConnector.EVENT_PACKET_CLOSED 
									| IPacketConnector.EVENT_PACKET_RECEIVED, this);
							connector.release();
						}
						break;
					}
					case IPacketConnector.EVENT_PACKET_RECEIVED: {
						try {
							// process received packet
							IPacket packet = (IPacket)event.getData();
							byte[] buffer = packet.getPayload();
							ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
							ObjectInputStream ois = new ObjectInputStream(bis);
							if (ois.readBoolean()) {
								// simple remote announcement
								DeviceDescription device = (DeviceDescription)ois.readObject();
								if (device != null && ! SystemID.SYSTEM.equals(device.getSystemID())) { 
									SystemID id = device.getSystemID();
									Logging.debug(getClass(), "Received announce from " + id);
									int plugins = ois.readInt();
									for (int i = 0; i < plugins; i++) {
										PluginDescription plugin = (PluginDescription)ois.readObject();
										manager.registerPlugin(id, plugin, REMOVAL_PERIOD);
									}
									manager.registerDevice(device, REMOVAL_PERIOD);
									boolean startBurst = true;
									int systems = ois.readInt();
									for (int i = 0; i < systems; i++) {
										Object system = ois.readObject();
										if (SystemID.SYSTEM.equals(system)) { 
											startBurst = false;
											break;
										}
									}
									synchronized (this) {
										long time = System.currentTimeMillis() + DISCOVERY_PERIOD + DISCOVERY_SLACK;
										boolean add = true;
										for (int i = 0; i < announcements.size(); i++) {
											Annoucement a = (Annoucement)announcements.elementAt(i);
											if (a.ability.equals(ability) && a.system.equals(id)) {
												announcements.removeElementAt(i);
												a.time = time;
												a.missed = 0;
												announcements.addElement(a);
												add = false;
												break;
											}
										}
										if (add) {
											Annoucement a = new Annoucement();
											a.time = time;
											a.system = id;
											a.ability = ability;
											a.missed = 0;
											announcements.addElement(a);
										}
										if (startBurst) {
											Logging.log(getClass(), "Starting burst mode due to system " + device.getSystemID() + ".");
											burst = DISCOVERY_BURST;
											for (int i = 0; i < announcements.size(); i++) {
												Annoucement a = (Annoucement)announcements.elementAt(i);
												if (SystemID.SYSTEM.equals(a.system)) {
													announcements.removeElementAt(i);
													a.time = System.currentTimeMillis();
													announcements.insertElementAt(a, 0);
													break;
												}
											}
										}
									}
									if (startBurst) {
										synchronized (monitor) {
											monitor.notifyAll();
										}
									}
								}
							} else {
								// negative acknowledgement
								SystemID system = (SystemID)ois.readObject();
								if (SystemID.SYSTEM.equals(system) && ability != null) {
									Logging.debug(getClass(), "Received negative acknowledgement over " + ability);
									// received for me, find connector
									synchronized (this) {
										if (!negatives.contains(ability)) {
											negatives.add(ability);
										}
									}
									if (! negatives.isEmpty()) {
										synchronized (monitor) {
											monitor.notify();
										}
									}
								} else {
									// received for someone else, suppress my nack
									Logging.debug(getClass(), "Overhearing negative acknowledgement for " + system + " over " + ability);
									synchronized (this) {
										for (int i = 0; i < announcements.size(); i++) {
											Annoucement a = (Annoucement)announcements.elementAt(i);
											if (ability.equals(a.ability) && a.system.equals(system)) {
												if (a.time - System.currentTimeMillis() < DISCOVERY_SLACK) {
													Logging.debug(getClass(), "Suppressing negative acknowledgement for " + system + " over " + ability);
													announcements.removeElementAt(i);
													a.time += DISCOVERY_SLACK;
													a.missed += 1;
													insert(a);
												}
												break;
											}
										}
									}
								}
							}
						} catch (Throwable t) {
							Logging.error(getClass(), "Received malformed packet.", t);
						}
						break;
					}
					default:
						// will never happen
				}	
			}
		}
	}

}
