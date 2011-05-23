package info.pppc.basex.plugin.semantic;

import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.ISemantic;
import info.pppc.base.system.plugin.ISemanticManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;
import info.pppc.base.system.util.Static;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The rmi semantic plug-in is the new default plug-in for asynchronous and synchronous 
 * method calls. It tries to reuse streams whenever possible within a single invocation.
 * Thus, this plug-in is much more efficient and less fragile with respect to 
 * timings than the previously used simplex semantic. However, it is less flexible
 * when it comes to reselections of protocol stacks. 
 * 
 * @author Marcus Handte
 */
public class RmiSemantic implements ISemantic {
	
	/**
	 * The number of connection retries upon a broken connection.
	 */
	private static final int RETRY_CONNECT = 2;
	
	/**
	 * The time to wait between (re-)connect retries in millis.
	 */
	private static final long WAIT_CONNECT = 50;
	
	/**
	 * The time to wait before the target system will dispose the result
	 * of an invocation, although it is not delivered.
	 */
	private static final long WAIT_DISPOSE = 5000;
	
	/**
	 * The ability of the plug-in. [5][3].
	 */
	public static final short PLUGIN_ABILITY = 0x0503;

	/**
	 * The plug-in description of the ip plug-in.
	 */
	private PluginDescription description = new PluginDescription
		(PLUGIN_ABILITY, EXTENSION_SEMANTIC);

	/**
	 * Hash table of hash tables hashed by system id. The internal hash table
	 * is hashed by invocation id and contains the incoming connector
	 * for the invocation.
	 */
	private Hashtable incoming = new Hashtable();

	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * The local semantic plug-in manager. Used to synchronize invocations
	 * that do not use the same semantic plug-in.
	 */
	private ISemanticManager manager;

	/**
	 * Creates a new semantic plug-in that provides a reliable remote method
	 * invocation semantic.
	 */
	public RmiSemantic() { }
	
	/**
	 * Sets the plug-in manager that is used to retrieve remote plug-in descriptions.
	 * 
	 * @param manager The semantic plug-in manager.
	 */
	public void setSemanticManager(ISemanticManager manager) {
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
	 * Called to start the plug-in. This method initializes the plug-in and
	 * enables the creation of connectors. All open calls will fail before
	 * this method has been called.
	 */
	public void start() {
		started = true;
	}

	/**
	 * Called to stop the plug-in. After this method has been called, all
	 * open calls will fail.
	 */
	public void stop() {
		started = false;
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
		if (dim.getHardValue().equals(new Short((short)NFCollection.TYPE_SYNCHRONOUS))) {
			s.setLocal(Static.TRUE); // true denotes synchronous invocation
			s.setRemote(new byte[] { 1 });
			return true;
		} else if (dim.getHardValue().equals(new Short((short)NFCollection.TYPE_ASYNCHRONOUS))) {
			s.setLocal(Static.FALSE); // false denotes asynchronous invocation
			s.setRemote(new byte[] { 0 });
			return true;	
		} else {
			return false;
		}
	}
	
	/**
	 * Delivers an outgoing invocation and modifies the original invocation
	 * to contain the results of the remote invocation after this method
	 * returns.
	 * 
	 * @param invocation The invocation that should be delivered.
	 * @param session The session data that can be used to request a new
	 * 	connection.
	 */
	public void performOutgoing(final Invocation invocation, final ISession session) {
		if (SystemID.SYSTEM.equals(invocation.getTarget().getSystem())) {
			if (session.getLocal() == Static.TRUE) {
				// if it is a local, synchronous call, execute it synchronously
				manager.dispatchSynchronous(invocation, session);	
			} else {
				// if it is a local, asynchronous call, execute it asynchronously
				IOperation asyncExec = new IOperation() {
					public void perform(IMonitor monitor) throws Exception {
						manager.dispatchSynchronous(invocation, session);
					}
				};
				manager.performOperation(asyncExec);
			}
		} else {
			IStreamConnector connector = null;
			// first, try to send the invocation
			for (int i = 0; i <= RETRY_CONNECT; i++) {
				try {
					connector = openSession(session, invocation.getRequirements());
					IObjectOutput out = (IObjectOutput)connector.getOutputStream();
					out.writeObject(Static.FALSE);
					out.writeObject(invocation);
					if (session.getLocal() == Static.TRUE) {
						// if it is a synchronous call, continue to receive the result
						break;	
					} else {
						// if it is an asynchronous call, return now
						connector.release();
						return;
					}
				} catch (IOException e) {
					// if the problem is caused by the serializer, the connector must be released
					if (connector != null) connector.release();
					Logging.debug(getClass(), "Connection attempt failed.");
					// Logging.error(getClass(), "Connection attempt failed.", e);
					// if retry exhausted, generate exception message and exit, else retry
					if (i == RETRY_CONNECT) {
						invocation.setException(new InvocationException("Could not deliver invocation."));
						return;
					} else {
						try {
							Thread.sleep(WAIT_CONNECT);
						} catch (InterruptedException e1) {
							Logging.debug(getClass(), "Thread got interrupted.");
						}	
					}
				}			
			}
			// here, the invocation has been transfered successfully
			for (int i = 0; i <= RETRY_CONNECT; i++) {
				try {
					IObjectInput in = (IObjectInput)connector.getInputStream();
					Invocation result = (Invocation)in.readObject();
					invocation.setResult(result.getResult());
					invocation.setException(result.getException());
					break;
				} catch (IOException e) {
					// if the failure is caused by the serializer, the connector must be released
					connector.release();
					Logging.debug(getClass(), "Reception attempt failed.");
					// if retry exhausted, generate exception message and exit, else reconnect
					if (i == RETRY_CONNECT) {
						invocation.setException(new InvocationException("Could not deliver invocation."));
						return;
					} else {
						try {
							Thread.sleep(WAIT_CONNECT);
						} catch (InterruptedException e1) {
							Logging.debug(getClass(), "Thread got interrupted.");
						}	
						// now try to reinitialize the connection
						try {
							connector = openSession(session, invocation.getRequirements());
							IObjectOutput out = (IObjectOutput)connector.getOutputStream();
							// prepare reconnect message and transmit
							out.writeObject(Static.TRUE);
							out.writeObject(invocation.getSource().getSystem());
							out.writeObject(invocation.getID());
						} catch (IOException ex) {
							Logging.debug(getClass(), "Reinialization failed.");
						}
					}
				} 
			} 
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
		NFDimension req = new NFDimension(NFDimension.IDENTIFIER_REQUIRED, Static.TRUE);
		collection.addDimension(EXTENSION_SERIALIZATION, req);
		collection.addDimension(EXTENSION_TRANSCEIVER, req);	
		// now prepare the session if possible
		session = manager.prepareSession(session, collection);
		return manager.openSession(session);
	}
	
	/**
	 * Delivers an incoming invocation and executes it if necessary.
	 * 
	 * @param connector The connector used to retrieve the invocation.
	 * @param session The session data of the connector.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session) {
		try {
			IObjectInput in = (IObjectInput)connector.getInputStream();
			Boolean sync = (Boolean)in.readObject();
			if (sync.booleanValue()) {
				SystemID system = (SystemID)in.readObject();
				Integer id = (Integer)in.readObject();
				synchronized (incoming) {
					boolean exists = false;
					Hashtable hash = (Hashtable)incoming.get(system);
					if (hash != null) {
						exists = (hash.get(id) != null);
					} 
					if (exists) {
						hash.put(id, connector);
						incoming.notifyAll();
					} else {
						Logging.debug(getClass(), "Received expired connect.");
						connector.release();						
					}					
				}
			} else {
				Invocation invoke = (Invocation)in.readObject();
				SystemID system = invoke.getSource().getSystem();
				Integer id = invoke.getID();
				synchronized (incoming) {
					Hashtable hash = (Hashtable)incoming.get(system);
					if (hash == null) {
						hash = new Hashtable();
						incoming.put(system, hash);
					}
					hash.put(id, connector);
				}
				manager.dispatchSynchronous(invoke, session);
				invoke.setArguments(null);
				invoke.setSignature(null);
				invoke.setRequirements(null);
				long now = System.currentTimeMillis();
				long timeout = now + WAIT_DISPOSE;
				retry: while (true) {
					Hashtable hash = (Hashtable)incoming.get(system);
					IStreamConnector c = (IStreamConnector)hash.get(id);
					try {
						if (c != null) {
							// do not send a result, if the call is asynchronous
							if (session.getRemote()[0] == 0) {
								break;
							} else {
								IObjectOutput out = (IObjectOutput)c.getOutputStream();
								out.writeObject(invoke);
								break;								
							}
						}	
					} catch (IOException ie) { 
						hash.remove(id);
						c.release();
					}
					synchronized (incoming) {
						while (now < timeout) {
							// retrieve connector, if changed retry
							c = (IStreamConnector)hash.get(id);
							if (c != null)
								continue retry;	
							// else wait until signal or expire
							try {
								incoming.wait(timeout - now);
							} catch (InterruptedException ie) {
								Logging.debug(getClass(), "Got interrupted.");
							}
							now = System.currentTimeMillis();
						}
						break retry;
					}	
				}
				synchronized (incoming) {
					Hashtable hash = (Hashtable)incoming.get(system);
					if (hash != null) {
						IStreamConnector c = (IStreamConnector)hash.remove(id);
						if (c != null) {
							c.release();
						}
						if (hash.isEmpty()) {
							incoming.remove(system);
						}
					}
				}
				// END clean up
			}
		} catch (IOException e) {
			// release connector if failure occurred in deserializer
			connector.release();
			Logging.debug(getClass(), "Could not receive invoke.");
			//Logging.error(getClass(), "Could not receive invoke.", e);
		}
	}

	/**
	 * Returns a string representation of the plug-ins internal structure.
	 * The string representation contains the currently running remote 
	 * invocations.
	 * 
	 * @return A string representation of the plug-ins internal data structures.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer("INCOMING <");
		synchronized (incoming) {
			Enumeration ienum = incoming.keys();
			int total = 0;
			while (ienum.hasMoreElements()) {
				Hashtable table = (Hashtable)incoming.get(ienum.nextElement());
				total += table.size();
			}
			b.append(total);
			b.append("|");
			Enumeration tenum = incoming.keys();
			while (tenum.hasMoreElements()) {
				Object key = tenum.nextElement();
				b.append("(");
				b.append(key);
				b.append("|");
				Hashtable table = (Hashtable)incoming.get(key);
				Enumeration senum = table.keys();
				while (senum.hasMoreElements()) {
					b.append(senum.nextElement());
					if (senum.hasMoreElements()) {
						b.append(",");	
					}
				}
				b.append(")");
			}
			b.append(">");
		}
		return b.toString();
	}
	
}
