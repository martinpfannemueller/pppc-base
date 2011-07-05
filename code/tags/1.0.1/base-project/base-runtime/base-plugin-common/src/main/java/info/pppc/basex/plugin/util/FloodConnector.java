package info.pppc.basex.plugin.util;

import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.IOperator;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.Packet;
import info.pppc.base.system.util.Logging;

/**
 * The flood connector performs a scoped flooding for all packets that
 * it receives. To eliminate broadcast storms it assigns packet ids and
 * it maintains a buffer of a number of packets that have been received
 * lately. To scope the flooding, a hop count is assigned to each packet.
 * 
 * @author Marcus Handte
 */
public class FloodConnector implements IPacketConnector, IListener {
	
	/**
	 * The packet header that is prepended to each message sent through the
	 * flood connector.
	 * 
	 * @author Marcus Handte
	 */
	private static class Header {
		
		/**
		 * The total length of a header.
		 */
		public static final int LENGTH = SystemID.LENGTH + 4;
		
		/**
		 * The source system.
		 */
		private SystemID source;
		
		/**
		 * The packet id assigned by the source system.
		 */
		private short id;
		
		/**
		 * The remaining hop count.
		 */
		private short count;
		
		/**
		 * Creates a new packet header with for the specified source
		 * system with the specified id and the specified hop count.
		 * 
		 * @param source The source of the packet.
		 * @param id The id of the packet.
		 * @param count The hop count of the packet.
		 * @throws NullPointerException Thrown if the source system
		 * 	is null.
		 * @throws IllegalArgumentException Thrown if the count is
		 * 	negative (not including 0).
		 */
		public Header(SystemID source, short id, short count) {
			if (source == null) {
				throw new NullPointerException("Source must not be null.");
			}
			if (count < 0) {
				throw new IllegalArgumentException("Hop count must not be negative.");
			}
			this.source = source;
			this.id = id;
			this.count = count;
		}
		
		/**
		 * Returns the header that is located within the specified byte
		 * array at the specified offset.
		 * 
		 * @param bytes The bytes that should be parsed.
		 * @param offset The offset at which the header is located.
		 * @return The header that has been parsed from the byte array.
		 * @throws NumberFormatException Thrown if the byte array does not
		 * 	contain a header at the specified offset.
		 * @throws NullPointerException Thrown if the byte array is null.
		 */
		public static Header valueOf(byte[] bytes, int offset) throws NumberFormatException {
			if (bytes.length < offset + LENGTH) {
				throw new NumberFormatException("Data does not contain a header.");
			}
			byte[] sid = new byte[SystemID.LENGTH];
			System.arraycopy(bytes, offset, sid, 0, SystemID.LENGTH);
			SystemID source = new SystemID(sid);
			short id = (short)((bytes[SystemID.LENGTH + offset] << 8) |
			 (bytes[SystemID.LENGTH + offset + 1] & 0xFF));
			short count = (short)((bytes[SystemID.LENGTH + offset + 2] << 8) |
			 (bytes[SystemID.LENGTH + offset + 3] & 0xFF));
			return new Header(source, id, count);
		}
		
		/**
		 * Returns a byte representation of the current header.
		 * 
		 * @return A byte representation of this header.
		 */
		public byte[] getBytes() {
			byte[] bytes = new byte[LENGTH];
			byte[] sid = source.getBytes();
			System.arraycopy(sid, 0, bytes, 0, SystemID.LENGTH);
			bytes[SystemID.LENGTH] = (byte)(id >> 8);
			bytes[SystemID.LENGTH + 1] = (byte)id;
			bytes[SystemID.LENGTH + 2] = (byte)(count >> 8);
			bytes[SystemID.LENGTH + 3] = (byte)count;
			return bytes;
		}
		
		/**
		 * Determines whether the passed object is equal. The comparison
		 * is based on the content of a potential header. The hop count
		 * is ignored.
		 * 
		 * @param o The object to compare to.
		 * @return True if the objects are the same.
		 */
		public boolean equals(Object o) {
			if (o != null && o.getClass() == getClass()) {
				Header h = (Header)o;
				return id == h.id && source.equals(h.source);
			}
			return false;
		}

		/**
		 * Returns a content-based hash code. The hop count
		 * does not influence the hash code.
		 * 
		 * @return The hash code of the object.
		 */
		public int hashCode() {
			return source.hashCode() + id;
		}
		
		/**
		 * Decrements the hop count of the header.
		 */
		public void decCount() {
			count -= 1;
		}
		
		/**
		 * Returns a string representation of this header.
		 * 
		 * @return A string representation.
		 */
		public String toString() {
			return source.toString() + " ID: " + id + " COUNT: " + count;
		}

	}
	
	/**
	 * The maximum wait period for a rebroadcast. The connector will perform
	 * a randomization between 0 and the maximum wait period.
	 */
	private static final short MAXIMUM_WAIT = 20;

	/**
	 * The default scope of the connector. With this scope each packet is
	 * flooded within 2 additional hops.
	 */
	private static final short DEFAULT_SCOPE = 3;
	
	/**
	 * The default buffer size for packet ids that have been received lately.
	 */
	private static final int DEFAULT_BUFFER = 100;
	
	/**
	 * The underlying packet connector that is used to perform the
	 * actual data transfer.
	 */
	protected IPacketConnector connector;
	
	/**
	 * The scope of the connector. This is essentially the maximum
	 * hop count for messages.
	 */
	protected short scope;
	
	/**
	 * The buffer for messages that have been received so far. The
	 * size of this buffer will be limited to the value specified 
	 * at creation time. 
	 */
	protected Vector buffer = new Vector();

	/**
	 * The buffer limit for packet ids stored by this connector.
	 */
	protected int limit;
	
	/**
	 * The operator to schedule new transmissions.
	 */
	protected IOperator operator;
	
	/**
	 * The packet id that is prepended to each packet.
	 */
	protected short packetID = Short.MIN_VALUE;

	/**
	 * The randomizer used to delay rebroadcasts of incoming 
	 * packets.
	 */
	protected Random random = new Random();
	
	/**
	 * The listeners that are receiving packets.
	 */
	protected ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * Creates a new flood connector that uses the specified connector
	 * as underlying implementation. The scope and the buffer limit will
	 * be set to default values. 
	 * 
	 * @param operator The operator to schedule new transmissions.
	 * @param connector The underlying connector used to transmit and 
	 * 	receive packets.
	 * @throws NullPointerException Thrown if the connector is null.
	 */
	public FloodConnector(IOperator operator, IPacketConnector connector) {
		this(operator, connector, DEFAULT_SCOPE);
	}
	
	/**
	 * Creates a flood connector that uses the specified connector as
	 * underlying implementation. The scope will be set to the specified
	 * value and the buffer limit will be set to the default value.
	 * 
	 * @param operator The operator to schedule new transmissions.
	 * @param connector The connector used to receive and transmit packets.
	 * @param scope The flooding scope (i.e., the maximum hop count).
	 * @throws NullPointerException Thrown if the connector is null.
	 * @throws IllegalArgumentException Thrown if the scope is not a
	 * 	positive value (excluding 0).
	 */
	public FloodConnector(IOperator operator, IPacketConnector connector, short scope) {
		this(operator, connector, scope, DEFAULT_BUFFER);
	}
	
	/**
	 * Creates a flood connector that uses the specified connector as
	 * underlying implementation. The scope and the buffer limit will
	 * be set to the specified values.
	 * 
	 * @param operator The operator to schedule new transmissions.
	 * @param connector The connector used to receive and transmit packets.
	 * @param scope The flooding scope (i.e., the maximum hop count).
	 * @param limit The maximum buffer size for packet ids.
	 * @throws NullPointerException Thrown if the connector is null.
	 * @throws IllegalArgumentException Thrown if the scope or the buffer
	 * 	limit are not positive values (excluding 0). 
	 */
	public FloodConnector(IOperator operator, IPacketConnector connector, short scope, int limit) {
		if (operator == null) {
			throw new NullPointerException("Operator must not be null.");
		}
		if (connector == null) {
			throw new NullPointerException("Connector must not be null.");
		}
		if (scope < 1) {
			throw new IllegalArgumentException("Flooding scope must be positive.");
		}
		if (limit < 1) {
			throw new IllegalArgumentException("Buffer limit must be positive.");		
		}
		this.connector = connector;
		this.scope = scope;
		this.limit = limit;
		this.operator = operator;
		connector.addPacketListener(EVENT_PACKET_CLOSED | EVENT_PACKET_RECEIVED, this);
	}
	
	/**
	 * Adds a packet listener for the specified set of events.
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
	 * @param type The type of events to unregister.
	 * @param listener The listener to remove.
	 * @return True if removed, false otherwise.
	 */
	public boolean removePacketListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}
	
	
	/**
	 * Creates a packet that can be used to send and receive data.
	 * 
	 * @return A packet that can be used to send and receive data.
	 * @throws IOException Thrown if the packet cannot be created.
	 */
	public IPacket createPacket() throws IOException {
		return new Packet(connector.getPacketLength() - Header.LENGTH);
	}
	
	/**
	 * Returns the plug-in that is associated with the underlying
	 * connector.
	 * 
	 * @return The plug-in of the underlying connector.
	 */
	public IPlugin getPlugin() {
		return connector.getPlugin();
	}
	
	/**
	 * Called whenever the underlying connector creates an event. Upon close
	 * event, the event is simply forwarded. Upon receive event, the packet
	 * is matched against the buffer and if it is not contained in the buffer,
	 * it will be delivered. In addition, if it needs to be broadcasted again,
	 * this will be done as well.
	 * 
	 * @param event The event received from the underlying connector.
	 */
	public void handleEvent(Event event) {
		switch (event.getType()) {
			case EVENT_PACKET_RECEIVED: {
				IPacket p = (IPacket)event.getData();
				byte[] data = p.getPayload();
				// check whether packet must be received
				final Header h = Header.valueOf(data, 0);
				synchronized (buffer) {
					if (buffer.contains(h)) {
						return;
					} else {
						buffer.insertElementAt(h, 0);
						if (buffer.size() > limit) {
							buffer.removeElementAt(buffer.size() - 1);
						}
					}
				}
				// packet must be delivered
				final byte[] payload = new byte[data.length - Header.LENGTH];
				System.arraycopy(data, Header.LENGTH, payload, 0, payload.length);
				h.decCount();
				// check whether it should be broadcasted
				if (h.count > 0) {
					operator.performOperation(new IOperation() {
						public void perform(IMonitor monitor) throws Exception {
							// rebroadcast packet
							try {
								// wait for randomized time
								int wait = Math.abs(random.nextInt()) % MAXIMUM_WAIT;
								try {
									Thread.sleep(wait);	
								} catch (InterruptedException e) {
									Logging.error(getClass(), "Rebroadcast interrupted.", e);
								}
								sendPacket(h, payload);
							} catch (IOException e) {
								Logging.debug(getClass(), "Could rebroadcast incoming packet.");
							}
						}
					});
				}
				Packet packet = new Packet(connector.getPacketLength() - Header.LENGTH);
				packet.setPayload(payload);
				listeners.fireEvent(EVENT_PACKET_RECEIVED, packet);
				break;
			}
			case EVENT_PACKET_CLOSED: {
				listeners.fireEvent(EVENT_PACKET_CLOSED);
				break;	
			}
			default:
				break;
		}
	}
	
	/**
	 * Releases this connector as well as the underlying connector.
	 */
	public void release() {
		connector.release();
	}
	
	/**
	 * Sends the specified packet if possible. This method creates a
	 * header and adds it to the set of local headers before it 
	 * transmits the method using the private send method.
	 * 
	 * @param packet The packet that should be transfered.
	 * @throws IOException Thrown by the underlying connector.
	 */
	public void sendPacket(IPacket packet) throws IOException {
		byte[] data = packet.getPayload();
		if (data != null) {
			Header header = null;
			synchronized (this) {
				header = new Header(SystemID.SYSTEM, packetID, scope);
				packetID += 1;
			}		
			synchronized (buffer) {
				buffer.insertElementAt(header, 0);
				if (buffer.size() > limit) {
					buffer.removeElementAt(buffer.size() - 1);		
				}
			}
			sendPacket(header, data);
		}
	}
	
	/**
	 * Sends a packet with the specified header.
	 * 
	 * @param header The header of the packet.
	 * @param data The data of the packet.
	 * @throws IOException Thrown if the packet could not be transfered.
	 */
	protected void sendPacket(Header header, byte[] data) throws IOException {
		IPacket p = connector.createPacket();
		byte[] hdata = header.getBytes();
		byte[] payload = new byte[Header.LENGTH + data.length];
		System.arraycopy(hdata, 0, payload, 0, Header.LENGTH);
		System.arraycopy(data, 0, payload, Header.LENGTH, data.length);
		p.setPayload(payload);
		connector.sendPacket(p);
	}
	
	/**
	 * Returns the maximum packet length. The length is defined by the
	 * underlying connector - the length of the header that is needed
	 * for scoping and duplicate detection.
	 * 
	 * @return The packet length.
	 */
	public int getPacketLength() {
		return connector.getPacketLength() - Header.LENGTH;
	}
	
}
