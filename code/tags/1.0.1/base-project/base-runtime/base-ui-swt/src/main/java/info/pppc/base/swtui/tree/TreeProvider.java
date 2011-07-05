package info.pppc.base.swtui.tree;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A generic tree content provider that uses the tree nodes to determine
 * children, parents and contents. 
 * 
 * @author Marcus Handte
 */
public class TreeProvider implements ITreeContentProvider {

	/**
	 * Creates a new tree model for tree nodes. This tree
	 * provider does not update dirty nodes, it simply ignores
	 * them.
	 */
	public TreeProvider() {
		super();
	}

	/***
	 * Returns the children of the tree node.
	 * 
	 * @param parentElement The parent tree node.
	 * @return The children of the tree node.
	 */
	public Object[] getChildren(Object parentElement) {
		return ((TreeNode)parentElement).getChildren();
	}

	/**
	 * Returns the parent of the specified tree node.
	 * 
	 * @param element The node whose parent should be returned.
	 * @return The parent of the node.
	 */
	public Object getParent(Object element) {
		return ((TreeNode)element).getParent();
	}

	/***
	 * Determines whether the tree node has children.
	 * 
	 * @param element The tree node to lookup.
	 * @return True if the tree node has children, false otherwise.
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/***
	 * Returns the children of the tree node.
	 * 
	 * @param parentElement The parent tree node.
	 * @return The children of the tree node.
	 */
	public Object[] getElements(Object parentElement) {
		return getChildren(parentElement);
	}

	/**
	 * Disposes the tree model.
	 */
	public void dispose() {
		// nothing to be done here, since nothing is cached.
	}

	/**
	 * Called whenever the root of the viewer changes.
	 * 
	 * @param viewer The corresponding viewer.
	 * @param oldInput The old input object of the viewer.
	 * @param newInput The new input object of the viewer.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to be done here, since nothing is cached.
	}

}
