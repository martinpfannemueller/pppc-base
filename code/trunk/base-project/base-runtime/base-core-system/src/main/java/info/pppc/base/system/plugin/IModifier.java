package info.pppc.base.system.plugin;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.nf.NFCollection;

import java.io.IOException;

/**
 * A modifier is a plug-in that changes the stream type provided
 * by another plug-in. The modifier interface must be implemented by
 * several different types of plug-ins. These include serialization,
 * compression and encryption plug-ins. 
 * 
 * @author Marcus Handte
 */
public interface IModifier extends IPlugin {

	/**
	 * Sets the plug-in manager of this plug-in. The plug-in manager enables 
	 * a plug-in to request a communication stack and to perform operations. 
	 * This method is guaranteed to be called before the start method is
	 * invoked the first time.
	 * 
	 * @param manager The manager of the plug-in.
	 */
	public void setPluginManager(IPluginManager manager);

	/**
	 * Connects a streaming connector to the specified connector. 
	 * 
	 * @param child The child connector that the new connector must
	 * 	connect to.
	 * @param context The communication context that provides session
	 * 	data created by the remote plug-in.
	 * @return The connector that is connected to the specified child.
	 * @throws IOException Thrown if the connector could not be created.
	 */
	public IStreamConnector openSession(IStreamConnector child, ISession context) throws IOException;

	/**
	 * Prepares a session to communicate with the specified target under
	 * the specified requirements.
	 * 
	 * @param session The session that can be used to negotiate session
	 * 	specific data and to store local information used to initiate the
	 * 	communication.
	 * @param collection The non-functional requirements.
	 * @param description The plug-in description of the remote device.
	 * @return False if the specified context cannot be fulfilled.
	 */
	public boolean prepareSession(PluginDescription description, NFCollection collection, ISession session);

}
