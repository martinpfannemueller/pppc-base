package info.pppc.base.swtui.service;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.action.RemoveAction;
import info.pppc.base.swtui.service.data.InvokeData;
import info.pppc.base.swtui.widget.ImageButton;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

/**
 * The invoke builder control enables a user to manually create an
 * invocation.
 * 
 * @author Marcus Handte
 */
public class InvokeBuilderControl extends AbstractElementControl {

	/**
	 * The name of the invoke builder control as contained in the
	 * resource property file.
	 */
	public static final String UI_TEXT = "info.pppc.base.swtui.service.InvokeBuilderControl.TEXT";

	/**
	 * The resource key for the name label text.
	 */
	public static final String UI_NAME = "info.pppc.base.swtui.service.InvokeBuilderControl.NAME";
	
	/**
	 * The resource key for the return type label text.
	 */
	public static final String UI_RETURN = "info.pppc.base.swtui.service.InvokeBuilderControl.RETURN";
	
	/**
	 * The resource key for the parameter label text.
	 */
	public static final String UI_PARAMETER = "info.pppc.base.swtui.service.InvokeBuilderControl.PARAMETER";
	
	/**
	 * The resource key for the result label text.
	 */
	public static final String UI_RESULT = "info.pppc.base.swtui.service.InvokeBuilderControl.RESULT";
	
	
	/**
	 * The invocation that is configured using the object.
	 */
	private InvokeData invoke = new InvokeData();
	
	/**
	 * The parameter list that contains the parameters.
	 */
	private List paramList;
	
	/**
	 * The outline viewer that contains the outline.
	 */
	private Text outlineViewer;
	
	/**
	 * Creates a new invocation builder control for the specified
	 * manager.
	 * 
	 * @param manager The manager used to create the invocation.
	 */
	public InvokeBuilderControl(IElementManager manager) {
		super(manager);
	}

	/**
	 * Returns the name of the control as shown in the user interface.
	 * 
	 * @return The name of the control.
	 */
	public String getName() {
		return BaseUI.getText(UI_TEXT);
	}

	/**
	 * Returns the image of the control as shown in the user interface.
	 * 
	 * @return The image of the control.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_INVOKE);
	}

	/**
	 * Creates the control on the specified parent component.
	 * 
	 * @param parent The parent of the control.
	 */
	public void showControl(Composite parent) {
		Composite p = new Composite(parent, SWT.NONE);
		p.setLayout(new GridLayout(3, false));
		setControl(p);
		// create the name fields
		Label nameLabel = new Label(p, SWT.NONE);
		nameLabel.setText(BaseUI.getText(UI_NAME));
		nameLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Text nameText = new Text(p, SWT.SINGLE | SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		// create the return type fields
		Label returnLabel = new Label(p, SWT.NONE);
		returnLabel.setText(BaseUI.getText(UI_RETURN));
		nameLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Combo returnCombo = new Combo(p, SWT.BORDER | SWT.FLAT | SWT.SINGLE | SWT.READ_ONLY);
		returnCombo.setItems(new String[] { "void", "int", "String"});
		returnCombo.select(0);
		returnCombo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		// create the parameter fields
		Label paramLabel = new Label(p, SWT.NONE);
		paramLabel.setText(BaseUI.getText(UI_PARAMETER));
		paramLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Combo paramTypeCombo = new Combo(p, SWT.BORDER | SWT.FLAT | SWT.SINGLE | SWT.READ_ONLY);
		paramTypeCombo.setItems(new String[] { "int", "String" });
		paramTypeCombo.select(0);
		paramTypeCombo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		Label paramSpace = new Label(p, SWT.NONE);
		paramSpace.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 4));
		final Text paramValText = new Text(p, SWT.BORDER | SWT.SINGLE);
		paramValText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
		final ImageButton paramAdd = new ImageButton(p, SWT.FLAT);
		paramAdd.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		paramAdd.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		paramAdd.setEnabled(false);
		paramList = new List(p, SWT.SINGLE | SWT.BORDER);
		paramList.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 3));
		final ImageButton paramDown = new ImageButton(p, SWT.FLAT);
		paramDown.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_UP));
		paramDown.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		paramDown.setEnabled(false);
		final ImageButton paramUp = new ImageButton(p, SWT.FLAT);
		paramUp.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_DOWN));
		paramUp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		paramUp.setEnabled(false);
		final ImageButton paramRemove = new ImageButton(p, SWT.FLAT);
		paramRemove.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		paramRemove.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		paramRemove.setEnabled(false);
		// create the outline fields
		Label outlineLabel = new Label(p, SWT.NONE);
		outlineLabel.setText(BaseUI.getText(UI_RESULT));
		outlineLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		outlineViewer = new Text(p, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
		outlineViewer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 2));
		final ImageButton outlineAdd = new ImageButton(p, SWT.FLAT);
		outlineAdd.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_OK));
		outlineAdd.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		outlineAdd.setEnabled(false);
		Label outlineSpace1 = new Label(p, SWT.NONE);
		outlineSpace1.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1));
		Label outlineSpace2 = new Label(p, SWT.NONE);
		outlineSpace2.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1));
		// add the text listeners
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = nameText.getText().trim();
				invoke.setName(name);
				outlineAdd.setEnabled(! name.equals(""));
				update();
			}
		});
		returnCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				switch (returnCombo.getSelectionIndex()) {
					case 0:
						invoke.setReturnType(InvokeData.TYPE_VOID);
						break;
					case 1:
						invoke.setReturnType(InvokeData.TYPE_INTEGER);
						break;
					case 2:
						invoke.setReturnType(InvokeData.TYPE_STRING);
						break;
					default:
						// will never happen
				}
				update();
			}
		});
		paramTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = paramValText.getText();
				if (paramTypeCombo.getSelectionIndex() == 0) {
					try {
						Integer.parseInt(text);
						paramAdd.setEnabled(true);
					} catch (NumberFormatException ex) {
						paramAdd.setEnabled(false);
					}
				} else {
					paramAdd.setEnabled(true);
				}
			}
		});
		paramValText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String text = paramValText.getText();
				if (paramTypeCombo.getSelectionIndex() == 0) {
					try {
						Integer.parseInt(text);
						paramAdd.setEnabled(true);
					} catch (NumberFormatException ex) {
						paramAdd.setEnabled(false);
					}
				} else {
					paramAdd.setEnabled(true);
				}
			}
		});
		paramList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = paramList.getSelectionIndex();
				if (idx == -1) {
					paramRemove.setEnabled(false);
					paramUp.setEnabled(false);
					paramDown.setEnabled(false);
				} else {
					paramRemove.setEnabled(true);
					paramUp.setEnabled(idx != paramList.getItemCount() - 1);
					paramDown.setEnabled(idx != 0);
				}
			};
		});
		// add all ImageButton listeners
		paramAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = paramValText.getText();
				if (paramTypeCombo.getSelectionIndex() == 0) {
					try {
						int i = Integer.parseInt(text);
						invoke.addParameter(new Integer(i));
					} catch (NumberFormatException ex) {
						// will never happen
					}
					
				} else {
					invoke.addParameter(text);
				}
				update();
			};
		});
		paramRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = paramList.getSelectionIndex();
				if (idx != -1) {
					invoke.removeParameter(idx);
					update();
				}
			};
		});
		paramUp.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = paramList.getSelectionIndex();
				if (idx != -1 && idx != paramList.getItemCount() - 1) {
					invoke.moveParameter(idx, idx + 1);
					update();
					paramList.select(idx + 1);
					paramList.notifyListeners(SWT.Selection, null);
				}
			}
		});
		paramDown.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = paramList.getSelectionIndex();
				if (idx > 0) {
					invoke.moveParameter(idx, idx - 1);
					update();
					paramList.select(idx - 1);
					paramList.notifyListeners(SWT.Selection, null);
				}
			}
		});
		outlineAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ServiceControl.invocations.addElement(invoke);
				ServiceControl focus = null;
				AbstractElementControl[] cs = getManager().getElements();
				for (int i = 0; i < cs.length; i++) {
					if (cs[i] != null && cs[i] instanceof ServiceControl) {
						ServiceControl sc = (ServiceControl)cs[i];
						sc.refresh();
						focus = sc;
					}
				}
				getManager().removeElement(InvokeBuilderControl.this);
				if (focus != null) {
					getManager().focusElement(focus);
				}
			};
		});
		// update all elements
		update();
	}
	
	/**
	 * Updates the views using the invocation.
	 */
	private void update() {
		// update the outline viewer
		outlineViewer.setText(LabelProvider.toString(invoke));
		// update the parameter list
		paramList.removeAll();
		Object[] params = invoke.getParameters();
		String[] items = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			if (param instanceof String){
				items[i] = "String=" + param;
			} else if (param instanceof Integer) {
				items[i] = "int=" + param;
			} else {
				items[i] = "?"; // will never happen	
			}
		}
		paramList.setItems(items);
		paramList.deselectAll();
		paramList.notifyListeners(SWT.Selection, null);
	}


	/**
	 * Returns the menu actions for the control. At the
	 * present time, this will only return the remove action.
	 * 
	 * @return The menu actions of the control that are 
	 * 	available at the moment.
	 */
	public Action[] getMenuActions() {
		return new Action[] { 
			new RemoveAction(this, getManager()) 
		};
	}
	
}
