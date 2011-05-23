package info.pppc.base.system.plugin;

import info.pppc.base.system.IExtension;
import info.pppc.base.system.PluginDescription;

/**
 * This is the base interface that must be implemented by all plug-in 
 * implementations. All plug-ins must be able to provide a plug-in
 * descriptions and they must support the start and stop callbacks 
 * that are issued by the plug-in manager upon installation and 
 * removal of the plug-in. 
 * 
 * @author Marcus Handte
 */
public interface IPlugin extends IExtension {

	/**
	 * Called by the plug-in manager whenever the plug-in is installed. This
	 * call signals that the plug-in should start processing. Note that a 
	 * call to this method does not mean that a transceiver plug-in should 
	 * open a communication end point as the set enabled method of the 
	 * transceiver interface is used to signal that.
	 */
	public void start();
	
	/**
	 * Called by the plug-in manager whenever the plug-in is removed.
	 * This call signals that the plug-in should refrain from performing
	 * any further computation and it should release all resources that
	 * it has acquired during the execution.
	 */
	public void stop();
	
	/**
	 * Returns the plug-in description of the plug-in. The plug-in description
	 * contains the plug-in specific parameters that need to be announced
	 * in order to communicate with the plug-in. Furthermore, it describes
	 * the ability of the plug-in and its extensions.
	 * 
	 * @return The plug-in description.
	 */
	public PluginDescription getPluginDescription();

}
