package info.pppc.base.lcdui.action;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.system.SystemElement;

/**
 * The system action enables a user to bring up the system browser.
 * 
 * @author Marcus Handte
 */
public class SystemAction extends ElementAction {

	/**
	 * The resource key for the action name.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.action.SystemAction.TEXT";
	
	/**
	 * The element manager that will be used to create the browser.
	 */
	protected IElementManager manager;
	
	/**
	 * Creates a new system browser action using the specified manager.
	 * 
	 * @param manager The manager used by the action.
	 */
	public SystemAction(IElementManager manager) {
		super(BaseUI.getText(UI_TEXT), true);
		this.manager = manager;
	}
	
	/**
	 * Called whenever the action is executed. This will focus the
	 * last existing system browser or it will open a new one if
	 * none exists.
	 */
	public void run() {
		AbstractElement[] elements = manager.getElements();
		for (int i = elements.length - 1; i >= 0; i--) {
			if (elements[i] instanceof SystemElement) {
				manager.focusElement(elements[i]);
				return;
			}
		}
		SystemElement system = new SystemElement(manager);
		manager.addElement(system);
		manager.focusElement(system);
	}

}
