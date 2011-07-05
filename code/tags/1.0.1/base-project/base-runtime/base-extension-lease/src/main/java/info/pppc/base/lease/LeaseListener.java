package info.pppc.base.lease;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.util.Logging;

/**
 * The lease listener is a helper class that can be used as a default
 * listener at the lease registry if the detection of an expired lease 
 * should only be performed at certain times. Incoming state changes
 * are signalled via notification to the listener itself, thus users
 * can wait on the listener, if desired.
 * 
 * @author Mac
 */
public class LeaseListener implements IListener {
		
	/**
	 * The flag that determines whether the lease has expired.
	 */
	private boolean expired = false;
	
	/**
	 * Creates a new lease listener that has not expired yet.
	 */
	public LeaseListener() {
		super();
	}
	
	/**
	 * This method is part of the listener interface, it should not
	 * be called by user code. It changes the state of the expired
	 * flag to true, if the right event type is detected.
	 * 
	 * @param event The event delivered to the listener.
	 */
	public void handleEvent(Event event) {
		if (event.getType() == LeaseRegistry.EVENT_LEASE_EXPIRED) {
			Logging.debug(getClass(), "Lease expired: " + event);
			synchronized (this) {
				expired = true;
				this.notifyAll();
			}
		} else {
			Logging.debug(getClass(), "Unknown lease event: " + event);
		}
	}
	
	/**
	 * Determines whether the lease has been expired. Returns true
	 * if the lease has expired, false otherwise.
	 * 
	 * @return True if the lease has expired, false otherwise.
	 */
	public synchronized boolean isExpired() {
		return expired;
	}
	
	/**
	 * Resets the state of the expired flag. This method can be used
	 * to reuse the lease listner without creating a new one.
	 */
	public synchronized void reset() {
		expired = false;
	}

}
