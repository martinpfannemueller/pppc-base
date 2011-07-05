/**
 * 
 */
package info.pppc.basex.plugin.transceiver.bt;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.transceiver.MxBluetoothTransceiver;

/**
 * The manual discovery strategy implements a bluetooth discovery
 * strategy that can be initiated manually (e.g. by a user interface).
 * To initiate a discovery operation, the user must call the run
 * method. The status of the discovery operation will be signaled
 * asynchronously through the listener interface. That way it is
 * possible to track the state of the discovery which can be used
 * to provide feedback (e.g. by disabling a button that runs the
 * discovery). 
 * 
 * @author Mac
 */
public class ManualDiscoveryStrategy implements MxBluetoothTransceiver.IDiscoveryStrategy {

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
	 * A flag that indicates whether the next call to the
	 * execute method will be blocked.
	 */
	private boolean block = false;
	
	/**
	 * A flag that indicates whether the underlying plug-in
	 * is running.
	 */
	private boolean running = false;
	
	/**
	 * Creates a new manual discovery strategy.
	 */
	public ManualDiscoveryStrategy() {
		super();
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
	 * This method must be called externally to run a discovery
	 * operation.
	 */
	public synchronized void run() {
		block = false;
		notify();
	}
	

	/**
	 * This method is called by the bluetooth plug-in upon
	 * startup. It fires the corresponding event to all listener.
	 */
	public synchronized void start() {
		this.block = true;
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
		while (running && block) {
			try {
				wait();	
			} catch (InterruptedException e) {
				Logging.error(getClass(), "Thread got interrupted.", e);
			}
		}
		if (running) {
			listeners.fireEvent(EVENT_DISCOVERY_RUNNING);
			block = true;
		}
	}
	
	/**
	 * This method is called by the bluetooth plug-in when the
	 * plug-in stops. It unblocks any discovery thread and
	 * fires the corresponding event. 
	 */
	public synchronized void stop() {
		this.block = false;
		this.running = false;
		this.notify();
		listeners.fireEvent(EVENT_DISCOVERY_STOPPED);
	}
	
	
}
