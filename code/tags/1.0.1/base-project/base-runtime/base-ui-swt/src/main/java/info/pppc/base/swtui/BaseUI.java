package info.pppc.base.swtui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import info.pppc.base.swtui.action.SystemAction;
import info.pppc.base.system.util.Logging;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * This is a helper class that eases the startup of the base default user interface. 
 * It automatically adds the system browser to the system menu which will be desirable 
 * in most cases. Furthermore, it manages the resources for the pcom default gui
 * elements using an image registry and a property bundle. 
 * 
 * @author Marcus Handte
 */
public class BaseUI {

	/**
	 * The image key for the logo image.
	 */
	public static final String IMAGE_LOGO = "LOGO";

	/**
	 * The image key for the identifier image.
	 */
	public static final String IMAGE_IDENTIFIER = "IDENTIFIER";

	/**
	 * The image key for the default device image.
	 */	
	public static final String IMAGE_DEVICE = "DEVICE";

	/**
	 * The image key for the default plug-in image.
	 */		
	public static final String IMAGE_PLUGIN = "PLUGINS";
	
	/**
	 * The image key for the property image.
	 */
	public static final String IMAGE_PROPERTY = "PROPERTY";
	
	/**
	 * The image key for the default service image.
	 */
	public static final String IMAGE_SERVICE = "SERVICES";
	
	/**
	 * The image key for the default ability image.
	 */
	public static final String IMAGE_ABILITY = "ABILITY";

	/**
	 * The image key for the default close image.
	 */
	public static final String IMAGE_CLOSE = "CLOSE";
	
	/**
	 * The image key for the default refresh image.
	 */
	public static final String IMAGE_REFRESH = "REFRESH";
	
	/**
	 * The image key for the default browser image.
	 */
	public static final String IMAGE_SYSTEM = "SYSTEM";

	/**
	 * The image key for the default exit image.
	 */
	public static final String IMAGE_EXIT = "EXIT";

	/**
	 * The image key for the console image.
	 */
	public static final String IMAGE_CONSOLE = "CONSOLE";

	/**
	 * The image key for the run image.
	 */
	public static final String IMAGE_RUN = "RUN";

	/**
	 * The image key for the arrow left image.
	 */
	public static final String IMAGE_LEFT = "LEFT";
	
	/**
	 * The image key for the arrow right image.
	 */
	public static final String IMAGE_RIGHT = "RIGHT";

	/**
	 * The image key for the name image.
	 */
	public static final String IMAGE_NAME = "NAME";

	/**
	 * The image key of the search image.
	 */
	public static final String IMAGE_SEARCH = "SEARCH";
	
	/**
	 * The image key of the invoke image.
	 */
	public static final String IMAGE_INVOKE = "INVOKE";
	
	/**
	 * The image for iconized add buttons.
	 */
	public static final String IMAGE_BUTTON_ADD = "BUTTON_ADD";

	/**
	 * The image for iconized remove buttons.
	 */
	public static final String IMAGE_BUTTON_REMOVE = "BUTTON_REMOVE";
	
	/**
	 * The image for iconized up buttons.
	 */
	public static final String IMAGE_BUTTON_UP = "BUTTON_UP";
	
	/**
	 * The image for iconized down buttons.
	 */
	public static final String IMAGE_BUTTON_DOWN = "BUTTON_DOWN";
	
	/**
	 * The image for iconized left buttons.
	 */
	public static final String IMAGE_BUTTON_LEFT = "BUTTON_LEFT";
	
	/**
	 * The image for iconized right buttons.
	 */
	public static final String IMAGE_BUTTON_RIGHT = "BUTTON_RIGHT";
	
	/**
	 * The image for iconized ok buttons.
	 */
	public static final String IMAGE_BUTTON_OK = "BUTTON_OK";

	/**
	 * The image for iconized run buttons.
	 */
	public static final String IMAGE_BUTTON_RUN = "BUTTON_RUN";
	
	/**
	 * The image registry used to share common images.
	 */
	private static ImageRegistry imageRegistry;
	
	/**
	 * The resource bundle used to provide localized strings.
	 */
	private static ResourceBundle resourceBundle;
	
	/**
	 * The system browser that has been registered with the register system browser method.
	 */
	private static ActionContributionItem systemBrowser;

	
	/**
	 * Registers the system browser at the current base ui. If the browser
	 * has already been registered, this method does nothing.
	 */
	public static void registerSystemBrowser() {
		if (systemBrowser == null) {
			final Application application = Application.getInstance();
			application.run(new Runnable() {
				public void run() {
					systemBrowser = new ActionContributionItem(new SystemAction(application));
					application.addContribution(systemBrowser);

				}
			});
		}
	}

	/**
	 * Removes a previously registered system browser from the current base ui. 
	 * If the browser has not been registered, this method does nothing.
	 */
	public static void unregisterSystemBrowser() {
		if (systemBrowser != null) {
			final Application application = Application.getInstance();
			application.run(new Runnable() {
				public void run() {
					application.removeContribution(systemBrowser);
					systemBrowser = null;
				}
			});
		}
	}
	
	/**
	 * Returns a localized text message for the specified key.
	 * 
	 * @param id The key used to retrieve a localized text message.
	 * @return The string for the key or an error message if the
	 * 	key is not contained in the resource file.
	 */
	public static String getText(String id) {
		String text = null;
		try {
			text = getResourceBundle().getString(id);	
		} catch (MissingResourceException e) {
			Logging.log(BaseUI.class, "Could not get text for " + id + ".");
		}			
		if (text == null || text.equals("")) {
			return "(!) MISSING: " + id;	
		} else {
			return text;	
		}
	}
	
	/**
	 * Returns an image for the specified key or null if the key is
	 * not registered or the image cannot be loaded.
	 * 
	 * @param id The key to lookup.
	 * @return The image or null if it cannot be loaded.
	 */
	public static Image getImage(String id) {
		return getImageRegistry().get(id);
	}
	
	/**
	 * Returns the image descriptor that is associated with the specified
	 * key or null if the key is not registered.
	 * 
	 * @param id The key to lookup.
	 * @return The image descriptor of the key or null.
	 */
	public static ImageDescriptor getDescriptor(String id) {
		return getImageRegistry().getDescriptor(id);
	}
	
	/**
	 * Returns the image registry and loads it if it has not been
	 * loaded already.
	 * 
	 * @return The local image registry.
	 */
	private static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry();
			loadImage(IMAGE_LOGO, "logo.gif");
			loadImage(IMAGE_ABILITY, "ability.gif");
			loadImage(IMAGE_DEVICE + ".0", "device.0.gif");
			loadImage(IMAGE_DEVICE + ".1", "device.1.gif");
			loadImage(IMAGE_DEVICE + ".2", "device.2.gif");
			loadImage(IMAGE_DEVICE + ".3", "device.3.gif");
			loadImage(IMAGE_DEVICE + ".4", "device.4.gif");
			loadImage(IMAGE_DEVICE + ".5", "device.5.gif");
			loadImage(IMAGE_DEVICE + ".6", "device.6.gif");
			loadImage(IMAGE_DEVICE + ".7", "device.7.gif");
			loadImage(IMAGE_DEVICE + ".8", "device.8.gif");
			loadImage(IMAGE_NAME, "name.gif");
			loadImage(IMAGE_PLUGIN, "plugin.gif");
			loadImage(IMAGE_PROPERTY, "property.gif");
			loadImage(IMAGE_SERVICE, "service.gif");
			loadImage(IMAGE_IDENTIFIER, "identifier.gif");
			loadImage(IMAGE_SYSTEM, "system.gif");
			loadImage(IMAGE_CLOSE, "close.gif");
			loadImage(IMAGE_REFRESH, "refresh.gif");
			loadImage(IMAGE_EXIT, "exit.gif");
			loadImage(IMAGE_CONSOLE, "console.gif");
			loadImage(IMAGE_RUN, "run.gif");
			loadImage(IMAGE_RIGHT, "right.gif");
			loadImage(IMAGE_LEFT, "left.gif");
			loadImage(IMAGE_SEARCH, "search.gif");
			loadImage(IMAGE_INVOKE, "invoke.gif");
			loadImage(IMAGE_BUTTON_ADD, "button_add.gif");
			loadImage(IMAGE_BUTTON_DOWN, "button_down.gif");
			loadImage(IMAGE_BUTTON_LEFT, "button_left.gif");
			loadImage(IMAGE_BUTTON_OK, "button_ok.gif");
			loadImage(IMAGE_BUTTON_REMOVE, "button_remove.gif");
			loadImage(IMAGE_BUTTON_RIGHT, "button_right.gif");
			loadImage(IMAGE_BUTTON_RUN, "button_run.gif");
			loadImage(IMAGE_BUTTON_UP, "button_up.gif");
			
		}
		return imageRegistry;
	}
	
	/**
	 * Returns the local resource bundle with the default 
	 * localization.
	 * 
	 * @return The local resource bundle.
	 */
	private static ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			resourceBundle = ResourceBundle.getBundle("info/pppc/base/swtui/resource/text");
		}
		return resourceBundle;
	}
	
	/**
	 * Loads the specified image from the default image folder
	 * and places it into the image registry. 
	 * 
	 * @param key The key used to place the image.
	 * @param image The file name of the image to load.
	 */
	public static void loadImage(String key, String image) {
		getImageRegistry().put(key, ImageDescriptor.createFromURL
			(BaseUI.class.getResource("resource/" + image)));
	}
}
