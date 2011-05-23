/**
 * 
 */
package info.pppc.basex.plugin.modifier;

import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.plugin.IModifier;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.security.ISymmetricKey;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.security.io.SecureInputStream;
import info.pppc.base.system.security.io.SecureOutputStream;
import info.pppc.base.system.security.sym.AESSymmetricKey;
import info.pppc.base.system.security.sym.HMACSymmetricKey;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The security plug-in provides authentication and encryption. The plug-in
 * can be configured for each session to use authentication only mode or
 * authentication and encryption mode. This is controlled via the 
 * 
 * @author Marcus Handte
 */
public class SecureModifier implements IModifier {

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
		 * The encryption key to use.
		 */
		private AESSymmetricKey encryption;
		
		/**
		 * The signing key to use.
		 */
		private HMACSymmetricKey signature;
		
		/**
		 * Lazy initializer of the output stream.
		 */
		private OutputStream output;
		
		/**
		 * Lazy initializer of the input stream.
		 */
		private InputStream input;
		
		/**
		 * Creates a new stream connector that uses the specified connector
		 * to create input and output streams.
		 * 
		 * @param connector The connector used to create basic input and
		 * 	output streams.
		 * @param encryption The encryption key to use.
		 * @param signature The signature key to use.
		 */
		public StreamConnector(IStreamConnector connector, AESSymmetricKey encryption, HMACSymmetricKey signature) {
			this.connector = connector;
			this.encryption = encryption;
			this.signature = signature;
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
				input = new SecureInputStream(connector.getInputStream(), encryption, signature);
				//input = connector.getInputStream();
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
				output = new SecureOutputStream(connector.getOutputStream(), encryption, signature);
				//output = connector.getOutputStream();
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
			return SecureModifier.this;
		}
	}

	/**
	 * The property in the plug-in description that contains a
	 * system id.
	 */
	public static String PROPERTY_SYSTEM = "ID";
	
	/**
	 * The ability of the plug-in. [2][0].
	 */
	public static final short PLUGIN_ABILITY = 0x0200;

	/**
	 * The plug-in description of the ip plug-in.
	 */
	private PluginDescription description = new PluginDescription(PLUGIN_ABILITY, EXTENSION_ENCRYPTION);

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
	 * A reference to the key store that manages the session keys.
	 */
	private KeyStore store = KeyStore.getInstance();
	
	/**
	 * Creates a new secure modifier.
	 */
	public SecureModifier() {
		description.setProperty(PROPERTY_SYSTEM, SystemID.SYSTEM, false);
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
		// only introduce encryption if this is really needed.
		NFDimension required = collection.getDimension(EXTENSION_ENCRYPTION, NFDimension.IDENTIFIER_REQUIRED);
		if (required != null && required.getHardValue().equals(new Boolean(true))) {
			Object id = description.getProperty(PROPERTY_SYSTEM);
			if (id != null && id instanceof SystemID) {
				SystemID system = (SystemID)id;
				// determine whether there is a session key
				long ts = store.getTimestamp(system);
				if (ts == KeyStore.TIMESTAMP_MISSING) {
					Logging.debug(getClass(), "Session key unavailable.");
					// create a new session key
					if (! store.createKey(system)) {
						Logging.debug(getClass(), "Could not establish key.");
						return false;
					}
					ts = store.getTimestamp(system);
				}
				// validate keys
				ISymmetricKey enc = store.getEncryption(system);
				if (enc == null || ! (enc instanceof AESSymmetricKey)) return false;
				ISymmetricKey sig = store.getSignature(system);
				if (sig == null || ! (sig instanceof HMACSymmetricKey)) return false;
				session.setLocal(new Object[] { system, new Long(ts), enc, sig});
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the plugin is in a valid state. If the plugin
	 * has been started and initialized properly, the result of a call to this
	 * method will be a new stream connector that supports secure input and output
	 * streams.
	 * 
	 * @param connector The connector used to create the connector.
	 * @param context The session data used to communicate with a remote plugin.
	 * @return A stream connector that is connected to the specified stream connector.
	 */
	public IStreamConnector openSession(IStreamConnector connector, ISession context) throws IOException {
		checkPlugin();
		// check whether session key is available on both sides of the connection
		if (context.isIncoming()) {
			ObjectInputStream i = new ObjectInputStream(connector.getInputStream());
			SystemID system = (SystemID)i.readObject();
			long ts = i.readLong();
			ObjectOutputStream o = new ObjectOutputStream(connector.getOutputStream());
			if (ts != store.getTimestamp(system)) {
				Logging.debug(getClass(), "Session key expired. " + system);
				store.removeKey(system);
				o.writeBoolean(false);
				o.flush();
				connector.release();
			} else {
				ISymmetricKey enc = store.getEncryption(system);
				ISymmetricKey sig = store.getSignature(system);
				if (sig != null && sig instanceof HMACSymmetricKey 
							&& enc != null && enc instanceof AESSymmetricKey) {
					o.writeBoolean(true);
					o.flush();
					return new StreamConnector(connector, (AESSymmetricKey)enc, (HMACSymmetricKey)sig);
				} else {
					Logging.debug(getClass(), "Session key invalid.");
					store.removeKey(system);
					o.writeBoolean(false);
					o.flush();
					connector.release();
				}
			}
		} else {
			Object[] setup = (Object[])context.getLocal();
			SystemID system = (SystemID)setup[0];
			long ts = ((Long)setup[1]).longValue();
			AESSymmetricKey enc = (AESSymmetricKey)setup[2];
			HMACSymmetricKey sig = (HMACSymmetricKey)setup[3];
			ObjectOutputStream o = new ObjectOutputStream(connector.getOutputStream());
			o.writeObject(SystemID.SYSTEM);
			o.writeLong(ts);
			o.flush();
			ObjectInputStream i = new ObjectInputStream(connector.getInputStream());
			if (!i.readBoolean()) {
				Logging.debug(getClass(), "Session key expired. " + system);
				store.removeKey(system);
			} else {
				return new StreamConnector(connector, enc, sig);
			}
		}
		throw new IOException();
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
