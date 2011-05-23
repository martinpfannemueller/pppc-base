package info.pppc.irsock;

import java.io.IOException;

/** 
 * This class implements a server socket for irda protocol. 
 * A IRServerSocket registers a Service Name and listens on it. 
 * 
 * @author bator
 */
public class IRServerSocket {

	/**
	 * The implementation of the socket.
	 */
	private IRSocketImpl implementation = new IRSocketImpl();

	/** 
	 * Creates an infrared server socket which provides given serviceName.
	 * 
	 * @param serviceName The name of service to provide. This is
	 * 	equivalent to a port number.
	 * @exception IOException If an I/O error occurs while creating the server 
	 * 	socket
	 */

	public IRServerSocket(String serviceName) throws IOException {
		this(serviceName, 5);
	}

	/** 
	 * Creates an infrared server socket with specified service name and
	 * a given backlog.
	 * 
	 * @param serviceName The name of the service to bind to.
	 * @param backlog The backlog of the server socket.
	 * @exception IOException Thrown if an exception occurs while creating
	 * 	the socket. 
	 */

	public IRServerSocket(String serviceName, int backlog) throws IOException {
		implementation.create();
		try {
			implementation.bind(serviceName);
			implementation.listen(backlog);
		} catch (IOException e) {
			implementation.close();
			throw e;
		}
	}

	/** 
	 * Listen for incoming connections and block until a client connects to socket.
	 * 
	 * @return An IRDASocket for the connection.
	 * @exception IOException Thrown if an I/O error occurs while waiting for the 
	 * 	connection.
	 */

	public IRSocket accept() throws IOException {
		IRSocketImpl client = new IRSocketImpl();
		implementation.accept(client);
		return new IRSocket(client);
	}

	/**
	 * Closes this socket.
	 *
	 * @exception IOException Thrown if an I/O error occurs when 
	 * 	closing the socket.
	 */

	public void close() throws IOException {
		implementation.close();
	}

}
