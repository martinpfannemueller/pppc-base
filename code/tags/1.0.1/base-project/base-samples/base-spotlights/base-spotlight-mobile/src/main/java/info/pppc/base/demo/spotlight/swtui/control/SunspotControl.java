package info.pppc.base.demo.spotlight.swtui.control;

import info.pppc.base.demo.spotlight.service.LedServiceProxy;
import info.pppc.base.demo.spotlight.service.LedState;
import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.IRefreshable;
import info.pppc.base.swtui.element.action.RefreshAction;
import info.pppc.base.swtui.element.action.RemoveAction;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.util.Logging;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;


/**
 * The sunspot control provides a user interface to control
 * the leds on a sunspot via its led service.
 * 
 * @author Marcus Handte
 */
public class SunspotControl extends AbstractElementControl implements IRefreshable {

	/**
	 * A vector with the state of the leds.
	 */
	protected Vector states = new Vector();

	/**
	 * The buttons to set the led color.
	 */
	protected Button[] colorButtons;
	
	/**
	 * The checkboxes to enable a led.
	 */
	protected Button[] enableButtons;
	
	/**
	 * The descriptor of the service.
	 */
	private final ServiceDescriptor descriptor;

	/**
	 * The proxy to the service.
	 */
	private final LedServiceProxy proxy;
	
	/**
	 * Creates a new control for a sunspot service.
	 * 
	 * @param manager The manager that hosts the control.
	 * @param desc The service descriptor for the service.
	 */
	public SunspotControl(IElementManager manager, ServiceDescriptor desc)
	{
		super(manager);
		this.descriptor = desc;
		this.proxy = new LedServiceProxy();
		proxy.setSourceID(new ReferenceID(SystemID.SYSTEM));
		proxy.setTargetID(descriptor.getIdentifier());
		try {
			manager.run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Loading colors ...", 2);
						monitor.worked(1);
						states = proxy.getLedStates();
					} catch (InvocationException e) {
						Logging.error(getClass(), "Could not load colors.", e);
					}
					monitor.done();					
				}
			}, false);			
		} catch (Throwable t) {
			Logging.error(getClass(), "Could not create control.", t);
		}
	}

	/***
	 * Returns the name of the system browser.
	 * 
	 * @return The name of the system browser.
	 */
	public String getName() {
		return "SunSpot on (" + descriptor.getIdentifier().getSystem() + ")";
	}

	/**
	 * Returns the default image of the browser tab.
	 * 
	 * @return The default image of the browser tab.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_SYSTEM);
	}

	/**
	 * Creates the control on the parent.
	 * 
	 * @param parent The parent of the control.
	 */
	public void showControl(final Composite parent) {
		// create the executor and creator panes
		ScrolledComposite scroll = new ScrolledComposite(parent,
				SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setExpandVertical(false);
		scroll.setExpandHorizontal(false);
		final Composite service = new Composite(scroll, SWT.NONE);
		scroll.setContent(service);
		scroll.setLayout(new GridLayout(1, false));
		scroll.setContent(service);
		// plus 1 for 
		service.setLayout(new GridLayout(4, false));
		setControl(scroll);
		// create LED optionss
		enableButtons = new Button[states.size()];
		colorButtons = new Button[states.size()];
		for (int i = 0; i < states.size(); i++) {
			LedState state = (LedState)states.elementAt(i); 
			final Button colorButton = new Button(service, SWT.NORMAL);
			colorButton.setText("Color");
			colorButton.setBackground(new Color(parent.getDisplay(), state.getRed(), state.getGreen(), state.getBlue()));
			colorButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			colorButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent arg0) {
					ColorDialog d = new ColorDialog(parent.getShell());
					RGB rgb = d.open();
					if (rgb != null) {
						colorButton.setBackground(new Color(parent.getDisplay(), rgb));
					}
				}				
			});
			final Button enableButton = new Button(service, SWT.CHECK);
			enableButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			enableButton.setText("On");
			enableButton.setSelection(state.isEnabled());
			enableButtons[i] = enableButton;
			colorButtons[i] = colorButton;
		}
		Button sendButton = new Button(service, SWT.NORMAL);
		sendButton.setText("Update");
		sendButton.setLayoutData(new GridData(SWT.CENTER,SWT.CENTER,false,false,4,1));
		sendButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				try {
					final Vector changed = new Vector();
					for (int i = 0; i < states.size(); i++) {
						LedState state = (LedState)states.elementAt(i);
						Button colorButton = colorButtons[i];
						Button enableButton = enableButtons[i];
						Color c = colorButton.getBackground();
						boolean enabled = enableButton.getSelection();
						if (c.getBlue() != state.getBlue() ||
								c.getRed() != state.getRed() ||
								c.getGreen() != state.getGreen() ||
								enabled != state.isEnabled()) {
							state.setBlue(c.getBlue());
							state.setGreen(c.getGreen());
							state.setRed(c.getRed());
							state.setEnabled(enabled);
							changed.addElement(state);
						}
					}
					getManager().run(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								monitor.beginTask("Sending changes ...", 2);
								monitor.worked(1);
								if (changed.size() > 0) {
									proxy.setLedStates(changed);
								}
							} catch (InvocationException e) {
								Logging.error(getClass(), "Could not update colors.", e);
							}
							monitor.done();					
						}
					}, false);
				} catch (Throwable t) {
					Logging.error(getClass(), "Could not create control.", t);
				}
			}				
		});
		// compute the size of the content pane
		Point point = service.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		service.setSize(point);
		scroll.setMinSize(point);
	}
	
	
	/**
	 * Called whenever the control is disposed.
	 */
	public synchronized void disposeControl() {
		super.disposeControl();
	}


	/**
	 * Refreshes the contents of the control by loading them from the
	 * sunspot service.
	 */
	public synchronized void refresh() {
		// load the colors
		try {
			getManager().run(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Loading colors ...", 2);
						monitor.worked(1);
						states = proxy.getLedStates();
					} catch (InvocationException e) {
						Logging.error(getClass(), "Could not load colors.", e);
					}
					monitor.done();				
				}
			}, false);
		} catch (Throwable t) {
			Logging.error(getClass(), "Could not create control.", t);
		}
		// update the buttons
		for (int i = 0; i < states.size(); i++) {
			LedState state = (LedState)states.elementAt(i);
			colorButtons[i].setBackground(new Color(colorButtons[i].getDisplay(), 
					state.getRed(), state.getGreen(), state.getBlue()));
			enableButtons[i].setSelection(state.isEnabled());
		}
	}

	/**
	 * Returns the menu actions for the view menu.
	 * 
	 * @return The menu actions for the view menu.
	 */
	public Action[] getMenuActions() {
		return new Action[] {
			new RefreshAction(this),
			new RemoveAction(this, getManager())
		};
	}
	
	/**
	 * Returns the service descriptor attached to the control.
	 * 
	 * @return The service descriptor of the control.
	 */
	public ServiceDescriptor getDescriptor() {
		return descriptor;
	}
	
}
