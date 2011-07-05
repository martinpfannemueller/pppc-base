package info.pppc.base.swtui.tree;

import java.util.Vector;

/**
 * A generic tree node that supports a pre-order traversal. The parent node
 * is always automatically maintained by the add and remove methods. Each
 * node has a must have type attribute that can be used to execute generic
 * search methods.
 * 
 * @author Marcus Handte
 */
public class TreeNode {

	/**
	 * The parent node of the tree node. 
	 */
	private TreeNode parent;
	
	/**
	 * The children of the tree node.
	 */
	private Vector children = new Vector();

	/**
	 * The user defined data object of the tree node.
	 */
	private Object data;

	/**
	 * The user defined type attribute of the tree node.
	 */
	private int type;

	/**
	 * Creates a new tree node that has no parent and no children
	 * that has the specified type and no user object.
	 * 
	 * @param type The type of the tree node.
	 */
	public TreeNode(int type) {
		this(type, null);
	}
	
	/**
	 * Creates a new tree node that has no parent and no children
	 * with the specified type and the specified user object.
	 * 
	 * @param type The type of the tree node.
	 * @param data The data object of the tree node.
	 */
	public TreeNode(int type, Object data) {
		this.type = type;
		this.data = data;
	}
	
	
	/**
	 * Returns the root node of this tree. 
	 * 
	 * @return The root node of this tree.
	 */
	public TreeNode getRoot() {
		if (getParent() == null) {
			return this;
		} else {
			return getParent().getRoot();
		}
	}
	
	/**
	 * Returns the parent of the tree node or null if this node
	 * is not part of a tree.
	 * 
	 * @return The parent of the tree node or null.
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * Returns the parent with the specified type. If no parent
	 * has the specified type, this method will return null.
	 * 
	 * @param type The type of the parent.
	 * @return The first parent with the specified type or null
	 * 	if no parent has the specified type.
	 */
	public TreeNode getParent(int type) {
		TreeNode node = this;
		while (node.getParent() != null) {
			node = node.getParent();
			if (node.getType() == type) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Adds the specified node as a child and adjusts the parent of
	 * the child.
	 * 
	 * @param node The node to add.
	 * @return True if the node has been added, false if the node
	 * 	was already a child of this node.
	 */
	public boolean addChild(TreeNode node) {
		if (node.parent != this) {
			node.parent = this;
			children.addElement(node);
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Removes the specified node as a child of this node and sets
	 * the parent of the child to null.
	 * 
	 * @param node The node to remove.
	 * @return True if the node has been removed, false if the node
	 * 	was not a child of this node.
	 */
	public boolean removeChild(TreeNode node) {
		if (node.parent == this) {
			children.remove(node);
			node.parent = null;
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Returns all children in the order in which they have been added
	 * to the tree.
	 * 
	 * @return The children of this node in the order in which they 
	 * 	have been added.
	 */
	public TreeNode[] getChildren() {
		return (TreeNode[])children.toArray
			(new TreeNode[children.size()]);
	}
	
	/**
	 * Returns the children that have the specified type attribute. The
	 * flag indicates whether the lookup should be recursive.
	 * 
	 * @param type The type of the children to lookup.
	 * @param recurse True to perform a recursive lookup.
	 * @return The children (possibly recursive children) that have the
	 * 	specified type.
	 */
	public TreeNode[] getChildren(int type, boolean recurse) {
		Vector v = new Vector();
		for (int i = 0; i < children.size(); i++) {
			TreeNode child = (TreeNode)children.elementAt(i);
			if (child.getType() == type) {
				v.addElement(child);
			}
			if (recurse) {
				TreeNode[] rChildren = child.getChildren(type, true);
				for (int j = 0; j < rChildren.length; j++) {
					v.addElement(rChildren[j]);
				}
			}
		}
		TreeNode[] result = new TreeNode[v.size()];
		for (int i = 0; i < v.size(); i++) {
			result[i] = (TreeNode)v.elementAt(i);
		}
		return result;
	}
	

	/**
	 * Removes all children from this node and adjusts their parents 
	 * accordingly.
	 */
	public void removeAllChildren() {
		for (int i = children.size() - 1; i >= 0; i--) {
			TreeNode node = (TreeNode)children.elementAt(i);
			children.removeElementAt(i);
			node.parent = null;
		}
	}

	/**
	 * Returns the type of the tree node.
	 * 
	 * @return The user defined type of the node.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the type of the tree node.
	 * 
	 * @param i The new type of the tree node.
	 */
	public void setType(int i) {
		type = i;
	}

	/**
	 * Sets the user defined data object of this node.
	 * 
	 * @param data The new user defined data object of the node.
	 */
	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * Returns the data object of this node.
	 * 
	 * @return The data object of this node.
	 */
	public Object getData() {
		return data;
	}
	
	/**
	 * Traverses the tree in pre-order traversal.
	 * 
	 * @param walker The tree walker that will be updated on every
	 * 	step through the tree.
	 */
	public void traverse(ITreeWalker walker) {
		boolean walk = walker.visit(this);
		if (walk) {
			for (int i = 0; i < children.size(); i++) {
				TreeNode node = (TreeNode)children.elementAt(i);
				node.traverse(walker);	
			}
		}
	}


}
