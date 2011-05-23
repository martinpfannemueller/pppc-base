package info.pppc.base.system;


/**
 * The session is used to create a communication stack and to request new stacks.
 * Additionally it is used to transfer some session data to a remote plug-in. A
 * semantic plug-in can reuse the context to create multiple stacks with the same
 * connection properties.
 * 
 * @author Marcus Handte
 */
public interface ISession {
	
	/**
	 * Returns some local parameters that have been set previously. These
	 * parameters are not transfered to a remote plug-in.
	 * 
	 * @return The local data that has been negotiated previously.
	 */
	public Object getLocal();
	
	/**
	 * Sets the local parameters during the negotiation phase. These 
	 * parameters are not transfered to a remote plug-in.
	 * 
	 * @param data The local parameters to set.
	 */
	public void setLocal(Object data);
	
	/**
	 * Sets the remote parameters that are transfered to a remote
	 * plug-in whenever the session is opened. Note that this option
	 * is not available for transceiver plug-ins. A plug-in can 
	 * determine the validity of this call by checking the value of
	 * the is remote method.
	 * 
	 * @param data The remote data that is transfered to the other
	 * 	end point.
	 * @throws RuntimeException Thrown if the remote data cannot
	 * 	be accessed.
	 */
	public void setRemote(byte[] data);

	/**
	 * Returns the remote parameters that are set at this session.
	 * Note that this option is not available for transceiver plug-ins.
	 * A plug-in can determine the validity of this call by checking
	 * the value of the is remote method.
	 * 
	 * @return The remote data that is set at this plug-in.
	 * @throws RuntimeException Thrown if the remote data cannot
	 * 	be accessed.
	 */
	public byte[] getRemote();

	/**
	 * Determines whether the session specification supports remote
	 * parameters. Most session specifications support remote parameters.
	 * The only session specification that does not support remote
	 * parameters is the one used for transceiver plug-ins. However,
	 * transceiver plug-ins can serialize their remote parameters before any
	 * connector is passed to the broker. Thus, transceiver plug-ins
	 * can perform their own initialization without remote data.
	 * 
	 * @return True if the session supports remote parameters, false
	 * 	otherwise.
	 */
	public boolean supportsRemote();
	
	/**
	 * Determines whether the session specification denotes an incoming
	 * session specification. This means that this session data has 
	 * been created by some remote plug-in. Note that incoming session
	 * specifications cannot be used to initialize connectors. However,
	 * semantic plug-ins can use the plug-in manager's prepare session
	 * method to transform an incoming session into an outgoing.
	 * 
	 * @return True if the session is incoming, false if it is outgoing.
	 */
	public boolean isIncoming();
	
	/**
	 * Determines whether the session specification denotes an outgoing
	 * session. Outgoing sessions can be used to initialize connectors.
	 * 
	 * @return True if the session is outgoing, false otherwise.
	 */
	public boolean isOutgoing();
	
	/**
	 * Returns the target of the session. This is the device id
	 * of the device for which this session has been created.
	 * 
	 * @return The target device of the session.
	 */
	public SystemID getTarget();
	
	/**
	 * Returns the ability of the session. This is the ability
	 * of the plug-in that is bound to this session.
	 * 
	 * @return The ability of the session.
	 */
	public short getAbility();

	/**
	 * Returns the child session bound to this session. This is the
	 * session that represents the plug-in below the current
	 * session. If no session is bound, the method returns null.
	 * 
	 * @return The child session or null, if there is none.
	 */
	public ISession getChild();
}
