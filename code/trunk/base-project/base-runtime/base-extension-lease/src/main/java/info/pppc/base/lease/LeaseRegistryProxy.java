
package info.pppc.base.lease;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class LeaseRegistryProxy extends info.pppc.base.system.Proxy implements info.pppc.base.lease.ILeaseRegistry {
	
	/**
	 * Default constructor to create a new object.
	 */
	public LeaseRegistryProxy() { }
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param system see info.pppc.base.lease.ILeaseRegistry
	 * @param lease see info.pppc.base.lease.ILeaseRegistry
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.lease.ILeaseRegistry
	 * @see info.pppc.base.lease.ILeaseRegistry
	 */
	public void remove(info.pppc.base.system.SystemID system, info.pppc.base.lease.Lease lease) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[2];
		__args[0] = system;
		__args[1] = lease;
		String __method = "void remove(info.pppc.base.system.SystemID, info.pppc.base.lease.Lease)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeSynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		return ;
	}
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param system see info.pppc.base.lease.ILeaseRegistry
	 * @param lease see info.pppc.base.lease.ILeaseRegistry
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.lease.ILeaseRegistry
	 * @see info.pppc.base.lease.ILeaseRegistry
	 */
	public void unhook(info.pppc.base.system.SystemID system, info.pppc.base.lease.Lease lease) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[2];
		__args[0] = system;
		__args[1] = lease;
		String __method = "void unhook(info.pppc.base.system.SystemID, info.pppc.base.lease.Lease)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeSynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		return ;
	}
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param system see info.pppc.base.lease.ILeaseRegistry
	 * @param leases see info.pppc.base.lease.ILeaseRegistry
	 * @return seeinfo.pppc.base.lease.ILeaseRegistry
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.lease.ILeaseRegistry
	 * @see info.pppc.base.lease.ILeaseRegistry
	 */
	public java.util.Vector update(info.pppc.base.system.SystemID system, java.util.Vector leases) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[2];
		__args[0] = system;
		__args[1] = leases;
		String __method = "java.util.Vector update(info.pppc.base.system.SystemID, java.util.Vector)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeSynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		return (java.util.Vector)__result.getValue();
	}
	
}
