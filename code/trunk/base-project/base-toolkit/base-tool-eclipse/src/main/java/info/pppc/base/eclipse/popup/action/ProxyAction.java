/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.generator.Generator;

/**
 * The proxy action generates the proxy for an interface.
 * 
 * @author Mac
 */
public class ProxyAction extends AbstractAction {

	/**
	 * Creates a new proxy action.
	 */
	public ProxyAction() {
		super();
	}

	/**
	 * Returns the proxy modifier.
	 * 
	 * @return The proxy modifier.
	 */
	public int[] getModifiers() {
		return new int[] {
			Generator.MODIFIER_PROXY
		};
	}

}
