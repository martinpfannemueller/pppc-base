package info.pppc.base.swtui.element.action;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.IRefreshable;

import org.eclipse.jface.action.Action;

/**
 * This generic action can be used to add a refresh button to a menu.
 * Whenever the action is called it performs a refresh on the specified
 * refreshable element.
 * 
 * @author Marcus Handte
 */
public class RefreshAction extends Action {

	/**
	 * The default text of the refresh element action. 
	 */
	private static final String UI_TEXT = "info.pppc.base.swtui.element.action.RefreshAction.TEXT";
	
	/**
	 * The refreshable that is refreshed whenever the action
	 * is activated.
	 */
	private IRefreshable refreshable;
	
	/**
	 * Creates a new refresh action that calls the refreshable
	 * whenever it is activated. 
	 * 
	 * @param refreshable The refreshable that performs the refresh.
	 */
	public RefreshAction(IRefreshable refreshable) {
		this.refreshable = refreshable;
		setText(BaseUI.getText(UI_TEXT));
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_REFRESH));
	}

	/**
	 * Called by jface whenever the action is executed. Calls
	 * the refresh method of the specified refreshable.
	 */
	public void run() {
		refreshable.refresh();
	}

}
