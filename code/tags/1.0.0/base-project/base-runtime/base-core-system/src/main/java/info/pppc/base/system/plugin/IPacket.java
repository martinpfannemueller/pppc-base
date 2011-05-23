package info.pppc.base.system.plugin;

/**
 * A packet is a data container that is used as transfer-unit in packet
 * connectors. It provides setters and getters for packet payload. Note
 * that in most packet implementations, the maximum size of the payload
 * will be limited by the communication technology.
 * 
 * @author Marcus Handte
 */
public interface IPacket {


	/**
	 * Returns the payload of the packet. This method is typically used
	 * to receive the data from a packet that has been passed to the
	 * receive method of a packet connector. If the payload is not known
	 * because the packet has no payload yet, the method will return null.
	 * 
	 * @return The payload of the packet or null if the payload is undefined.
	 */
	public byte[] getPayload();
	
	/**
	 * Sets the payload of the packet. This method is typically used before
	 * a packet is sent with the send method of the packet connector.
	 * 
	 * @param payload The payload of the packet.
	 * @throws IndexOutOfBoundsException Thrown if the payload does not fit into
	 * 	the length of the packet.
	 */
	public void setPayload(byte[] payload) throws IndexOutOfBoundsException;


}
