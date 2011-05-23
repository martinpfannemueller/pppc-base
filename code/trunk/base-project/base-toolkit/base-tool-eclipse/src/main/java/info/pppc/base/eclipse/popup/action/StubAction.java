/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.generator.Generator;

/**
 * The stub action generates all stubs for an interface.
 * 
 * @author Mac
 */
public class StubAction extends AbstractAction {

	/**
	 * Creates a new action.
	 */
	public StubAction() {
		super();
	}

	/**
	 * Returns the proxy and the skeleton modifier.
	 * 
	 * @return All modifiers of service stubs.
	 */
	public int[] getModifiers() {
		return new int[] {
			Generator.MODIFIER_PROXY,
			Generator.MODIFIER_SKELETON,
			Generator.MODIFIER_SECURE_PROXY,
			Generator.MODIFIER_SECURE_SKELETON
		};
	}

}
