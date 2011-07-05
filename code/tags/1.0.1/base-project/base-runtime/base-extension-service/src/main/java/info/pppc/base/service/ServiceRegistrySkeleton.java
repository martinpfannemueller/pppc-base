
package info.pppc.base.service;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class ServiceRegistrySkeleton extends info.pppc.base.system.Skeleton  {
	
	/**
	 * Default constructor to create a new object.
	 */
	public ServiceRegistrySkeleton() { }
	
	/**
	 * Dispatch method that dispatches incoming invocations to the skeleton's implementation.
	 *
	 * @param method The signature of the method to call.
	 * @param args The parameters of the method call.
	 * @return The result of the method call.
	 */
	protected info.pppc.base.system.Result dispatch(String method, Object[] args) {
		info.pppc.base.service.IServiceRegistry impl = (info.pppc.base.service.IServiceRegistry)getImplementation();
		try {
			if (method.equals("java.util.Vector lookup(java.lang.String, java.lang.String[], info.pppc.base.service.ServiceProperties, int)")) {
				Object result = impl.lookup((java.lang.String)args[0], (java.lang.String[])args[1], (info.pppc.base.service.ServiceProperties)args[2], ((Integer)args[3]).intValue());
				return new info.pppc.base.system.Result(result, null);
			}return new info.pppc.base.system.Result(null, new info.pppc.base.system.InvocationException("Illegal signature."));
		} catch (Throwable t) {
			return new info.pppc.base.system.Result(null, t);
		}
	}
	
}
