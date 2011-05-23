package info.pppc.base.system.plugin;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.nf.NFCollection;

import java.io.IOException;

/**
 * The streamer interface is implemented by plug-ins that establish 
 * connections to other systems. This can be either transceiver plug-ins
 * or routing plug-ins that in turn use transceiver plug-ins. 
 * 
 * @author Marcus Handte
 */
public interface IStreamer {

	/**
	 * Opens an outgoing streaming connector that connects to the specified 
	 * target system. The context object provides the connector of the 
	 * communication. 
	 * 
	 * @param session A session that has been negotiated previously using the
	 * 	prepare method.
	 * @return A connector that connects to the specified target system
	 * 	and provides the specified type or null if such a connector cannot
	 * 	be created.
	 * @throws IOException Thrown if the connector cannot be opened.
	 */	
	public IStreamConnector openSession(ISession session) throws IOException;
	
	/**
	 * Prepares a session to communicate with the specified target under
	 * the specified requirements.
	 * 
	 * @param s The session that can be used to negotiate session
	 * 	specific data and to store local information used to initiate the
	 * 	communication.
	 * @param c The non-functional requirements.
	 * @param d The plugin description of the remote plugin.
	 * @return False if the specified context cannot be fulfilled.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s);
	
}
