package info.pppc.base.system.operation;


/**
 * The operator is an interface that is used to signal that a certain
 * entity is capable of executing operations.
 * 
 * @author Marcus Handte
 */
public interface IOperator {

	/**
	 * Performs the specified operation asynchronously using some monitor.
	 * 
	 * @param operation The operation to perform.
	 */
	public void performOperation(IOperation operation);
	
	/**
	 * Performs the specified operation asynchronously using the specified
	 * monitor.
	 * 
	 * @param operation The operation that should be performed.
	 * @param monitor The monitor to perform the operation.
	 */
	public void performOperation(IOperation operation, IMonitor monitor);
}
