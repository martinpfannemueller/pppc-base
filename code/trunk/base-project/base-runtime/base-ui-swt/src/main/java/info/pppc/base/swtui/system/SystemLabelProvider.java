package info.pppc.base.swtui.system;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.tree.TreeNode;
import info.pppc.base.system.IExtension;
import info.pppc.base.system.ObjectID;
import info.pppc.base.service.IServiceRegistry;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * The browser label provider provides the labels for models generated
 * by the browser control.
 * 
 * @author Marcus Handte
 */
public class SystemLabelProvider extends LabelProvider implements ISystemModel {

	/**
	 * The id of the string for null types.
	 */
	private static final String UI_NULL = "info.pppc.base.swtui.system.SystemLabelProvider.NULL";

	/**
	 * The id of the string for device names.
	 */	
	private static final String UI_DEVICE = "info.pppc.base.swtui.system.SystemLabelProvider.DEVICE";
	
	/**
	 * The id of the string for the device name.
	 */
	private static final String UI_NAME = "info.pppc.base.swtui.system.SystemLabelProvider.NAME";
	
	/**
	 * The id of the string for the device type.
	 */
	private static final String UI_TYPE = "info.pppc.base.swtui.system.SystemLabelProvider.TYPE";
	
	/**
	 * The id of the string for system identifiers.
	 */
	private static final String UI_IDENTIFIER = "info.pppc.base.swtui.system.SystemLabelProvider.IDENTIFIER";
	
	/**
	 * The id of the string for services.
	 */	
	private static final String UI_SERVICES = "info.pppc.base.swtui.system.SystemLabelProvider.SERVICES";
	
	/**
	 * The id of the string for service types.
	 */	
	private static final String UI_SERVICE = "info.pppc.base.swtui.system.SystemLabelProvider.SERVICE";
	
	/**
	 * The id of the string for plug-ins.
	 */
	private static final String UI_PLUGINS = "info.pppc.base.swtui.system.SystemLabelProvider.PLUGINS";
	
	/**
	 * The id of the string for plug-in types.
	 */
	private static final String UI_PLUGIN = "info.pppc.base.swtui.system.SystemLabelProvider.PLUGIN";
	
	/**
	 * The id of the string for abilities.
	 */
	private static final String UI_ABILITY = "info.pppc.base.swtui.system.SystemLabelProvider.ABILITY";

	/**
	 * The id of the string for properties.
	 */
	private static final String UI_PROPERTY = "info.pppc.base.swtui.system.SystemLabelProvider.PROPERTY";

	/**
	 * Creates a new browser label provider.
	 */
	public SystemLabelProvider() {
		super();
	}

	/**
	 * Returns the text of a specified element.
	 * 
	 * @param element The element whose text should be retrieved.
	 * @return The text of the element or an empty string if the
	 * 	element does not have a text. 
	 */
	public String getText(Object element) {
		if (element instanceof TreeNode) {
			TreeNode node = (TreeNode)element;
			switch (node.getType()) {
				case TYPE_NULL:
					return BaseUI.getText(UI_NULL);
				case TYPE_DEVICE:
					TreeNode[] cs = node.getChildren();
					String name = BaseUI.getText(UI_DEVICE);
					for (int i = 0; i < cs.length; i++) {
						if (cs[i].getType() == TYPE_NAME) {
							name = (String)cs[i].getData();
						}
					}
					return name;
				case TYPE_NAME:	
					return BaseUI.getText
						(UI_NAME) + " (" + node.getData() + ")";
				case TYPE_TYPE:
					return BaseUI.getText
						(UI_TYPE) + " (" + BaseUI.getText
							(UI_TYPE + "." + node.getData()) + ")";
				case TYPE_IDENTIFIER:
					return BaseUI.getText
						(UI_IDENTIFIER) + " (" + node.getData() + ")";
				case TYPE_SERVICES:
					return BaseUI.getText(UI_SERVICES);
				case TYPE_SERVICE:
					return getServiceText((ObjectID)node.getData());
				case TYPE_PLUGINS:
					return BaseUI.getText(UI_PLUGINS);
				case TYPE_PLUGIN:
					return getPluginText(((Short)node.getData()).shortValue());
				case TYPE_ABILITY:
					return BaseUI.getText
						(UI_ABILITY) + " " + node.getData();
				case TYPE_PROPERTY:
					return BaseUI.getText
						(UI_PROPERTY) + " (" + node.getData() + ")";
				default:
					return "";
			}
		} else {
			return "";
		}
	}

	/**
	 * Returns the image for the specified element.
	 * 
	 * @param element The element whose image should be retrieved.
	 * @return The image of the element or null if it does not have an
	 * 	element.
	 */
	public Image getImage(Object element) {
		if (element instanceof TreeNode) {
			TreeNode node = (TreeNode)element;
			switch (node.getType()) {
				case TYPE_NULL:
					return null;
				case TYPE_DEVICE:
					TreeNode[] cs = node.getChildren();
					Image image = BaseUI.getImage(BaseUI.IMAGE_DEVICE + ".0");
					for (int i = 0; i < cs.length; i++) {
						if (cs[i].getType() == TYPE_TYPE) {
							Short id = (Short)cs[i].getData();
							Image temp = BaseUI.getImage(BaseUI.IMAGE_DEVICE + "." + id);
							image = temp;
						}
					}
					return image;
				case TYPE_NAME:
					return BaseUI.getImage(BaseUI.IMAGE_NAME);
				case TYPE_TYPE:
					Short id = (Short)node.getData();
					return BaseUI.getImage(BaseUI.IMAGE_DEVICE + "." + id);
				case TYPE_IDENTIFIER:
					return BaseUI.getImage(BaseUI.IMAGE_IDENTIFIER);
				case TYPE_SERVICES:
					return BaseUI.getImage(BaseUI.IMAGE_SERVICE);
				case TYPE_SERVICE:
					return BaseUI.getImage(BaseUI.IMAGE_SERVICE);
				case TYPE_PLUGINS:
					return BaseUI.getImage(BaseUI.IMAGE_PLUGIN);
				case TYPE_PLUGIN:
					return BaseUI.getImage(BaseUI.IMAGE_PLUGIN);
				case TYPE_ABILITY:
					return BaseUI.getImage(BaseUI.IMAGE_ABILITY);
				case TYPE_PROPERTY:
					return BaseUI.getImage(BaseUI.IMAGE_PROPERTY);
				default:
					return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the plug-in name by extension identifier.
	 * 
	 * @param extension The extension of the plug-in.
	 * @return The label of the entry for the plug-in.
	 */
	protected String getPluginText(short extension) {
		switch (extension) {
			case IExtension.EXTENSION_COMPRESSION:
			case IExtension.EXTENSION_DISCOVERY:
			case IExtension.EXTENSION_ENCRYPTION:
			case IExtension.EXTENSION_SEMANTIC:
			case IExtension.EXTENSION_ROUTING:
			case IExtension.EXTENSION_SERIALIZATION:
			case IExtension.EXTENSION_TRANSCEIVER:
				return BaseUI.getText
					(UI_PLUGIN + "." + extension);
			default:
				return BaseUI.getText
					(UI_PLUGIN) + " (" + extension + ")";
		}
	}
	
	/**
	 * Returns the service name by object identifier.
	 * 
	 * @param id The object identifier of the service.
	 * @return The label of the entry for the service.
	 */
	protected String getServiceText(ObjectID id) {
		if (id.equals(IServiceRegistry.REGISTRY_ID)) {
			return BaseUI.getText(UI_SERVICE + "." + 1);
		} else {
			return BaseUI.getText(UI_SERVICE) + " (" + id + ")";
		}
	}

}
