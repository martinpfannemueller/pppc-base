package info.pppc.base.system;

import info.pppc.base.system.plugin.IStreamConnector;

/**
 * The stream descriptor is used to initiate streaming connections at the application 
 * layer using base plug-ins. A descriptor consists of a user-defined data object
 * that must be serializable and that will be transfered from client to server during 
 * initialization as well as a stream connector that is initialized by base.
 * 
 * @author Marcus Handte
 */
public class StreamDescriptor {

	/**
	 * The stream connector that describes the stream.
	 */
	private IStreamConnector connector;
	
	/**
	 * The user-defined data object that describes the connection.
	 */
	private Object data;
	
	/**
	 * Creates a new uninitialized stream connector.
	 */
	public StreamDescriptor() { }
	
	/**
	 * Returns the connector associated with the stream.
	 * 
	 * @return The connector of the stream.
	 */
	public IStreamConnector getConnector() {
		return connector;
	}	
	
	/**
	 * Sets the connector associated with the stream.
	 * 
	 * @param connector The connector to set.
	 */
	public void setConnector(IStreamConnector connector) {
		this.connector = connector;
	}
	
	/**
	 * Returns the user data associated with the descriptor.
	 * 
	 * @return The user data of the descriptor.
	 */
	public Object getData() {
		return data;
	}
	
	/**
	 * Sets the user data associated with the descriptor.
	 * 
	 * @param data The user data.
	 */
	public void setData(Object data) {
		this.data = data;
	}
	
	
}
