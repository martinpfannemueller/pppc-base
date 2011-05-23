package info.pppc.base.demo.spotlight.swtui.action;

import java.util.Vector;

import info.pppc.base.demo.spotlight.swtui.control.SunspotControl;
import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.service.ServiceRegistry;
import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.system.util.Logging;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;


/**
 * The sunspot action is used to search for sunspot led services and
 * to attach 
 * 
 * @author Marcus Handte
 */
public class SunspotAction extends Action {

	/**
	 * The element manager that will receive the new sunspot controls.
	 */
	protected IElementManager manager;

	/**
	 * Creates a new browser action that can add a sunspot control.
	 * 
	 * @param manager The element manager used to create the new
	 * 	sunspot controls.
	 */
	public SunspotAction(IElementManager manager) {
		this.manager = manager;
		setText("Search Sunspots");
		setImageDescriptor(BaseUI.getDescriptor(BaseUI.IMAGE_BUTTON_RUN));
	}
	
	/**
	 * Searches for sun spot services and attaches a control
	 * for each of them if the control does not exist already.
	 */
	public void run() {
		final Vector result = new Vector();
		try {
			manager.run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask("Searching ...", 2);
					monitor.worked(1);
					ServiceRegistry registry = ServiceRegistry.getInstance();
					ServiceDescriptor[] descriptors = registry.lookup("SunSpot", ServiceRegistry.LOOKUP_BOTH);
					for (int i = 0; i < descriptors.length; i++) {
						result.addElement(descriptors[i]);
					}
					monitor.done();
					Logging.log(getClass(), "Found " + descriptors.length + " SunSpot services.");
				}
			}, false);			
		} catch (Throwable t) {
			Logging.error(getClass(), "Could not search for services.", t);
		}
		descriptor: for (int i = 0; i < result.size(); i++) {
			ServiceDescriptor descriptor = (ServiceDescriptor)result.elementAt(i);
			// check whether the control is available already
			AbstractElementControl[] controls = manager.getElements();
			for (int j = 0; j < controls.length; j++) {
				if (controls[j] instanceof SunspotControl) {
					ServiceDescriptor d = ((SunspotControl)controls[j]).getDescriptor();
					if (d.getIdentifier().equals(descriptor.getIdentifier())) {
						manager.focusElement(controls[j]);
						continue descriptor;
					}
				}
			}
			// add new control since it does not exist, yet
			SunspotControl control = new SunspotControl(manager, descriptor);
			manager.addElement(control);
			manager.focusElement(control);
		}
	}


}
