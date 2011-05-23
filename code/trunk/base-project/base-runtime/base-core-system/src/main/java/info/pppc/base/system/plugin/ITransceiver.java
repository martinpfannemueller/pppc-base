package info.pppc.base.system.plugin;

import info.pppc.base.system.event.IListener;

import java.io.IOException;

/**
 * A communication transceiver describes a plug-in that can receive and transmit
 * data to a remote system. A communication transceiver is either enabled, i.e.
 * it can send and receive data or it is disabled. If it is disabled, it 
 * should release all handles to the communication card it is associated
 * with, since BASE might decide to disable the card which will typically
 * be invisible to the plug-in.
 * 
 * @author Marcus Handte
 */
public interface ITransceiver extends IPlugin, IStreamer {
	
	/**
	 * An event type constant that signals that the transceiver has been enabled.
	 * The data object of this event will be null.
	 */
	public static final int EVENT_TRANCEIVER_ENABLED = 1;
	
	/**
	 * An event type constant that signals that the transceiver has been disabled.
	 * The data object of this event will be null.
	 */
	public static final int EVENT_TRANCEIVER_DISABLED = 2;

	/**
	 * Sets the plug-in manager of this plug-in. The plug-in manager enables 
	 * a plug-in to request a communication stack and to perform operations. 
	 * This method is guaranteed to be called before the start method is
	 * invoked the first time.
	 * 
	 * @param manager The manager of the plug-in.
	 */
	public void setTransceiverManager(ITransceiverManager manager);

	/**
	 * Adds a listener to the transceiver that signals changes to the state of
	 * the transceiver. When the transceiver is enabled, the transceiver will fire an
	 * EVENT_TRANCEIVER_ENABLED event. If it is disabled, an EVENT_TRANCEIVER_DISABLED
	 * event will be fired. The data object of these events is null.
	 * 
	 * @param type The type of event to register for. At the present time the
	 * 	transceiver must support EVENT_TRANCEIVER_ENABLED and EVENT_TRANCEIVER_DISABLED
	 * 	events.
	 * @param listener The listener to register for the specified events.
	 * @throws NullPointerException Thrown if the listener that is registered
	 * 	is null.
	 */
	public void addTransceiverListener(int type, IListener listener);
		
	/**
	 * Removes the specified listener for the specified set of event types.
	 * 
	 * @param type The types of events to unregister.
	 * @param listener The listener to unregister for the specified types of
	 * 	events.
	 * @return True if the listener is no longer registered for any event,
	 * 	false if the listener is still registered or if it has not been
	 * 	registered.
	 * @throws NullPointerException Thrown if the listener that should be 
	 * 	unregistered is null.
	 */
	public boolean removeTransceiverListener(int type, IListener listener);

	/**
	 * Enables or disables this communication transceiver.
	 * 
	 * @param enabled True to enable the communication transceiver, false to
	 * 	disable.
	 */
	public void setEnabled(boolean enabled);
	
	/**
	 * Determines whether this communication transceiver is enabled.
	 * 
	 * @return True if it is enabled, false if it is disabled.
	 */
	public boolean isEnabled();
	
	/**
	 * Opens an outgoing or incoming packet based connector that exchanges 
	 * packets with other devices using broadcast semantics.
	 * 
	 * @return The connector that is capable of exchanging packets or 
	 * null if such a connector cannot be created.
	 * @throws IOException Thrown if the connector cannot be opened.
	 */
	public IPacketConnector openGroup() throws IOException;
}
