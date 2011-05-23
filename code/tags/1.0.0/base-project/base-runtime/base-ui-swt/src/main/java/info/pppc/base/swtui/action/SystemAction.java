package info.pppc.base.swtui.action;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.system.SystemControl;

import org.eclipse.jface.action.Action;

/**
 * The browser action can add a new system browser to the current view
 * of a element manager.
 * 
 * @author Marcus Handte
 */
public class SystemAction extends Action {

	/**
	 * The id of the text resource of the action.
	 */
	private static final String UI_TEXT = "info.pppc.base.swtui.action.SystemAction.TEXT";

	/**
	 * The element manager that will receive the new system browser.
	 */
	protected IElementManager manager;

	/**
	 * Creates a new browser action that can add a system browser to
	 * the specified element manager.
	 * 
	 * @param manager The element manager used to create a new system
	 * 	browser.
	 */
	public SystemAction(IElementManager manager) {
		this.manager = manager;
		setText(BaseUI.getText(UI_TEXT));
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_SYSTEM));
	}
	
	/**
	 * Adds a new system browser to the specified element manager.
	 */
	public void run() {
		AbstractElementControl[] elements = manager.getElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i] instanceof SystemControl) {
				manager.focusElement(elements[i]);
				return;
			}
		}
		SystemControl bc = new SystemControl(manager);
		manager.addElement(bc);
		manager.focusElement(bc);
	}


}
