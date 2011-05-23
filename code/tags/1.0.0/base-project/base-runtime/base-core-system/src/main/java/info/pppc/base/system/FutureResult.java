package info.pppc.base.system;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.util.Logging;

/**
 * The future service result is the result that 
 * is returned in response to deferred synchronous
 * calls.
 * 
 * @author Marcus Handte
 */
public final class FutureResult {
	
	/**
	 * This event is raised once the result becomes available.
	 * The data object will be the result, the source will be
	 * the future result.
	 */
	public static final int EVENT_RESULT_AVAILABLE = 1;
	
	/**
	 * Result of the remote call
	 */
	protected Result result;
	
	/**
	 * Flag to determine the result status
	 */
	protected boolean returned = false;
	
	/**
	 * A listener that is notified when the result becomes available.
	 */
	protected IListener listener;
	
	/**
	 * Creates a new future result.
	 */
	public FutureResult() {	}
	
	
	/**
	 * Sets a listener to the result and returns whether the
	 * result is already available.
	 * 
	 * @param listener The listener that will be notified once
	 * 	the result gets available.
	 */
	public synchronized void setListener(IListener listener) {
		this.listener = listener;
		if (returned) {
			try {
				if (listener != null) listener.handleEvent
					(new Event(EVENT_RESULT_AVAILABLE, this, result, false));	
			} catch (Throwable t) {
				Logging.error(getClass(), "Exception in listener.", t);
			}
		}
	}
	
	/**
	 * Method to set the real result of the remote call. Notifies
	 * the waiting application on the arrival of the result.
	 * 
	 * @param result The service result of the call.
	 */
	public synchronized void setResult(Result result) {
		this.result = result;
		returned = true;
		notify();
		try {
			if (listener != null) listener.handleEvent
				(new Event(EVENT_RESULT_AVAILABLE, this, result, false));	
		} catch (Throwable t) {
			Logging.error(getClass(), "Exception in listener.", t);
		}
	}
	
	/**
	 * Method to access the result of the remote call. If called
	 * it waits for the result to be delivered and blocks the
	 * calling thread.
	 * 
	 * @return ServiceResult the result of the remote call
	 */
	public synchronized Result getResult() {
		while (!returned) {
			try {
				 wait();
			} catch (InterruptedException e) {
				Logging.error(getClass(), 
					"Interrupted while waiting for service result.", e);
			}			
		}
		return result;
	}

	/**
	 * Determines whether the result is already available. If this
	 * method returns true, getting the result will not block.
	 * 
	 * @return True if the result is available, false otherwise.
	 */
	public synchronized boolean isAvailable() {
		return returned;
	}

}
