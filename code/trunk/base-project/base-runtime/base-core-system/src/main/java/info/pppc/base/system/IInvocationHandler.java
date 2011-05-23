package info.pppc.base.system;


/**
 * An invocation handler is something that is capable of handling
 * invocations. This interface is implemented by skeletons and other
 * objects that support remote interaction.
 * 
 * @author Marcus Handte
 */
public interface IInvocationHandler {
	
	/**
	 * Processes the specified invocation.
	 * 
	 * @param invocation The invocation to process.
	 * @param session The session used to receive the invocation.
	 */
	public void invoke(Invocation invocation, ISession session);

}
