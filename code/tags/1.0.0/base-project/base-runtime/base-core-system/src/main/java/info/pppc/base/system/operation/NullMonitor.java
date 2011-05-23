package info.pppc.base.system.operation;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

/**
 * The null monitor implements a simple monitor that can be canceled.
 * 
 * @author Marcus Handte
 */
public class NullMonitor implements IMonitor {

	/**
	 * A flag that indicates whether the operation is
	 * already done.
	 */	
	private boolean done = false;

	/**
	 * A flag that indicates whether the operation should be canceled.
	 * The meaning of this flag is undefined if the operation is no
	 * longer running.
	 */
	private boolean canceled = false;

	/**
	 * The name of operation as set by the operation that is executed.
	 */
	private String name = null;

	/**
	 * The total number of steps or STEP_UNKNOWN if it is not known.
	 */
	private int total = STEP_UNKNOWN;
	
	/**
	 * The number of steps that have been performed already.
	 */
	private int step = 0;

	/**
	 * The listener bundle that contains the monitor listeners.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * Creates a new null monitor that can be used to observe a task.
	 */
	public NullMonitor() {
		super();
	}
	
	/**
	 * Adds the specified listener for the specified set of events.
	 * 
	 * @param types The types of events to register.
	 * @param listener The listener to register.
	 */
	public void addMonitorListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}

	/**
	 * Removes a previously registered listener from the set of
	 * registered listeners.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been unregistered.
	 */
	public boolean removeMonitorListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Returns the name of the current operation.
	 * 
	 * @return The name of the current operation.
	 */
	public synchronized String getName() {
		return name;
	}

	/**
	 * Sets the name of the operation that is currently executed.
	 * 
	 * @param operation The name of the operation that is currently
	 * 	executed.
	 */
	public synchronized void setName(String operation) {
		name = operation;
		listeners.fireEvent(EVENT_MONITOR_NAME, operation);
	}

	/**
	 * Starts the operation and sets the total number of steps performed
	 * by the operation.
	 * 
	 * @param total The total number of steps performed by the operation.
	 */
	public synchronized void start(int total) {
		this.total = total;
		listeners.fireEvent(EVENT_MONITOR_START, new Integer(total));
	}
	
	/**
	 * Returns the total number of steps performed by the current operation.
	 * 
	 * @return The total number of steps.
	 */
	public synchronized int getTotal() {
		return total;
	}

	/**
	 * Informs the monitor that the specified number of steps have been
	 * performed.
	 * 
	 * @param steps The number of steps that have been performed.
	 */
	public synchronized void step(int steps) {
		step += steps;
		listeners.fireEvent(EVENT_MONITOR_STEP, new Integer(steps));
	}

	/**
	 * Returns the current step of the operation.
	 * 
	 * @return The current step of the operation.
	 */
	public synchronized int getStep() {
		return step;
	}


	/**
	 * Called to signal that the operation has been finished.
	 */
	public synchronized void done() {
		if (! done) {
			done = true;
			listeners.fireEvent(EVENT_MONITOR_DONE);
			notifyAll();
			
		}			
	}

	/**
	 * Determines whether the operation is still running. Note that
	 * while an operation is running, the state of the success flag
	 * is not relevant.
	 * 
	 * @return True if the operation is still running.
	 */
	public synchronized boolean isDone() {
		return done;
	}

	/**
	 * Called to signal that the operation has been canceled.
	 */
	public synchronized void cancel() {
		if (! canceled) {
			canceled = true;
			notifyAll();
			listeners.fireEvent(EVENT_MONITOR_CANCEL);			
		}
	}

	/**
	 * Determines whether the task should be canceled.
	 * 
	 * @return True if the task should be canceled, false otherwise.
	 */
	public synchronized boolean isCanceled() {
		return canceled;
	}
	
	/**
	 * Waits until the operation that is connected to the monitor
	 * has been completed.
	 * 
	 * @throws InterruptedException If the thread is interrupted 
	 * 	while waiting for the join.
	 */
	public synchronized void join() throws InterruptedException {
		while (! done) {
			wait();
		}
	}
	
}
