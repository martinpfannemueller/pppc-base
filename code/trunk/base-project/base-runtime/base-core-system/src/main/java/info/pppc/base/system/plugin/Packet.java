package info.pppc.base.system.plugin;

/**
 * A basic packet implementation that can be reused to implement connectors. 
 * This is a very simple implementation of the packet interface that is used 
 * consistently by higher level connectors to avoid duplicate implementations.
 * 
 * @author Marcus Handte
 */
public class Packet implements IPacket {
	
	/**
	 * The payload of the packet.
	 */
	private byte[] payload = new byte[0];
	
	/**
	 * The maximum packet length.
	 */
	private int length;
	
	/**
	 * Creates a new packet with an empty payload and
	 * the specified maximum length.
	 * 
	 * @param length The maximum length.
	 */
	public Packet(int length) {
		this.length = length;
	}
	
	/**
	 * Returns the payload of the packet.
	 * 
	 * @return The packet payload.
	 */
	public byte[] getPayload() {
		return payload;
	}
	
	/**
	 * Sets the payload of the packet.
	 * 
	 * @param payload The payload of the packet.
	 * @throws IndexOutOfBoundsException Thrown if the
	 * 	maximum packet size is exceeded.
	 */
	public void setPayload(byte[] payload) {
		if (payload.length > length) {
			throw new IndexOutOfBoundsException("Maximum packet size exceeded.");
		}
		this.payload = payload;
	}
	
}