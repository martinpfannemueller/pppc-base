package info.pppc.base.swtui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * The base class for all controls this class provides helper methods
 * and a common interface to show and dispose the view of the control.
 * This class is not intended to be sub classed. Users will typically
 * subclass the abstract class element control which has a broader 
 * interface used for gui plug-ins.
 * 
 * @author Marcus Handte
 */
public abstract class AbstractControl {

	/**
	 * The base control of this abstract control. This is
	 * basically the base element of the view of this control.
	 */
	private Control control;

	/**
	 * Creates a new control.
	 */
	public AbstractControl() {
		super();		
	}
	
	/**
	 * Sets the base control of this control. This method
	 * should be called from the show control method with
	 * the base element created by this control.
	 * 
	 * @param control The graphical control that
	 * 	is the base control of this control.
	 */
	public void setControl(Control control) {
		this.control = control; 
	}
	
	/**
	 * Returns the control that is currently used as base
	 * control of this abstract control. This is basically
	 * the view of the control.
	 * 
	 * @return The view of the control.
	 */
	public Control getControl() {
		return control;
	}

	/**
	 * Returns the display that is used to display the base
	 * control. If the control is not set or already disposed,
	 * this method returns null.
	 * 
	 * @return The current display or null if the control is
	 * 	not shown or disposed.
	 */
	public Display getDisplay() {
		try {
			if (control != null) {
				return control.getDisplay();	
			} else {
				return null;
			}			
		} catch (Throwable t) {
			return null;
		}
	}
	
	/**
	 * Returns the shell that is used to display the base 
	 * control. If the control is not set or already disposed,
	 * this method return null.
	 * 
	 * @return The current shell or null if the control is not
	 * 	shown or disposed.
	 */
	public Shell getShell() {
		try {
			if (control != null) {
				return control.getShell();
			} else {
				return null;
			}			
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Displays the control on the specified parent control. A
	 * call to this method must set the base control using the
	 * set control method.
	 * 
	 * @param parent The parent of the control.
	 */
	public abstract void showControl(Composite parent);
 
	/**
	 * Disposes the specified control. A call to this method 
	 * should remove the base control including all children
	 * that might reside on it.
	 */
	public void disposeControl() {
		if (control != null) {
			control.dispose();	
		}
	}	

}
