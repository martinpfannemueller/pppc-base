package info.pppc.base.system.plugin;

import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.nf.NFCollection;

/**
 * This is the interface that must be implemented by semantic plug-ins.
 * Semantic plug-ins implement communication abstractions such as RPCs,
 * streams or event-based communication. As such, they are responsible
 * for distributing and processing invocations to the right set of systems.
 * To do this, they may request communication stacks.
 * 
 * @author Marcus Handte
 */
public interface ISemantic extends IPlugin, IDispatcher {

	/**
	 * Prepares a session to communicate with the specified target under
	 * the specified requirements.
	 * 
	 * @param s The session that can be used to negotiate session
	 * 	specific data and to store local information used to initiate the
	 * 	communication.
	 * @param c The non-functional requirements.
	 * @param d The plug-in description of the remote device.
	 * @return False if the specified context cannot be fulfilled.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s);

	/**
	 * Sets the plug-in manager of this plug-in. The plug-in manager enables 
	 * a plug-in to perform operations. The semantic plug-in manager provides 
	 * a synchronized table that provides wait methods and check calls. This 
	 * method is guaranteed to be called before the start method is invoked 
	 * the first time.
	 * 
	 * @param manager The manager of the plug-in.
	 */
	public void setSemanticManager(ISemanticManager manager);

	/**
	 * Tells the semantic plug-in to perform the specified invocation under
	 * the specified session context.
	 * 
	 * @param invocation The invocation to perform.
	 * @param session The session that can be used to request further sessions.
	 */		
	public void performOutgoing(Invocation invocation, ISession session);

}
