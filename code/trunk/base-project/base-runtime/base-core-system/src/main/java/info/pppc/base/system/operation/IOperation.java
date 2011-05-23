package info.pppc.base.system.operation;

/**
 * An operation is a special form of executable that is used by BASE.
 * This is similar to a runnable, however, it takes a monitor as input
 * that enables other threads to cancel the operation.
 * 
 * @author Marcus Handte
 */
public interface IOperation {

	/**
	 * Performs the operation. The operation should update the monitor
	 * and signal its progress to the monitor. A long-running operation
	 * should check the cancel flag from time to time. After an operation
	 * completes it must call the cancel or finish method at the progress
	 * monitor. If the progress monitor indicates that the 
	 * 
	 * @param monitor An monitor that observes the performance of
	 * 	the operation.
	 * @throws Exception An exception that can be thrown while 
	 * 	the operation is performed.
	 */
	public void perform(IMonitor monitor) throws Exception;

}
