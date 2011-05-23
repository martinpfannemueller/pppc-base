package info.pppc.basex.plugin.routing.swing;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * The gateway tree model is used to draw the tree
 * in the gateway panel.
 * 
 * @author Mac
 */
public class GatewayTreeModel implements TreeModel {

	/**
	 * The model listeners.
	 */
	private Vector<TreeModelListener> treeModelListeners = new Vector<TreeModelListener>();
	
	/**
	 * The root of the tree.
	 */
	private Gateway rootGateway;

	/**
	 * A simple tree node.
	 * 
	 * @author Mac
	 */
	public class Gateway {
		Gateway father;
		Vector<Gateway> children;
		private String systemId;

		/**
		 * Creates a new gateway.
		 * 
		 * @param SystemId The name.
		 */
		public Gateway(String SystemId) {
			this.systemId = SystemId;
			this.father = null;
			this.children = new Vector<Gateway>();
		}

		/**
		 * Returns the parent.
		 * 
		 * @return The parent.
		 */
		public Gateway getFather() {
			return father;
		}

		/**
		 * Sets the parent.
		 * 
		 * @param father The parent.
		 */
		public void setFather(Gateway father) {
			this.father = father;
		}

		/**
		 * Returns the children.
		 * 
		 * @return The children.
		 */
		public Vector<Gateway> getChildren() {
			return children;
		}

		/**
		 * Adds a child.
		 * 
		 * @param child The child.
		 */
		public void addChildren(Gateway child) {
			this.children.add(child);
		}

		/**
		 * Returns the name.
		 * 
		 * @return The name.
		 */
		public String getSystemId() {
			return systemId;
		}

		/**
		 * Sets the name.
		 * 
		 * @param systemId The name.
		 */
		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}

		/**
		 * Returns the index of a child.
		 * 
		 * @param child The child.
		 * @return The index.
		 */
		public int getIndexOfChild(Gateway child) {
			return children.indexOf(child);
		}

		/**
		 * Returns a string representation.
		 * 
		 * @return A string representation.
		 */
		@Override
		public String toString() {

			return systemId;
		}

	}

	/**
	 * Creates a new gateway tree model from the set of gateways
	 * with their devices.
	 * 
	 * @param gwMap The map of gateways with devices.
	 */
	public GatewayTreeModel(Hashtable<String, Vector<String>> gwMap) {
		this.rootGateway = new GatewayTreeModel.Gateway("SERVER");
		try {

			for (Entry<String, Vector<String>> entry : gwMap.entrySet()) {
				GatewayTreeModel.Gateway tempGw = new GatewayTreeModel.Gateway(
						entry.getKey());
				for (String target : entry.getValue()) {
					tempGw.addChildren(new GatewayTreeModel.Gateway(target));
				}
				this.rootGateway.addChildren(tempGw);
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

	}

	// ////////////// Fire events //////////////////////////////////////////////

	/**
	 * The only event raised by this model is TreeStructureChanged with the root
	 * as path, i.e. the whole tree has changed.
	 */
	protected void fireTreeStructureChanged(Gateway oldRoot) {
		TreeModelEvent e = new TreeModelEvent(this, new Object[] { oldRoot });
		for (TreeModelListener tml : treeModelListeners) {
			tml.treeStructureChanged(e);
		}
	}

	// ////////////// TreeModel interface implementation ///////////////////////

	/**
	 * Adds a listener for the TreeModelEvent posted after the tree changes.
	 */
	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.addElement(l);
	}

	/**
	 * Returns the child of parent at index index in the parent's child array.
	 */
	public Object getChild(Object parent, int index) {
		Gateway p = (Gateway) parent;
		return p.getChildren().get(index);
	}

	/**
	 * Returns the number of children of parent.
	 */
	public int getChildCount(Object parent) {
		Gateway p = (Gateway) parent;
		return p.getChildren().size();
	}

	/**
	 * Returns the index of child in parent.
	 */
	public int getIndexOfChild(Object parent, Object child) {
		Gateway p = (Gateway) parent;
		return p.getIndexOfChild((Gateway) child);
	}

	/**
	 * Returns the root of the tree.
	 */
	public Object getRoot() {
		return rootGateway;
	}

	/**
	 * Returns true if node is a leaf.
	 */
	public boolean isLeaf(Object node) {
		Gateway p = (Gateway) node;
		return p.getChildren().size() == 0;
	}

	/**
	 * Removes a listener previously added with addTreeModelListener().
	 */
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.removeElement(l);
	}

	/**
	 * Messaged when the user has altered the value for the item identified by
	 * path to newValue. Not used by this model.
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
		System.out.println("*** valueForPathChanged : " + path + " --> "
				+ newValue);
	}
}
