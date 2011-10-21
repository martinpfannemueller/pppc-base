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
 * A discovery plug-in that performs a simple proactive announcement of plug-in descriptions.
 * It listens to state changes within the plug-in manager (additions and removals of plug-ins)
 * and it detects enabled/disabled transceivers and starts or stops discovery operations
 * depending on the state.
 * 
 * @author Marcus Handte
 */
public class ProactiveDiscovery implements IDiscovery, IListener, IOperation {
		
	/**
	 * The ability of the plug-in. [0][0].
	 */
	private static final short PLUGIN_ABILITY = 0x0000;
	
	/**
	 * The group id used by discovery plug-ins.
	 */
	private static final short DISCOVERY_GROUP = 0;
		
	/**
	 * The amount of time that lies between two announcements.
	 */
	private static final int DISCOVERY_PERIOD = 4000;
	
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
	 * The monitor of the announcement operation.
	 */
	private IMonitor monitor;

	/**
	 * Creates a new simple discovery plug-in.
	 */
	public ProactiveDiscovery() {
		super();
	}

	/**
	 * Called to start the plug-in. This method initializes the plug-in and
	 * starts a thread that announces plug-ins.
	 */
	public synchronized void start() {
		Logging.debug(getClass(), "Starting proactive discovery with " + DISCOVERY_PERIOD + "/" + REMOVAL_PERIOD + ".");
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
		Vector keys = new Vector();
		while (! monitor.isCanceled()) {
			synchronized (this) {
				Enumeration e = connectors.keys();
				while (e.hasMoreElements()) {
					keys.addElement(e.nextElement());
				}
			}
			while (! keys.isEmpty()) {
				Short ability = (Short)keys.elementAt(0);
				keys.removeElementAt(0);
				IPacketConnector connector = null;
				synchronized (this) {
					connector = (IPacketConnector)connectors.get(ability);
					if (connector == null) continue;				
				}
				try {
					// device description
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(bos);
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
					oos.writeInt(announcement.size());
					for (int i = 0; i < announcement.size(); i++) {
						oos.writeObject((PluginDescription)announcement.elementAt(i));
					}
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
			try {
				synchronized (monitor) {
					monitor.wait(DISCOVERY_PERIOD);	
				}
			} catch (InterruptedException ex) {
				Logging.debug(getClass(), "Thread got interrupted.");
			}
		}
		Logging.debug(getClass(), "Stopping discovery.");
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
				switch (event.getType()) {
					case IPacketConnector.EVENT_PACKET_CLOSED: {
						Enumeration e = connectors.keys();
						while (e.hasMoreElements()) {
							Short key = (Short)e.nextElement();
							if (event.getSource() == connectors.get(key)) {
								IPacketConnector connector = (IPacketConnector)connectors.remove(key);
								connector.removePacketListener(IPacketConnector.EVENT_PACKET_CLOSED 
										| IPacketConnector.EVENT_PACKET_RECEIVED, this);
								connector.release();
								break;
							}
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
							DeviceDescription device = (DeviceDescription)ois.readObject();
							if (device != null && ! SystemID.SYSTEM.equals(device.getSystemID())) { 
								SystemID id = device.getSystemID();
								int plugins = ois.readInt();
								for (int i = 0; i < plugins; i++) {
									PluginDescription plugin = (PluginDescription)ois.readObject();
									manager.registerPlugin(id, plugin, REMOVAL_PERIOD);
								}
								manager.registerDevice(device, REMOVAL_PERIOD);
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
