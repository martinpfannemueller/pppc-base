package info.pppc.base.system.operation;

import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * The multi operation is a helper class that enables a user to 
 * start multiple operations and synchronize on their completion.
 * 
 * @author Marcus Handte
 */
public class MultiOperation {

	/**
	 * The operations that should be executed. Each element in the
	 * vector is a triple that contains the operation to start,
	 * a monitor used to synchronize on the operation and a boolean
	 * flag that indicates whether the operation has been executed
	 * already.
	 */
	protected Vector operations = new Vector();

	/**
	 * The operator used to execute the operations.
	 */
	protected IOperator operator;
	
	/**
	 * Creates a new multi operation without any operations. This
	 * multi operation starts operation using the specified operator.
	 * 
	 * @param operator The operator used to execute the operations.
	 */
	public MultiOperation(IOperator operator) {
		if (operator == null) throw new NullPointerException("Operator must not be null.");
		this.operator = operator;
	}
	
	/**
	 * Adds the specified operation to the set of operations that should
	 * be executed.
	 * 
	 * @param operation The operation to execute.
	 */
	public void addOperation(IOperation operation) {
		Object[] entry = new Object[] { operation, new NullMonitor(), new Boolean(false) };
		operations.addElement(entry);
	}
	
	/**
	 * Removes the specified operation from the set of operations that
	 * should be executed.
	 * 
	 * @param operation The operation to execute.
	 * @return True if the operation has been removed, false otherwise.
	 */
	public boolean removeOperation(IOperation operation) {
		for (int i = 0; i < operations.size(); i++) {
			Object[] entry = (Object[])operations.elementAt(i);
			if (entry[0].equals(operation)) {
				operations.removeElementAt(i);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the operations that are contained in the multi operation. 
	 * 
	 * @return The operations of the multi operation.
	 */
	public IOperation[] getOperations() {
		IOperation[] result = new IOperation[operations.size()];
		for (int i = 0; i < operations.size(); i++) {
			Object[] entry = (Object[])operations.elementAt(i);
			result[i] = (IOperation)entry[0];
		}
		return result;
	}
	
	/**
	 * Performs the operations that have not been executed already an
	 * returns immediately.
	 */
	public void performAsynchronous() {
		for (int i = 0; i < operations.size(); i++) {
			Object[] entry = (Object[])operations.elementAt(i);
			Boolean running = (Boolean)entry[2];
			if (! running.booleanValue()) {
				entry[2] = new Boolean(true);
				operator.performOperation((IOperation)entry[0], (IMonitor)entry[1]);
			}
		}			
	}
	
	/**
	 * Performs the operations that have not been executed already and
	 * synchronizes on their end. Calling this method is more efficient
	 * than calling performAsynchronous and synchronizing on the end
	 * since this method will reuse the calling thread to perform
	 * one operation.
	 */
	public void performSynchronous() {
		// the operation that will be performed synchronously
		IOperation operation = null;
		IMonitor monitor = null;
		// find the synchronous operation and start all asynchronous operations
		for (int i = 0; i < operations.size(); i++) {
			Object[] entry = (Object[])operations.elementAt(i);
			Boolean running = (Boolean)entry[2];
			if (! running.booleanValue()) {
				entry[2] = new Boolean(true);
				if (operation == null) {
					operation = (IOperation)entry[0];
					monitor = (IMonitor)entry[1];
				} else {
					operator.performOperation((IOperation)entry[0], (IMonitor)entry[1]);
				}
			}
		}
		// start the operation that is synchronous
		if (operation != null) {
			try {
				operation.perform(monitor);
			} catch (Throwable t) {
				Logging.error(getClass(), "Operation crashed.", t);
			} finally {
				monitor.done();
			}
		}
		// wait on all threads
		synchronize();
	}
	
	/**
	 * Synchronizes on all operations that are currently executing
	 * and waits until they are finished.
	 */
	public void synchronize() {
		for (int i = 0; i < operations.size(); i++) {
			Object[] entry = (Object[])operations.elementAt(i);
			Boolean running = (Boolean)entry[2];
			if (running.booleanValue()) {
				NullMonitor monitor = (NullMonitor)entry[1];
				synchronized (monitor) {
					while (! monitor.isDone()) {
						try {
							monitor.join();
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Thread got interrupted.", e);
						}					
					}					
				}
			}
		}
	}
	
}
