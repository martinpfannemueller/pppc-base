package info.pppc.basex.plugin.routing.server;

import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.basex.plugin.util.MultiplexFactory;

/**
 * The gateway registry provider is an interface that
 * enables the registration of gateways for devices.
 * 
 * @author Mac
 */
public interface IGatewayRegistryProvider {

	/**
	 * Associates the specified gateway with the specified factory.
	 * 
	 * @param gateway The gateway to register.
	 * @param factory The factory that is connected to the gateway.
	 */
	public void addGateway(SystemID gateway, MultiplexFactory factory,PluginDescription[] transceivers);
	
	/**
	 * Returns the transceiver plugins for the specified gateway.
	 */
	public PluginDescription[] getGatewayPlugins(SystemID gateway);
	
	/**
	 * Removes the specified gateway and its factory.
	 * 
	 * @param gateway The gateway to remove.
	 * @param factory The factory to remove.
	 */
	public void removeGateway(SystemID gateway, MultiplexFactory factory);
	
	/**
	 * Adds an association between a gateway and a device.
	 * 
	 * @param gateway The gateway.
	 * @param target The target that is reachable via the gateway.
	 */
	public void addDevice(SystemID gateway, SystemID target);
	
	/**
	 * Removes an association between a gateway and a device.
	 * 
	 * @param gateway The gateway.
	 * @param target The target that is no longer reachable via the gateway.
	 */
	public void removeDevice(SystemID gateway, SystemID target);
	
}
