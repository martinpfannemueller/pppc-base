
package info.pppc.base.tutorial.rmi;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class RmiProxy extends info.pppc.base.system.Proxy implements info.pppc.base.tutorial.rmi.IRmi {
	
	/**
	 * Default constructor to create a new object.
	 */
	public RmiProxy() { }
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param string see info.pppc.base.tutorial.rmi.IRmi
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.tutorial.rmi.IRmi
	 * @see info.pppc.base.tutorial.rmi.IRmi
	 */
	public void println(java.lang.String string) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = string;
		String __method = "void println(java.lang.String)";
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
	 * Proxy method that creates and transfers a deferred synchronous invocation.
	 *
	 * @param string see info.pppc.base.tutorial.rmi.IRmi
	 * @return A future result that delivers the return value and exceptions. * @see info.pppc.base.tutorial.rmi.IRmi
	 */
	public info.pppc.base.system.FutureResult printlnDef(java.lang.String string)  {
		Object[] __args = new Object[1];
		__args[0] = string;
		String __method = "void println(java.lang.String)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	/**
	 * Proxy method that creates and transfers an asynchronous call.
	 *
	 * @param string see info.pppc.base.tutorial.rmi.IRmi
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.tutorial.rmi.IRmi
	 * @see info.pppc.base.tutorial.rmi.IRmi
	 */
	public void printlnAsync(java.lang.String string) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = string;
		String __method = "void println(java.lang.String)";
		info.pppc.base.system.Invocation __invocation = proxyCreateAsynchronous(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeAsynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		return ;
	}
	
}
