package info.pppc.base.swtui.element.action;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;

import org.eclipse.jface.action.Action;

/**
 * This is a default action that can be used to remove an element from
 * the application view.
 * 
 * @author Marcus Handte
 */
public class RemoveAction extends Action {

	/**
	 * The default text of the remove element action. 
	 */
	private static final String UI_TEXT = "info.pppc.base.swtui.element.action.RemoveAction.TEXT";
	
	/**
	 * The element manager that is called whenever the action is executed.
	 */
	private IElementManager manager;
	
	/**
	 * The element that will be removed from the manager.
	 */
	private AbstractElementControl element;
	
	/**
	 * Creates a new remove element action that will remove the specified
	 * element from the specified manager. 
	 * 
	 * @param manager The manger used to perform the removal.
	 * @param element The element removed from the manager.
	 */
	public RemoveAction(AbstractElementControl element, IElementManager manager) {
		this.manager = manager;
		this.element = element;
		setText(BaseUI.getText(UI_TEXT));
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_CLOSE));
	}

	/**
	 * Called by jface whenever the action is executed. Removes the element
	 * from the manager.
	 */
	public void run() {
		manager.removeElement(element);
	}

}
