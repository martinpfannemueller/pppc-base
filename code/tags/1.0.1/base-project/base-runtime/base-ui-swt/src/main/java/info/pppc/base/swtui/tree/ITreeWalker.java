package info.pppc.base.swtui.tree;

/**
 * The tree walker interface is used to perform generic traversals on
 * tree nodes.
 * 
 * @author Marcus Handte
 */
public interface ITreeWalker {

	/**
	 * Called whenever a node is visited. The return value determines
	 * whether the children of this node should also be traversed. True
	 * indicates that the traversal continues, false indicates that the
	 * traversal should stop.
	 * 
	 * @param node The node to visit at the moment.
	 * @return True to visit the children of the node, false to continue
	 * 	without visiting them.
	 */
	public boolean visit(TreeNode node);

}
