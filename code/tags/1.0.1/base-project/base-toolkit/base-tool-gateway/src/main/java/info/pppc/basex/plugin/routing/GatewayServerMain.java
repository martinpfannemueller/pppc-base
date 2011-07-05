package info.pppc.basex.plugin.routing;

import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.StringTokenizer;

/**
 * The launcher class for the gateway server. This main class starts headless (i.e.
 * without a user interface). It exports the the gateway server as an RMI object
 * if it is running on the host that is specified in the remote interface. This
 * allows the usage of the applet to connect to the server. If you are running
 * a local version of the server, try using the swing application instead of
 * this. 
 * 
 * @author Mac
 */
public class GatewayServerMain {
	
	/**
	 * Starts the server using the specified arguments. The method accepts 0 and
	 * 2 parameters. For the 2 argument case, it expects a the host ip address
	 * and port number. In the 0 parameters case, default values are assumed.
	 * 
	 * @param args The command line arguments.
	 */
	public static void main(String[] args) {
		byte[] address = null;
		short port = 0;
		if (args.length == 0) {
			address = ProactiveRoutingGateway.ROUTER_ADDRESS;
			port = ProactiveRoutingGateway.ROUTER_PORT;
		} else if (args.length == 2) {
			address = getAddress(args[0]);
			try {
				port = Short.parseShort(args[1]);
			} catch (NumberFormatException e) {
				address = null;
			}
		}
		if (address != null) {
			boolean export = false;
			try {
				Logging.log(GatewayServerMain.class, "Trying to resolve " + IRemoteGatewayServer.HOST_NAME + ".");
				InetAddress inet = InetAddress.getByName(IRemoteGatewayServer.HOST_NAME);	
				Logging.log(GatewayServerMain.class, "Successfully resolved as " + inet.toString() + ".");
				byte[] bytes = inet.getAddress();
				boolean match = true;
				for (int i = 0; i < bytes.length; i++) {
					if (bytes[i] != address[i]) {
						Logging.log(GatewayServerMain.class, "Specified address is different.");
						match = false;
						break;
					}
				}
				export = match;
			} catch (UnknownHostException e) {
				Logging.debug(GatewayServerMain.class, "Could not resolve " + IRemoteGatewayServer.HOST_NAME + ".");
			}
			ProactiveGatewayServer s = new ProactiveGatewayServer(address, port);
			if (export) {
				try {
					Logging.log(GatewayServerMain.class, "Trying to create local registry.");
					LocateRegistry.createRegistry(IRemoteGatewayServer.REGISTRY_PORT);
					Logging.log(GatewayServerMain.class, "Local registry created.");
				} catch (RemoteException ex) {
					Logging.log(GatewayServerMain.class, "Failed to create local registry.");
				}
				try {
					Logging.log(GatewayServerMain.class, "Trying to export remote object.");
					IRemoteGatewayServer stub = (IRemoteGatewayServer) UnicastRemoteObject .exportObject(s, IRemoteGatewayServer.OBJECT_PORT);
					Naming.rebind("//" + IRemoteGatewayServer.HOST_NAME + ":" + IRemoteGatewayServer.REGISTRY_PORT + "/" + IRemoteGatewayServer.OBJECT_NAME, stub);
					Logging.log(GatewayServerMain.class, "Remote object exported.");
				} catch (MalformedURLException ex) {
					Logging.error(GatewayServerMain.class, "Failed to export remote object.", ex);
				} catch (RemoteException ex) {
					Logging.error(GatewayServerMain.class, "Remote exception during export.", ex);
				}
			} else {
				Logging.log(GatewayServerMain.class, "Skipping export as remote object.");				
			}
			s.start(export);
		} else {
			printUsage();
		}
	}

	/**
	 * Prints the usage to the standard out.
	 */
	public static void printUsage() {
		System.out.println("Usage: GatewayServerMain "
				+ "<ipaddress> <port>");
		System.out.println("Example: GatewayServerMain "
				+ "123.123.123.123 5555");
	}

	/**
	 * Returns an ip address as series of bytes from a string or null if the
	 * address is not correct.
	 * 
	 * @param string
	 *            The string to parse.
	 * @return The ip address or null.
	 */
	public static byte[] getAddress(String string) {
		StringTokenizer t = new StringTokenizer(string, ".");
		if (t.countTokens() != 4)
			return null;
		else {
			byte[] result = new byte[4];
			for (int i = 0; i < 4; i++) {
				try {
					int value = Integer.parseInt(t.nextToken());
					if (value < 0 || value > 255)
						return null;
					result[i] = (byte) value;
				} catch (NumberFormatException e) {
					return null;
				}
			}
			return result;
		}
	}

}
