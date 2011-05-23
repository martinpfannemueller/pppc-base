package info.pppc.basex.plugin.transceiver.ip;

import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The stream connector transforms an incoming or outgoing socket into a
 * stream connector that can be used by base. The stream connector uses
 * the connector vector to maintain references to all opened connectors.
 * If a connector is closed, it is automatically removed from the vector. 
 * 
 * @author Marcus Handte
 */
public class IPStreamConnector implements IStreamConnector {

	/**
	 * The socket that is used to communicate with a remote system.
	 */
	private Socket socket = null;

	/**
	 * The socket's input stream, if it has been opened already.
	 */
	private InputStream in = null;

	/**
	 * The socket's output stream, if it has been opened already.
	 */
	private OutputStream out = null;

	/**
	 * The plug-in that uses this stream connector.
	 */
	private IIPPlugin plugin;
	
	/**
	 * Creates a new stream connector that uses the specified
	 * socket to communicate and adds the new stream connector
	 * to the connector list.
	 * 
	 * @param socket The socket of the connector.
	 * @param plugin The responsible plug-in.
	 */
	public IPStreamConnector(IIPPlugin plugin, Socket socket) {
		this.socket = socket;
		this.plugin = plugin;
	}

	/**
	 * Returns the input stream of the socket. The input stream is cached
	 * so that it is not retrieved multiple times.
	 * 
	 * @return The input stream of the socket. The input stream is only
	 * 	retrieved at most once per stream connector.
	 * @throws IOException Thrown if the stream cannot be opened.
	 */
	public InputStream getInputStream() throws IOException { 
		if (in == null) {
			try {
				in = socket.getInputStream();	
			} catch (IOException e) {
		
				throw e;
			}
		}
		return in;
	}

	/**
	 * Returns the output stream of the socket. The output stream is cached
	 * so that it is not retrieved multiple times.
	 * 
	 * @return The output stream of the socket. The output stream is only
	 * 	retrieved at most once per stream connector.
	 * @throws IOException Thrown if the stream cannot be opened.
	 */
	public OutputStream getOutputStream() throws IOException {
		if (out == null) {
			try {
				out = socket.getOutputStream();	
			} catch (IOException e) {
		
				throw e;
			}				
		}
		return out;
	}

	/**
	 * Closes the socket bound to the connector, thereby canceling the
	 * input and output streams.
	 */
	public void release() {
		try {
			socket.close();
		} catch (IOException e) {
			Logging.error(getClass(), "Cannot close socket.", e);
		}
		plugin.release(this);
	}

	/**
	 * Returns a reference to the underlying plug-in.
	 * 
	 * @return A reference to the underlying plug-in.
	 */
	public IPlugin getPlugin() {
		return plugin;
	}

}
