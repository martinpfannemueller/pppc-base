package info.pppc.base.swtui.action;

import info.pppc.base.swtui.BaseUI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.ApplicationWindow;

/**
 * This default action can be used to close the application window which
 * will ultimately exit the application.
 * 
 * @author Marcus Handte
 */
public class ExitAction extends Action {

	/**
	 * The default text of the exit application action.
	 */
	private static final String UI_TEXT = "info.pppc.base.swtui.action.ExitAction.TEXT";

	/**
	 * The application window that will be closed by the action.
	 */
	private ApplicationWindow window;

	/**
	 * Creates a new exit application action that will close the specified
	 * application window.
	 * 
	 * @param window The window that is closed whenever the action is 
	 * 	executed.
	 */
	public ExitAction(ApplicationWindow window) {
		this.window = window;
		setText(BaseUI.getText(UI_TEXT));
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_EXIT));
	}

	/**
	 * Called by jface whenever the action is executed. This will close the
	 * application window passed to the constructor.
	 */
	public void run() {
		window.close();
	}


}
