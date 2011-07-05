
package info.pppc.base.tutorial.serial;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class SerialProxy extends info.pppc.base.system.Proxy implements info.pppc.base.tutorial.serial.ISerial {
	
	/**
	 * Default constructor to create a new object.
	 */
	public SerialProxy() { }
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param object see info.pppc.base.tutorial.serial.ISerial
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.tutorial.serial.ISerial
	 * @see info.pppc.base.tutorial.serial.ISerial
	 */
	public void print(info.pppc.base.tutorial.serial.SerialObject object) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = object;
		String __method = "void print(info.pppc.base.tutorial.serial.SerialObject)";
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
	 * @param object see info.pppc.base.tutorial.serial.ISerial
	 * @return A future result that delivers the return value and exceptions. * @see info.pppc.base.tutorial.serial.ISerial
	 */
	public info.pppc.base.system.FutureResult printDef(info.pppc.base.tutorial.serial.SerialObject object)  {
		Object[] __args = new Object[1];
		__args[0] = object;
		String __method = "void print(info.pppc.base.tutorial.serial.SerialObject)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	/**
	 * Proxy method that creates and transfers an asynchronous call.
	 *
	 * @param object see info.pppc.base.tutorial.serial.ISerial
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.tutorial.serial.ISerial
	 * @see info.pppc.base.tutorial.serial.ISerial
	 */
	public void printAsync(info.pppc.base.tutorial.serial.SerialObject object) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = object;
		String __method = "void print(info.pppc.base.tutorial.serial.SerialObject)";
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
