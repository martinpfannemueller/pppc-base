package info.pppc.base.system;

/**
 * The stream handler interface is used to enable streaming with
 * BASE plug-ins at the application layer. If an application
 * interface implements this interface, the proxy generator
 * will generate special proxy and skeleton methods.
 * 
 * A client can initiate a new connection by creating a new
 * stream descriptor with an undefined stream connector and an
 * optional data value that is transmitted to the server side
 * during stream initialization. After the method returns, the
 * stream connector will be initialized and usable. If an
 * invocation exception occurs, the connector will be null or
 * unusable.
 * 
 * A server must implement this method and accept incoming calls.
 * If a server refuses to accept an incoming stream, it can
 * either close the connector passed in the descriptor, or it
 * can throw an invocation exception, alternatively. If an
 * invocation exception is thrown, the connector will be 
 * closed by BASE. At the server side, this method should 
 * return quickly in order to free up memory in the broker. 
 * Long-running communication should be done using an individual 
 * operation.
 * 
 * @author Marcus Handte
 */
public interface IStreamHandler {
	
	/**
	 * Called on the proxy to connect to a server using streams or
	 * called on the server to signal that a client has requested
	 * a connection.
	 * 
	 * @param descriptor The connection setup on the client or
	 * 	the connection description on the server.
	 * @throws InvocationException Thrown if the initialization fails or
	 * 	should fail.
	 */
	public void connect(StreamDescriptor descriptor) throws InvocationException;

}
