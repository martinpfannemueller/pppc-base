package info.pppc.base.swtui.system.action;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.service.ServiceControl;
import info.pppc.base.system.SystemID;

import org.eclipse.jface.action.Action;

/**
 * The service action is used to start the service control
 * for devices that contain a service registry.
 * 
 * @author Marcus Handte
 */
public class ServiceAction extends Action {

	/**
	 * The string that is used to describe the action. 
	 */
	public static final String UI_TEXT = "info.pppc.base.swtui.system.action.ServiceAction.TEXT";
	
	/**
	 * The element manager that will be used to create the service control.
	 */
	private IElementManager manager;
	
	/**
	 * The system id of the system that will be used to perform queries.
	 */
	private SystemID systemID;
	
	/**
	 * The name of the system that will be used to perform queries.
	 */
	private String name;
	
	/**
	 * Creates a new service action that can start the service
	 * control for the specified system.
	 * 
	 * @param manager The element manager used to create the control.
	 * @param systemID The system id of the system that will be used to perform queries.
	 * @param name The name of the system used to concatenate with the name of
	 * 	the service control
	 */ 
	public ServiceAction(IElementManager manager, SystemID systemID, String name) {
		super(BaseUI.getText(UI_TEXT), BaseUI.getDescriptor(BaseUI.IMAGE_SERVICE));
		this.manager = manager;
		this.systemID = systemID;
		this.name = name;
	}
	
	/**
	 * Detects whether the element manager already contains
	 * a service control for the specified system id and
	 * either focuses the existing control or creates a new
	 * one.
	 */
	public void run() {
		AbstractElementControl[] controls = manager.getElements();
		for (int i = 0; i < controls.length; i++) {
			if (controls[i] != null && controls[i] instanceof ServiceControl) {
				ServiceControl sc = (ServiceControl)controls[i];
				if (sc.getSystemID().equals(systemID)) {
					manager.focusElement(sc);
					return;
				}
			}
		}
		ServiceControl sc = new ServiceControl(manager, systemID, name);
		manager.addElement(sc);
		manager.focusElement(sc);
	}

}
