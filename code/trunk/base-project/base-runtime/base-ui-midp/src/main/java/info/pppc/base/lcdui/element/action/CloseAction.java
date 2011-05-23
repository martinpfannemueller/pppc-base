package info.pppc.base.lcdui.element.action;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.element.ElementAction;

/**
 * The close action removes a certain view from the set of
 * views.
 * 
 * @author Marcus Handte
 */
public class CloseAction extends ElementAction {

	/**
	 * The key for the text of the close action.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.element.action.CloseAction.TEXT";
	
	/**
	 * The element that should be closed by the action.
	 */
	private AbstractElement element;
	
	/**
	 * Creates a new close action for the specified element.
	 * 
	 * @param element The element that should be closed.
	 */
	public CloseAction(AbstractElement element) {
		super(BaseUI.getText(UI_TEXT), true);
		this.element = element;
	}

	/**
	 * Removes the element of the action from its manager.
	 */
	public void run() {
		element.getManager().removeElement(element);
	}

}
