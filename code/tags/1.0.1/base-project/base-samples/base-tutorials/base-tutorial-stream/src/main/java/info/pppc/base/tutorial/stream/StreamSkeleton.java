
package info.pppc.base.tutorial.stream;

/**
 * Do not modify this file. This class has been generated.
 * Use inheritance or composition to add functionality.
 *
 * @author 3PC Base Tools
 */
public class StreamSkeleton extends info.pppc.base.system.Skeleton  {
	
	/**
	 * Default constructor to create a new object.
	 */
	public StreamSkeleton() { }
	
	/**
	 * Dispatch method that dispatches incoming invocations to the skeleton's implementation.
	 *
	 * @param method The signature of the method to call.
	 * @param args The parameters of the method call.
	 * @return The result of the method call.
	 */
	protected info.pppc.base.system.Result dispatch(String method, Object[] args) {
		info.pppc.base.tutorial.stream.IStream impl = (info.pppc.base.tutorial.stream.IStream)getImplementation();
		try {
			if (method.equals("void connect(info.pppc.base.system.StreamDescriptor)")) {
				Object result = null;
				info.pppc.base.system.StreamDescriptor __desc = new info.pppc.base.system.StreamDescriptor();
				__desc.setData(args[1]);
				__desc.setConnector((info.pppc.base.system.plugin.IStreamConnector)args[0]);
				impl.connect(__desc);;
				return new info.pppc.base.system.Result(result, null);
			}return new info.pppc.base.system.Result(null, new info.pppc.base.system.InvocationException("Illegal signature."));
		} catch (Throwable t) {
			return new info.pppc.base.system.Result(null, t);
		}
	}
	
}
