
package info.pppc.base.demo.spotlight.service;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class LedServiceSkeleton extends info.pppc.base.system.Skeleton  {
	
	/**
	 * Default constructor to create a new object.
	 */
	public LedServiceSkeleton() { }
	
	/**
	 * Dispatch method that dispatches incoming invocations to the skeleton's implementation.
	 *
	 * @param method The signature of the method to call.
	 * @param args The parameters of the method call.
	 * @return The result of the method call.
	 */
	protected info.pppc.base.system.Result dispatch(String method, Object[] args) {
		info.pppc.base.demo.spotlight.service.ILedService impl = (info.pppc.base.demo.spotlight.service.ILedService)getImplementation();
		try {
			if (method.equals("int getLedCount()")) {
				Object result = new Integer(impl.getLedCount());
				return new info.pppc.base.system.Result(result, null);
			}
			else if (method.equals("void setLedState(eu.peces.demo.newcastle.spot.LedState)")) {
				Object result = null;
				impl.setLedState((info.pppc.base.demo.spotlight.service.LedState)args[0]);
				return new info.pppc.base.system.Result(result, null);
			}
			else if (method.equals("java.util.Vector getLedStates()")) {
				Object result = impl.getLedStates();
				return new info.pppc.base.system.Result(result, null);
			}
			else if (method.equals("void setLedStates(java.util.Vector)")) {
				Object result = null;
				impl.setLedStates((java.util.Vector)args[0]);
				return new info.pppc.base.system.Result(result, null);
			}return new info.pppc.base.system.Result(null, new info.pppc.base.system.InvocationException("Illegal signature."));
		} catch (Throwable t) {
			return new info.pppc.base.system.Result(null, t);
		}
	}
	
}
