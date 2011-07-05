package info.pppc.base.system;


/**
 * The superclass for all skeletons. A skeleton keeps a reference
 * to an implementation that can be used to dispatch calls. An
 * invocation that is forwarded by the invocation broker to a 
 * skeleton will first be sent to the invoke method. The
 * invoke method then forwards the signature and the parameters
 * of the invocation as well as the current implementation to
 * the dispatch method. The dispatch method should then handle
 * signatures and parameters and return a result object that
 * encapsulates possible return values and exceptions.
 * 
 * @author Marcus Handte
 */
public abstract class Skeleton implements IInvocationHandler {

	/**
	 * The implementation of this skeleton.
	 */
	private Object implementation;

	/**
	 * Creates a new skeleton.
	 */
	public Skeleton() {
		super();
    }

	/**
	 * Sets the implementation of this skeleton.
	 * 
	 * @param implementation The implementation of this skeleton.
	 */
    public synchronized void setImplementation(Object implementation) {
		this.implementation = implementation;
    }

	/**
	 * Returns the implementation that is associated with this skeleton.
	 * 
	 * @return The implementation of this skeleton.
	 */
	public synchronized Object getImplementation() {
		return implementation;
	}

	/**
	 * Handles an invocation. The skeleton handles an invocation by dispatching
	 * it to its implementation. 
	 * 
	 * @param msg The message to forward and dispatch.
	 * @param session The session used to receive the invocation.
	 */
	public void invoke(Invocation msg, ISession session) {
		Result result = dispatch(msg.getSignature(), msg.getArguments());
		msg.setResult(result.getValue());
		msg.setException(result.getException());
	}
	
	/**
	 * Dispatches the specified method call to the specified implementation. 
	 * 
	 * @param signature The signature of the method call.
	 * @param p The parameters of the method call.
	 * @return The result object that contains the results of the dispatch.
	 */
    protected abstract Result dispatch(String signature, Object[] p);

}
