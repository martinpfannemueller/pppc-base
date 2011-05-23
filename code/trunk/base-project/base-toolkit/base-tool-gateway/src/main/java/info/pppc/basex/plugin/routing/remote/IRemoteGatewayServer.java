package info.pppc.basex.plugin.routing.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The remote interface of the gateway server.
 * 
 * @author Mac
 *
 */
public interface IRemoteGatewayServer extends Remote {

	/**
	 * The port of the rmi registry.
	 */
	public final static int REGISTRY_PORT = 20001;
	
	/**
	 * The port of the object.
	 */
	public final static int OBJECT_PORT = 20002;

	/**
	 * The name of the remote object.
	 */
	public final static String OBJECT_NAME = "GatewayServer";
	
	/**
	 * The name of the host that hosts the rmi registry.
	 */
	public final static String HOST_NAME = "peces.nes.uni-due.de";
	
	/**
	 * Returns a hashtable of gateways with a list of devices
	 * that are connected to the gateways.
	 * 
	 * @return A hashtable of gateways hashed to a list of connected
	 * 	devices.
	 * @throws RemoteException Thrown if the call fails.
	 */
	public Hashtable<String, Vector<String>> getGateways() throws RemoteException;

	/**
	 * Returns the last set of log messages.
	 * 
	 * @return The log messages.
	 * @throws RemoteException Thrown if the call fails.
	 */
	public ArrayList<String> getMessages() throws RemoteException;

}
