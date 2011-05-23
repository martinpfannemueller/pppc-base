package info.pppc.base.system.plugin;

import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.nf.NFCollection;

import java.io.IOException;

/**
 * The semantic manager is the interface that enables semantic plug-ins to
 * access the internals of the invocation broker. Furthermore, it enables
 * them to request the negotiation and creation of new connections to 
 * remote systems.
 * 
 * @author Marcus Handte
 */
public interface ISemanticManager extends IPluginManager {

	/**
	 * Requests a new session that can be used to create a connection to the
	 * specified device with the specified non-functional properties.
	 * 
	 * @param session The original session that has been prepared in the
	 * 	prepare method of the semantic plug-in.
	 * @param requirements The requirements towards the connection.
	 * @return A session that fulfills all requirements.
	 */
	public ISession prepareSession(ISession session, NFCollection requirements);
	
	/**
	 * Opens connector with the specified session properties.
	 * 
	 * @param session The session that specifies the connector's properties.
	 * @return The connector with the specified session properties.
	 * @throws IOException Thrown if the connector cannot be created.
	 */
	public IStreamConnector openSession(ISession session) throws IOException;

	/**
	 * Delivers the specified invocation originating from the specified
	 * session.
	 * 
	 * @param invocation The invocation to deliver.
	 * @param session The session of the invocation.
	 */
	public void dispatchSynchronous(Invocation invocation, ISession session);

	/**
	 * Creates a session to communicate with the specified target.
	 * 
	 * @param target The target of the call.
	 * @param ability The ability of the calling plug-in. 
	 * @return The session object.
	 */
	public ISession createSession(SystemID target, short ability);
	
}
