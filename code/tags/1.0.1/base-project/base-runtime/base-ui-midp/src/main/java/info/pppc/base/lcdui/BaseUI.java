package info.pppc.base.lcdui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import info.pppc.base.lcdui.util.ResourceBundle;
import info.pppc.base.system.util.Logging;

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
	 * The image key for the default browser image.
	 */
	public static final String IMAGE_SYSTEM = "SYSTEM";
	
	/**
	 * The image key for the default device image.
	 */	
	public static final String IMAGE_DEVICE = "DEVICE";

	/**
	 * The image key for the default service image.
	 */	
	public static final String IMAGE_SERVICE = "SERVICE";

	/**
	 * The image key for the default plug-in image.
	 */	
	public static final String IMAGE_PLUGIN = "PLUGIN";

	/**
	 * The image key for the default property image.
	 */	
	public static final String IMAGE_PROPERTY = "PROPERTY";

	/**
	 * The image key for the default property image.
	 */	
	public static final String IMAGE_ABLILITY = "ABILITY";
	
	/**
	 * The image key for the default property image.
	 */	
	public static final String IMAGE_NAME = "NAME";
	
	/**
	 * The image key for the default property image.
	 */	
	public static final String IMAGE_IDENTIFIER = "IDENTIFIER";
	
	/**
	 * The resource bundle used to provide localized strings.
	 */
	private static ResourceBundle resourceBundle;
	
	/**
	 * The image registry used to provide common shared images.
	 */
	private static Hashtable imageRegistry;
	
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
		} catch (RuntimeException e) {
			Logging.log(BaseUI.class, "Could not get text for " + id + ".");
		}			
		if (text == null || text.equals("")) {
			return "(!) MISSING: " + id;	
		} else {
			return text;	
		}
	}
	
	/**
	 * Returns the local resource bundle with the default localization.
	 * 
	 * @return The local resource bundle.
	 */
	private static ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			resourceBundle = ResourceBundle.getBundle("info/pppc/base/lcdui/resource/text");
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
		Hashtable registry = getImageRegistry();
		if (! registry.containsKey(key)) {
			// fix bug in j2me wtk 3.0 that prevents gif images from
		    // being loaded - simply switch to jpg instead
			try {
				InputStream s = BaseUI.class.getResourceAsStream("resource/" + image + ".gif");
				Image raw = Image.createImage(s);
				registry.put(key, raw);
			} catch (IOException e) {
				try {
					InputStream s = BaseUI.class.getResourceAsStream("resource/" + image + ".jpg");
					Image raw = Image.createImage(s);
					registry.put(key, raw);	
				} catch (IOException x) {
					Logging.log(BaseUI.class, "Could not load image: " + image);	
				}
			}
		}
	}
	
	/**
	 * Returns the image registry of the base user interface.
	 * 
	 * @return The image registry of the user interface.
	 */
	private static Hashtable getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new Hashtable();
			loadImage(IMAGE_LOGO, "logo" );
			loadImage(IMAGE_SYSTEM, "system" );
			loadImage(IMAGE_DEVICE + ".0", "device.0" );
			loadImage(IMAGE_DEVICE + ".1", "device.1" );
			loadImage(IMAGE_DEVICE + ".2", "device.2" );
			loadImage(IMAGE_DEVICE + ".3", "device.3" );
			loadImage(IMAGE_DEVICE + ".4", "device.4" );
			loadImage(IMAGE_DEVICE + ".5", "device.5" );
			loadImage(IMAGE_DEVICE + ".6", "device.6" );
			loadImage(IMAGE_SERVICE, "service" );
			loadImage(IMAGE_PLUGIN, "plugin" );
			loadImage(IMAGE_PROPERTY, "property" );
			loadImage(IMAGE_ABLILITY, "ability" );
			loadImage(IMAGE_NAME, "name" );
			loadImage(IMAGE_IDENTIFIER, "identifier" );
		}
		return imageRegistry;
	}
	
	/**
	 * Returns an image for the specified key or null if the key is
	 * not registered or the image cannot be loaded.
	 * 
	 * @param id The key to lookup.
	 * @return The image or null if it cannot be loaded.
	 */
	public static Image getImage(String id) {
		return (Image)getImageRegistry().get(id);
	}
	
}
