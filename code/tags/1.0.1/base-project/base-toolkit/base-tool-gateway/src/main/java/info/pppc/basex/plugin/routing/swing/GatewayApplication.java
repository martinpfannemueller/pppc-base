package info.pppc.basex.plugin.routing.swing;

import info.pppc.basex.plugin.routing.ProactiveGatewayServer;
import info.pppc.basex.plugin.routing.ProactiveRoutingGateway;
import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;

import java.awt.BorderLayout;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * This class starts a local instance of the gateway server and 
 * attaches a swing gui to it. Starting this class is probably the
 * easiest way to debug something locally.
 * 
 * @author Mac
 *
 */
public class GatewayApplication {
		
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
			final ProactiveGatewayServer s = new ProactiveGatewayServer(address, port);
			Thread t = new Thread() {
				public void run() {
					s.start(true);					
				};
			};
			t.setDaemon(true);
			t.start();
			// Schedule a job for the event dispatch thread:
			// creating and showing this application's GUI.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// Turn off metal's use of bold fonts
					UIManager.put("swing.boldMetal", Boolean.FALSE);
					// Create and set up the window.
					JFrame frame = new JFrame("BASE Gateway");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					// Add content to the window.
					frame.add(new GatewayPane(new IGatewayProvider() {
						public IRemoteGatewayServer getGatewayServer() {
							return s;
						}
					}), BorderLayout.CENTER);
					// Display the window.
					frame.pack();
					frame.setVisible(true);
				}
			});
		} else {
			printUsage();
		}
	}

	/**
	 * Prints the usage to the standard out.
	 */
	public static void printUsage() {
		System.out.println("Usage: GatewayApplication "
				+ "<ipaddress> <port>");
		System.out.println("Example: GatewayApplication "
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
