package info.pppc.base.swtui.element;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * This class provides a selection listener that computes and adjusts the 
 * contents of a menu based on the current actions provided by a 
 * element control. It can also automatically force an menu update on the
 * underlying manager of the element.
 * 
 * @author Marcus Handte
 */
public class ElementListener implements ISelectionChangedListener {

	/**
	 * The control that provides the menu contents
	 */
	private AbstractElementControl control;

	/**
	 * The menu manager that needs to be updated.
	 */
	private IMenuManager manager;

	/**
	 * A flag that indicates whether the manager of the control 
	 * should be notified about the menu changes.
	 */
	private boolean update = true;

	/**
	 * Creates a new element listener that will receive its actions
	 * from the specified control.
	 * 
	 * @param control The element control that provides the actions.
	 * @param manager The menu manager that needs to be updated.
	 */
	public ElementListener(AbstractElementControl control, IMenuManager manager) {
		this.control = control;
		this.manager = manager;
	}
	
	/**
	 * Called by jface whenever the selection changes. This method
	 * computes the update
	 * 
	 * @param event The event that signals the selection change.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		manager.removeAll();
		Action[] actions = control.getMenuActions();
		for (int i = 0; i < actions.length; i++) {
			if (actions[i] != null) {
				manager.add(actions[i]);
			} else {
				manager.add(new Separator());
			}
		}
		manager.update(true);
		if (isUpdate()) {
			IElementManager em = control.getManager();
			em.updateElement();
		}
	}

	/**
	 * Returns whether this listener will automatically update the
	 * manager of the element. Typically this will update the view
	 * menu in the menu bar.
	 * 
	 * @return True to perform auto updates whenever the selection
	 * 	changes, false to manually update the view menu.
	 */
	public boolean isUpdate() {
		return update;
	}

	/**
	 * Sets the update flag. True to signal that the manager of the
	 * control should be notified whenever the selection changes,
	 * false otherwise.
	 * 
	 * @param b True to request an auto update of the view menu,
	 * 	false otherwise.
	 */
	public void setUpdate(boolean b) {
		update = b;
	}

}
