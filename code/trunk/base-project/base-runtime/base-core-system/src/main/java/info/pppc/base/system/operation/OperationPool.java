package info.pppc.base.system.operation;

import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * The operation pool can be used to limit the number of threads created
 * by the system to some fixed value. It can also be used to control the
 * default number of threads that will be created and kept for future
 * calls. This can be used to increase the performance of systems that
 * execute a large number of short running operations. 
 * 
 * @author Marcus Handte
 */
public class OperationPool implements IOperator {

	/**
	 * This thread is used by the pool to execute operations.
	 * 
	 * @author Marcus Handte
	 */
	private class OperationThread extends Thread {

		/**
		 * Creates a new operation thread.
		 */
		public OperationThread() {
			super();
		}
		
		/**
		 * Runs the thread and runs operations as long as there are
		 * any available. Deactivates the thread whenever the pool
		 * is shutdown and there are no more operations left or whenever
		 * there are too many threads (i.e., more than the default
		 * number).
		 */
		public void run() {
			operation: while (true) {
				Object[] operation = null;
				synchronized (OperationPool.this) {
					if (operations.size() > 0) {
						operation = (Object[])operations.elementAt(0);
						operations.removeElementAt(0);
					} else {
						if (defaultThreads == 0) {
							// if we do not want to keep an unused thread, stop here
							createdThreads -= 1;
							break operation;
						} else if (defaultThreads < createdThreads) {
							// if we have more threads than the default number, stop here
							createdThreads -= 1;
							break operation;
						} else {
							// keep this thread for further operations
							idleThreads += 1;
							while (operations.size() == 0 && ! shutdown) {
								try {
									OperationPool.this.wait();	
								} catch (InterruptedException e) {
									Logging.error(getClass(), "Thread got interrupted.", e);
								}
							}
							if (operations.size() == 0 && shutdown) {
								// if there are no more operations and we should shutdown, stop here
								idleThreads -= 1;
								createdThreads -= 1;
								break operation;								
							} else {
								// ignore the shutdown flag in cases where there are still operations
								idleThreads -= 1;
								operation = (Object[])operations.elementAt(0);
								operations.removeElementAt(0);
							}
						}
					}
				}
				// at this point, we have an ioperation in operation
				IOperation o = (IOperation)operation[0];
				IMonitor m = (IMonitor)operation[1];
				try {
					o.perform(m);					
				} catch (Throwable t) {
					Logging.error(getClass(), "Thread crashed.", t);
				} finally {
					m.done();
				}
				// continue with the next operation
			}
		}
	}
	
	/**
	 * The default number of threads.
	 */
	private int defaultThreads = 0;
	
	/**
	 * The maximum number of threads.
	 */
	private int maximumThreads = 0;
	
	/**
	 * The current number of idle threads.
	 */
	private int idleThreads = 0;
	
	/**
	 * The number of threads that are running at the moment.
	 */
	private int createdThreads = 0;
	
	/**
	 * A flag that indicates whether the operation pool should shutdown.
	 */
	private boolean shutdown = false;
	
	/**
	 * The operations that have not been executed so far. This vector
	 * contains object arrays of length 2. The first entry is an operation.
	 * The second entry is the monitor used by the operation. 
	 */
	private Vector operations = new Vector();
	
	/**
	 * Creates an operation pool with an unlimited number of threads
	 * and the specified number of default threads. The number of
	 * threads shrinks to the number of default threads when
	 * ever a thread finishes. 
	 * 
	 * @param defaultThreads The default number of threads that
	 * 	will be kept, set to 0 to remove all threads after an 
	 * 	operation has been executed.
	 * @throws IllegalArgumentException Thrown if the default number
	 * 	of threads is negative.
	 */
	public OperationPool(int defaultThreads) {
		this(defaultThreads, 0);
	}
	
	/**
	 * Creates an operation pool with the specified number of default
	 * threads that will be hold even if no operations must be executed
	 * and the specified number of maximum threads. If maximum threads
	 * is set to 0, the threads will not be limited. 
	 * 
	 * @param defaultThreads The number of threads that will be kept even
	 * 	if no operations need to be executed, set to 0 to remove all
	 * 	threads after they have been created.
	 * @param maximumThreads The number of threads that will be created.
	 *  Set to 0 to allow an unlimited number of new threads.
	 * @throws IllegalArgumentException Thrown if the default number or
	 * 	the maximum number of threads is negative.
	 */
	public OperationPool(int defaultThreads, int maximumThreads) {
		if (defaultThreads < 0) throw new IllegalArgumentException("Illegal number of default threads.");
		if (maximumThreads < 0) throw new IllegalArgumentException("Illegal number of maximum threads.");
		this.defaultThreads = defaultThreads;
		this.maximumThreads = maximumThreads;
	}
	
	/**
	 * Executes the specified operation using a default monitor.
	 * 
	 * @param operation The operation to execute.
	 */
	public synchronized void performOperation(IOperation operation) {
		performOperation(operation, new NullMonitor());
	}
	
	/**
	 * Executes the specified operation using the specified monitor.
	 * 
	 * @param operation The operation to execute.
	 * @param monitor The monitor used by the operation.
	 */
	public synchronized void performOperation(IOperation operation, IMonitor monitor) {
		operations.addElement(new Object[] { operation, monitor });
		// determine whether there are already enough threads or whether the limit is reached
		if (idleThreads >= operations.size() || maximumThreads == createdThreads && maximumThreads != 0) {
			notify();
			return;
		}
		// create a new thread to execute the operations
		createdThreads += 1;
		OperationThread thread = new OperationThread();
		thread.start();
	}
	
	/**
	 * Performs a shutdown on the pool. If the pool is shutdown
	 * future operations will still be performed, however, the
	 * pool semantics will change as each thread will be shutdown
	 * as soon as no more operations are available.
	 */
	public synchronized void shutdown() {
		shutdown = true;
		notifyAll();
	}

}
