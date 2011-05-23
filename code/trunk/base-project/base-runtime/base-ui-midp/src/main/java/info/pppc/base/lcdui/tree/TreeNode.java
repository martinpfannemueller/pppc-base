package info.pppc.base.lcdui.tree;

import java.util.Vector;

import javax.microedition.lcdui.Image;

/**
 * The tree node is used to build a tree structure for the tree item.
 * It has a label and optionally an image that will be displayed by
 * the tree item. Furthermore it might have multiple children and it
 * has a flag that indicates whether it is expanded and a flag that
 * indicates whether the tree node is selectable. If it is selectable,
 * the tree item will show a select command whenever the tree node is
 * focused. This will enable the user to select the node which in turn
 * will fire an event that can be used by application logic.
 * 
 * @author Marcus Handte
 */
public class TreeNode {

	/**
	 * A user-defined data object that can be associated with the node.
	 */
	private Object data;
	
	/**
	 * The label that is shown in the user interface.
	 */
	private String label;
	
	/**
	 * The image of the tree node.
	 */
	private Image image;
	
	/**
	 * The parent of the tree node.
	 */
	private TreeNode parent;
	
	/**
	 * The children of the tree node.
	 */
	private Vector children = new Vector();
	
	/**
	 * A flag that indicates whether the node is expanded.
	 */
	private boolean expanded;
	
	/**
	 * A flag that indicates whether the node is selectable.
	 */
	private boolean selectable;
	
	/**
	 * Creates a non-selectable node with the specified label
	 * and user data object.
	 * 
	 * @param label The label of the tree node.
	 * @param data The user data object of the node.
	 */
	public TreeNode(Object data, String label) {
		this(data, label, null, false);
	}
	
	/**
	 * Creates a non-selectable node with the specified label,
	 * image and user data object.
	 * 
	 * @param label The label of the tree node.
	 * @param data The user data object of the node.
	 * @param image The image of the tree node.
	 */
	public TreeNode(Object data, String label, Image image) {
		this(data, label, image, false);
	}
	
	/**
	 * Creates a tree-node with the specified label, image and user data object
	 * with the specified selectable state.
	 * 
	 * @param label The label of the tree node.
	 * @param data The user data object of the tree node.
	 * @param image The image of the tree node.
	 * @param selectable A flag that indicates whether the tree node is selectable.
	 */
	public TreeNode(Object data, String label, Image image, boolean selectable) {
		this.label = label;
		this.image = image;	
		this.data = data;
		this.selectable = selectable;
	}

	/**
	 * Returns the image of the tree node.
	 * 
	 * @return The image of the node.
	 */
	public Image getImage() {
		return image;
	}
	
	/**
	 * Returns the label of the node.
	 * 
	 * @return The label of the node.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the current parent of the node.
	 * 
	 * @return The parent of the node.
	 */
	public TreeNode getParent() {
		return parent;
	}
	
	/**
	 * Returns the user data object of the node.
	 * 
	 * @return The user data object of the node.
	 */
	public Object getData() {
		return data;
	}
	
	/**
	 * Sets the user data object of the node to the specified value.
	 * 
	 * @param data The data object of the node.
	 */
	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * A flag that indicates whether the current node is expanded.
	 * 
	 * @return True if the node is expanded, false otherwise.
	 */
	public boolean isExpanded() {
		return expanded;
	}
	
	/**
	 * Sets the expansion state of the tree node to the specified value.
	 * 
	 * @param expanded The expanded state of the tree node.
	 */
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	/**
	 * Returns a flag that indicates whether the tree node is selectable.
	 * 
	 * @return True if the node is selectable, false otherwise.
	 */
	public boolean isSelectable() {
		return selectable;
	}
	
	/**
	 * Sets the flag that indicates whether the tree node is selectable.
	 * 
	 * @param selectable True if selectable, false otherwise.
	 */
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}
	
	/**
	 * Adds the specified node as a child. If the tree structure would
	 * be broken due to the addition, a runtime exception will be thrown.
	 * 
	 * @param node The tree node that should be added as child.
	 * @throws RuntimeException Thrown if the addition would break the tree.
	 */
	public void addChild(TreeNode node) {
		if (node == this) throw new RuntimeException("Illegal structure.");
		TreeNode p = getParent();
		while (p != null) {
			if (p == node) throw new RuntimeException("Illegal structure.");
			p = p.getParent();
		}
		children.addElement(node);
		if (node.parent != null) {
			node.parent.removeChild(node);
		}
		node.parent = this;
	}
	
	/**
	 * Removes the specified tree node from this node.
	 * 
	 * @param node The child node to remove.
	 * @return True if the node has been removed, false if the
	 * 	node was not a child.
	 */
	public boolean removeChild(TreeNode node) {
		if (children.removeElement(node)) {
			node.parent = null;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the vector of children of this node. This vector
	 * should not be modified directly as it is a reference only.
	 * 
	 * @return The vector that contains the children of the node.
	 */
	public Vector getChildren() {
		return children;
	}
	
	/**
	 * Determines whether the tree node has children. True if the node
	 * has children, false if not.
	 * 
	 * @return True if the tree node has children, false otherwise.
	 */
	public boolean hasChildren() {
		return children.size() > 0;
	}
	
}
