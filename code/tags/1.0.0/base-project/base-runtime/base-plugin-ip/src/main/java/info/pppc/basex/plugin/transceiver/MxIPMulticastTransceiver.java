package info.pppc.basex.plugin.transceiver;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The multiplexing ip transceiver is a net transceiver that performs 
 * connection multiplexing in order to reduce the number of tcp
 * connections opened by the transceiver. This should greatly reduce
 * the delay of invocations at the cost of double-buffered messages.
 * 
 * @author Marcus Handte
 */
public class MxIPMulticastTransceiver extends IPMulticastTransceiver implements IMultiplexPlugin {

	/**
	 * This class provides a wrapper for transceiver managers that catches
	 * the accept methods for incoming connections in order to encapsulate
	 * them in multiplexers.
	 * 
	 * @author Marcus Handte
	 */
	private class TransceiverManager implements ITransceiverManager {

		/**
		 * The wrapped transceiver manager.
		 */
		private ITransceiverManager manager;

		/**
		 * Creates a new wrapper for the specified transceiver manager.
		 * 
		 * @param manager The manager wrapped by this wrapper.
		 */
		public TransceiverManager(ITransceiverManager manager) {
			this.manager = manager;
		}


		/**
		 * Called whenever a new socket has been created. This method
		 * reads the hash key of the socket and creates a new multiplexer. 
		 * 
		 * @param connector The incoming stream connector for which a multiplexer must
		 * 	be created.
		 */
		public void acceptSession(IStreamConnector connector) {
			synchronized (MxIPMulticastTransceiver.this) {
				try {
					DataInputStream dis = new DataInputStream(connector.getInputStream());	
					Long key = new Long(dis.readLong());
					MultiplexFactory mux = new MultiplexFactory(MxIPMulticastTransceiver.this, 
						connector.getInputStream(), connector.getOutputStream());
					connectors.put(mux, connector);
					Vector muxs = (Vector)multiplexers.get(key);
					if (muxs == null) {
						muxs = new Vector();
						multiplexers.put(key, muxs);
					}
					muxs.addElement(mux);
				} catch (IOException e) {
					Logging.error(getClass(), "Could not open multiplexer.", e);
				}
			}
		}

		/**
		 * A simple pass through method that retrieves the description from 
		 * the actual manager.
		 * 
		 * @param system The system id.
		 * @return The device description from the manager.
		 */
		public DeviceDescription getDeviceDescription(SystemID system) {
			return manager.getDeviceDescription(system);
		}
		
		/**
		 * A simple pass through method that retrieves the descriptions from
		 * the actual manager.
		 * 
		 * @param system The system id.
		 * @return The plug-in descriptions from the manager.
		 */
		public PluginDescription[] getPluginDescriptions(SystemID system) {
			return manager.getPluginDescriptions(system);
		}
		
		/**
		 * Returns the devices of the plug-in manager.
		 * 
		 * @return The devices.
		 */
		public SystemID[] getDevices() {
			return manager.getDevices();
		}
		
		/**
		 * A simple pass through method that performs the operation on the
		 * manager.
		 * 
		 * @param operation The operation to perform.
		 */
		public void performOperation(IOperation operation) {
			manager.performOperation(operation);		
		}
		
		/**
		 * A simple pass through method that performs the operation on the
		 * manager.
		 * 
		 * @param operation The operation to perform.
		 * @param monitor The monitor of the operation.
		 */
		public void performOperation(IOperation operation, IMonitor monitor) {
			manager.performOperation(operation, monitor);		
		}

	}

	/**
	 * The ability of the plug-in [1][1].
	 */
	private static final short PLUGIN_ABILITY = 0x0101;

	/**
	 * The plug-in description.
	 */
	private PluginDescription description;

	/**
	 * The multiplexers hashed by address/port (as long).
	 */
	private Hashtable multiplexers = new Hashtable();

	/**
	 * The connectors hashed by multiplexers.
	 */
	private Hashtable connectors = new Hashtable();

	/**
	 * The actual transceiver manager.
	 */
	private ITransceiverManager manager;

	/**
	 * Creates a transceiver that binds to any address. 
	 */
	public MxIPMulticastTransceiver() {
		super(true);
	}
	
	/**
	 * Creates a transceiver that binds to the specified address.
	 * 
	 * @param address A byte array of length 4 that contains the
	 * 	ip address to bind to.
	 */
	public MxIPMulticastTransceiver(byte[] address) {
		super(address, true);
	}

	/**
	 * Returns the plug-in description of the transceiver.
	 * 
	 * @return The plug-in description of the transceiver.
	 */
	public PluginDescription getPluginDescription() {
		if (description == null) {
			description = new PluginDescription(PLUGIN_ABILITY, EXTENSION_TRANSCEIVER);
		}
		return description;
	}


	/**
	 * Called by a multiplexer whenever a new connector is opened due to remote
	 * system request.
	 * 
	 * @param source The multiplexer that received the request.
	 * @param connector The connector that has been established by the multiplexer.
	 */
	public void acceptConnector(MultiplexFactory source, IStreamConnector connector) {
		manager.acceptSession(connector);
	}

	/**
	 * Called by a multiplexer whenever the multiplexer is closed.
	 * 
	 * @param multiplexer The multiplexer that closed the connection.
	 */
	public synchronized void closeMultiplexer(MultiplexFactory multiplexer) {
		IStreamConnector c = (IStreamConnector)connectors.remove(multiplexer);
		if (c != null) {
			c.release();	
		}
		Enumeration e = multiplexers.keys();
		while (e.hasMoreElements()) {
			Object k = e.nextElement();
			Vector v = (Vector)multiplexers.get(k);
			if (v != null) {
				if (v.removeElement(multiplexer)) {
					if (v.size() == 0) {
						multiplexers.remove(k);
					}
					return;
				}
			} 
		}	
	}

	/**
	 * Called whenever a connection should be established. This method will
	 * first determine whether there are any cached connections and then it
	 * might decide to open a new one if the existing ones do not satisfy
	 * the needs.
	 * 
	 * @param session The session that contains the necessary data. The local
	 * 	data object contains the port and the ip address of the remote system.
	 * @return The stream connector for the session.
	 * @throws IOException Thrown if the connector could not be created.
	 */
	public IStreamConnector openSession(ISession session) throws IOException {
		Object[] params = (Object[])session.getLocal();
		byte[] address = (byte[])params[0];
		int port = ((Integer)params[1]).intValue();
		Long key = getHashkey(address, port);
		Vector muxs = null;
		synchronized (this) {
			muxs = (Vector)multiplexers.get(key);
		}
		if (muxs == null) {
			IStreamConnector c = super.openSession(session);
			// create and transfer remote hash key
			Long remoteKey = getHashkey(getAddress(), getPort());
			DataOutputStream dos = new DataOutputStream(c.getOutputStream());
			dos.writeLong(remoteKey.longValue());
			dos.flush();
			// create and register multiplexer
			MultiplexFactory mux = new MultiplexFactory(this, c.getInputStream(), c.getOutputStream());
			synchronized (this) {
				if (isEnabled()) {
					muxs = (Vector)multiplexers.get(key);
					if (muxs == null) muxs = new Vector();
					muxs.addElement(mux);
					multiplexers.put(key, muxs);
					connectors.put(mux, c);	
					return mux.openConnector();
				} else {
					mux.close();
					throw new IOException("Plugin disabled.");
				}
			}
		} else {
			MultiplexFactory m = (MultiplexFactory)muxs.elementAt(0);
			return m.openConnector();	
		}
	}

	/**
	 * Sets the transceiver manager to a wrapper in order to enable the 
	 * interception of session accepts.
	 * 
	 * @param manager The real manager provided by the plug-in manager.
	 */
	public void setTransceiverManager(ITransceiverManager manager) {
		this.manager = manager;
		if (manager != null) {
			super.setTransceiverManager(new TransceiverManager(manager));	
		} else {
			super.setTransceiverManager(null);
		}
	}

	/**
	 * Returns the hash key for the specified address and port.
	 * 
	 * @param address The address (a 4 byte array).
	 * @param port The port.
	 * @return A hash key used to retrieve multiplexers.
	 */
	private Long getHashkey(byte[] address, int port) {
		return new Long(address[0] << 24 | address[1] << 18 | address[2] << 16 | address[3] << 8 | port);
	}
	
	/**
	 * Returns a reference to the plug-in manager as required by the
	 * multiplexer plug-in interface.
	 * 
	 * @return The plug-in manager of the plug-in.
	 */
	public IPluginManager getPluginManager() {
		return manager;
	}


}
