package info.pppc.basex.plugin.routing.swing;

import info.pppc.basex.plugin.routing.remote.IRemoteGatewayServer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;

/**
 * This class creates the user interface for the gateway
 * applet and the gateway application.
 * 
 * @author Mac
 */
public class GatewayPane extends JPanel {
	
	/**
	 * The serial version.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The provider that provides abstract access to the server.
	 */
	private transient IGatewayProvider provider;
	
	/**
	 * Creates a new user interface attached to the specified
	 * provider.
	 * 
	 * @param provider The provider that gives access to the
	 * 	server.
	 */
	public GatewayPane(IGatewayProvider provider) {
		super(new GridLayout(1, 1));
		this.provider = provider;
		JTabbedPane tabbedPane = new JTabbedPane();
		ImageIcon icon = null;
		tabbedPane.addTab("Devices", icon, new GatewayPanel(), "Shows registered devices.");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		JComponent panel3 = new LogPanel();
		tabbedPane.addTab("Messages", icon, panel3, "Shows log messages.");
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		tabbedPane.setPreferredSize(new Dimension(600, 300));
		add(tabbedPane);
		// The following line enables to use scrolling tabs.
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	/**
	 * The class that implements the gateway panel.
	 * 
	 * @author Mac
	 */
	public class GatewayPanel extends JPanel {
		/**
		 * The serial version.
		 */
		private static final long serialVersionUID = 1L;
		
		/**
		 * Creates a new gateway panel.
		 */
		public GatewayPanel() {
			JLabel filler = new JLabel("Shows the current gatways with connected devices.");
			JButton button = new JButton("Update");
			final JTree tree = new JTree(getTreeModel());
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tree.setModel(getTreeModel());
				}
			});
			filler.setHorizontalAlignment(JLabel.LEFT);
			filler.setVerticalAlignment(JLabel.TOP);
			BorderLayout bl = new BorderLayout();
			// Put the drawing area in a scroll pane.
			JScrollPane scroller = new JScrollPane(tree);
			scroller.setPreferredSize(new Dimension(200, 200));
			this.setLayout(bl);
			this.add(filler, BorderLayout.NORTH);
			this.add(scroller, BorderLayout.CENTER);
			this.add(button, BorderLayout.SOUTH);
		}
	}

	/**
	 * A class that implements the logging panel.
	 * 
	 * @author Mac
	 */
	public class LogPanel extends JPanel {
		/**
		 * The serial version.
		 */
		private static final long serialVersionUID = 1L;
		
		/**
		 * Creates a new log panel.
		 */
		public LogPanel() {
			JLabel filler = new JLabel("Shows the most recent log messages.");
			JButton button = new JButton("Update");
			filler.setHorizontalAlignment(JLabel.LEFT);
			filler.setVerticalAlignment(JLabel.TOP);
			final JTextArea tf = new JTextArea(getMessages());
			tf.setEditable(false);
			BorderLayout bl = new BorderLayout();
			// Put the drawing area in a scroll pane.
			JScrollPane scroller = new JScrollPane(tf);
			scroller.setPreferredSize(new Dimension(200, 200));
			this.setLayout(bl);
			this.add(filler, BorderLayout.NORTH);
			this.add(scroller, BorderLayout.CENTER);
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.add(button, BorderLayout.SOUTH);
			this.add(buttonPanel, BorderLayout.SOUTH);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tf.setText(getMessages());
				}
			});
		}
	}

	/**
	 * Returns the tree model from the server.
	 * 
	 * @return The tree model from the server.
	 */
	private GatewayTreeModel getTreeModel() {
		Hashtable<String, Vector<String>> gwMap = new Hashtable<String, Vector<String>>();
		IRemoteGatewayServer server = provider.getGatewayServer();
		if (server != null) {
			try {
				gwMap = server.getGateways();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return new GatewayTreeModel(gwMap);
	}

	/**
	 * Returns the log messages from the server.
	 * 
	 * @return The log messages from the server.
	 */
	private String getMessages() {
		IRemoteGatewayServer server = provider.getGatewayServer();
		if (server != null) {
			try {
				ArrayList<String> logList = server.getMessages();
				String logString = "";
				for (String string : logList) {
					logString = logString + string;;
				}
				return logString;

			} catch (RemoteException e) {
				e.printStackTrace();
			} 			
		}
		return "";
	}
}