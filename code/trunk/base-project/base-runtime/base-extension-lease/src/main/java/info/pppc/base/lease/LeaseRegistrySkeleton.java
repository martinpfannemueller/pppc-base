
package info.pppc.base.lease;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class LeaseRegistrySkeleton extends info.pppc.base.system.Skeleton  {
	
	/**
	 * Default constructor to create a new object.
	 */
	public LeaseRegistrySkeleton() { }
	
	/**
	 * Dispatch method that dispatches incoming invocations to the skeleton's implementation.
	 *
	 * @param method The signature of the method to call.
	 * @param args The parameters of the method call.
	 * @return The result of the method call.
	 */
	protected info.pppc.base.system.Result dispatch(String method, Object[] args) {
		info.pppc.base.lease.ILeaseRegistry impl = (info.pppc.base.lease.ILeaseRegistry)getImplementation();
		try {
			if (method.equals("void remove(info.pppc.base.system.SystemID, info.pppc.base.lease.Lease)")) {
				Object result = null;
				impl.remove((info.pppc.base.system.SystemID)args[0], (info.pppc.base.lease.Lease)args[1]);
				return new info.pppc.base.system.Result(result, null);
			}
			else if (method.equals("void unhook(info.pppc.base.system.SystemID, info.pppc.base.lease.Lease)")) {
				Object result = null;
				impl.unhook((info.pppc.base.system.SystemID)args[0], (info.pppc.base.lease.Lease)args[1]);
				return new info.pppc.base.system.Result(result, null);
			}
			else if (method.equals("java.util.Vector update(info.pppc.base.system.SystemID, java.util.Vector)")) {
				Object result = impl.update((info.pppc.base.system.SystemID)args[0], (java.util.Vector)args[1]);
				return new info.pppc.base.system.Result(result, null);
			}return new info.pppc.base.system.Result(null, new info.pppc.base.system.InvocationException("Illegal signature."));
		} catch (Throwable t) {
			return new info.pppc.base.system.Result(null, t);
		}
	}
	
}
