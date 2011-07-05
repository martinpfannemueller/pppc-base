/**
 * 
 */
package info.pppc.base.system.security;

import java.util.Hashtable;
import java.util.Vector;

import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.IExtension;
import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.Skeleton;
import info.pppc.base.system.SystemID;

/**
 * The secure skeleton supports stack analysis.
 * 
 * @author Mac
 */
public abstract class SecureSkeleton extends Skeleton {

	/**
	 * The session objects hashed by current thread.
	 */
	private Hashtable sessions = new Hashtable();
	
	/**
	 * Creates a new secure skeleton.
	 */
	public SecureSkeleton() { }

	/**
	 * Dispatches an invocation to the actual implementation. In addition
	 * to the method of the base class which simply calls the dispatch
	 * method, this method caches the session in a hashtable using the
	 * current thread. This allows the application to test whether the
	 * context of a call is secure and whether it has been issued by
	 * a particular source.
	 * 
	 * @param msg The invocation that shall be dispatched.
	 * @param session The session object that denotes the stack configuration.
	 */
	public void invoke(Invocation msg, ISession session) {
		boolean contains = sessions.containsKey(Thread.currentThread());
		Object entry = sessions.put(Thread.currentThread(), session);
		if (contains) {
			if (entry instanceof ISession) {
				Vector v = new Vector();
				v.addElement(entry);
				v.addElement(session);
				sessions.put(Thread.currentThread(), v);
				super.invoke(msg, session);
				v.removeElementAt(v.size() - 1);
			} else {
				Vector v = (Vector) entry;
				v.addElement(session);
				sessions.put(Thread.currentThread(), v);
				super.invoke(msg, session);
				v.removeElementAt(v.size() - 1);
			}
		} else {
			super.invoke(msg, session);
			sessions.remove(Thread.currentThread());
		}
	}
	
	/**
	 * Determines whether the current thread is executed in a secure
	 * context. This means that the call must either be passed directly
	 * from a local semantic plug-in or it uses the encryption plug-in
	 * which also authenticates the source.
	 * 
	 * @return True if the context of the call is verified to be
	 * 	secure, false otherwise. 
	 */
	public boolean isSecure() {
		ISession session = getSession();
		if (session == null) {
			// this happens if the semantic plug-in is broken
			// and has passed null to the plug-in manager as
			// a session, in this case, we assume it is not secure
			return false;
		}
		if (session.getChild() == null) {
			// this happens if the call is local and has not
			// been transmitted with anything else than the
			// semantic plug-in, in this case, no encryption is secure
			return true;
		} 
		// search for the encryption plug-in and if it is contained,
		// the communication is secure, else it is insecure
		InvocationBroker broker = InvocationBroker.getInstance();
		DeviceRegistry registry = broker.getDeviceRegistry();
		PluginDescription[] plugins = registry.getPluginDescriptions(SystemID.SYSTEM);
		while (session != null) {
			for (int i = 0; i < plugins.length; i++) {
				if (plugins[i].getAbility() == session.getAbility()) {
					if (plugins[i].getExtension() == IExtension.EXTENSION_ENCRYPTION) {
						return true;
					}
				}
			}
			session = session.getChild();
		}
		return false;
	}
	
	/**
	 * Determines the source of a call using the current thread
	 * context. If the call is secure, the source has been 
	 * verified, otherwise the source may be manipulated by
	 * a malicious host. Note that the source may be null, if
	 * the session has not been passed properly to the invocation
	 * handler, i.e. if the semantic plug-in is broken.
	 * 
	 * @return The source of the call that is executed using
	 * 	the current thread or null, if it cannot be determined.
	 */
	public SystemID getSource() {
		ISession session = getSession();
		if (session == null) return null;
		else return session.getTarget();
	}
	
	
	/**
	 * Returns the session for the current thread or null, if
	 * there is none.
	 * 
	 * @return The session for the current thread.
	 */
	private ISession getSession() {
		Object entry = sessions.get(Thread.currentThread());
		if (entry instanceof ISession) {
			return (ISession)entry;
		} else {
			Vector v = (Vector)entry;
			return (ISession)v.elementAt(v.size() - 1);			
		}
	}
}
