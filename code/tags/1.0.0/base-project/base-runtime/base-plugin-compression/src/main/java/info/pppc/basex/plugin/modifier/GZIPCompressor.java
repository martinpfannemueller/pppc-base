package info.pppc.basex.plugin.modifier;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.plugin.IModifier;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 
 * The gzip compressor is a modifier that provides gzipped input and output streams. 
 * As such the streams provided by this modifier can be used to reduce the size of
 * data using the gzip compression algorithm. The implementation of this plug-in is 
 * intended for all J2ME configurations starting from CDC with foundation profile. 
 *
 * @author Marcus Handte
 */
public class GZIPCompressor implements IModifier {

	/**
	 * The stream connector provides gzip input and output streams depending
	 * on the implementation of the underlying stream connector.
	 * 
	 * @author Marcus Handte
	 */
	public class StreamConnector implements IStreamConnector {
		
		/**
		 * The underlying stream connector used to send and receive data.
		 */
		private IStreamConnector connector;
		
		/**
		 * Lazy initializer of the output stream.
		 */
		private GZIPOutputStream output;
		
		/**
		 * Lazy initializer of the input stream.
		 */
		private GZIPInputStream input;
		
		/**
		 * Creates a new stream connector that uses the specified connector
		 * to create input and output streams.
		 * 
		 * @param connector The connector used to create basic input and
		 * 	output streams.
		 */
		public StreamConnector(IStreamConnector connector) {
			this.connector = connector;
		}

		/**
		 * Returns an gzip input stream that is piped to the input stream
		 * of the underlying connector.
		 * 
		 * @return An input stream piped to the underlying connector.
		 * @throws IOException Thrown by the underlying stream.
		 */
		public InputStream getInputStream() throws IOException {
			if (input == null) {
				input = new GZIPInputStream(connector.getInputStream());
			}
			return input;
		}

		/**
		 * Returns an gzip output stream that is piped to the output stream
		 * of the underlying connector.
		 * 
		 * @return An output stream piped to the underlying connector.
		 * @throws IOException Thrown by the underlying stream.
		 */
		public OutputStream getOutputStream() throws IOException {
			if (output == null) {
				output = new GZIPOutputStream(connector.getOutputStream());
			}
			return output;
		}

		/**
		 * Releases the connector and closes all potentially open input and
		 * output streams.
		 */
		public void release() {
			if (input != null) {
				try {
					input.close();	
				} catch (IOException e) {
					// nothing to be done here
				}
				input = null;	
			}
			if (output != null) {
				try {
					output.close();	
				} catch (IOException e) {
					// nothing to be done here
				}
				output = null;
			}
			connector.release();
		}

		/**
		 * Returns a reference to the plug-in that created the connector.
		 * 
		 * @return A reference to the plug-in instance that created the connector.
		 */
		public IPlugin getPlugin() {
			return GZIPCompressor.this;
		}
	}

	/**
	 * The ability of the plug-in. [3][0].
	 */
	private static final short PLUGIN_ABILITY = 0x0300;

	/**
	 * The plug-in description of the ip plug-in.
	 */
	private PluginDescription description = new PluginDescription(PLUGIN_ABILITY, EXTENSION_COMPRESSION);

	/**
	 * The plug-in manager used to perform operations.
	 */
	private IPluginManager manager = null;

	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * Creates a new simple serializer.
	 */
	public GZIPCompressor() {
		super();
	}

	/**
	 * Negotiates the session properties of a connection with a remote target.
	 * Since this plug-in has only one mode of operation, negotiation is not
	 * necessary.
	 * 
	 * @param collection The non-functional parameters.
	 * @param session The session data used to create a connector.
	 * @param description The plug-in description of the remote plug-in.
	 * @return Always true, since this plug-in does not deal with nonfunctional
	 * 	parameters.
	 */
	public boolean prepareSession(PluginDescription description, NFCollection collection, ISession session) {
		// only introduce compressor if this is really needed.
		NFDimension required = collection.getDimension(EXTENSION_COMPRESSION, NFDimension.IDENTIFIER_REQUIRED);
		return (required != null && required.getHardValue().equals(new Boolean(true)));
	}

	/**
	 * Determines whether the serializer is in a valid state. If the serializer
	 * has been started and initialized properly, the result of a call to this
	 * method will be a new stream connector that supports gzip input and output
	 * streams.
	 * 
	 * @param connector The connector used to create the serializer connector.
	 * @param context The session data used to communicate with a remote serializer.
	 * @return A stream connector that is connected to the specified stream connector.
	 */
	public synchronized IStreamConnector openSession(IStreamConnector connector, ISession context) {
		checkPlugin();
		return new StreamConnector(connector);
	}

	/**
	 * Called to start the plug-in. This method initializes the plug-in and
	 * enables the creation of connectors. All open calls will fail before
	 * this method has been called.
	 */
	public synchronized void start() {
		if (! started) {
			started = true;
		}
	}

	/**
	 * Called to stop the plug-in. After this method has been called, all
	 * open calls will fail.
	 */
	public synchronized void stop() {
		if (started) {
			started = false;
		}
	}

	/**
	 * Sets the plug-in manager that is used to retrieve remote plug-in descriptions.
	 * 
	 * @param manager The plug-in manager.
	 */
	public void setPluginManager(IPluginManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the plug-in description of this plug-in. There will be only one instance
	 * of the plug-in description per instance of this plug-in.
	 * 
	 * @return The plug-in description of this plug-in.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}
	
	/**
	 * Validates whether the plug-in can open a connection and respond to
	 * connection requests. This method throws an exception if the current
	 * state of the plug-in does not allow the initialization or a 
	 * connector.
	 */
	private void checkPlugin() {
		if (manager == null) throw new RuntimeException("Manager not set.");
		if (! started) throw new RuntimeException("Plugin not started.");
	}

}
