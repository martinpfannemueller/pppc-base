package info.pppc.basex.plugin.routing.swing;

import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

/**
 * The gateway applet provides appletized access to the gatway server
 * through Java RMI.
 * 
 * @author Mac
 *
 */
public class GatewayApplet extends JApplet implements ActionListener {

	/**
	 * The serial id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * A provider that performs rmi lookups to get access to the server.
	 * 
	 * @author Mac
	 *
	 */
	public class Provider implements IGatewayProvider {

		/**
		 * Performs a registry lookup to find the server.
		 * 
		 * @return The server or null, if it is not available.
		 */
		public IRemoteGatewayServer getGatewayServer() {
			try {
				Registry registry = LocateRegistry.getRegistry(IRemoteGatewayServer.HOST_NAME, IRemoteGatewayServer.REGISTRY_PORT);
				IRemoteGatewayServer server = (IRemoteGatewayServer) registry.lookup(IRemoteGatewayServer.OBJECT_NAME);
				return server;
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
			return null;
		}

	}
	
	/**
	 * Called upon an action.
	 * 
	 * @param e The action.
	 */
	public void actionPerformed(ActionEvent e) { }

	/**
	 * Called to initialize the applet. This method
	 * creates the gui and that is it.
	 */
	@Override
	public void init() {
		super.init();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					getContentPane().setLayout(new BorderLayout());
					getContentPane().add(new GatewayPane(new Provider()), BorderLayout.CENTER);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
