package info.pppc.base.swtui.element;

import info.pppc.base.swtui.*;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.Image;

/**
 * The element control is the basic extension unit that can be plugged into
 * the graphical user interface. In order to communicate with another control,
 * each control is equipped with an interface that enables it to access 
 * other controls directly.
 * 
 * @author Marcus Handte
 */
public abstract class AbstractElementControl extends AbstractControl {

	/**
	 * The element manager that enables communication between elements
	 * within the graphical user interface application.
	 */
	private IElementManager manager;

	/**
	 * Creates a new element control with the specified manager.
	 * 
	 * @param manager The manager that enables communication between
	 * 	gui parts.
	 */
	public AbstractElementControl(IElementManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Returns the manager of the element control.
	 * 
	 * @return The manager of the element control.
	 */
	public IElementManager getManager() {
		return manager;
	}

	/**
	 * Returns the name of the element control. The name should be
	 * human readable as it is displayed as tab identifier in the
	 * gui. Note that this attribute must not change during the
	 * execution.
	 * 
	 * @return The name of the control as it should be displayed.
	 */
	public abstract String getName();

	/**
	 * Returns the image of the element control. This method may
	 * return null to signal that no image should be used. The
	 * image is displayed in the tab to identify the element 
	 * graphically. 
	 * 
	 * @return The image of this element or null if no image 
	 * 	should be used.
	 */
	public abstract Image getImage();

	/**
	 * Returns the menu actions defined by this control. This method
	 * is called by the manager whenever the control receives the
	 * focus and thus, the menu must be shown. The default implementation
	 * returns an empty array to signal that there is no menu at all.
	 * Subclasses may override this method to provide menu actions.
	 * Note that null actions are interpreted as separators. 
	 * 
	 * @return The menu actions.
	 */
	public Action[] getMenuActions() {
		return new Action[0];
	}

}
