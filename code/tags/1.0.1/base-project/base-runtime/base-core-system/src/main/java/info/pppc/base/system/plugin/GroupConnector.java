package info.pppc.base.system.plugin;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.util.Vector;

/**
 * The group packet connector is used to multiplex packet connectors.
 * It adds a group identifier as prefix to the packet to multiplex the 
 * individual channels.
 * 
 * @author Marcus Handte
 */
public class GroupConnector implements IPacketConnector, IListener {

	/**
	 * A flag that indicates whether the packet connector
	 * has been released already. If this flag is true,
	 * all methods will throw an IOException.
	 */
	private boolean released = false;

	/**
	 * A vector that contains the packet connectors that are
	 * used to send and receive packets.
	 */
	private Vector connectors = new Vector();

	/**
	 * The listeners that receive packets and detect a closed
	 * connector.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The group id of the connector.
	 */
	private short group;

	/**
	 * The ability or null if none.
	 */
	private Short ability;
	
	/**
	 * The packet length supported by the connector.
	 */
	private int length;
	
	/**
	 * Creates a new packet connector that transfers and
	 * receives data.
	 * 
	 * @param group The group of the connector.
	 * @param length The maximum packet length that shall be
	 * 	supported. If a connector does not support this length,
	 * 	this connector will start fragmenting the connector.
	 */
	public GroupConnector(int length, short group) {		
		this(length, group, null);
	}
	
	/**
	 * Creates a new packet connector that transfers and
	 * receives data.
	 * 
	 * @param group The group of the connector.
	 * @param ability The ability of the connector.
	 * @param length The maximum packet length that shall be
	 * 	supported. If a connector does not support this length,
	 * 	this connector will start fragmenting the connector.
	 */
	public GroupConnector(int length, short group, Short ability) {		
		this.group = group;
		this.length = length;
		this.ability = ability;
	}
	
	/**
	 * Returns the maximum packet length.
	 * 
	 * @return The maximum packet length.
	 */
	public int getPacketLength() {
		return length;
	}
	
	/**
	 * Adds a packet listener for the specified type of events.
	 * 
	 * @param type The event types.
	 * @param listener The listener to register.
	 */
	public void addPacketListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}
	
	/**
	 * Removes a listener for the specified type of events.
	 * 
	 * @param type The type of events.
	 * @param listener The listener to remove.
	 * @return True if removed, false otherwise.
	 */
	public boolean removePacketListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}
	
	/**
	 * Called by the underlying connectors to signal an incoming 
	 * packet or a closed connector.
	 * 
	 * @param event The event created by one of the underlying connectors.
	 */
	public void handleEvent(Event event) {
		switch (event.getType()) {
			case EVENT_PACKET_CLOSED: {
				synchronized (this) {
					connectors.removeElement(event.getSource());
				}
				break;
			}
			case EVENT_PACKET_RECEIVED: {
				IPacket p = (IPacket)event.getData();
				byte[] payload = p.getPayload();
				if (payload.length >= 2 & (((payload[0] & 0xff) << 8) | (payload[1] & 0xff)) == group) {
					Packet result = new Packet(length);
					byte[] data = new byte[payload.length - 2];
					System.arraycopy(payload, 2, data, 0, data.length);
					result.setPayload(data);
					listeners.fireEvent(EVENT_PACKET_RECEIVED, result);
				}
				break;
			}
			default:
				break;
		}
	}

	/**
	 * Adds the specified packet connector to the set of
	 * connectors used by this connector.
	 * 
	 * @param c The packet connector to add.
	 */
	public synchronized void addConnector(IPacketConnector c) {
		if (! released) {
			// add fragmentation, if necessary
			if (c.getPacketLength() < length + 2) {
				c = new FragmentConnector(c);
			}
			connectors.addElement(c);
			c.addPacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
		} 
	}
	
	/**
	 * Adds the specified packet connector to the set of
	 * connectors used by this connector.
	 * 
	 * @param c The packet connector to add.
	 */
	public synchronized void removeConnector(IPacketConnector c) {
		if (! released) {
			connectors.removeElement(c);
			c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
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
		} else {
			return new Packet(length);	
		}
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
		byte[] payload = packet.getPayload();
		byte[] gpayload = new byte[payload.length + 2];
		System.arraycopy(payload, 0, gpayload, 2, payload.length);
		gpayload[0] = (byte)(group << 8);
		gpayload[1] = (byte)group;
		for (int i = 0; i < copy.size(); i++) {
			IPacketConnector c = (IPacketConnector)copy.elementAt(i);
			try {
				IPacket p = c.createPacket();
				p.setPayload(gpayload);
				c.sendPacket(p);											
			} catch (IOException e) {
				Logging.debug(getClass(), "Exception in connector (" 
						+ c.getClass().getName() + ":" + e.getMessage() + ").");
				synchronized (this) {
					//connectors.removeElement(c);
					//c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
				}
			}
		}
	}

	/**
	 * Releases the specified packet connector and removes all references.
	 */
	public synchronized void release() {
		this.released = true;
		while (! connectors.isEmpty()) {
			IPacketConnector c = (IPacketConnector)connectors.elementAt(0);
			connectors.removeElementAt(0);
			c.removePacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
		}
		listeners.fireEvent(EVENT_PACKET_CLOSED);
	}

	/**
	 * Returns the group of the packet connector.
	 * 
	 * @return The group of the connector.
	 */
	public short getGroup() {
		return group;
	}
	
	/**
	 * Returns the ability or null.
	 * 
	 * @return The ability of the connector.
	 */
	public Short getAbility() {
		return ability;
	}
	
	/**
	 * Returns the plug-in that created the connector. This
	 * will be null since the connector has not been created
	 * by a plug-in.
	 * 
	 * @return This connector will return null.
	 */
	public IPlugin getPlugin() {
		return null;
	}
	
}