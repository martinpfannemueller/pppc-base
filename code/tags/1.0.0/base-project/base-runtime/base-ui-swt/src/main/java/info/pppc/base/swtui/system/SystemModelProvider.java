package info.pppc.base.swtui.system;

import info.pppc.base.swtui.tree.TreeNode;
import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.ObjectID;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;

/**
 * The browser model provider creates the tree model for the browser control.
 * For details on the tree structure see the browser model interface.
 * 
 * @author Marcus Handte
 */
public class SystemModelProvider implements ISystemModel {

	/**
	 * The device registry used to create the browser model.
	 */
	private DeviceRegistry registry;

	/**
	 * Creates a new browser model provider for the specified device
	 * registry.
	 * 
	 * @param registry The device registry used to create the model. 
	 */
	public SystemModelProvider(DeviceRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Creates a new model and returns the root node of the model.
	 * 
	 * @param control The control that performs the request.
	 * @return The root node of the new browser model.
	 */
	public TreeNode getModel(SystemControl control) {
		// create tree structure
		TreeNode rootNode = new TreeNode(TYPE_NULL);
		// for all devices
		SystemID[] devices = registry.getDevices();
		for (int i = 0; i < devices.length; i++) {
			SystemID dev = devices[i];
			DeviceDescription devDesc = registry.getDeviceDescription(dev);
			PluginDescription[] pluDesc = registry.getPluginDescriptions(dev);
			ObjectID[] sers = devDesc.getServices();
			if (devDesc != null) {
				// device node
				TreeNode devNode = new TreeNode(TYPE_DEVICE);
				rootNode.addChild(devNode);
				// system id node
				TreeNode idNode = new TreeNode(TYPE_IDENTIFIER, dev);
				devNode.addChild(idNode);
				// system name node
				TreeNode nameNode = new TreeNode(TYPE_NAME, devDesc.getName());
				devNode.addChild(nameNode);
				// system type node
				TreeNode typeNode = new TreeNode(TYPE_TYPE, new Short(devDesc.getType()));
				devNode.addChild(typeNode);
				// services node
				if (sers != null && sers.length > 0) {
					TreeNode sersNode = new TreeNode(TYPE_SERVICES);
					devNode.addChild(sersNode);
					// all services
					for (int j = 0; j < sers.length; j++) {
						TreeNode serNode =new TreeNode(TYPE_SERVICE, sers[j]);
						sersNode.addChild(serNode);
					} 					
				}
				// plug-in node
				if (pluDesc != null && pluDesc.length > 0) {
					TreeNode plusNode = new TreeNode(TYPE_PLUGINS);
					devNode.addChild(plusNode);
					// all plug-ins
					for (int j = 0; j < pluDesc.length; j++) {
						PluginDescription pd = pluDesc[j];
						// plug-in node
						TreeNode pluNode = new TreeNode
							(TYPE_PLUGIN, new Short(pd.getExtension()));
						plusNode.addChild(pluNode);
						// ability node					
						TreeNode abNode = new TreeNode
							(TYPE_ABILITY, getAbilityText(pd.getAbility()));
						pluNode.addChild(abNode);
						// property nodes
						String[] props = pd.getProperties();
						for (int k = 0; k < props.length; k++) {
							TreeNode propNode = new TreeNode
								(TYPE_PROPERTY, props[k] + " = " 
									+ getPropertyText(pd.getProperty(props[k])));
							pluNode.addChild(propNode);
						}
					}					
				}
			}
		}
		return rootNode;
	}
	
	/**
	 * Returns the ability as a pretty printed string.
	 * 
	 * @param ability The ability to pretty print.
	 * @return The pretty printed ability.
	 */
	private String getAbilityText(short ability) {
		byte b1 = (byte)(ability >> 8);
		byte b2 = (byte)ability;
		return "[" + (b1 & 0xff) + "][" + (b2 & 0xff) + "]";
		
	}
	
	/**
	 * Returns the text for plug-in properties. Converts
	 * arrays to readable data.
	 * 
	 * @param o The property that needs to be written.
	 * @return The string to display.
	 */
	private String getPropertyText(Object o) {
		if (o == null) {
			return "null";
		} else if (o instanceof byte[]) {
			String s = "";
			byte[] b = (byte[])o;
			for (int i = 0; i < b.length; i++) {
				s += (b[i] & 0xFF);
				if (i != b.length - 1) {
					s += "."; 
				}
			}
			return s;
		} else {
			return o.toString();
		}
		
	}

}
