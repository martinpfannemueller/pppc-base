package info.pppc.basex.plugin.routing.server;

import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.routing.RoutingOperation;
import info.pppc.basex.plugin.transceiver.ip.IIPPlugin;
import info.pppc.basex.plugin.transceiver.ip.IPPacketConnector;
import info.pppc.basex.plugin.transceiver.ip.IPStreamConnector;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import info.pppc.basex.plugin.util.MultiplexFactory;

import java.io.IOException;

/**
 * The multiplex plugin adapter provides a minimalistic implementation the
 * multiplex plugin interface to support the use of multiplex factories.
 * 
 * @author Mac
 */
public class MultiplexPluginAdapter implements IMultiplexPlugin, IIPPlugin {

	/**
	 * The plugin manager.
	 */
	private IPluginManager pluginManager;

	/**
	 * The connector provider used to establish connections.
	 */
	private IStreamConnectorProvider connectorProvider;

	/**
	 * The registry provider used to associate devices with gateways.
	 */
	private IGatewayRegistryProvider registryProvider;

	/**
	 * A flag that indicates whether the plugin is connected.
	 */
	private boolean connected = false;

	/**
	 * A monitor used to stop the plug-in.
	 */
	private NullMonitor monitor;

	/**
	 * Creates a new plugin adapter with the specified manager. That uses the
	 * specified connector provider to establish connections and the specified
	 * registry provider to reachability information.
	 * 
	 * @param pluginManager The plug-in manager.
	 * @param connectorProvider The provider for connectors.
	 * @param registryProvider The provider for registry information.
	 */
	public MultiplexPluginAdapter(IPluginManager pluginManager,
			IStreamConnectorProvider connectorProvider,
			IGatewayRegistryProvider registryProvider) {
		this.pluginManager = pluginManager;
		this.connectorProvider = connectorProvider;
		this.registryProvider = registryProvider;
	}

	/**
	 * Called whenever a new connection is established. The first connection is
	 * used to update the registry. Other connections are forwarded through the
	 * connection provider.
	 * 
	 * @param source
	 *            The source of the request.
	 * @param connector
	 *            The connector that is incoming.
	 */
	public synchronized void acceptConnector(final MultiplexFactory source,
			final IStreamConnector connector) {
		if (!connected) {
			// handle incoming control connection
			connected = true;
			pluginManager.performOperation(new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					SystemID gateway = null;
					try {
						ObjectInputStream in = new ObjectInputStream(connector
								.getInputStream());
						gateway = (SystemID) in.readObject();
						int x = in.readInt();
						PluginDescription[] incomingPlugins = new PluginDescription[x];
						for (int i = 0; i < x; i++) {
							incomingPlugins[i] = (PluginDescription) in
									.readObject();
						}
						Logging.debug(getClass(), "Adding gateway " + gateway
								+ ".");
						registryProvider.addGateway(gateway, source,
								incomingPlugins);

						while (!monitor.isCanceled()) {
							Boolean b = (Boolean) in.readObject();
							SystemID s = (SystemID) in.readObject();
							if (b.booleanValue()) {
								Logging.debug(getClass(),
										"Adding association between " + gateway
												+ " and " + s + ".");
								registryProvider.addDevice(gateway, s);
							} else {
								Logging.debug(getClass(),
										"Removing association between "
												+ gateway + " and " + s + ".");
								registryProvider.removeDevice(gateway, s);
							}
						}
					} catch (Throwable e) {
						Logging.error(getClass(),
								"Exception while reading updates.", e);
					}
					if (gateway != null) {
						Logging.debug(getClass(), "Removing gateway " + gateway
								+ ".");
						registryProvider.removeGateway(gateway, source);
					}
					connector.release();
				}
			}, monitor);
		} else {
			// handle incoming connection requests.
			pluginManager.performOperation(new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					try {
						ObjectInputStream in = new ObjectInputStream(connector
								.getInputStream());
						NFCollection collection = (NFCollection) in
								.readObject();
						SystemID target = (SystemID) in.readObject();
						Logging.log(getClass(), "Received route request for "
								+ target + ".");
						// Send transceiver plugins to establish other route
						PluginDescription[] transceivers = registryProvider
								.getGatewayPlugins(target);
						if (transceivers == null) {
							Logging.debug(getClass(), "No gateway for "
									+ target + ".");
							connector.release();
							return;
						}
						ObjectOutputStream out = new ObjectOutputStream(
								connector.getOutputStream());
						out.writeInt(transceivers.length);
						for (int i = 0; i < transceivers.length; i++) {
							out.writeObject((PluginDescription) transceivers[i]);
						}
						out.flush();
						Logging.log(getClass(),
								"plugins for direct connection transmitted ");
						boolean needsServerConnection = in.readBoolean();
						Logging.log(getClass(), "needs Server connection:"
								+ needsServerConnection);

						if (needsServerConnection) {
							IStreamConnector c = connectorProvider
									.getConnector(target);
							if (c != null) {
								try {
									out = new ObjectOutputStream(c
											.getOutputStream());
									out.writeObject(collection);
									out.writeObject(target);
									out.flush();
									NullMonitor monitor1 = new NullMonitor();
									NullMonitor monitor2 = new NullMonitor();
									pluginManager.performOperation(
											new RoutingOperation(c, connector,
													monitor2), monitor1);
									pluginManager.performOperation(
											new RoutingOperation(connector, c,
													monitor1), monitor2);
									Logging.debug(getClass(),
											"Forward link established.");
								} catch (IOException e) {
									Logging.error(getClass(),
											"Exception while creating forward link to "
													+ target + ".", e);
									connector.release();
									c.release();
								}
							} else {
								Logging.debug(getClass(), "No gateway for "
										+ target + ".");
								connector.release();
							}
						} else {
							Logging.log(getClass(),
									"direct connection established");
						}
					} catch (Throwable t) {
						Logging.error(getClass(),
								"Exception while establishing route.", t);
						connector.release();
					}
				}
			});
		}
	}

	/**
	 * Called when the multiplexer is closed.
	 * 
	 * @param multiplexer
	 *            The closed multiplexer.
	 */
	public void closeMultiplexer(MultiplexFactory multiplexer) {
		// nothing to be done
	}

	/**
	 * Returns null in this implementation.
	 */
	public PluginDescription getPluginDescription() {
		return null;
	}

	/**
	 * Returns the plugin manager.
	 */
	public IPluginManager getPluginManager() {
		return pluginManager;
	}

	/**
	 * Does nothing in this adapter implementation.
	 */
	public void start() {
		monitor = new NullMonitor();
	}

	/**
	 * Does nothing in this adapter implementation.
	 */
	public void stop() {
		try {
			synchronized (monitor) {
				monitor.cancel();
				monitor.notify();
				while (!monitor.isDone()) {
					monitor.wait();
				}
			}
		} catch (InterruptedException e) {
			Logging.error(getClass(), "Thread got interrupted.", e);
		}
	}

	/**
	 * Called whenever a connector is released, does nothing.
	 */
	public void release(IPPacketConnector connector) {
	}

	/**
	 * Called whenever a connector is released, does nothing.
	 */
	public void release(IPStreamConnector connector) {
	}

}
