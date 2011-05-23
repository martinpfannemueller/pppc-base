package info.pppc.irsock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class implements the client socket for the ir library. A socket
 * can be used to connecto to a remote device using a device info and
 * a service name. 
 * 
 * @author bator
 */
public class IRSocket {

	/**
	 * The implementation of the ir socket.
	 */
	private IRSocketImpl implementation = null;

	/**
	 * Creates a new ir socket that connects to the specified device
	 * and service.
	 * 
	 * @throws IOException Thrown if the socket could not be created.
	 */
	public IRSocket(IRDevice device, String service) throws IOException {
		implementation = new IRSocketImpl();
		implementation.create();
		try {
			connect(device, service);
		} catch (IOException e) {
			implementation.close();	
			throw e;
		}
	}

	/**
	 * Creates a socket from the specified socket implementation.
	 * 
	 * @param impl The socket implementation to use.
	 */
	protected IRSocket(IRSocketImpl impl) {
		implementation = impl;
	}

	/**
	 * Returns the used socket implementation.
	 * 
	 * @return instance of socket implementation
	 */
	protected IRSocketImpl getImplementation() {
		return implementation;
	}

	/**
	 * Connect to a discovered device.
	 * 
	 * @param device A device that has been obtained through a call to the
	 * 	discover method.
	 * @param serviceName the service name to connect to (analog to a port number)
	 * @throws IOException Thrown if the connect call fails.
	 */
	protected void connect(IRDevice device, String serviceName) throws IOException {
		implementation.connect(device.getAddress(), serviceName);
	}

	/**
	 * Returns the input stream for this socket.
	 * 
	 * @return input An input stream for this socket.
	 * @throws IOException Thrown if the stream could not be created.
	 */
	public InputStream getInputStream() throws IOException {
		return implementation.getInputStream();
	}

	/**
	 * Returns the output stream of the socket.
	 * 
	 * @return The output stream of this socket.
	 * @throws IOException Thrown if the stream could not be created.
	 */
	public OutputStream getOutputStream() throws IOException {
		return implementation.getOutputStream();
	}

	/**
	 * Close this socket.
	 *
	 * @throws IOException Thrown if the close call fails.
	 */
	public void close() throws IOException {
		implementation.close();
	}

}
