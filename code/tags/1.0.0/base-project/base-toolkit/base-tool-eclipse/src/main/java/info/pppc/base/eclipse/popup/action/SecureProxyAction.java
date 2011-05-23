/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.generator.Generator;

/**
 * The secure proxy action generates the secure proxy for an interface.
 * 
 * @author Mac
 */
public class SecureProxyAction extends AbstractAction {

	/**
	 * Creates a new secure proxy action.
	 */
	public SecureProxyAction() {
		super();
	}

	/**
	 * Returns the secure proxy modifier.
	 * 
	 * @return The secure proxy modifier.
	 */
	public int[] getModifiers() {
		return new int[] {
			Generator.MODIFIER_SECURE_PROXY
		};
	}

}
