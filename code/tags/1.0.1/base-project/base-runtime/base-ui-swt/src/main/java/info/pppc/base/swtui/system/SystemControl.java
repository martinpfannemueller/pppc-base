package info.pppc.base.swtui.system;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.ElementListener;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.IRefreshable;
import info.pppc.base.swtui.element.action.RefreshAction;
import info.pppc.base.swtui.element.action.RemoveAction;
import info.pppc.base.swtui.tree.TreeNode;
import info.pppc.base.swtui.tree.TreeProvider;
import info.pppc.base.system.InvocationBroker;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * The system browser shows the system that have been discovered by the local
 * system. It creates its model from the local device registry of base.
 * 
 * @author Marcus Handte
 */
public class SystemControl extends AbstractElementControl implements IRefreshable {

	/**
	 * The name of the system browser as shown by the gui.
	 */
	public static final String UI_TEXT = "info.pppc.base.swtui.system.SystemControl.TEXT";

	/**
	 * The action provider for the selections of the system browser tree
	 */
	protected SystemActionProvider actionProvider = new SystemActionProvider();
	
	/**
	 * The label provider of the system browser tree.
	 */
	protected SystemLabelProvider labelProvider = new SystemLabelProvider();
	
	/** 
	 * The model provider of the system browser tree.
	 */
	protected SystemModelProvider modelProvider = new SystemModelProvider
		(InvocationBroker.getInstance().getDeviceRegistry());

	/**
	 * The content provider of the system browser tree.
	 */
	protected TreeProvider contentProvider = new TreeProvider();
	
	/**
	 * The tree view that displays the system browsers contents.
	 */
	protected TreeViewer tree = null;

	/**
	 * Creates a new system browser control.
	 * 
	 * @param manager The manager used to communicate with other
	 * 	controls.
	 */
	public SystemControl(IElementManager manager) {
		super(manager);
	}

	/***
	 * Returns the name of the system browser.
	 * 
	 * @return The name of the system browser.
	 */
	public String getName() {
		return BaseUI.getText(UI_TEXT);
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
	 * Creates the controls that are necessary to browse systems. At
	 * the present time, the view is constructed from a tree view.
	 * 
	 * @param parent the parent control.
	 */
	public synchronized void showControl(Composite parent) {
		tree = new TreeViewer(parent);
		final MenuManager manager = new MenuManager();
		tree.getControl().setMenu(manager.createContextMenu(tree.getControl()));
		tree.addSelectionChangedListener(new ElementListener(this, manager));
		tree.setContentProvider(contentProvider);
		tree.setLabelProvider(labelProvider);
		setControl(tree.getControl());
		refresh();
	}
	
	/**
	 * Returns the menu actions for the system browser. At the present
	 * time these actions are the generic remove action and the generic
	 * refresh action.
	 * 
	 * @return The menu actions of the view.
	 */
	public Action[] getMenuActions() {
		Action[] contextActions = new Action[0];
		if (tree != null && !tree.getControl().isDisposed()) {
			IStructuredSelection sel = (IStructuredSelection)tree.getSelection();
			Object element = sel.getFirstElement();
			if (element != null) {
				contextActions = actionProvider.getMenuActions(this, element);	
			}
		}
		if (contextActions.length > 0) {
			Action[] result = new Action[contextActions.length + 3];
			for (int i = 0; i < contextActions.length; i++) {
				result[i] = contextActions[i];
			}
			result[result.length - 3] = null;
			result[result.length - 2] = new RefreshAction(this);
			result[result.length - 1] = new RemoveAction(this, getManager());
			return result;
		} else {
			return new Action[] {
				new RefreshAction(this),
				new RemoveAction(this, getManager())
			};			
		}
	}
	
	/**
	 * Called whenever the control is disposed. Disposes the internal
	 * reference to the tree viewer.
	 */
	public synchronized void disposeControl() {
		tree = null;
		super.disposeControl();
	}


	/**
	 * Refreshes the contents of the browser control by creating a tree
	 * model from the device registry.
	 */
	public synchronized void refresh() {
		final TreeNode model = modelProvider.getModel(this);
		// change tree structure of viewer
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (tree != null && ! tree.getControl().isDisposed()) {
						tree.setInput(model);
					}
				}
			});
		}		
	}

}
