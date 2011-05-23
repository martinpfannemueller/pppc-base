package info.pppc.base.lcdui.element.action;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.lcdui.element.IRefreshable;

/**
 * The refresh action is used to refresh an element.
 * 
 * @author Marcus Handte
 */
public class RefreshAction extends ElementAction {

	/**
	 * The resource key for the action name.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.element.action.RefreshAction.TEXT";
	
	/**
	 * The refreshable that is refreshed by calling the action.
	 */
	private IRefreshable refreshable;
	
	/**
	 * Creates a new refresh action for the specified refreshable.
	 * 
	 * @param refreshable The refreshable that should be refreshed.
	 */
	public RefreshAction(IRefreshable refreshable) {
		super(BaseUI.getText(UI_TEXT), true);
		this.refreshable = refreshable;
	}
	
	/**
	 * Called whenever the action is executed. This will cause a
	 * call to the refreshable and then an update.
	 */
	public void run() {
		refreshable.refresh();
	}


}
