
package info.pppc.base.tutorial.stream;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class StreamProxy extends info.pppc.base.system.Proxy implements info.pppc.base.tutorial.stream.IStream {
	
	/**
	 * Default constructor to create a new object.
	 */
	public StreamProxy() { }
	
	/**
	 * Proxy method that creates an invocation to open a stream.
	 *
	 * @param descriptor see info.pppc.base.system.IStreamHandler
	 * @throws info.pppc.base.system.InvocationException see info.pppc.base.system.IStreamHandler
	 * @see info.pppc.base.system.IStreamHandler
	 */
	public void connect(info.pppc.base.system.StreamDescriptor descriptor) throws info.pppc.base.system.InvocationException {
		Object[] __args = new Object[1];
		__args[0] = descriptor.getData();
		String __method = "void connect(info.pppc.base.system.StreamDescriptor)";
		info.pppc.base.system.Invocation __invocation = proxyCreateStream(__method, __args);
		info.pppc.base.system.Result __result = proxyInvokeSynchronous(__invocation);
		if (__result.hasException()) {
			if (__result.getException() instanceof info.pppc.base.system.InvocationException) {
				throw (info.pppc.base.system.InvocationException)__result.getException();
			}
			throw (RuntimeException)__result.getException();
		}
		descriptor.setConnector( (info.pppc.base.system.plugin.IStreamConnector)__result.getValue());
	}
	
}
