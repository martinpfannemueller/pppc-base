package info.pppc.base.system.operation;

import info.pppc.base.system.event.IListener;

/**
 * A monitor is passed to an operation to signal the current status to
 * a user interface or to some other external observing entity. For operations
 * that are not observed, the framework provides a default null monitor that simply
 * observes an operation. 
 * 
 * @author Marcus Handte
 */
public interface IMonitor {

	/**
	 * This constant is used to represent an event that signals 
	 * a call to the start method. The source of the event will be 
	 * the monitor, the data object will contain the integer that 
	 * denotes the number of steps of the task.
	 */
	public static final int EVENT_MONITOR_START = 1;
	
	/**
	 * This constant is used to represent an event that signals
	 * a call to the stop method. The source of the event will be
	 * the monitor, the data object will contain the integer that
	 * denotes the number of steps that has been performed.
	 */
	public static final int EVENT_MONITOR_STEP = 2;
	
	/**
	 * This constant is used to represent an event that signals 
	 * a call to the set name method. The source of the event will
	 * be the monitor, the data object will contain the string
	 * that denotes the new name of the operation.
	 */
	public static final int EVENT_MONITOR_NAME = 4;
	
	/**
	 * This constant is used to represent an event that signals
	 * that the operation monitored by this monitor has called 
	 * the done method. The source of the event will be the 
	 * monitor, the data object will be null.
	 */
	public static final int EVENT_MONITOR_DONE = 8;

	/**
	 * This constant is used to represent an event that signals
	 * that the operation should be canceled. The source of the
	 * event will be the monitor, the data object will be null.
	 */
	public static final int EVENT_MONITOR_CANCEL = 16;

	/**
	 * This constant should be used if the total number of steps 
	 * performed by the operation is not known.
	 */
	public static final int STEP_UNKNOWN = 0;

	/**
	 * Adds the specified listener to the set of listeners that need
	 * to be informed whenever the state of the monitor changes. The
	 * monitor must at least support the event constants defined by
	 * this interface with the corresponding source and data objects.
	 * 
	 * @param types The types of events to register for. The types
	 * 	supported depend on the monitor implementation. However,
	 * 	each monitor must implement at least the event constants 
	 * 	defined by this interface.
	 * @param listener The listener that listens to changes.
	 */
	public void addMonitorListener(int types, IListener listener);
	
	/**
	 * Removes the specified listener from the set of registered
	 * listeners for the specified types of events.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been unregistered, false
	 * 	otherwise.
	 */
	public boolean removeMonitorListener(int types, IListener listener);

	/**
	 * Sets the name of the current operation.
	 * 
	 * @param operation The name of the current operation.
	 */
	public void setName(String operation);

	/**
	 * Begins an operation that has the specified total number of
	 * steps to perform.
	 * 
	 * @param total The total number of steps to perform.
	 */
	public void start(int total);
	
	/**
	 * Informs the monitor that the specified number of steps has
	 * been performed.
	 * 
	 * @param steps The number of steps that has been performed.
	 */
	public void step(int steps);
	
	/**
	 * Informs the monitor that the operation has finished.
	 */
	public void done();
		
	/**
	 * Determines whether the progress monitor assumes that the 
	 * operation is still running.
	 * 
	 * @return True if the progress monitor assumes that the operation
	 * 	is still running, false otherwise.
	 */
	public boolean isDone();

	/**
	 * Sets the canceled state of the monitor to true. After a call
	 * to this method returns, the monitor state must return true
	 * for canceled.
	 */
	public void cancel();

	/**
	 * Queries the monitor to determine whether the operation has 
	 * been canceled. Note that this operation is not the opposite
	 * of running. An operation might be canceled although it is 
	 * still running.
	 * 
	 * @return True if the operation has been canceled, false if the
	 * 	operation should be performed.
	 */	
	public boolean isCanceled();

	/**
	 * Waits until the monitor's state is set to done or an the
	 * thread is interrupted.
	 * 
	 * @throws InterruptedException Thrown if the thread is 
	 * 	interrupted.
	 */
	public void join() throws InterruptedException;

}
