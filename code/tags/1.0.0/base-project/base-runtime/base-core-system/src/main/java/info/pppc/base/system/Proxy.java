package info.pppc.base.system;

import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;

/**
 * The superclass for all proxies. The proxy synchronizes access to the
 * source and the target of invocations on itself. Users can synchronize
 * on the proxy in order to perform an atomic set or get on both, the
 * target and the source of the proxy. The methods that execute an 
 * invocation are deliberately unsynchronized. The proxy offers methods
 * to create default invocations for remote method calls. At the present
 * time the proxy enables the transfer of asynchronous, synchronous and
 * deferred synchronous method calls.
 * 
 * Note that all methods of the proxy have a prefix called "proxy". Thus,
 * user code should not use base as a prefix for methods in order to 
 * avoid naming conflicts with user interfaces.
 * 
 * @author Marcus Handte
 */
public abstract class Proxy {

	/**
	 * The reference to the target.
	 */
	private ReferenceID target;
	
	/**
	 * The reference to the source.
	 */
	private ReferenceID source;

	/**
	 * A boolean flag to determine whether the proxy should use gateways.
	 */
	private boolean gateway;
	
	/**
	 * A set of plugin descriptions of the remote device that will
	 * be installed at the device registry during the call.
	 */
	private PluginDescription[] plugins;
	
	/**
	 * Creates a new proxy.
	 */
	public Proxy() {
		super();
    }
    
	/**
	 * Returns a flag to determine whether the calls made
	 * by the proxy should use remote gateways.
	 * 
	 * @return True if remote gateways are used.
	 */
	public boolean isGateway() {
		return gateway;
	}
	
	/**
	 * Sets a flag to determine whether the calls made by
	 * the proxy should use remote gateways
	 * 
	 * @param gateway The gateway toggle flag, true to use gateways.
	 */
	public void setGateway(boolean gateway) {
		this.gateway = gateway;
	}
	
    /**
     * Sets the object id of the target object.
     * 
     * @param id The object id of the target object.
     */
    public void setTargetID(ReferenceID id) {
        target = id;
    }
    
    /**
     * Sets the object id of the source system.
 	 *
     * @param id The object id of the source system.
     */
    public void setSourceID(ReferenceID id) {
    	source = id;
    }
    
    /**
     * Returns the object id of the target object.
     * 
     * @return ObjectID The object id of the target object.
     */
    public ReferenceID getTargetID(){
        return target;
    }
    
    /**
     * Returns the object id of the source object.
     * 
     * @return ObjectID The object id of the source object.
     */
    public ReferenceID getSourceID() {
        return source;
	}

    /**
     * Gets the plug-ins that will be installed automatically
     * during calls.
     * 
     * @return The plug-in descriptions to be set during calls.
     */
	public PluginDescription[] getPlugins() {
		return plugins;
	}

	/**
	 * Set the plug-in descriptions that will be installed 
	 * during calls.
	 * 
	 * @param plugins The plug-in descriptions.
	 */
	public void setPlugins(PluginDescription[] plugins) {
		this.plugins = plugins;
	}
    
	/**
	 * Sends an invocation as a deferred synchronous method call.
	 * 
	 * @param invocation The invocation to send.
	 * @return The future result used to synchronize on the 
	 * 	invocation.
	 */
	protected FutureResult proxyInvokeDeferred(final Invocation invocation) {
		FutureResult result = null;
		if (invocation.getSource() == null || invocation.getTarget() == null) {
			result = new FutureResult();
			invocation.setResult(null);
			invocation.setException(new InvocationException("Reference is null."));
			result.setResult(new Result(invocation.getResult(), invocation.getException()));
		} else {
			final InvocationBroker broker = InvocationBroker.getInstance();
			final DeviceRegistry registry = broker.getDeviceRegistry();
			final PluginDescription[] p = plugins;
			final SystemID system = invocation.getTarget().getSystem();
			result = new FutureResult();
			if (p != null && system != null) {
				for (int j = 0; j < p.length; j++) {
					registry.registerPlugin(system, p[j]);	
				}
					
			}
			final FutureResult future = result;
			IOperation performer = new IOperation() {
				public void perform(IMonitor monitor) {
					broker.invoke(invocation);
					// remove plug-ins after call returned
					if (p != null && system != null) {
						for (int i = 0; i < p.length; i++) {
							registry.removePlugin(system, p[i]);
						}						
					}
					future.setResult(new Result
						(invocation.getResult(), invocation.getException()));
				}
			};
			broker.performOperation(performer);
		}
		return result;		
	}
	
	/**
	 * Sends an invocation as a synchronous method call.
	 * 
	 * @param invocation The invocation to send.
	 * @return The result used to determine the outcome.
	 */
	protected Result proxyInvokeSynchronous(Invocation invocation) {
		if (invocation.getSource() == null || invocation.getTarget() == null) {
			invocation.setResult(null);
			invocation.setException(new InvocationException("Reference is null."));
		} else {
			InvocationBroker broker = InvocationBroker.getInstance();
			DeviceRegistry registry = broker.getDeviceRegistry();
			SystemID system = invocation.getTarget().getSystem();
			PluginDescription[] p = plugins;
			if (p != null && system != null) {
				for (int j = 0; j < p.length; j++) {
					registry.registerPlugin(system, p[j]);	
				}	
			}
			broker.invoke(invocation);
			if (p != null && system != null) {
				for (int j = 0; j < p.length; j++) {
					registry.removePlugin(system, p[j]);	
				}	
			}
			
		}
		return new Result(invocation.getResult(), invocation.getException());
	}
	
	/**
	 * Sends an invocation as an asynchronous method call.
	 * 
	 * @param invocation The invocation to send.
	 * @return A result object that may contain an exception.
	 */
	protected Result proxyInvokeAsynchronous(Invocation invocation) {
		InvocationBroker broker = InvocationBroker.getInstance();
		if (invocation.getSource() == null || invocation.getTarget() == null) {
			invocation.setResult(null);
			invocation.setException(new InvocationException("Reference is null."));
		} else {
			DeviceRegistry registry = broker.getDeviceRegistry();
			SystemID system = invocation.getTarget().getSystem();
			PluginDescription[] p = plugins;
			if (p != null && system != null) {
				for (int j = 0; j < p.length; j++) {
					registry.registerPlugin(system, p[j]);	
				}	
			}
			broker.invoke(invocation);
			if (p != null && system != null) {
				for (int j = 0; j < p.length; j++) {
					registry.removePlugin(system, p[j]);	
				}	
			}
		}
		return new Result(invocation.getResult(), invocation.getException());
	}
	
	/**
	 * Returns an invocation for the specified method call that has the default 
	 * requirements towards an streaming method call.
	 * 
	 * @param method The signature of the method to call.
	 * @param params The parameters of the method call.
	 * @return An invocation that can be used to initialize a stream.
	 */
	protected Invocation proxyCreateStream(String method, Object[] params) {
		Invocation invocation = new Invocation();
		invocation.setSignature(method);
		invocation.setTarget(getTargetID());
		invocation.setSource(getSourceID());
		invocation.setArguments(params);
		invocation.setRequirements(NFCollection.getDefault(NFCollection.TYPE_STREAM, gateway));
		return invocation;		
	}

	/**
	 * Returns an invocation for the specified method call that has the default 
	 * requirements towards an asynchronous method call.
	 * 
	 * @param method The signature of the method to call.
	 * @param params The parameters of the method call.
	 * @return An invocation that can be used to communicate with a remote system
	 * 	through an asynchronous method call.
	 */
	protected Invocation proxyCreateAsynchronous(String method, Object[] params) {
		Invocation invocation = new Invocation();
		invocation.setSignature(method);
		invocation.setTarget(getTargetID());
		invocation.setSource(getSourceID());
		invocation.setArguments(params);
		invocation.setRequirements(NFCollection.getDefault
			(NFCollection.TYPE_ASYNCHRONOUS, gateway));
		return invocation;		
	}
	
	/**
	 * Returns an invocation for the specified method call that has the default
	 * requirements towards an synchronous method call.
	 * 
	 * @param method The signature of the method call.
	 * @param params The parameters of the method call.
	 * @return An invocation that can be used to communicate with a remote system
	 * 	through a synchronous method call.
	 */
	protected Invocation proxyCreateSynchronous(String method, Object[] params) {
		Invocation invocation = new Invocation();
		invocation.setSignature(method);
		invocation.setTarget(getTargetID());
		invocation.setSource(getSourceID());
		invocation.setArguments(params);
		invocation.setRequirements(NFCollection.getDefault
			(NFCollection.TYPE_SYNCHRONOUS, gateway));
		return invocation;		
	}

	/**
	 * Returns an invocation for the specified method call that has the default
	 * requirements towards a deferred synchronous method call. 
	 * 
	 * @param method The signature of the method call.
	 * @param params The parameters of the method call.
	 * @return An invocation that can be used to communicate with a remote system
	 * 	through a deferred synchronous method call.
	 */
	protected Invocation proxyCreateDeferred(String method, Object[] params) {
		Invocation invocation = new Invocation();
		invocation.setSignature(method);
		invocation.setTarget(getTargetID());
		invocation.setSource(getSourceID());
		invocation.setArguments(params);
		invocation.setRequirements(NFCollection.getDefault
			(NFCollection.TYPE_SYNCHRONOUS, gateway));
		return invocation;
	}


}
