package info.pppc.base.eclipse;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop. The plugin
 * class is used to access the resources of the plugin contained
 * in the property file.
 * 
 * @author Mac
 */
public class Plugin extends AbstractUIPlugin {

	/**
	 * The shared instance of the plugin.
	 */
	private static Plugin plugin;
	
	/**
	 * The resource bundle of the plugin. 
	 */
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public Plugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation.
	 * 
	 * @param context The context of the plugin.
	 * @throws Exception Thrown if the plugin could not be started.
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// load the resource bundle from the plugin property file
		resourceBundle = Platform.getResourceBundle(context.getBundle());
	}

	/**
	 * This method is called when the plug-in is stopped.
	 * 
	 * @param context The context of the plugin.
	 * @throws Exception Thrown if the plugin could not be started.
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return The shared plugin instance.
	 */
	public static Plugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("info.pppc.eclipse.base", path);
	}
	
	/**
	 * Returns the workspace instance.
	 * 
	 * @return The workspace of the instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 * 
	 * @param key The key to lookup.
	 * @return The string for the key or the key if the key is unknown.
	 */
	public String getResourceString(String key) {
		try {
			if (resourceBundle != null) {
				return resourceBundle.getString(key);	
			} else {
				error("Cannot load resource bundle.", null);
				return key;				
			}
		} catch (MissingResourceException e) {						
			error("Cannot find key " + key + " in resource bundle.", e);
			return key;
		}
	}
	
	/**
	 * Logs an error to the plugin-specific log.
	 * 
	 * @param message The error message to log.
	 * @param t The exception that caused the error or null if not available.
	 */
    public void error(String message, Throwable t) {
    	Status s = new Status(IStatus.ERROR, getBundle().getSymbolicName(), IStatus.OK, message, t);
    	getLog().log(s);
    }
	
}
