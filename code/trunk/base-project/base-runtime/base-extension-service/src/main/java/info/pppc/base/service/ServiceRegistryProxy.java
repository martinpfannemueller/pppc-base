
package info.pppc.base.service;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class ServiceRegistryProxy extends info.pppc.base.system.Proxy implements info.pppc.base.service.IServiceRegistry {
	
	/**
	 * Default constructor to create a new object.
	 */
	public ServiceRegistryProxy() { }
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param name see info.pppc.base.service.IServiceRegistry
	 * @param interfaces see info.pppc.base.service.IServiceRegistry
	 * @param properties see info.pppc.base.service.IServiceRegistry
	 * @param scope see info.pppc.base.service.IServiceRegistry
	 * @return seeinfo.pppc.base.service.IServiceRegistry
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.service.IServiceRegistry
	 * @see info.pppc.base.service.IServiceRegistry
	 */
	public java.util.Vector lookup(java.lang.String name, java.lang.String[] interfaces, info.pppc.base.service.ServiceProperties properties, int scope) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[4];
		__args[0] = name;
		__args[1] = interfaces;
		__args[2] = properties;
		__args[3] = new Integer(scope);
		String __method = "java.util.Vector lookup(java.lang.String, java.lang.String[], info.pppc.base.service.ServiceProperties, int)";
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
