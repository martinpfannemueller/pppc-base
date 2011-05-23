package info.pppc.base.lcdui.system;

import javax.microedition.lcdui.Image;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.element.IRefreshable;
import info.pppc.base.lcdui.element.action.CloseAction;
import info.pppc.base.lcdui.element.action.RefreshAction;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.tree.TreeItem;
import info.pppc.base.lcdui.tree.TreeNode;
import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.ObjectID;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;

/**
 * The system element is used to visualize the available systems 
 * with their services and plug-ins in a tree view. 
 * 
 * @author Marcus Handte
 */
public class SystemElement extends AbstractElement implements IRefreshable {

	/**
	 * The title of the system element as shown in the ui.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.system.SystemElement.TEXT";
	
	/**
	 * The device type label and various type names
	 */
	private static final String UI_TYPE = "info.pppc.base.lcdui.system.SystemElement.TYPE";
	
	/**
	 * The device name label.
	 */
	private static final String UI_NAME = "info.pppc.base.lcdui.system.SystemElement.NAME";
	
	/**
	 * The device identifier label.
	 */
	private static final String UI_IDENTIFIER = "info.pppc.base.lcdui.system.SystemElement.IDENTIFIER";
	
	/**
	 * The device plug-ins label.
	 */
	private static final String UI_PLUGINS = "info.pppc.base.lcdui.system.SystemElement.PLUGINS";
	
	/**
	 * The device plug-in labels for various plug-in types.
	 */
	private static final String UI_PLUGIN = "info.pppc.base.lcdui.system.SystemElement.PLUGIN";
	
	/**
	 * The device plug-in ability label.
	 */
	private static final String UI_ABILITY = "info.pppc.base.lcdui.system.SystemElement.ABILITY";
	
	/**
	 * The device plug-in property label.
	 */
	private static final String UI_PROPERTY = "info.pppc.base.lcdui.system.SystemElement.PROPERTY";
	
	/**
	 * The device services label.
	 */
	private static final String UI_SERVICES = "info.pppc.base.lcdui.system.SystemElement.SERVICES";

	/**
	 * The device labels for various services including the unknown service.
	 */
	private static final String UI_SERVICE = "info.pppc.base.lcdui.system.SystemElement.SERVICE";
	
	/**
	 * The tree item that is used by the system browser.
	 */
	protected TreeItem tree;
	
	/**
	 * The items of the system browser element.
	 */
	protected FormItem[] items;
	
	/**
	 * The actions of the system browser element.
	 */
	protected ElementAction[] actions;
	
	/**
	 * The device registry that is used to create the view.
	 */
	protected DeviceRegistry registry;
	
	/**
	 * Creates a new system browser element using
	 * the specified manager.
	 * 
	 * @param manager The element manager to use. 
	 */
	public SystemElement(IElementManager manager) {
		super(manager);
		tree = new TreeItem(manager, "", "");
		actions = new ElementAction[] {  
			new RefreshAction(this), 
			new CloseAction(this)
		};
		items = new FormItem[] { tree };
		registry = InvocationBroker.getInstance().getDeviceRegistry();
		refresh();
	}
	
	/**
	 * Returns the image of the system browser.
	 * 
	 * @return The image of the system browser.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_SYSTEM);
	}
	
	/**
	 * Returns the name of the system browser.
	 * 
	 * @return The name of the system browser.
	 */
	public String getName() {
		return BaseUI.getText(UI_TEXT);
	}
	
	/**
	 * Returns the items of the system browser.
	 *
	 * @return The items of the system browser.
	 */
	public FormItem[] getItems() {
		return items;
	};

	/**
	 * Returns the actions of the system browser. 
	 * This is solely the close action.
	 * 
	 * @return The actions of the system browser.
	 */
	public ElementAction[] getActions() {
		return actions;
	}
	
	/**
	 * Creates the tree from the device registry and displays it.
	 */
	public void refresh() {
		tree.setContent(createTree());
		//getManager().updateElement(this);
	}
	
	/**
	 * Creates and returns the tree that is shown in the system browser.
	 * 
	 * @return The tree that is shown in the system browser.
	 */
	protected TreeNode createTree() {
		TreeNode root = new TreeNode(null, "");
		SystemID[] systems = registry.getDevices();
		for (int i = 0, s = systems.length; i < s; i++) {
			DeviceDescription device = registry.getDeviceDescription(systems[i]);
			if (device != null) {
				root.addChild(createTree(device));	
			}
		}
		return root;
	}
	
	/**
	 * Creates the subtree for a certain device.
	 * 
	 * @param device The device whose subtree should be created.
	 * @return The new subtree for the specified device.
	 */
	protected TreeNode createTree(DeviceDescription device) {
		TreeNode root = new TreeNode(device, device.getName(), 
			BaseUI.getImage(BaseUI.IMAGE_DEVICE + "." + device.getType()));
		// the id node
		TreeNode identifier = new TreeNode(device.getSystemID(), BaseUI.getText(UI_IDENTIFIER) 
				+ " (" + device.getSystemID() + ")", BaseUI.getImage(BaseUI.IMAGE_IDENTIFIER));
		root.addChild(identifier);
		// the name node
		TreeNode name = new TreeNode(device.getName(), BaseUI.getText(UI_NAME) + " (" 
				+ device.getName() + ")", BaseUI.getImage(BaseUI.IMAGE_NAME));
		root.addChild(name);
		// the type node
		TreeNode type = new TreeNode(new Short(device.getType()), BaseUI.getText(UI_TYPE) + " (" 
				+ BaseUI.getText(UI_TYPE + "." + device.getType()) + ")", 
				BaseUI.getImage(BaseUI.IMAGE_DEVICE + "." + device.getType()));
		root.addChild(type);
		// the services subtree
		TreeNode services = new TreeNode(null, BaseUI.getText(UI_SERVICES), BaseUI.getImage(BaseUI.IMAGE_SERVICE));
		ObjectID[] oids = device.getServices();
		for (int i = 0, s = oids.length; i < s; i++) {
			services.addChild(createTree(oids[i]));
		}
		root.addChild(services);
		// the plugin subtree
		TreeNode plugins = new TreeNode(null, BaseUI.getText(UI_PLUGINS), BaseUI.getImage(BaseUI.IMAGE_PLUGIN));
		PluginDescription[] pds = registry.getPluginDescriptions(device.getSystemID());
		for (int i = 0, s = pds.length; i < s; i++) {
			plugins.addChild(createTree(pds[i]));
		}
		root.addChild(plugins);
		return root;
	}
	
	/**
	 * Creates and returns the tree for the specified service.
	 * 
	 * @param objectID The object id of the service.
	 * @return The tree for the service.
	 */
	protected TreeNode createTree(ObjectID objectID) {
		String string;
		if (objectID.equals(new ObjectID(1))) {
			string = BaseUI.getText(UI_SERVICE + "." + 1);
		} else if (objectID.equals(new ObjectID(2))) {
			string = BaseUI.getText(UI_SERVICE + "." + 2);
		} else {
			string = BaseUI.getText(UI_SERVICE) + " (" + objectID + ")";
		}
		return new TreeNode(objectID, string, BaseUI.getImage(BaseUI.IMAGE_SERVICE));
	}
	
	/**
	 * Creates and returns the tree for the specified plug-in.
	 * 
	 * @param plugin The plug-in whose tree should be created.
	 * @return The tree of the plug-in.
	 */
	protected TreeNode createTree(PluginDescription plugin) {
		TreeNode root = new TreeNode(plugin, BaseUI.getText(UI_PLUGIN + "." 
			+ plugin.getExtension()), BaseUI.getImage(BaseUI.IMAGE_PLUGIN));
		// the ability node
		TreeNode ability = new TreeNode(new Short(plugin.getAbility()), BaseUI.getText(UI_ABILITY) 
				+ " [" + ((plugin.getAbility() >> 8) & 0xff) + "][" + (plugin.getAbility() & 0xff) + "]", 
					BaseUI.getImage(BaseUI.IMAGE_ABLILITY));
		root.addChild(ability);
		// the property tree nodes
		String[] keys = plugin.getProperties();
		for (int i = 0; i < keys.length; i++) {
			root.addChild(createTree(plugin, keys[i]));
		}
		return root;
	}
	
	/**
	 * Creates and returns the tree for the specified plug-in property.
	 * 
	 * @param plugin The plug-in description of the plug-in.
	 * @param key The key of the property to return.
	 * @return The tree for the property.
	 */
	protected TreeNode createTree(PluginDescription plugin, String key) {
		return new TreeNode(key, BaseUI.getText(UI_PROPERTY) + " (" + key 
			+ "=" + toString(plugin.getProperty(key)) + ")", 
				BaseUI.getImage(BaseUI.IMAGE_PROPERTY));
	}
	
	/**
	 * Returns a string representation for various specialized plug-in
	 * properties that are known to be used.
	 * 
	 * @param property The property whose string should be created.
	 * @return The string for the property.
	 */
	protected String toString(Object property) {
		if (property == null) {
			return "null";
		} else if (property instanceof byte[]) {
			String s = "";
			byte[] b = (byte[])property;
			for (int i = 0; i < b.length; i++) {
				s += (b[i] & 0xFF);
				if (i != b.length - 1) {
					s += "."; 
				}
			}
			return s;
		} else {
			return property.toString();
		}
	}
}
