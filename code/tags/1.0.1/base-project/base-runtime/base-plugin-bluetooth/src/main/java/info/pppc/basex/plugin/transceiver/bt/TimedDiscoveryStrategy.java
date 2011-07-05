package info.pppc.basex.plugin.transceiver.bt;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.transceiver.MxBluetoothTransceiver;

/**
 * The timed discovery strategy implements a discovery strategy
 * that blocks the discovery for a certain about of time. The
 * timeout can be specified by passing it to the constructor
 * and it can be redefined during the execution by the corresponding
 * setter. Note that the timeout must be positive.
 * 
 * @author Mac
 */
public class TimedDiscoveryStrategy implements MxBluetoothTransceiver.IDiscoveryStrategy {

	/**
	 * This event is fired to listeners when the start method
	 * is called. It signals that the plug-in has been started. 
	 */
	public static final int EVENT_DISCOVERY_STARTED = 1;
	
	/**
	 * This event is fired when the execute method is called and
	 * the discovery thread is blocked. It signals that the previous
	 * discovery has finished (if there was one).
	 */
	public static final int EVENT_DISCOVERY_WAITING = 2;
	
	/**
	 * This event is fired when the execute method has unblocked
	 * the discovery thread and the discovery is running.
	 */
	public static final int EVENT_DISCOVERY_RUNNING = 4;
	
	/**
	 * This event is fired when the plug-in is stopped and the
	 * discovery operation will no longer be executed.
	 */
	public static final int EVENT_DISCOVERY_STOPPED = 8;
	
	/**
	 * The listeners of the discovery strategy.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	
	/**
	 * A flag that indicates whether the underlying plug-in
	 * is running.
	 */
	private boolean running = false;
	
	private long timeout = 0;
	
	/**
	 * Creates a new discovery strategy with the
	 * specified timeout.
	 */
	public TimedDiscoveryStrategy(long timeout) {
		if (timeout < 0) throw new IllegalArgumentException("Timeout must be positive.");
		this.timeout = timeout;
	}
	
	/**
	 * Returns the current timeout. That has been
	 * set through the initializer or through a call
	 * to the setter.
	 * 
	 * @return The current timeout.
	 */
	public synchronized long getTimeout() {
		return timeout;
	}
	
	/**
	 * Sets the timeout. The new timeout may not be 
	 * considered until the next execution of the
	 * discovery operation.
	 * 
	 * @param timeout The new timeout.
	 */
	public synchronized void setTimeout(long timeout) {
		if (timeout < 0) throw new IllegalArgumentException("Timeout must be positive.");
		this.timeout = timeout;
	}

	/**
	 * Adds a listener to the strategy which is informed about
	 * event as defined in this class.
	 * 
	 * @param types The types of events to register.
	 * @param listener The listener to register.
	 */
	public void addListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}
	
	/**
	 * Removes a previously registered listener.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if successful, false otherwise.
	 */
	public boolean removeListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * This method is called by the bluetooth plug-in upon
	 * startup. It fires the corresponding event to all listener.
	 */
	public synchronized void start() {
		this.running = true;
		listeners.fireEvent(EVENT_DISCOVERY_STARTED);
	}
	
	/**
	 * This method is called by the bluetooth plugin when
	 * the discovery operation has finished. It blocks until
	 * the run method is called.
	 */
	public synchronized void execute() {
		if (running) {
			listeners.fireEvent(EVENT_DISCOVERY_WAITING);
		}
		long now = System.currentTimeMillis();
		long next = now + timeout;
		while (running && now < next) {
			try {
				wait(next - now);	
			} catch (InterruptedException e) {
				Logging.error(getClass(), "Thread got interrupted.", e);
			}
			now = System.currentTimeMillis();
		}
		if (running) {
			listeners.fireEvent(EVENT_DISCOVERY_RUNNING);
		}
	}
	
	/**
	 * This method is called by the bluetooth plug-in when the
	 * plug-in stops. It unblocks any discovery thread and
	 * fires the corresponding event. 
	 */
	public synchronized void stop() {
		this.running = false;
		this.notify();
		listeners.fireEvent(EVENT_DISCOVERY_STOPPED);
	}
	
	
}
