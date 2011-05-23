/**
 * 
 */
package info.pppc.base.system.security;

import info.pppc.base.system.Invocation;
import info.pppc.base.system.Proxy;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.util.Static;

/**
 * The secure proxy is a base class for generated proxies that
 * ensures that calls are made through an encrypted connection.
 * 
 * @author Mac
 */
public class SecureProxy extends Proxy {
 
	/**
	 * Creates a new secure proxy.
	 */
	public SecureProxy() {
		super();
	}
	
	/**
	 * Creates an invocation for an asynchronous call.
	 * 
	 * @param method The method name.
	 * @param params The method parameters.
	 */
	protected Invocation proxyCreateAsynchronous(String method, Object[] params) {
		return proxyCreateSecure(super.proxyCreateAsynchronous(method, params));
	}
	
	/**
	 * Creates an invocation for a deferred synchronous call.
	 * 
	 * @param method The method name.
	 * @param params The method parameters.
	 */
	protected Invocation proxyCreateDeferred(String method, Object[] params) {
		return proxyCreateSecure(super.proxyCreateDeferred(method, params));
	}
	
	/**
	 * Creates an invocation for a streaming connection.
	 * 
	 * @param method The method name.
	 * @param params The method parameters.
	 */
	protected Invocation proxyCreateStream(String method, Object[] params) {
		return proxyCreateSecure(super.proxyCreateStream(method, params));
	}
	
	/**
	 * Creates an invocation for an synchronous call.
	 * 
	 * @param method The method name.
	 * @param params The method parameters.
	 */
	protected Invocation proxyCreateSynchronous(String method, Object[] params) {
		return proxyCreateSecure(super.proxyCreateSynchronous(method, params));
	}
	
	/**
	 * Adds security requirements to the non-functional requirements of
	 * an invocation.
	 * 
	 * @param invocation The invocation to manipulate.
	 * @return The manipulated invocation.
	 */
	protected Invocation proxyCreateSecure(Invocation invocation) {
		NFCollection c = invocation.getRequirements();
		c.addDimension(NFCollection.EXTENSION_ENCRYPTION, 
				new NFDimension(NFDimension.IDENTIFIER_REQUIRED, Static.TRUE));
		return invocation;
	}
	
}
