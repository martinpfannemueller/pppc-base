package info.pppc.base.system.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A stream connector is a connector that delivers streams to transfer and/or
 * receive data. To do this, a client opens an input or an output
 * depending on whether it sends or receives data.
 * 
 * @author Marcus Handte
 */
public interface IStreamConnector extends IConnector {

	/**  
	 * Returns an input stream that can be used to receive data. This
	 * method should only be called once per connector, although a connector can
	 * declare that it supports multiple calls to this method. The specific type
	 * returned by this method can be queries using the get type method. 
	 * 
	 * @return An input stream that can be used to receive data. 
	 * @throws IOException Thrown if the connector cannot open an input stream.
	 */
	public InputStream getInputStream() throws IOException;
	
	/**
	 * Returns an output stream that can be used to transfer data. Be
	 * aware that this method should only be called once per connector. The 
	 * specific type returned by this method is defined by the get type method.
	 * 
	 * @return An output stream that can be used to transfer data.
	 * @throws IOException Thrown if the connector cannot open an output stream.
	 */
	public OutputStream getOutputStream() throws IOException;
	
}
