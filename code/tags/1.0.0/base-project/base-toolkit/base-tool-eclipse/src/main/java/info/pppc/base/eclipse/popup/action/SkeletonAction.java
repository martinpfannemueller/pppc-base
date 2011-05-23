/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.generator.Generator;

/**
 * The skeleton action generates a skeleton for an interface.
 * 
 * @author Mac
 */
public class SkeletonAction extends AbstractAction {

	/**
	 * Creates a new skeleton action.
	 */
	public SkeletonAction() {
		super();
	}

	/**
	 * Returns the skeleton modifier.
	 * 
	 * @return The skeleton modifier.
	 */
	public int[] getModifiers() {
		return new int[] {
			Generator.MODIFIER_SKELETON
		};
	}

}
