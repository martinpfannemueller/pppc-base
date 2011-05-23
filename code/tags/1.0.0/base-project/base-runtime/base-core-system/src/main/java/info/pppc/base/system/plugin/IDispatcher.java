package info.pppc.base.system.plugin;

import info.pppc.base.system.ISession;

/**
 * The dispatcher interface is the interface of plug-ins that dispatch 
 * incoming calls. This can be either a semantic plug-in that receives
 * a new incoming connection from a neighboring system or it can be a 
 * routing plug-in that receives an incoming end-to-end connection that
 * may have been routed through other devices.
 * 
 * @author Marcus Handte
 */
public interface IDispatcher {

	/**
	 * Connects the dispatcher plug-in to the specified incoming connector. 
	 * 
	 * @param connector The connector of an incoming connection.
	 * @param session The session data of the incoming connection.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session);
	
}
