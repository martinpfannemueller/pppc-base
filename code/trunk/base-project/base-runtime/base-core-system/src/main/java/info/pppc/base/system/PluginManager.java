package info.pppc.base.system;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.GroupConnector;
import info.pppc.base.system.plugin.IDiscovery;
import info.pppc.base.system.plugin.IDiscoveryManager;
import info.pppc.base.system.plugin.IDispatcher;
import info.pppc.base.system.plugin.IModifier;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IRouting;
import info.pppc.base.system.plugin.IRoutingManager;
import info.pppc.base.system.plugin.ISemantic;
import info.pppc.base.system.plugin.ISemanticManager;
import info.pppc.base.system.plugin.ISessionStrategy;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.IStreamer;
import info.pppc.base.system.plugin.ITransceiver;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;


/**
 * This class is the core of the plug-in architecture. It loads
 * plug-ins and provides supportive functionality. The plug-in manager
 * also provides a default implementation of the plug-in strategy that
 * does not perform any plug-in ordering.
 *  
 * @author Marcus Handte
 */
public final class PluginManager implements IPluginManager, ISemanticManager, 
		ITransceiverManager, IDiscoveryManager, IRoutingManager, IExtension {

	/**
	 * this core.
	 */
	public final static int PACKET_LENGTH = 1024;
	
	/**
	 * The session implementation used by the plug-in manager. At the
	 * present time this is a list. If we want to add multicast at
	 * a lower level, we would have to transform this into a tree 
	 * structure.
	 * 
	 * @author Marcus Handte
	 */
	public final class Session implements ISession {

		/**
		 * The parent of the session. This denotes a plug-in
		 * that lies above this plug-in.
		 */
		private Session parent = null;
		
		/**
		 * The child of the session. This denotes a plug-in
		 * that lies below this plug-in.
		 */
		private Session child = null;

		/**
		 * A place holder to store local data during the negotiation
		 * phase that is later on used to open a connector.
		 */
		private Object localData = null;
		
		/**
		 * A place holder to store remote data during the negotiation
		 * phase that is later on used to open a connector at the
		 * other end point.
		 */
		private byte[] remoteData = null;

		/**
		 * The ability of the plug-in denoted by this session.
		 */
		private short ability = 0;

		/**
		 * A flag that indicates whether it is allowed to set and
		 * retrieve the remote data. For transceiver plug-ins it is
		 * not allowed to access the remote data as the plug-in
		 * manager cannot not serialize this data.
		 */
		private boolean remote = false;

		/**
		 * The system id that denotes the target of the session.
		 * This is used to reduce the number of parameters required
		 * by a semantic plug-in to renew a session.
		 */
		private SystemID target = null;

		/**
		 * A flag that indicates whether the session is incoming.
		 */
		private	boolean incoming = false;
		
		/**
		 * Creates a new session object that denotes a plug-in with the
		 * specified ability. The boolean parameter determines whether
		 * the plug-in will throw an exception if the remote data is
		 * set. Setting remote data is not allowed for transceiver 
		 * plug-ins.
		 * 
		 * @param ability The ability of the plug-in denoted by this
		 * 	session object.
		 * @param remote True if the remote session data can be stored,
		 * 	false if storing remote session data should throw an 
		 * 	exception.
		 * @param target The remote system that would be the target
		 * 	of the session.
		 * @param incoming A flag that indicates whether the specified
		 * 	session is incoming. 
		 */
		public Session(SystemID target, short ability, boolean remote, boolean incoming) {
			this.ability = ability;
			this.remote = remote;
			this.target = target;
			this.incoming = incoming;
		}

		/**
		 * Returns the local data object that has been negotiated during the
		 * negotiation phase.
		 * 
		 * @return The local data object negotiated previously.
		 */
		public Object getLocal() {
			return localData;
		}

		/**
		 * Sets the local data object that contains the negotiated connection
		 * properties that are required to open a connection.
		 * 
		 * @param data The local data object required to open a connection.
		 */
		public void setLocal(Object data) {
			localData = data;
		}

		/**
		 * Sets the remote data that will be transfered to the other endpoint.
		 * If the remote data cannot be accessed, this method will thrown an
		 * exception.
		 * 
		 * @param data The remote data to set.
		 * @throws RuntimeException Thrown if the remote data cannot
		 * 	be accessed.
		 */
		public void setRemote(byte[] data) {
			if (remote) {
				remoteData = data;
			} else {
				throw new RuntimeException("Cannot set remote data.");
			}
		}

		/**
		 * Returns the remote data that is currently set. If the remote
		 * data cannot be accessed this method will throw an exception.
		 * 
		 * @return The remote data that has been set previously.
		 * @throws RuntimeException Thrown if the remote data cannot
		 * 	be accessed.
		 */
		public byte[] getRemote() {
			if (remote) {
				return remoteData;
			} else {
				throw new RuntimeException("Cannot access remote data.");
			}
		}

		/**
		 * Determines whether it is valid to set and retrieve the remote
		 * data bytes. 
		 * 
		 * @return True if the remote data can be accessed, false otherwise.
		 */
		public boolean supportsRemote() {
			return remote;
		}

		/**
		 * Determines whether the session is incoming.
		 * 
		 * @return True if it is incoming, false otherwise. 
		 */
		public boolean isIncoming() {
			return incoming;
		}
		
		/**
		 * Determines whether the session is outgoing.
		 * 
		 * @return True if it is outgoing, false otherwise.
		 */
		public boolean isOutgoing() {
			return !incoming;
		}


		/**
		 * Sets the parent of the session. This is the communication
		 * layer above this session.
		 * 
		 * @param parent The new parent of the session.
		 */
		public void setParent(Session parent) {
			this.parent = parent;
		}
		
		/**
		 * Returns the parent of the session. This is the communication
		 * layer above this plug-in.
		 * 
		 * @return The parent of the session or null if none is set.
		 */		
		public Session getParent() {
			return parent;
		}
		
		/**
		 * Sets the child of the session. This is the communication
		 * layer below this session.
		 * 
		 * @param child The new child of the session.
		 */
		public void setChild(Session child) {
			this.child = child;
		}
		
		/**
		 * Returns the child of the session. This is the communication
		 * layer below this session.
		 * 
		 * @return The child of the session or null if none is set.
		 */
		public ISession getChild() {
			return child;
		}
		
		/**
		 * Returns the target of the session. This is the device id
		 * of the device for which this session has been created.
		 * 
		 * @return The target device of the session.
		 */
		public SystemID getTarget() {
			return target;
		}
		
		/**
		 * Returns the ability of the session. This is the ability
		 * of the plug-in that is bound to this session.
		 * 
		 * @return The ability of the session.
		 */
		public short getAbility() {
			return ability;
		}
		
		/**
		 * Creates a copy of the session. The copy can either be
		 * deep or shallow. If it is deep, the parent and child
		 * of the session will be copied recursively. Otherwise
		 * the parent and child of the new copy will be null. 
		 * 
		 * @param deep A flag that indicates whether the copy should
		 * 	be deep or shallow.
		 * @return The deep or shallow copy of the session.
		 */
		public Session copy(boolean deep) {
			Session session = new Session(target, ability, remote, incoming);
			session.localData = localData;
			session.remoteData = remoteData;
			session.target = target;
			if (deep) {
				if (parent != null) {
					session.parent = parent.copy(deep);
				}
				if (child != null) {
					session.child = child.copy(deep);
				}
			}
			return session;
		}
		
	}

	/**
	 * The default session strategy that is used in all cases where no 
	 * specialized strategy is available. This strategy will add extension
	 * layers only if there is a need for them and it does not perform an 
	 * additional ordering of plug-ins.
	 * 
	 * @author Marcus Handte
	 */
	public final class SessionStrategy implements ISessionStrategy {

		/**
		 * Performs a selection on the plug-ins for the specified extension layer.
		 * 
		 * @param extension The extension point to select for.
		 * @param plugins The plug-in descriptions to select from.
		 * @param collection The non-functional requirements towards the selection.
		 * @return A vector of plug-ins that correspond to the query.
		 */
		public Vector getPlugin(short extension, Vector plugins, NFCollection collection) {
			return plugins;	
		}
	}
	
	/**
	 * Database which holds all known plug-ins and plug-in descriptions 
	 * per system. These can be used for processing messages and 
	 * invocations between the local and remotely available systems.
	 */
	private DeviceRegistry registry;

	/**
	 * The invocation broker used to deliver incoming calls that must be
	 * executed locally.
	 */
	private InvocationBroker broker;

	/**
	 * Implementation of a specific strategy which controls the usage of 
	 * specific plug-ins regarding their nonfunctional attributes.
	 */
	private ISessionStrategy strategy = new SessionStrategy();
	
	/**
	 * The plug-in listeners that are registered at the plug-in manager.
	 * The plug-in listeners are informed about changes to plug-ins, i.e.
	 * if they are removed and added.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The plug-ins that are currently installed in this broker.
	 */
	private Vector plugins = new Vector();
	
	/**
	 * The connectors from transceivers that are currently
	 */
	private Vector connectors = new Vector();
	
	/**
	 * The group connectors that are currently opened.
	 */
	private Vector groups = new Vector();
	
	/**
	 * The manager listener is used to open the packet connectors
	 * of transceiver plug-ins for group communication.
	 */
	private IListener manager = new IListener() {
		public void handleEvent(Event event) {
			ITransceiver p = (ITransceiver)event.getSource();
			switch (event.getType()) {
				case ITransceiver.EVENT_TRANCEIVER_ENABLED: {
					try {
						IPacketConnector c = p.openGroup();
						connectors.addElement(c);
						for (int i = groups.size() - 1; i >= 0; i--) {
							GroupConnector gc = (GroupConnector)groups.elementAt(i);
							if (gc.getAbility() == null || gc.getAbility().shortValue() 
									== p.getPluginDescription().getAbility()) {
								gc.addConnector(c);
							}
						}
					} catch (IOException e) {
						Logging.debug(getClass(), "Could not open packet connector from transceiver.");
					}
					break;
				}
				case ITransceiver.EVENT_TRANCEIVER_DISABLED: {
					for (int i = connectors.size() - 1; i >= 0; i--) {
						IPacketConnector c = (IPacketConnector)connectors.elementAt(i);
						if (c.getPlugin() == p) {
							connectors.removeElementAt(i);
							c.release();
						}
					}
					break;
				}
				default:
					break;
			}
		}
	};
	
	/**
	 * Creates a new plug-in manager that uses the specified invocation
	 * broker to deliver incoming messages and to update the device
	 * registry and to announce the device.
	 * 
	 * @param broker The invocation broker used by this plug-in manager.
	 * @throws NullPointerException Thrown if the broker is null.
	 */
	protected PluginManager(InvocationBroker broker) {
		this.broker = broker;
		this.registry = broker.getDeviceRegistry();
		broker.addBrokerListener(InvocationBroker.EVENT_BROKER_SHUTDOWN, new IListener() {
			public void handleEvent(Event event) {
				Logging.debug(getClass(), "Removing plugin manager due to broker shutdown.");
				synchronized (plugins) {
					while (! plugins.isEmpty()) {
						removePlugin((IPlugin)plugins.elementAt(0));
						plugins.removeElementAt(0);
					}
				}
			}
		});
	}

	/**
	 * Add a set of plug-ins to this manager. This method updates internal
	 * data structures, sets call backs and starts plug-ins.
	 * 
	 * @param newPlugins A set of local plug-ins.
	 */
	public void addPlugins(IPlugin[] newPlugins) {
		for (int i = 0; i < newPlugins.length; i++) {
			addPlugin(newPlugins[i]);
		}
	}
	
	/**
	 * Adds and starts the specified plug-in. This method updates internal 
	 * data structures, sets the call back and starts the plug-in.
	 * 
	 * @param plugin The plug-in to add.
	 */
	public void addPlugin(IPlugin plugin) {
		Logging.debug(getClass(), "Installing plugin " + plugin.getClass().getName());
		synchronized (plugins) {
			PluginDescription pd = plugin.getPluginDescription();
			IPlugin p = getPlugin(pd.getAbility());
			if (p == null) {
				plugins.addElement(plugin);
				registry.registerPlugin(SystemID.SYSTEM, pd);
				try {
					switch (pd.getExtension()) {
						case EXTENSION_SEMANTIC:
							ISemantic sp = (ISemantic)plugin;
							sp.setSemanticManager(this);
							break;
						case EXTENSION_ROUTING:
							IRouting rp = (IRouting)plugin;
							rp.setRoutingManager(this);
							break;
						case EXTENSION_SERIALIZATION:
							IModifier zp = (IModifier)plugin;
							zp.setPluginManager(this);
							break;
						case EXTENSION_COMPRESSION:
							IModifier cp = (IModifier)plugin;
							cp.setPluginManager(this);
							break;
						case EXTENSION_ENCRYPTION:
							IModifier ep = (IModifier)plugin;
							ep.setPluginManager(this);
							break;
						case EXTENSION_TRANSCEIVER:
							ITransceiver tp = (ITransceiver)plugin;
							tp.setTransceiverManager(this);
							tp.addTransceiverListener(ITransceiver.EVENT_TRANCEIVER_DISABLED 
									| ITransceiver.EVENT_TRANCEIVER_ENABLED, manager);
							break;
						case EXTENSION_DISCOVERY:
							IDiscovery dp = (IDiscovery)plugin;	
							dp.setDiscoveryManager(this);
							break;
						default:
							Logging.debug(getClass(), "Unkown plugin extension found.");
							throw new RuntimeException("Unkown plugin extension.");
					}
					plugin.start();
				} catch (Throwable t) {
					Logging.error(getClass(), "Could not install plugin.", t);
					plugins.removeElement(plugin);
					registry.removePlugin(SystemID.SYSTEM, pd);
				}
				listeners.fireEvent(EVENT_PLUGIN_ADDED, pd);
			} else {
				throw new IllegalArgumentException("Cannot install plugin with same ability.");
			}
		}
	}

	/**
	 * Removes the specified plug-in from the set of plug-ins that 
	 * are installed and managed by this plug-in manager.
	 * 
	 * @param plugin The plug-in that should be removed.
	 */
	public void removePlugin(IPlugin plugin) {
		Logging.debug(getClass(), "Removing plugin " + plugin.getClass().getName());
		synchronized (plugins) {
			PluginDescription pd = plugin.getPluginDescription();
			for (int i = 0; i < plugins.size(); i++) {
				IPlugin p = (IPlugin)plugins.elementAt(i);
				if (p == plugin) {
					try {
						plugin.stop();
					} catch (Throwable t) {
						Logging.error(getClass(), 
							"Caught exception while removing plugin.", t);
					}
					if (p.getPluginDescription().getExtension() == EXTENSION_TRANSCEIVER) {
						ITransceiver transceiver = (ITransceiver)p;
						transceiver.removeTransceiverListener(ITransceiver.EVENT_TRANCEIVER_DISABLED 
								| ITransceiver.EVENT_TRANCEIVER_ENABLED, manager);
					}
					listeners.fireEvent(EVENT_PLUGIN_REMOVED, pd);
					registry.removePlugin(SystemID.SYSTEM, pd);
					plugins.removeElement(this);
				}
			}
		}
	}
	
	/**
	 * Returns the plug-ins that are currently installed and managed
	 * by this plug-in manager.
	 * 
	 * @return The installed plug-ins that are currently managed by this
	 * 	plug-in manager.
	 */
	public IPlugin[] getPlugins() {
		synchronized (plugins) {
			IPlugin[] ps = new IPlugin[plugins.size()];
			for (int i = 0; i < ps.length; i++) {
				ps[i] = (IPlugin)plugins.elementAt(i);
			}
			return ps;
		}
	}

	/**
	 * Sets the strategy used by the plug-in manager.
	 * 
	 * @param newStrategy The strategy of the plug-in manager.
	 */
	public synchronized void setStrategy(ISessionStrategy newStrategy) {
		if (newStrategy == null) {
			newStrategy = new SessionStrategy();
		}			
		strategy = newStrategy;	
	}

	/**
	 * Called by the broker whenever an invocation must be delivered. This
	 * method forwards the invocation to an adequate semantic plug-in. To
	 * do this, the strategy will try to select a plug-in and a session will
	 * be created.
	 * 
	 * @param invocation The invocation to deliver.
	 */
	protected void sendSynchronous(Invocation invocation) {
		SystemID target = invocation.getTarget().getSystem();
		NFCollection collection = invocation.getRequirements();
		PluginDescription[] compatible = registry.getPluginDescriptions(SystemID.SYSTEM, target);
		Session session = prepareSession(EXTENSION_SEMANTIC, target, compatible, collection);
		if (session == null) {
			invocation.setException(new InvocationException("Could not satisfy requirements."));
		} else {
			try {
				short ability = session.getAbility();
				ISemantic plugin = (ISemantic)getPlugin(ability);
				plugin.performOutgoing(invocation, session);
			} catch (Throwable t) {
				invocation.setException(new InvocationException("Could not forward invocation."));
			}
		}
	}

	/**
	 * Creates a session to communicate with the specified target.
	 * 
	 * @param target The target of the call.
	 * @param ability The ability of the calling plug-in. 
	 * @return The session object.
	 */
	public ISession createSession(SystemID target, short ability) {
		return new Session(target, ability, true, false);
	}
	
	/**
	 * Called by a semantic plug-in whenever the semantic plug-in tries to
	 * recreate a communication stack, possibly with different properties.
	 * 
	 * @param session The session that was originally negotiated. This session
	 * 	is used as blueprint for the new session to create.
	 * @param requirements The nf-requirements. They might have changed since
	 * 	the last negotiation of the provided session.
	 * @return The prepared session for the specified parameters.
	 */
	public ISession prepareSession(ISession session, NFCollection requirements) {
		// prepares a new session, the passed session is used as basis for the new one
		Session s = (Session)session;
		SystemID target = s.getTarget();
		PluginDescription[] compatible = registry.getPluginDescriptions(SystemID.SYSTEM, target);
		Session child = prepareSession(EXTENSION_SERIALIZATION, target, compatible, requirements);
		if (child != null) {
			Session copy = new Session
				(s.getTarget(), s.getAbility(), true, false);
			copy.setLocal(s.getLocal());
			copy.setRemote(s.getRemote());
			copy.setChild(child);
			child.setParent(copy);
			return copy;
		} else {
			return null;	
		} 
	}
	
	/**
	 * Prepares a session starting from the specified layer down to the transceiver 
	 * layer. This method works only for the extensions specified in the extension
	 * interface. It does not perform checks, so specifying some non-existing extension
	 * might lead to endless loops or crashes. At the present time, this method 
	 * supports the non-functional dimensions NFDimension.IDENTIFIER_ABILITY and
	 * NFDimension.IDENTIFIER_REQUIRED. Using the ability identifier this method
	 * ensures that only those plug-ins that have a matching ability are used. The
	 * required layer is used to determine whether the composition fails without
	 * the specified layer. This method relies on the selection strategy to determine
	 * the preferences between plug-ins. 
	 *  
	 * @param extension The extension to start from.
	 * @param target The target device of the session.
	 * @param compatible The set of plug-ins that is available on the target and this device.
	 * @param collection The non-functional requirements.
	 * @return A session or null if no session could be created.
	 */
	private Session prepareSession(short extension, SystemID target, PluginDescription[] compatible, NFCollection collection) {
		// remove all plug-ins that are not on this layer
		Vector plugins = new Vector();
		for (int i = compatible.length - 1; i >= 0; i--) {
			PluginDescription pd = compatible[i];
			if (pd.getExtension() == extension) {
				plugins.addElement(pd);
			}
		}
		// remove all plug-ins that do not match a possibly specified ability
		NFDimension ability = collection.getDimension(extension, NFDimension.IDENTIFIER_ABILITY);
		if (ability != null) {
			short a = ((Short)ability.getHardValue()).shortValue();
			for (int i = plugins.size() - 1; i >= 0 ; i--) {
				PluginDescription pd = (PluginDescription)plugins.elementAt(i);
				if (pd.getAbility() != a) {
					plugins.removeElementAt(i);	
				}
			}
		}
		
		// perform selection using the strategy
		ISessionStrategy ss = null;
		synchronized (this) {
			ss = strategy;
		}
		Vector selected = ss.getPlugin(extension, plugins, collection);
		// try to create a session object and perform recursion if session
		// preparation was successfully performed
		for (int i = 0, s = selected.size(); i < s; i++) {
			PluginDescription pd = (PluginDescription)selected.elementAt(i);
			IPlugin plugin = getPlugin(pd.getAbility());
			// create a local or remote session, remote only for non-transceivers
			Session session = new Session(target, pd.getAbility(), 
				extension != EXTENSION_TRANSCEIVER && extension != EXTENSION_ROUTING, false);
			if (plugin != null) {
				// prepare session and continue, if successful
				boolean success = false;
				short next = 0;
				NFCollection copy = collection.copy(true);
				try {
					switch (extension) {
						case EXTENSION_SEMANTIC:
							ISemantic sp = (ISemantic)plugin;
							success = sp.prepareSession(pd, copy, session);
							break;
						case EXTENSION_SERIALIZATION:
							IModifier zp = (IModifier)plugin;
							success = zp.prepareSession(pd, copy, session);
							next = EXTENSION_COMPRESSION;
							break;
						case EXTENSION_COMPRESSION:
							IModifier cp = (IModifier)plugin;
							success = cp.prepareSession(pd, copy, session);
							next = EXTENSION_ENCRYPTION;
							break;
						case EXTENSION_ENCRYPTION:
							IModifier ep = (IModifier)plugin;
							success = ep.prepareSession(pd, copy, session);
							next = EXTENSION_ROUTING;
							break;
						case EXTENSION_ROUTING:
							IRouting rp = (IRouting)plugin;
							success = rp.prepareSession(pd, copy, session);
							break;
						case EXTENSION_TRANSCEIVER:
							ITransceiver tp = (ITransceiver)plugin;
							success = tp.prepareSession(pd, copy, session);
							break;
						default:
							// will never happen, except if plug-in is broken
					}
				} catch (Throwable t) {
					Logging.error(getClass(), "Could not prepare session.", t);
				}
				// if successful, perform recursive calls
				if (success) {
					if (next != 0) {
						// non transceiver plug-in, continue composition
						Session child = prepareSession(next, target, compatible, copy);
						if (child != null) {
							session.setChild(child);
							child.setParent(session);
							return session;
						}
					} else {
						// end recursion at transceiver plug-ins
						return session;
					}
				}
			}
		}
		// at this point, no session was planned, determine
		// whether layer is necessary and continue or abort
		NFDimension dim = collection.getDimension(extension, NFDimension.IDENTIFIER_REQUIRED);
		if (dim == null || dim.getHardValue().equals(new Boolean(false))) {
			// continue, it is not neccessarily required
			short next = 0;
			switch (extension) {
				case EXTENSION_SEMANTIC:
					next = EXTENSION_SERIALIZATION;
					break;
				case EXTENSION_SERIALIZATION:
					next = EXTENSION_COMPRESSION;
					break;
				case EXTENSION_COMPRESSION:
					next = EXTENSION_ENCRYPTION;
					break;
				case EXTENSION_ENCRYPTION:
					next = EXTENSION_ROUTING;
					break;
				case EXTENSION_ROUTING:
					next = EXTENSION_TRANSCEIVER;
					break;
				default:
					// will never happen, except if plug-in is broken
			}
			if (next != 0) {
				return prepareSession(next, target, compatible, collection);
			} 
		}
		return null;
	}

	/**
	 * Called by a semantic plug-in whenever a connector is required to 
	 * deliver an invocation or to perform some remote communication.
	 * 
	 * @param session The session information used to create the connector.
	 * @return A stream connector that is composed from the session parameters.
	 * @throws IOException Thrown if the connector cannot be opened. 
	 */
	public IStreamConnector openSession(ISession session) throws IOException {
		// determine whether session is null and throw io exception
		if (session == null) throw new IOException("Cannot create stack for null session.");
		// sanity check
		if (session.isIncoming()) {
			throw new IOException("Cannot open an incoming session.");
		}
		Session s = (Session)session;
		// start bottom up construction and count sessions
		short sessions = 0;
		while (s.getChild() != null) {
			s = (Session)s.getChild();
			sessions += 1;		
		}
		// lowest layer must be transceiver
		IStreamer transceiver = (IStreamer)getPlugin(s.getAbility());
		if (transceiver == null) {
			throw new IOException("Could not find transceiver plugin."); 
		} 
		IStreamConnector connector = transceiver.openSession((ISession)s);
		// serialize session data - top down
		try {
			// <source><sessioncount>(<ability><remotedatacount><remotedata>)*
			OutputStream stream = connector.getOutputStream();	
			ObjectOutputStream oos = new ObjectOutputStream(stream);
			oos.writeBytes(SystemID.SYSTEM.getBytes());
			oos.writeShort(sessions);
			Session sd = (Session)session;
			for (int i = 0; i < sessions; i++) {
				oos.writeShort(sd.getAbility());
				byte[] remote = sd.getRemote();
				if (remote == null) {
					oos.writeShort((short)-1);	
				} else {
					oos.writeShort((short)remote.length);
					oos.writeBytes(remote);
				}
				sd = (Session)sd.getChild();
			}
			oos.flush();
		} catch (IOException e) {
			Logging.debug(getClass(), "Could not serialize session data.");
			throw e;
		}
		// open remaining modifiers
		while (s.getParent() != session) {
			s = s.getParent();
			IModifier modifier = (IModifier)getPlugin(s.getAbility());
			if (modifier == null) {
				throw new IOException("Could not find modifier plugin.");
			}
			try {
				connector = modifier.openSession(connector, (ISession)s);	
			} catch (IOException e) {
				connector.release();
				throw e;
			}
		}
		return connector;
	}

	/***
	 * Called by transceiver plug-ins whenever a new connector is opened by a 
	 * remote device. This implementation tries to read the session information
	 * from the input stream and creates a communication stack according to the
	 * specification provided by the remote plug-in manager. See the open session
	 * command for details.
	 * 
	 * @param c The connector that has been opened by a remote device.
	 */
	public void acceptSession(final IStreamConnector c) {
		IOperation receiver = new IOperation() {
			public void perform(IMonitor monitor) throws Exception {
				try {
					IStreamConnector connector = c;
					// <source><sessioncount>(<ability><remotedatacount><remotedata>)*
					InputStream input = connector.getInputStream();
					ObjectInputStream ois = new ObjectInputStream(input);
					byte[] sid = new byte[SystemID.LENGTH];
					ois.readBytes(sid);
					SystemID source = new SystemID(sid);
					short sessions = ois.readShort();
					Session parent = null;
					for (int i = 0; i < sessions; i++) {
						short ability = ois.readShort();
						short count = ois.readShort();
						byte[] remote = null;
						if (count != -1) {
							remote = new byte[count];
							while (count > 0) {
								int read = ois.read(remote, remote.length - count, count);
								if (read == -1) {
									throw new IOException("Could not read remote data.");
								} else {
									count -= read;
								}
							}							
						}
						Session session = new Session(source, ability, true, true);
						if (parent != null) {
							parent.setChild(session);
							session.setParent(parent);
						}
						session.setRemote(remote);
						parent = session;
					}
					// compose stack, starting from lowest session
					for (int i = 0; i < sessions - 1; i++) {
						IModifier modifier = (IModifier)getPlugin(parent.getAbility());
						connector = modifier.openSession(connector, (ISession)parent);		
						parent = parent.getParent();	
					}
					// finally, open semantic plug-in and fire it out
					IDispatcher semantic = (IDispatcher)getPlugin(parent.getAbility());
					semantic.deliverIncoming(connector, parent);
				} catch (IOException e) {
					//Logging.error(getClass(), "Could not deserialize session data.", e);
					Logging.debug(getClass(), "Could not deserialize session data.");
					c.release();
				} catch (NullPointerException e) {
					Logging.error(getClass(), "Could not find plugin with ability.", e);
					c.release();
				} catch (ClassCastException e) {
					Logging.error(getClass(), "Could not cast plugin to type.", e);
					c.release();
				} catch (NumberFormatException e) {
					Logging.error(getClass(), "Could not read incoming system id.", e);
					c.release();
				}
			}
		};
		broker.performOperation(receiver);
	}

	/**
	 * Called by a semantic plug-in to deliver an incoming invocation to 
	 * the broker. This method simply forwards the incoming invocation.
	 * 
	 * @param invocation The invocation that has been received.
	 * @param session The session used to receive the invocation.
	 */
	public void dispatchSynchronous(Invocation invocation, ISession session) {
		broker.dispatchSynchronous(invocation, session);
	}

	/**
	 * Returns the plug-in descriptions of the specified device. This implementation
	 * simply forwards the request to the local device registry.
	 * 
	 * @param system The system id of the system.
	 * @return The plug-in descriptions of visible plug-ins of the system
	 */
	public PluginDescription[] getPluginDescriptions(SystemID system) {
		return registry.getPluginDescriptions(system);
	}

	/***
	 * Returns the device description of the local device. This implementation
	 * simply forwards the request to the device registry.
	 * 
	 * @param system The system to retrieve.
	 * @return The device description of the local system.
	 */
	public DeviceDescription getDeviceDescription(SystemID system) {
		return registry.getDeviceDescription(system);
	}
	
	/**
	 * Returns the devices that are present in the environment.
	 * 
	 * @return The available devices.
	 */
	public SystemID[] getDevices() {
		return registry.getDevices();
	}

	/***
	 * Adds a plug-in listener to the set of registered plug-ins listeners.
	 * 
	 * @param type The types of events to register for.
	 * @param listener The listener to register.
	 */
	public void addPluginListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}

	/***
	 * Removes a previously registered plug-in listener from the set of registered
	 * plug-in listeners.
	 * 
	 * @param type The types of events to unregister for.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed, false otherwise.
	 */
	public boolean removePluginListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Registers the specified device description of the specified system. The 
	 * device description stays valid for the specified time to live. This implementation
	 * simply forwards the device description to the device registry of the
	 * broker.
	 * 
	 * @param device The device description of the remote system.
	 * @param ttl The time to live for the device description in milliseconds.
	 */
	public void registerDevice(DeviceDescription device, long ttl) {
		if (device != null) {
			SystemID id = device.getSystemID();
			if (id != null) {
				if (ttl >= 0) {
					if (! id.equals(SystemID.SYSTEM)) {
						registry.registerDevice(device, ttl);									
					}
				} else {
					Logging.debug(getClass(), "Omitting negative device ttl.");
				}
			} else {
				Logging.debug(getClass(), "Omitting null device announcement.");
			}		
		} else {
			Logging.debug(getClass(), "Omitting unset device annoucement.");
		}
	}

	/**
	 * Registers the specified plug-in description of the specified remote device.
	 * The plug-in description stays valid for the specified time to live. This
	 * method of the plug-in manager simply forwards the request to the device
	 * registry.
	 * 
	 * @param id The id of the system that hosts the plug-in.
	 * @param plugin The plug-in description to register.
	 * @param ttl The time to live in milliseconds.
	 */
	public void registerPlugin(SystemID id, PluginDescription plugin, long ttl) {
		if (id != null) {
			if (plugin != null) {
				if (ttl >= 0) {
					if (! id.equals(SystemID.SYSTEM)) {
						registry.registerPlugin(id, plugin, ttl);									
					}
				} else {
					Logging.debug(getClass(), "Omitting negative plugin ttl.");
				}
			} else {
				Logging.debug(getClass(), "Omitting null plugin announcement.");
			}
		} else {
			Logging.debug(getClass(), "Omitting unset plugin annoucement.");
		}
	}

	/**
	 * Returns the plug-in with the specified ability or null if such a plug-in
	 * does not exist.
	 * 
	 * @param ability The ability of the plug-in to lookup.
	 * @return The plug-in with the specified ability or null if such a plug-in
	 * 	does not exist.
	 */
	private IPlugin getPlugin(short ability) {
		synchronized (plugins) {
			for (int i = 0; i < plugins.size(); i++) {
				IPlugin plugin = (IPlugin)plugins.elementAt(i);
				PluginDescription description = (PluginDescription)plugin.getPluginDescription();
				if (description.getAbility() == ability) {
					return plugin;	
				}
			}
			return null;
		}		
	}

	/**
	 * Opens a group connector with all transceivers to the specified group.
	 * 
	 * @param group The group to connect to.
	 * @return The packet connector for this group and plug-in.
	 */
	public IPacketConnector openGroup(short group) {
		final GroupConnector gc = new GroupConnector(PACKET_LENGTH, group);
		gc.addPacketListener(IPacketConnector.EVENT_PACKET_CLOSED, new IListener() {
			public void handleEvent(Event event) {
				groups.removeElement(gc);
			}
		});
		for (int i = 0; i < connectors.size(); i++) {
			gc.addConnector((IPacketConnector)connectors.elementAt(i));
		}
		groups.addElement(gc);
		return gc;
	}
	
	/**
	 * Opens a group connector with transceivers that have the specified
	 * ability.
	 * 
	 * @param group The group to connect to.
	 * @param ability The ability of the plug-in to use.
	 * @return The packet connector for this group and plug-in.
	 */
	public IPacketConnector openGroup(short group, short ability) {
		final GroupConnector gc = new GroupConnector(PACKET_LENGTH, group, new Short(ability));
		gc.addPacketListener(IPacketConnector.EVENT_PACKET_CLOSED, new IListener() {
			public void handleEvent(Event event) {
				groups.removeElement(gc);
			}
		});
		for (int i = 0; i < connectors.size(); i++) {
			IPacketConnector pc = (IPacketConnector)connectors.elementAt(i);
			if (pc.getPlugin().getPluginDescription().getAbility() == ability) {
				gc.addConnector(pc);
			}
		}
		groups.addElement(gc);
		return gc;
	}
	
	
	/**
	 * Performs the specified operation using the invocation broker that is
	 * bound to this plug-in manager.
	 * 
	 * @param operation The operation to perform.
	 */
	public void performOperation(IOperation operation) {
		broker.performOperation(operation);
	}

	/**
	 * Performs the specified operation using the specified operation monitor. The
	 * operation is executed by the invocation broker that uses this plug-in manager.
	 * 
	 * @param monitor The monitor used to observe the operation.
	 * @param operation The operation to perform.
	 */
	public void performOperation(IOperation operation, IMonitor monitor) {
		broker.performOperation(operation, monitor);
	}
	
}