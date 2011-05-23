/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.generator.Generator;

/**
 * The secure skeleton action generates a secure skeleton for an interface.
 * 
 * @author Mac
 */
public class SecureSkeletonAction extends AbstractAction {

	/**
	 * Creates a new secure skeleton action.
	 */
	public SecureSkeletonAction() {
		super();
	}

	/**
	 * Returns the secure skeleton modifier.
	 * 
	 * @return The secure skeleton modifier.
	 */
	public int[] getModifiers() {
		return new int[] {
			Generator.MODIFIER_SECURE_SKELETON
		};
	}

}
