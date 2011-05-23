package info.pppc.base.system.plugin;

import info.pppc.base.system.event.IListener;

import java.io.IOException;

/**
 * A packet connector is a connector that broadcasts packets. It supports a 
 * generic create method that enables the creation of packet that are used
 * to send and receive data. Transmission of data to remote systems is supported
 * via the send method. The reception is done via events in order to minimize
 * the number of threads that are needed for reception. However, since some
 * underlying thread will be used to signal the event, the code that handles it
 * must be fast.
 * 
 * @author Marcus Handte
 */
public interface IPacketConnector extends IConnector {

	/**
	 * Event constant to signal that the packet connector has been
	 * closed. The source of the event will be the connector and the
	 * data object will be null.
	 */
	public static final int EVENT_PACKET_CLOSED = 1;
	
	/**
	 * Event constant to signal that a packet has been received.
	 * The source of the event will be the connector and the data 
	 * object will be the packet.
	 */
	public static final int EVENT_PACKET_RECEIVED = 2;
	
	/**
	 * Adds a packet listener to receive a particular event. The supported
	 * events are packet closed to signal that the connector has been
	 * closed and packet received to signal an incoming packet.
	 * 
	 * @param type The type of the event.
	 * @param listener The listener to add.
	 */
	public void addPacketListener(int type, IListener listener);
	
	/**
	 * Called to remove a listener for a specified set of events.
	 * 
	 * @param type The type of the events.
	 * @param listener The listener to remove.
	 * @return True if successful, false otherwise.
	 */
	public boolean removePacketListener(int type, IListener listener);

	/**  
	 * Creates a packet that can be used to receive and transfer data.
	 * 
	 * @return The new packet that can receive or send the specified length.
	 * @throws IOException Thrown if the packet cannot be created.
	 */
	public IPacket createPacket() throws IOException;

	/**
	 * Transfers a packet. Note that the packet must have been created by the
	 * same connector. Otherwise the result is undefined.
	 * 
	 * @param packet The packet that contains the payload that is transfered 
	 * 	by the call.
	 * @throws NullPointerException Thrown if the packet is null.
	 * @throws IOException Thrown if the transmission failed. This can also
	 * 	be a result of a previously released connector.
	 */
	public void sendPacket(IPacket packet) throws IOException; 
	

	/**
	 * Returns the maximum length of the packet.
	 * 
	 * @return The maximum length of the packet.
	 */
	public int getPacketLength();

}
