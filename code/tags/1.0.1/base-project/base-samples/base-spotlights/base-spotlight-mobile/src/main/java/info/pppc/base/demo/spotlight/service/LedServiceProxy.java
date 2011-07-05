
package info.pppc.base.demo.spotlight.service;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class LedServiceProxy extends info.pppc.base.system.Proxy implements info.pppc.base.demo.spotlight.service.ILedService {
	
	/**
	 * Default constructor to create a new object.
	 */
	public LedServiceProxy() { }
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @return seeeu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public int getLedCount() throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[0];
		String __method = "int getLedCount()";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeSynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		return ((Integer)__result.getValue()).intValue();
	}
	/**
	 * Proxy method that creates and transfers a deferred synchronous invocation.
	 *
	 * @return A future result that delivers the return value and exceptions. * @see eu.peces.demo.newcastle.spot.ILedService
	 */
	public info.pppc.base.system.FutureResult getLedCountDef()  {
		Object[] __args = new Object[0];
		String __method = "int getLedCount()";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param state see eu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public void setLedState(info.pppc.base.demo.spotlight.service.LedState state) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = state;
		String __method = "void setLedState(eu.peces.demo.newcastle.spot.LedState)";
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
	 * @param state see eu.peces.demo.newcastle.spot.ILedService
	 * @return A future result that delivers the return value and exceptions. * @see eu.peces.demo.newcastle.spot.ILedService
	 */
	public info.pppc.base.system.FutureResult setLedStateDef(info.pppc.base.demo.spotlight.service.LedState state)  {
		Object[] __args = new Object[1];
		__args[0] = state;
		String __method = "void setLedState(eu.peces.demo.newcastle.spot.LedState)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	/**
	 * Proxy method that creates and transfers an asynchronous call.
	 *
	 * @param state see eu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public void setLedStateAsync(info.pppc.base.demo.spotlight.service.LedState state) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = state;
		String __method = "void setLedState(eu.peces.demo.newcastle.spot.LedState)";
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
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @return seeeu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public java.util.Vector getLedStates() throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[0];
		String __method = "java.util.Vector getLedStates()";
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
	/**
	 * Proxy method that creates and transfers a deferred synchronous invocation.
	 *
	 * @return A future result that delivers the return value and exceptions. * @see eu.peces.demo.newcastle.spot.ILedService
	 */
	public info.pppc.base.system.FutureResult getLedStatesDef()  {
		Object[] __args = new Object[0];
		String __method = "java.util.Vector getLedStates()";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	
	/**
	 * Proxy method that creates and transfers an invocation for the interface method.
	 *
	 * @param states see eu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public void setLedStates(java.util.Vector states) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = states;
		String __method = "void setLedStates(java.util.Vector)";
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
	 * @param states see eu.peces.demo.newcastle.spot.ILedService
	 * @return A future result that delivers the return value and exceptions. * @see eu.peces.demo.newcastle.spot.ILedService
	 */
	public info.pppc.base.system.FutureResult setLedStatesDef(java.util.Vector states)  {
		Object[] __args = new Object[1];
		__args[0] = states;
		String __method = "void setLedStates(java.util.Vector)";
		info.pppc.base.system.Invocation __invocation = proxyCreateSynchronous(__method, __args);
		return proxyInvokeDeferred(__invocation);
	}
	/**
	 * Proxy method that creates and transfers an asynchronous call.
	 *
	 * @param states see eu.peces.demo.newcastle.spot.ILedService
	 * @throws info.pppc.base.system.InvocationException see eu.peces.demo.newcastle.spot.ILedService
	 * @see info.pppc.base.demo.spotlight.service.ILedService
	 */
	public void setLedStatesAsync(java.util.Vector states) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = states;
		String __method = "void setLedStates(java.util.Vector)";
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
