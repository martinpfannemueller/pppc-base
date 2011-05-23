package info.pppc.base.swtui.system;

import info.pppc.base.service.IServiceRegistry;
import info.pppc.base.swtui.system.action.ServiceAction;
import info.pppc.base.swtui.tree.TreeNode;
import info.pppc.base.system.SystemID;

import java.util.Vector;

import org.eclipse.jface.action.Action;

/**
 * The browser action provider provides actions depending on the
 * current selection of the tree view of the system browser.
 * 
 * @author Marcus Handte
 */
public class SystemActionProvider implements ISystemModel {

	/**
	 * Creates a new browser action provider.
	 */
	public SystemActionProvider() {
		super();
	}
	
	/**
	 * Returns the actions depending on the element passed to the
	 * method.
	 * 
	 * @param control The control that performs the request.
	 * @param element The element that is currently selected in the
	 * 	tree. This should be an instance of a tree node that contains
	 * 	a browser content object.
	 * @return The set of available actions.
	 */
	public Action[] getMenuActions(SystemControl control, Object element) {
		Vector actions = new Vector();
		if (element instanceof TreeNode) {
			TreeNode node = (TreeNode)element;
			actions = getMenuActions(control, node);
		} 
		Action[] result = new Action[actions.size()];
		for (int i = 0; i < actions.size(); i++) {
			result[i] = (Action)actions.elementAt(i);
		}
		return result;	
	}
	
	/**
	 * Returns the menu actions for the browser control under the
	 * current selection.
	 * 
	 * @param control The control that requested the actions.
	 * @param node The node that is selected in the control.
	 * @return A vector containing actions that should be available.
	 */
	private Vector getMenuActions(SystemControl control, TreeNode node) {
		Vector result = new Vector();
		TreeNode device = null;
		if (node.getType() == TYPE_DEVICE) {
			device = node;
		} else {
			node.getParent(TYPE_DEVICE);
		}
		if (device != null && hasRegistry(device)) {
			TreeNode identifier = device.getChildren(TYPE_IDENTIFIER, false)[0];
			TreeNode name = device.getChildren(TYPE_NAME, false)[0];
			String n = (String)name.getData();
			SystemID id = (SystemID)identifier.getData();
			result.addElement(new ServiceAction(control.getManager(), id, n));
		}
		return result;
	}
	
	/**
	 * Determines whether the specified device node of the browser
	 * model also has a service registry system service.
	 * 
	 * @param device The device node.
	 * @return True if the device node also has a registry system service
	 * 	node, false if it does not have one or if the node is not a device
	 * 	node.
	 */
	private boolean hasRegistry(TreeNode device) {
		if (device.getType() == TYPE_DEVICE) {
			TreeNode[] services = device.getChildren(TYPE_SERVICE, true);
			for (int i = 0; i < services.length; i++) {
				if (IServiceRegistry.REGISTRY_ID.equals(services[i].getData())) {
					return true;
				}
			}
		}
		return false;
	}

}
