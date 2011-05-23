package info.pppc.basex.plugin.transceiver.ip;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * A connector that encapsulates a datagram socket. The ip transceiver 
 * plug-ins use datagram sockets to provide packet-based communication
 * groups.
 * 
 * @author Marcus Handte
 */
public class IPPacketConnector implements IPacketConnector, IOperation {

	/**
	 * The socket used to broadcast messages.
	 */
	private DatagramSocket socket;

	/**
	 * The port of the socket.
	 */
	private int socketPort;
	
	/**
	 * The maximum packet length.
	 */
	private int length;
	
	/**
	 * The inet address of the group.
	 */
	private InetAddress group;
	
	/**
	 * The plug-in that created the connector.
	 */
	private IIPPlugin plugin;
	
	/**
	 * The listeners that receive incoming packets.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * Flag to indicate whether this connector has been released.
	 */
	private boolean released = false;
	
	/**
	 * Creates a new packet connector that broadcasts messages
	 * using the specified datagram socket.
	 * 
	 * @param socket The socket used to broadcast messages.
	 * @param length The maximum packet length.
	 * @param plugin The creating plug-in.
	 * @param group The group id.
	 */
	public IPPacketConnector(IIPPlugin plugin, DatagramSocket socket, InetAddress group, int length) {
		this.socket = socket;
		this.socketPort = socket.getLocalPort();
		this.length = length;
		this.plugin = plugin;
		this.group = group;
	}

	/**
	 * Creates a packet that can be used to transfer and receive
	 * data from the connector.
	 * 
	 * @return A packet that can be used to receive and send data
	 * 	using this connector.
	 * @throws IOException Thrown if the connector is released.
	 */
	public IPacket createPacket() throws IOException {
		if (released) {
			throw new IOException("Connector has been released.");
		}
		return new IPPacket(length, group, socketPort);
	}

	/**
	 * Returns the ip transceiver plug-in as parent of this plug-in.
	 * 
	 * @return The ip transceiver plug-in 
	 */
	public IPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Transfers a packet using the underlying socket.
	 * If the operation fails, an exception will be 
	 * thrown.
	 * 
	 * @param packet The packet to transfer.
	 * @throws IOException Thrown if the operation failed.
	 */
	public void sendPacket(IPacket packet) throws IOException {
		DatagramPacket data = ((IPPacket)packet).getPacket();
		socket.send(data);
	}

	/**
	 * Adds a listener for the specified events.
	 * 
	 * @param type The events to listen for.
	 * @param listener The listener to add.
	 */
	public void addPacketListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}
	
	/**
	 * Removes the specified listener for the specified events.
	 * 
	 * @param type The types of events.
	 * @param listener The listener to remove.
	 * @return True if removed, false otherwise.
	 */
	public boolean removePacketListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}
	
	/**
	 * Returns the packet length.
	 * 
	 * @return The maximum packet length.
	 */
	public int getPacketLength() {
		return length;
	}
	
	/**
	 * Called to run the receiving thread.
	 * 
	 * @param monitor The monitor to abort the operation.
	 */
	public void perform(IMonitor monitor) {
		try {
			while (!monitor.isCanceled()) {
				try {
					DatagramPacket datagram = new DatagramPacket(new byte[length], length);
					socket.receive(datagram);
					int paylength = datagram.getLength();
					byte[] buffer = new byte[paylength];
					System.arraycopy(datagram.getData(), 0, buffer, 0, paylength);
					IPPacket packet = new IPPacket(length, group, socketPort);
					packet.setPayload(buffer);
					listeners.fireEvent(EVENT_PACKET_RECEIVED, packet);
				} catch (InterruptedIOException e) {
					// nothing to be done
				} 
			}
		} catch (IOException e) {
			Logging.debug(getClass(), "Exception while receiving packets.");
			release();
		}
	}
	
	/**
	 * Releases the socket and removes the connector from the
	 * list of connectors.
	 */
	public void release() {
		if (! released) {
			socket.close();
			listeners.fireEvent(EVENT_PACKET_CLOSED);
			plugin.release(this);
		}
	}
}