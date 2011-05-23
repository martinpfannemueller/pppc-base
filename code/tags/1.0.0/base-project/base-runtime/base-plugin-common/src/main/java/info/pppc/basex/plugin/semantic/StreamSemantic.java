package info.pppc.basex.plugin.semantic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.io.StreamBuffer;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.ISemantic;
import info.pppc.base.system.plugin.ISemanticManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

/**
 * The stream semantic is a pass-through semantic for stream connectors. It 
 * enables applications to access stream connectors directly through the
 * proxies and skeletons. This can be used to pass-by the overhead of base
 * and can be used to limit the amount of buffering, however, if you connectors
 * directly within the application, you will have to handle disconnections
 * within the application.
 * 
 * @author Marcus Handte
 */
public class StreamSemantic implements ISemantic {
	
	/**
	 * Implements a stream connector for internal use during
	 * forwarding on the same device.
	 * 
	 * @author Mac
	 */
	public class StreamConnector implements IStreamConnector {
		
		/**
		 * The input buffer.
		 */
		private StreamBuffer inBuffer;
		
		/**
		 * The output buffer.
		 */
		private StreamBuffer outBuffer;
		
		/**
		 * The input stream.
		 */
		private InputStream input;
		
		/**
		 * The output stream.
		 */
		private OutputStream output;
		
		/**
		 * Creates a new stream connector that uses the specified
		 * input and output buffers as stream providers.
		 * 
		 * @param input The input stream to read from.
		 * @param output The output stream to write to.
		 */
		public StreamConnector(StreamBuffer input, StreamBuffer output) {
			this.inBuffer = input;
			this.outBuffer = output;
		}
		
		/**
		 * Returns the input stream to the buffer.
		 * 
		 * @return The input stream to the buffer.
		 */
		public InputStream getInputStream() {
			if (input == null) {
				input = new ObjectInputStream(inBuffer.getInputStream());
			}
			return input;
		}
		
		/**
		 * Returns the output stream to the buffer.
		 * 
		 * @return The output stream to the buffer.
		 */
		public OutputStream getOutputStream() {
			if (output == null) {
				output = new ObjectOutputStream(outBuffer.getOutputStream());
			}
			return output;
		}
		
		/**
		 * Returns the plugin that created the connector.
		 * 
		 * @return The plugin that created the connector.
		 */
		public IPlugin getPlugin() {
			return StreamSemantic.this;
		}
		
		/**
		 * Releases the connector by closing the underlying
		 * input and output buffers.
		 */
		public void release() {
			inBuffer.close();
			outBuffer.close();
		}
		
	}
	
	/**
	 * The buffer size for stream buffers.
	 */
	private static final int BUFFER_SIZE = 20;
	
	/**
	 * The ability of the plug-in. [5][1].
	 */
	private static final short PLUGIN_ABILITY = 0x0501;

	/**
	 * The plug-in description of the semantic plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, EXTENSION_SEMANTIC);
	
	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * The local semantic plug-in manager.
	 */
	private ISemanticManager manager;
		
	/**
	 * Creates a new stream semantic plug-in.
	 */
	public StreamSemantic() { }
	
	/**
	 * Called when a remote device wants to initialize a stream.
	 * 
	 * @param connector The stream connector of the remote device.
	 * @param session The session of the with remote device.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session) {
		try {
			IObjectInput in = (IObjectInput)connector.getInputStream();
			Invocation invocation = (Invocation)in.readObject();
			Object[] arguments = new Object[2];
			arguments[0] = connector;
			arguments[1] = invocation.getArguments()[0];
			invocation.setArguments(arguments);
			manager.dispatchSynchronous(invocation, session);
		} catch (IOException e) {
			Logging.debug(getClass(), "Invocation reception failed.");
			connector.release();
		}
	}

	/**
	 * Called when a stream to a remote device should be initialized.
	 * 
	 * @param invocation The invocation.
	 * @param session The session with the remote device.
	 */
	public void performOutgoing(Invocation invocation, final ISession session) {
		if (SystemID.SYSTEM.equals(invocation.getTarget().getSystem())) {
			final StreamBuffer buffer1 = new StreamBuffer(BUFFER_SIZE);
			final StreamBuffer buffer2 = new StreamBuffer(BUFFER_SIZE);
			StreamConnector c1 = new StreamConnector(buffer1, buffer2);
			StreamConnector c2 = new StreamConnector(buffer2, buffer1);
			final Invocation invoke = new Invocation();
			invoke.setTarget(invocation.getTarget());
			invoke.setSource(invocation.getSource());
			invoke.setRequirements(invocation.getRequirements());
			invoke.setID(invocation.getID());
			invoke.setSignature(invocation.getSignature());
			Object[] arguments = new Object[2];
			arguments[0] = c1;
			arguments[1] = invocation.getArguments()[0];
			invoke.setArguments(arguments);
			manager.performOperation(new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					manager.dispatchSynchronous(invoke, session);
					if (invoke.getException() != null) {
						buffer1.close();
						buffer2.close();
					}
				}
			});
			invocation.setResult(c2);
		} else {
			try {
				IStreamConnector connector = openSession(session, invocation.getRequirements());
				try {
					IObjectOutput out = (IObjectOutput)connector.getOutputStream();
					out.writeObject(invocation);
					invocation.setResult(connector);
				} catch (IOException e) {
					Logging.debug(getClass(), "Invocation delivery failed.");
					invocation.setException(new InvocationException("Could not deliver invocation."));
					connector.release();
				}
			} catch (IOException e) {
				Logging.debug(getClass(), "Connection attempt failed.");
				invocation.setException(new InvocationException("Could not deliver invocation."));
			}			
		}
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
	
	/***
	 * Tests whether the specified requirements can be fulfilled by the plug-in
	 * and prepares a session for the specified requirements.
	 * 
	 * @param d The plug-in description of the remote semantic plug-in.
	 * @param c The requirements of the session.
	 * @param s The session to adjust.
	 * @return True if the session has been prepared, false if this is not possible.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		checkPlugin();
		// semantic must be synchronous or asynchronous call, otherwise return
		// that the requirements are not met by this semantic plug-in.
		NFDimension dim = c.getDimension(EXTENSION_SEMANTIC, NFDimension.IDENTIFIER_TYPE);
		if (dim.getHardValue().equals(new Short((short)NFCollection.TYPE_STREAM))) {
			return true;
		} else {
			return false;	
		}		
	}
	
	/**
	 * Tries to open a connector using the specified session data and under the
	 * specified set of requirements.
	 * 
	 * @param session The session data used to open the connector.
	 * @param collection The requirements used to negotiate a stack.
	 * @return A stream connector for the specified session.
	 * @throws IOException Thrown if the connector could not be opened.
	 */
	protected IStreamConnector openSession(ISession session, NFCollection collection) throws IOException {
		// do not adjust the original requirements to maintain them at the receiver side
		collection = collection.copy(false);
		// add a demand for a serializer and a transceiver plug-in
		NFDimension req = new NFDimension(NFDimension.IDENTIFIER_REQUIRED, new Boolean(true));
		collection.addDimension(EXTENSION_SERIALIZATION, req);
		collection.addDimension(EXTENSION_TRANSCEIVER, req);	
		// now prepare the session if possible
		session = manager.prepareSession(session, collection);
		return manager.openSession(session);
	}

	/**
	 * Called before the plug-in is started to set the semantic
	 * plug-in manager.
	 * 
	 * @param manager The semantic plug-in manager.
	 */
	public void setSemanticManager(ISemanticManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the description of the plug-in.
	 * 
	 * @return The description of the plug-in.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}

	/**
	 * Called to start the plug-in.
	 */
	public void start() {
		started = true;
	}

	/**
	 * Called to stop the plug-in.
	 */
	public void stop() {
		started = false;
	}
	
}
	