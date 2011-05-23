package info.pppc.base.system.plugin;

import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The fragment connector is a wrapper for packet connectors that
 * performs an automatic packet fragmentation and defragmentation.
 * Thus, the size of the packets supported by this connector exceeds
 * the packet size provided by the underlying connector. Note that
 * the fragment connector is completely unreliable. Thus sending 
 * longer packets will result in less successful transmissions.
 * 
 * @author Marcus Handte
 */
public class FragmentConnector implements IPacketConnector, IListener {
	
	/**
	 * The packet id is used to identify the packet that is the source of
	 * a certain fragment.
	 * 
	 * @author Marcus Handte
	 */
	private static class PacketID {
		
		/**
		 * The length of a packet id in bytes if it is written to an
		 * array using the get bytes method.
		 */
		public static final int LENGTH = SystemID.LENGTH + 2;
		
		/**
		 * The system id of the packet id. Together with the id, this
		 * identifies the packet.
		 */
		private SystemID system;
		
		/**
		 * The id of the packet id. This id should be locally unique
		 * for every packet.
		 */
		private short id;
		
		/**
		 * Creates a new local packet id from the specified id. The
		 * system will be set to the local system.
		 * 
		 * @param id The id of the packet.
		 */
		public PacketID(short id) {
			this(SystemID.SYSTEM, id);
		}
		
		/**
		 * Creates a new packet id from the specified system and with
		 * the specified id.
		 * 
		 * @param system The system id of the packet.
		 * @param id The id of the packet.
		 * @throws NullPointerException Thrown if the system id is null.
		 */
		public PacketID(SystemID system, short id) {
			if (system == null) {
				throw new NullPointerException("System id must not be null.");
			}
			this.system = system;
			this.id = id;
		}
		
		/**
		 * Parses a packet id from the specified data object. The parsing
		 * starts at the specified offset within the data array.
		 * 
		 * @param data The data that contains the packet id.
		 * @param offset The offset at which the packet id should be parsed from.
		 * @return The packet id created from the specified data.
		 * @throws NumberFormatException Thrown if the data object does 
		 * 	not contain a valid packet id.
		 * @throws NullPointerException Thrown if the data array is null.
		 * @throws IndexOutOfBoundsException Thrown if the offset is negative or
		 * 	if the data object does not contain as least offset + packetid.length
		 * 	bytes.
		 */
		public static PacketID valueOf(byte[] data, int offset) throws NumberFormatException {
			byte[] sid = new byte[SystemID.LENGTH];
			System.arraycopy(data, offset, sid, 0, SystemID.LENGTH);
			SystemID system = new SystemID(sid);
			short id = (short)((data[offset + SystemID.LENGTH] << 8) | 
				(data[offset + SystemID.LENGTH] & 0xFF));
			return new PacketID(system, id);
		}
		
		/**
		 * Writes the packet id to the specified data array.
		 * 
		 * @param data The data array.
		 * @param offset The offset.
		 * @throws IndexOutOfBoundsException Thrown if the data array is smaller
		 * 	than the length of the packet id + the specified offset or if the 
		 * 	offset is negative.
		 * @throws NullPointerException Thrown if the data array is null.
		 */
		public void getBytes(byte[] data, int offset) {
			byte[] bid = system.getBytes();
			System.arraycopy(bid, 0, data, offset, SystemID.LENGTH);
			data[offset + SystemID.LENGTH] = (byte)(id >> 8);
			data[offset + SystemID.LENGTH + 1] = (byte)id;
		}
		
		/**
		 * Returns the system id of the packet id.
		 * 
		 * @return The system id of the packet id.
		 */
		public SystemID getSystem() {
			return system;
		}
		
		/**
		 * Returns the local id of the packet id. 
		 * 
		 * @return The local id of the packet id.
		 */
		public short getID() {
			return id;
		}
		
		/**
		 * Determines whether two objects are equal. This performs
		 * a structural comparison.
		 * 
		 * @param o The object to compare to.
		 * @return True if the objects are the same.
		 */
		public boolean equals(Object o) {
			if (o != null && o.getClass() == getClass()) {
				PacketID pid = (PacketID)o;
				return id == pid.getID() && system.equals(pid.system);
			}
			return false;
		}
		
		/**
		 * Returns a hash code for the packet id.
		 * 
		 * @return The hash code of the packet id.
		 */
		public int hashCode() {
			return system.hashCode() + id;
		}

	}
	
	/**
	 * The representation of a fragment of a packet. The fragment
	 * has a packet id, a certain
	 * 
	 * @author Marcus Handte
	 */
	private static class Fragment {
		
		/**
		 * The length of the fragment header.
		 */
		public static final int LENGTH = PacketID.LENGTH + 2;
		
		/**
		 * The packet id of the fragment.
		 */
		private PacketID packet;
		
		/**
		 * The payload of the fragment.
		 */
		private byte[] payload;
		
		/**
		 * The fragment id. This is the position of this
		 * fragment within the packet. Only the positive 
		 * numbers may be used. The highest bit must never
		 * be set.
		 */
		private short fragment;
		
		/**
		 * A flag that indicates whether this is the last 
		 * fragment of a packet.
		 */
		private boolean last;

		/**
		 * Creates a new fragment with the specified packet and fragment id. The
		 * fragment id must be a positive short. The last flag indicates whether
		 * this fragment is the last fragment of a packet. The payload must not
		 * be null.
		 * 
		 * @param packet The packet id of the fragment.
		 * @param fragment The fragment id of this fragment.
		 * @param last A flag that indicates whether this is the last fragment.
		 * @param payload The payload of the fragment.
		 * @throws NullPointerException Thrown if the payload or the packet id
		 * 	is null.
		 * @throws IllegalArgumentException Thrown if the fragment id is not 
		 * 	a positive short (including 0).
		 */		
		public Fragment(PacketID packet, short fragment, boolean last, byte[] payload) {
			if (packet == null) {
				throw new NullPointerException("Packet must not be null.");
			}
			if (payload == null) {
				throw new NullPointerException("Payload must not be null.");
			}
			if (fragment < 0) {
				throw new IllegalArgumentException("Fragment id must be positive.");
			}
			this.packet = packet;
			this.fragment = fragment;
			this.payload = payload;
			this.last = last;
		}
		
		/**
		 * Parses a fragment from the specified data array. If the data array is
		 * illegal a number format exception will be thrown.
		 * 
		 * @param data The data to read from.
		 * @param offset To offset to start from.
		 * @return A fragment that has been created from the data.
		 * @throws NumberFormatException Thrown if the data object is too small.
		 * @throws NullPointerException Thrown if the data is null.
		 * @throws IndexOutOfBoundsException Thrown if the offset is negative.
		 */
		public static Fragment valueOf(byte[] data, int offset) throws NumberFormatException {
			if (data.length < LENGTH + offset) {
				throw new NumberFormatException("Illegal data size.");
			}
			PacketID packet = PacketID.valueOf(data, offset);
			boolean last = ((data[PacketID.LENGTH + offset] & 0x80) == 0x80);
			short fragment = (short)(((data[PacketID.LENGTH + offset] & 0x7F) << 8) |
				(data[PacketID.LENGTH + offset + 1] & 0xFF)); 
			byte[] payload = new byte[data.length - offset - LENGTH];
			System.arraycopy(data, offset + LENGTH, payload, 0, payload.length);
			return new Fragment(packet, fragment, last, payload);
		}
		
		/**
		 * Returns a byte representation of this fragment.
		 * 
		 * @return A byte representation of this fragment.
		 */
		public byte[] getBytes() {
			byte[] total = new byte[LENGTH + payload.length];
			packet.getBytes(total, 0);
			total[PacketID.LENGTH] = (byte)(fragment >> 8);
			if (last) {
				total[PacketID.LENGTH] = (byte)(total[PacketID.LENGTH] | 0x80);	
			}
			total[PacketID.LENGTH + 1] = (byte)(fragment); 
			System.arraycopy(payload, 0, total, LENGTH, payload.length);
			return total;
		}
		
		/**
		 * Returns the packet id of the fragment.
		 * 
		 * @return The packet id of the fragment.
		 */
		public PacketID getPacket() {
			return packet;
		}
		
		/**
		 * Returns the fragment id of the fragment.
		 * 
		 * @return The fragment id of the fragment.
		 */
		public short getFragment() {
			return fragment;
		}
		
		/**
		 * Determines whether the fragment is the last fragment of the
		 * packet.
		 * 
		 * @return True if the fragment is the last, false otherwise.
		 */
		public boolean isLast() {
			return last;
		}
		
		/**
		 * Returns the payload of the packet.
		 * 
		 * @return The payload of the packet.
		 */
		public byte[] getPayload() {
			return payload;
		}
		
	}
	
	/**
	 * The storage is used to store the arriving fragments of a single
	 * packet. The storage maintains an ordered vector of fragments.
	 * 
	 * @author Marcus Handte
	 */
	private class Storage {		
		
		/**
		 * The time at which the storage has been manipulated for the
		 * last time. This field is updated by the add method.
		 */
		private long time = System.currentTimeMillis();
		
		/**
		 * A vector that contains the fragments sorted by fragment ids.
		 */
		private Vector fragments = new Vector(1);
		
		/**
		 * The packet id of the packet that is stored in this storage.
		 */
		private PacketID packet;
		
		/**
		 * Creates a new storage for the specified packet.
		 * 
		 * @param packet The packet to store.
		 */
		public Storage(PacketID packet) {
			if (packet == null) {
				throw new NullPointerException("Packet id must not be null.");
			}
			this.packet = packet;
		}
		
		/**
		 * Adds the specified fragment to the storage and returns true
		 * if the fragments are complete. In this case, the complete
		 * packet can be retrieved using the get data method. Note that
		 * the fragment is only added if the packet id of the fragment 
		 * matches with the packet id of this storage. 
		 * 
		 * @param fragment The fragment to add.
		 * @return True if the storage contains a complete packet.
		 */
		public boolean addFragment(Fragment fragment) {
			if (fragment.getPacket().equals(packet)) {
				boolean added = false;
				for (int i = 0; i < fragments.size(); i++) {
					Fragment f = (Fragment)fragments.elementAt(i);
					if (f.getFragment() > fragment.getFragment()) {
						fragments.insertElementAt(fragment, i);
						added = true;
						break;
					}
				}
				if (! added) {
					fragments.addElement(fragment);
				}
				time = System.currentTimeMillis();	
			}
			return isComplete();
		}
		
		/**
		 * Returns the payload of the packet by combining all available fragments.
		 * Note that this method will combine fragments no matter whether the 
		 * packet is already complete.
		 * 
		 * @return The combined payload of all fragments that are available at 
		 * 	this time.
		 */
		public byte[] getPayload() {
			int size = 0;
			for (int i = 0; i < fragments.size(); i++) {
				size += ((Fragment)fragments.elementAt(i)).getPayload().length;
			}
			byte[] payload = new byte[size];
			int written = 0;
			for (int i = 0; i < fragments.size(); i++) {
				byte[] source = ((Fragment)fragments.elementAt(i)).getPayload();
				System.arraycopy(source, 0, payload, written, source.length);
				written += source.length;	
			}
			return payload;	
		}
		
		/**
		 * Determines whether the packet is complete. A packet is complete if
		 * it has at least one fragment and the number of fragments is equal 
		 * to the fragment size denoted by the last fragment.
		 * 
		 * @return True if the packet is complete, false otherwise.
		 */
		public boolean isComplete() {
			if (fragments.size() == 0) {
				return false;
			} else {
				Fragment f = (Fragment)fragments.lastElement();
				return (f.isLast() && fragments.size() == f.getFragment() + 1);
			}
		}
		
		/**
		 * Determines whether the storage must be still alive with 
		 * respect to the fragment timeout configured for this 
		 * fragment connector.
		 * 
		 * @return True if the timeout has not expired for this
		 * 	packet storage.
		 */
		public boolean isAlive() {
			return ((System.currentTimeMillis() - fragmentTimeout) < time); 
		}
		
		/**
		 * Returns the packet id of the packet fragments that are
		 * stored in this storage.
		 * 
		 * @return The packet id of the storage.
		 */
		public PacketID getPacket() {
			return packet;
		}
		
	}
	

	/**
	 * The default timeout after which the connector will drop
	 * incomplete fragments.
	 */
	private static final long FRAGMENT_TIMEOUT = 3000;

	/**
	 * The maximum packet size for packets created by this connector.
	 * The packet size is calculated whenever the first packet is 
	 * created by this connector. 
	 */
	protected int packetLength = -1;

	/**
	 * The packet connector used to transfer fragments.
	 */
	protected IPacketConnector connector;

	/**
	 * The timeout value after which the fragments will be
	 * dropped.
	 */
	protected long fragmentTimeout;

	/**
	 * The timeout value after which the reception of a packet
	 * will be aborted.
	 */
	protected int packetTimeout;

	/**
	 * The packet id prepended to each packet.
	 */
	protected short packetID = Short.MIN_VALUE;

	/**
	 * This hash table hashes system ids to vectors of fragment 
	 */
	protected Hashtable systems = new Hashtable(1);

	/**
	 * The listeners that are receiving incoming packets.
	 */
	protected ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * Creates a new fragment connector that uses the specified
	 * connector to send packets. The fragment timeout of the
	 * connector will be set to the default timeout value.
	 * 
	 * @param connector The connector used to send packets.
	 * @throws NullPointerException Thrown if the connector is
	 * 	null.
	 */
	public FragmentConnector(IPacketConnector connector) {
		this(connector, FRAGMENT_TIMEOUT);
	}

	/**
	 * Creates a new fragmenting packet connector that is capable
	 * of sending larger packets than the underlying connector.
	 * The timeout value indicates the time after which the
	 * connector will drop incomplete fragments that have not
	 * been completed. If the timeout value is smaller or equal
	 * to zero an illegal argument exception will be thrown.
	 * 
	 * @param connector The packet connector that is used to 
	 * 	send and receive data.
	 * @param timeout The maximum interval that lies between
	 * 	two fragments. This value can be used to cleanup the
	 * 	buffer of the fragment connector.
	 * @throws NullPointerException Thrown if the connector is
	 * 	null.
	 */
	public FragmentConnector(IPacketConnector connector, long timeout) {
		if (connector == null) {
			throw new NullPointerException("Connector must not be null."); 
		}
		if (timeout <= 0) {
			throw new IllegalArgumentException("Timeout must be larger than 0.");
		}
		this.connector = connector;
		this.fragmentTimeout = timeout;	  
		connector.addPacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
	}

	/**
	 * Adds a packet listener for the specified events.
	 * 
	 * @param type The type of events.
	 * @param listener The listener to add.
	 */
	public void addPacketListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}

	/**
	 * Removes a packet listener for the specified events.
	 * 
	 * @param type The type of events.
	 * @param listener The listener to remove.
	 * @return True if removed, false otherwise.
	 */
	public boolean removePacketListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Creates and returns a new packet for this connector.
	 * 
	 * @return A new packet for this connector.
	 * @throws IOException Thrown if the connector has been closed.
	 */
	public IPacket createPacket() throws IOException {
		return new Packet(getPacketLength());
	}
	
	/**
	 * Returns the packet length that is supported by the connector.
	 * The packet length of this connector depends on the underlying
	 * packet size - the length of the fragment header.
	 * 
	 * @return The packet length of the connector.
	 */
	public int getPacketLength() {
		if (packetLength == -1) {
			// calculate the maximum packet size
			int available = connector.getPacketLength() - Fragment.LENGTH;
			if (available > Short.MAX_VALUE) {
				packetLength = Integer.MAX_VALUE;	
			} else {
				packetLength = available * Short.MAX_VALUE;
			}
		}
		return packetLength;
	}
	
	/**
	 * Returns the plug-in of the underlying connector.
	 * 
	 * @return The plug-in of the underlying connector.
	 */
	public IPlugin getPlugin() {
		return connector.getPlugin();
	}
	
	/**
	 * Called whenever the underlying connector is signaling 
	 * a change.
	 * 
	 * @param event The event to deal with.
	 */
	public void handleEvent(Event event) {
		switch (event.getType()) {
			case EVENT_PACKET_RECEIVED: {
				// perform complete cleanup
				synchronized (systems) {
					Enumeration keys = systems.keys();
					while (keys.hasMoreElements()) {
						Object k = keys.nextElement();
						Vector storages = (Vector)systems.get(k);
						for (int i = storages.size() - 1; i >= 0; i--) {
							Storage s = (Storage)storages.elementAt(i);
							if (! s.isAlive()) {
								storages.removeElement(s);
							}
						}
						if (storages.isEmpty()) {
							systems.remove(k);
						}
					}
				}
				// handle packet
				IPacket p = (IPacket)event.getData();
				Fragment frag = Fragment.valueOf(p.getPayload(), 0);					
				PacketID pid = frag.getPacket();
				SystemID sid = pid.getSystem();
				synchronized (systems) {
					// find storages for system
					Vector storages = (Vector)systems.get(sid);
					if (storages == null) {
						storages = new Vector();
						systems.put(sid, storages);
					}
					// find storage for packet id
					Storage storage = null;
					for (int i = 0; i < storages.size(); i++) {
						Storage s = (Storage)storages.elementAt(i);
						if (pid.equals(s.getPacket())) {
							storage = s;
							break;
						}
						// perform intermediate cleanup
						if (! s.isAlive()) {
							storages.removeElementAt(i);
							i--;
						}
					}
					if (storage == null) {
						storage = new Storage(pid);
						storages.addElement(storage);
					}
					// add packet and perform final check
					if (storage.addFragment(frag)) {
						Packet packet = new Packet(getPacketLength());
						packet.setPayload(storage.getPayload());
						storages.removeElement(storage);
						if (storages.isEmpty()) {
							systems.remove(sid);
						}
						listeners.fireEvent(EVENT_PACKET_RECEIVED, packet);
					}
				}
				break;
			}
			case EVENT_PACKET_CLOSED: {
				synchronized (systems) {
					systems.clear();	
				}
				listeners.fireEvent(EVENT_PACKET_CLOSED);
				break;
			}
			default:
				break;
		}
		
	}
	
	/**
	 * Releases the underlying packet connector.
	 */
	public void release() {
		connector.release();	
	}

	/**
	 * Sends a packet and throws an exception if the packet 
	 * cannot be sent or if the connector has been closed already.
	 * 
	 * @param packet The packet to send.
	 * @throws IOException Thrown by the underlying connector.
	 */
	public void sendPacket(IPacket packet) throws IOException {
		PacketID pid = null;
		synchronized (this) {
			pid = new PacketID(packetID);
			packetID += 1;
		}
		byte[] payload = packet.getPayload();
		int written = 0;
		short fid = 0;
		while (written < payload.length) {
			// check whether maximum number of fragments has been reached.
			if (fid < 0) {
				throw new IOException("Maximum fragments exceeded.");
			}
			// check whether the packet size suffices for fragmentation
			IPacket p = connector.createPacket();
			if (connector.getPacketLength() <= Fragment.LENGTH) {
				throw new IOException("Maximum packet length is too small.");
			}
			// create the current fragment
			int fill = Math.min(connector.getPacketLength() - Fragment.LENGTH, (payload.length - written));
			byte[] data = new byte[fill];
			System.arraycopy(payload, written, data, 0, data.length);
			written += data.length;
			Fragment fragment = new Fragment(pid, fid, (written == payload.length), data);
			p.setPayload(fragment.getBytes());
			connector.sendPacket(p);
			fid += 1;
		}
	}

}
