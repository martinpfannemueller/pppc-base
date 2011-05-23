package info.pppc.base.service;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

/**
 * This is the abstract base class of all services. A service that
 * wants to use threading should use the perform operation methods on the
 * broker. In order to release the threads, the service should listen to
 * shutdown events of the broker. In order to allow base to shutdown its 
 * communication, a service declares its current state by means of an
 * active flag. If a service performs communication it must set its flag to 
 * running, if it sits on the device and waits for requests, it should set 
 * its state flag to idle, in order to enable the system to shut down 
 * communication in order to safe energy.
 * 
 * For incoming calls this state change is performed automatically
 * by the system, i.e. while a call is performed by the service,
 * base will assume that the state of the service is active. Thus,
 * a service that simply responds to incoming calls can set its
 * state permanently to passive, whereas a service that initiates
 * calls from time to time should set its state to active before
 * it tries to perform remote communication. Otherwise the 
 * communication might fail.
 * 
 * @author Marcus Handte
 */
public abstract class Service {

	/**
	 * This is the event constant that is used to signal that a
	 * certain service is now activated. The data object of the
	 * event will be null. The source will be the service that
	 * has been activated.
	 */
	public static final int EVENT_SERVICE_ACTIVATED = 1;
	
	/**
	 * This is the event constant that is used to signal that a
	 * certain service is now passivated. The data object of the
	 * event will be null. The source will be the service that
	 * has been passivated.
	 */
	public static final int EVENT_SERVICE_PASSIVATED = 2;

	/**
	 * Listeners that are registered for state changes of the service.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The current state of the service. A service can either be 
	 * active or passive. A passive service must not initiate
	 * communication before it turns its state to active.
	 */
	private boolean active = true;

	/**
	 * Creates a new abstract service that starts in the active state.
	 */
	public Service() {
		super();
	}

	/**
	 * Adds a listener that listens to state changes of this service.
	 * 
	 * @param types The types of events to register. At the present 
	 * 	time a service emits EVENT_SERVICE_ACTIVATED and 
	 * 	EVENT_SERVICE_PASSIVATED events to signal that a service 
	 * 	requires communication.
	 * @param listener The listener to register.
	 */
	public void addServiceListener(int types, IListener listener) {
		listeners.addListener(types, listener);	
	}
	
	/**
	 * Removes a previously registered listener for a set of events.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed.
	 */
	public boolean removeServiceListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Sets the state of the service and informs all listeners if
	 * a state change has been performed. If the new and the old
	 * state are the same, this method will not perform anything.
	 * 
	 * @param active True to set the state of the service to active,
	 * 	false to set the state of the service to passive.
	 */
	protected void setActive(boolean active) {
		if (active != this.active) {
			this.active = active;
			if (active) {
				listeners.fireEvent(EVENT_SERVICE_ACTIVATED);
			} else {
				listeners.fireEvent(EVENT_SERVICE_PASSIVATED);
			}
		}
	}

	/**
	 * Returns the current state of the service. This can either
	 * be active, if the service requires communication or passive
	 * if the service does not require communication.
	 * 
	 * @return The current state of the service, true if the state
	 * 	is active, false otherwise.
	 */
	public boolean isActive() {
		return active;
	}

}
