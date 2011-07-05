package info.pppc.base.swtui.action;

import info.pppc.base.swtui.Application;
import info.pppc.base.swtui.BaseUI;

import org.eclipse.jface.action.Action;

/**
 * The auto refresh action determines whether the application
 * will automatically refresh views whenever they are changed.
 * 
 * @author Marcus Handte
 */
public class AutoRefreshAction extends Action {

	/**
	 * The id of the externalized string.
	 */
	private static final String UI_TEXT = "info.pppc.base.swtui.action.AutoRefreshAction.TEXT";

	/**
	 * The application to control.
	 */
	private Application application;

	/**
	 * A flag that indicates whether the action will enable or
	 * disable the auto refresh. 
	 */
	private boolean enabled;

	/**
	 * The console action can add or remove the console from the
	 * current view of the application window.
	 * 
	 * @param application The application window to control.
	 * @param enabled Determines whether the action will enable or
	 *  disable the console. 
	 */
	public AutoRefreshAction(Application application, boolean enabled) {
		this.application = application;
		this.enabled = enabled;
		setChecked(! enabled);
		setText(BaseUI.getText(UI_TEXT));
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_REFRESH));
	} 

	/**
	 * Adds or removes the console from the view.
	 */
	public void run() {
		application.setAutoRefresh(enabled);
		setChecked(enabled);
		enabled = !enabled;
	}
}
