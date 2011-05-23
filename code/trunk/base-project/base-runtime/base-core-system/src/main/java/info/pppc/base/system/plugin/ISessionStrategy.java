package info.pppc.base.system.plugin;

import info.pppc.base.system.IExtension;
import info.pppc.base.system.nf.NFCollection;

import java.util.Vector;

/**
 * Generic interface for plug-in selection strategies. A strategy 
 * performs an ordering between possible plug-in implementations based on 
 * the parameters specified by a collection of non-functional parameters.
 * This can be used to implement device-specific selection policies such 
 * as "use the transceiver with the lowest energy consumption" or "use the
 * fastest transceiver".
 * 
 * @author Marcus Handte
 */
public interface ISessionStrategy extends IExtension {

	/**
	 * This method is called to determine the suitability of a set of plug-ins
	 * for a specified extension.
	 * 
	 * @param extension The extension layer that is currently considered.
	 * @param plugins The plug-in descriptions of compatible plug-ins for the 
	 * 	specified layer.
	 * @param collection The collection of non-functional parameters.
	 * @return A vector of suitable plug-ins ordered by preference. If a layer
	 * 	must not be added, the vector might as well be empty. If the requirements
	 * 	specified by the collection cannot be fulfilled this method must return
	 *  null.
	 */
	public Vector getPlugin(short extension, Vector plugins, NFCollection collection);



}