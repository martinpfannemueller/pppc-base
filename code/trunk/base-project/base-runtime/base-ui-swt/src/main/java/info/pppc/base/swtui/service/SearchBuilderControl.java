package info.pppc.base.swtui.service;

import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.action.RemoveAction;
import info.pppc.base.swtui.service.data.SearchData;
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
import org.eclipse.swt.widgets.Text;

/**
 * The search builder control is a tab control of the service control that
 * enables a user to create a service search.
 * 
 * @author Marcus Handte
 */
public class SearchBuilderControl extends AbstractElementControl {

	
	/**
	 * The name of the search builder control as contained in the
	 * resource property file.
	 */
	public static final String UI_TEXT = "info.pppc.base.swtui.service.SearchBuilderControl.TEXT";

	/**
	 * The resource key of the scope label text.
	 */
	public static final String UI_SCOPE = "info.pppc.base.swtui.service.SearchBuilderControl.SCOPE";
	
	/**
	 * The resource key of the service name label.
	 */
	public static final String UI_NAME = "info.pppc.base.swtui.service.SearchBuilderControl.NAME";
	
	/**
	 * The resource key of the service interface label.
	 */
	public static final String UI_INTERFACE = "info.pppc.base.swtui.service.SearchBuilderControl.INTERFACE";
	
	/**
	 * The resource key of the service property label.
	 */
	public static final String UI_PROPERTY = "info.pppc.base.swtui.service.SearchBuilderControl.PROPERTY";
	
	/**
	 * The resource key of the search result label.
	 */
	public static final String UI_RESULT = "info.pppc.base.swtui.service.SearchBuilderControl.RESULT";
	
	/**
	 * The search data configured by this control.
	 */
	private SearchData search = new SearchData();
	
	/**
	 * The viewer that displays the current state of the control.
	 */
	Text outlineViewer;
	
	/**
	 * Creates a new search builder with the specified manager.
	 * 
	 * @param manager The manager used to create the service builder.
	 */
	public SearchBuilderControl(IElementManager manager) {
		super(manager);
	}

	/**
	 * Returns the name of the search tab as shown by the
	 * service control.
	 * 
	 * @return The name of the search tab.
	 */
	public String getName() {
		return BaseUI.getText(UI_TEXT);
	}

	/**
	 * Returns the image of the search tab as shown by the
	 * service control.
	 * 
	 * @return The image of the search tab.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_SEARCH);
	}

	/**
	 * Creates the control on the specified parent.
	 * 
	 * @param parent The parent of the control.
	 */
	public void showControl(Composite parent) {
		Composite seachComposite = new Composite(parent, SWT.NONE);
		seachComposite.setLayout(new GridLayout(4, false));
		setControl(seachComposite);
		// add the lookup selector
		Label lookupLabel = new Label(seachComposite, SWT.NONE);
		lookupLabel.setText(BaseUI.getText(UI_SCOPE));
		lookupLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Combo lookupCombo = new Combo(seachComposite, SWT.BORDER | SWT.SINGLE | SWT.FLAT | SWT.READ_ONLY);
		lookupCombo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1));
		lookupCombo.setItems(new String[] { "LOCAL", "REMOTE", "BOTH" });
		lookupCombo.select(0);
		// add the name selector
		Label nameLabel = new Label(seachComposite, SWT.NONE);
		nameLabel.setText(BaseUI.getText(UI_NAME));
		nameLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Text nameText = new Text(seachComposite, SWT.BORDER | SWT.SINGLE | SWT.FLAT);
		nameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
		final ImageButton nameSet = new ImageButton(seachComposite, SWT.FLAT);
		nameSet.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		nameSet.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		nameSet.setEnabled(true);
		final ImageButton nameUnset = new ImageButton(seachComposite, SWT.FLAT);
		nameUnset.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		nameUnset.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		nameUnset.setEnabled(false);
		// add the interface selector
		Label ifaceLabel = new Label(seachComposite, SWT.NONE);
		ifaceLabel.setText(BaseUI.getText(UI_INTERFACE));
		ifaceLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Text ifaceText = new Text(seachComposite, SWT.BORDER | SWT.SINGLE | SWT.FLAT);
		ifaceText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
		final ImageButton ifaceAdd = new ImageButton(seachComposite, SWT.FLAT);
		ifaceAdd.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		ifaceAdd.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		ifaceAdd.setEnabled(true);
		final ImageButton ifaceRemove = new ImageButton(seachComposite, SWT.FLAT);
		ifaceRemove.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		ifaceRemove.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		ifaceRemove.setEnabled(false);
		// add the property selector
		Label propLabel = new Label(seachComposite, SWT.NONE);
		propLabel.setText(BaseUI.getText(UI_PROPERTY));
		propLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Text propNameText = new Text(seachComposite, SWT.BORDER | SWT.SINGLE | SWT.FLAT);
		propNameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		final ImageButton propRemove = new ImageButton(seachComposite, SWT.FLAT);
		propRemove.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		propRemove.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		propRemove.setEnabled(false);
		Label propSpace = new Label(seachComposite, SWT.NONE);
		propSpace.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final Text propValText = new Text(seachComposite, SWT.BORDER | SWT.SINGLE | SWT.FLAT);
		propValText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		final ImageButton propAdd = new ImageButton(seachComposite, SWT.FLAT);
		propAdd.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		propAdd.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		propAdd.setEnabled(true);
		// add the outline label
		Label outlineLabel = new Label(seachComposite, SWT.NONE);
		outlineLabel.setText(BaseUI.getText(UI_RESULT));
		outlineLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		// add the outline text box
		outlineViewer = new Text(seachComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
		outlineViewer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 2, 2));
		// add the outline add ImageButton
		final ImageButton outlineAdd = new ImageButton(seachComposite, SWT.FLAT);
		outlineAdd.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_OK));
		outlineAdd.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		Label outlineSpace1 = new Label(seachComposite, SWT.NONE);
		outlineSpace1.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1));
		Label outlineSpace2 = new Label(seachComposite, SWT.NONE);
		outlineSpace2.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 1, 1));
		// add text listeners
		lookupCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int lookup = 0;
				switch (lookupCombo.getSelectionIndex()) {
					case 0:
						lookup = SearchData.LOOKUP_LOCAL_ONLY;
						break;
					case 1:
						lookup = SearchData.LOOKUP_REMOTE_ONLY;
						break;
					case 2:
						lookup = SearchData.LOOKUP_BOTH;
						break;
					default:
						// will never happen
				}
				search.setLookup(lookup);
				update();
			}
		});
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (search.getName() != null) {
					nameSet.setEnabled(! search.getName().equals(nameText.getText()));
				} else {
					nameSet.setEnabled(true);
				}
			}
		});
		ifaceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String[] ifaces = search.getInterfaces();
				boolean contained = false;
				for (int i = 0; i < ifaces.length; i++) {
					if (ifaces[i].equals(ifaceText.getText())) {
						contained = true;
						break;
					}
				}
				ifaceAdd.setEnabled(!contained);
				ifaceRemove.setEnabled(contained);
			}
		});
		propNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String val = search.getProperty(propNameText.getText());
				boolean contained = (val != null);
				propRemove.setEnabled(contained);
				propAdd.setEnabled(!contained || ! val.equals(propValText.getText()));
			}
		});
		propValText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String val = search.getProperty(propNameText.getText());
				boolean contained = (val != null);
				propAdd.setEnabled(!contained || ! val.equals(propValText.getText()));
			}			
		});		
		// add all button listeners
		nameSet.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = nameText.getText().trim();
				search.setName(text);
				nameSet.setEnabled(false);
				nameUnset.setEnabled(true);
				update();
			};
		});
		nameUnset.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				search.setName(null);
				nameSet.setEnabled(true);
				nameUnset.setEnabled(false);
				update();
			}
		});
		ifaceAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = ifaceText.getText().trim();
				search.addInterface(text);
				ifaceAdd.setEnabled(false);
				ifaceRemove.setEnabled(true);
				update();
			};
		});
		ifaceRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = ifaceText.getText().trim();
				search.removeInterface(text);
				ifaceAdd.setEnabled(true);
				ifaceRemove.setEnabled(false);
				update();
			}
		});		
		propRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = propNameText.getText().trim();
				search.removeProperty(text);
				propRemove.setEnabled(false);
				propAdd.setEnabled(true);
				update();
			};
		});
		propAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String name = propNameText.getText().trim();
				String val = propValText.getText().trim();
				search.addProperty(name, val);
				propAdd.setEnabled(false);
				propRemove.setEnabled(true);
				update();
			}
		});
		outlineAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ServiceControl.searches.addElement(search);
				ServiceControl switching = null;
				AbstractElementControl[] cs = getManager().getElements();
				for (int i = 0; i < cs.length; i++) {
					if (cs[i] != null && cs[i] instanceof ServiceControl) {
						ServiceControl sc = (ServiceControl)cs[i];
						sc.refresh();
						switching = sc;
					}
				}
				getManager().removeElement(SearchBuilderControl.this);
				if (switching != null) {
					getManager().focusElement(switching);	
				}
			}
		});
		// update the view
		update();
	}

	/**
	 * Updates the state of the control by refreshing the text of 
	 * the search in the outline viewer.
	 */
	private void update() {
		outlineViewer.setText(LabelProvider.toString(search));
	}
	
	
	/**
	 * Returns the menu actions provided by this control. Currently just close.
	 * 
	 * @return The actions available for the current state.
	 */
	public Action[] getMenuActions() {
		return new Action[] {
				new RemoveAction(this, getManager())
		};
	}
	
}
