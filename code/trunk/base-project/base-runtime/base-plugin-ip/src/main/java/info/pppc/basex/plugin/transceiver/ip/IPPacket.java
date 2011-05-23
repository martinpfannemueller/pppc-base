package info.pppc.basex.plugin.transceiver.ip;

import info.pppc.base.system.plugin.IPacket;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * The ip packet is a packet that wraps the underlying datagram packet.
 * 
 * @author Marcus Handte
 */
public class IPPacket implements IPacket {

	/**
	 * The maximum packet length supported by the packet.
	 */
	private int length = 0;

	/**
	 * The packet that is used to receive data from the packet connector.
	 */
	private DatagramPacket packet;

	/**
	 * Creates a new packet with the specified length.
	 * 
	 * @param maximum The maximum length of the data packet.
	 * @param group The group to join.
	 * @param port The port of the socket.
	 */
	public IPPacket(int maximum, InetAddress group, int port) {
		length = maximum;
		packet = new DatagramPacket(new byte[maximum], maximum, group, port);
	}

	/**
	 * Returns the payload of the packet.
	 * 
	 * @return The payload that is currently stored in the packet.
	 */
	public byte[] getPayload() {
		return packet.getData();
	}

	/**
	 * Sets the payload of the packet.
	 * 
	 * @param payload The payload of the packet.
	 * @throws IndexOutOfBoundsException Thrown if the packet length is
	 * 	exceeded.
	 * @throws NullPointerException Thrown if the payload is null.
	 */
	public void setPayload(byte[] payload) {
		if (payload.length > length) {
			throw new IndexOutOfBoundsException("Payload to large.");
		} 
		packet.setData(payload);
		packet.setLength(payload.length);
	}

	/**
	 * Returns the internal datagram packet of this packet.
	 * 
	 * @return The internal datagram packet.
	 */
	public DatagramPacket getPacket() {
		return packet;
	}

}
