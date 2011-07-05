package info.pppc.base.tutorial.stream;

import info.pppc.base.service.Service;
import info.pppc.base.system.StreamDescriptor;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

/**
 * This service demonstrates the streaming plug-in and a service that
 * uses it to establish a connection. See the package description for
 * more details.
 * 
 * @author Marcus Handte
 */
public class StreamService extends Service implements IStream {

	/**
	 * Creates a new tutorial service.
	 */
	public StreamService() {}
		
	/**
	 * Accept an incoming stream, read a string, print it and close
	 * the connection.
	 * 
	 * @param descriptor The descriptor for the incoming stream.
	 */
	public void connect(StreamDescriptor descriptor) {
		try {
			Logging.log(getClass(), "Incoming connection: " + descriptor.getData());
			IStreamConnector connector = descriptor.getConnector();
			IObjectInput input = (IObjectInput)connector.getInputStream();
			Logging.log(getClass(), "Reading: " + input.readObject());
			connector.release();
			Logging.log(getClass(), "Released connection.");
		} catch (Throwable t) {
			Logging.error(getClass(), "Exception in connect.", t);
		}
 	}

}
