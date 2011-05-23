package info.pppc.basex.plugin.routing.server;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.IOperator;
import info.pppc.base.system.plugin.IPluginManager;

/**
 * A minimal implementation of the plug-in manager to support
 * the use of multiplex factories.
 * 
 * @author Mac
 */
public class PluginManagerAdapter implements IPluginManager {

	/**
	 * The operator used to create threads.
	 */
	private IOperator operator;
	
	/**
	 * Creates a new plug-in manager using the specified
	 * operator.
	 * 
	 * @param operator The operator used to create threads.
	 */
	public PluginManagerAdapter(IOperator operator) {
		this.operator = operator;
	}
	
	/**
	 * Returns null in this implementation.
	 * 
	 * @param system Ignored.
	 * @return Always null.
	 */
	public DeviceDescription getDeviceDescription(SystemID system) {
		return null;
	}

	/**
	 * Returns an empty array.
	 * 
	 * @return An empty array.
	 */
	public SystemID[] getDevices() {
		return new SystemID[0];
	}

	/**
	 * Returns an empty array.
	 *
	 * @param system Ignored.
	 * @return An empty array.
	 */
	public PluginDescription[] getPluginDescriptions(SystemID system) {
		return new PluginDescription[0];
	}

	/**
	 * Executes the specified operation using the operator.
	 * 
	 * @param operation The operation to execute.
	 */
	public void performOperation(IOperation operation) {
		operator.performOperation(operation);

	}

	/**
	 * Executes the specified operation using the specified monitor.
	 * 
	 * @param operation The operation to execute.
	 * @param monitor The monitor to use.
	 */
	public void performOperation(IOperation operation, IMonitor monitor) {
		operator.performOperation(operation, monitor);
	}

}
