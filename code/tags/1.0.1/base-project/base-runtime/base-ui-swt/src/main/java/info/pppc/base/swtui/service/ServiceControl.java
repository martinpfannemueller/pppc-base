package info.pppc.base.swtui.service;

import info.pppc.base.service.IServiceRegistry;
import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.service.ServiceProperties;
import info.pppc.base.service.ServiceRegistryProxy;
import info.pppc.base.swtui.BaseUI;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.IRefreshable;
import info.pppc.base.swtui.element.action.RefreshAction;
import info.pppc.base.swtui.element.action.RemoveAction;
import info.pppc.base.swtui.service.data.InvokeData;
import info.pppc.base.swtui.service.data.SearchData;
import info.pppc.base.swtui.widget.ImageButton;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.util.Logging;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
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
 * The service control is a control that enables users to 
 * query the local service registry and to create simple
 * invocations and execute them on some remote service.
 * 
 * @author Marcus Handte
 */
public class ServiceControl extends AbstractElementControl implements IRefreshable {

	/**
	 * The name of the service control as contained in the
	 * resource property file.
	 */
	public static final String UI_TEXT = "info.pppc.base.swtui.service.ServiceControl.TEXT";

	/**
	 * The resource key for the searches label.
	 */
	public static final String UI_SEARCHES = "info.pppc.base.swtui.service.ServiceControl.SEARCHES";
	
	/**
	 * The resource key for the invocations label.
	 */
	public static final String UI_INVOCATIONS = "info.pppc.base.swtui.service.ServiceControl.INVOCATIONS";
	
	/**
	 * The resource key for the search result label.
	 */
	public static final String UI_SEARCH_RESULTS = "info.pppc.base.swtui.service.ServiceControl.SEARCH_RESULTS";
	
	/**
	 * The resource key for the invoke result label.
	 */
	public static final String UI_INVOKE_RESULTS = "info.pppc.base.swtui.service.ServiceControl.INVOKE_RESULTS";
	
	/**
	 * The resource key to signal successful invocations.
	 */
	public static final String UI_INVOKE_OK = "info.pppc.base.swtui.service.ServiceControl.INVOKE_OK";
	
	/**
	 * The resource key to signal failed invocations.
	 */
	public static final String UI_INVOKE_FAIL = "info.pppc.base.swtui.service.ServiceControl.INVOKE_FAIL";
	
	/**
	 * The resource key to signal running invocations.
	 */
	public static final String UI_INVOKE_RUN = "info.pppc.base.swtui.service.ServiceControl.INVOKE_RUN";
	
	/**
	 * The maximum length for text items shown in the lists.
	 */
	private static final int TRIM_LENGTH = 40;
	
	/**
	 * A vector that contains the invocations that have been created. 
	 */
	public static Vector invocations = new Vector();
	
	/**
	 * A vector that contains the searches that have been created.
	 */
	public static Vector searches = new Vector();
	
	/**
	 * A vector that contains the latest results of the search.
	 */
	private Vector results = new Vector();
	
	/**
	 * The string that has been created in response to the last invokation.
	 */
	private String result = "";
	
	/**
	 * The list that contains created searches.
	 */
	private Combo searchList;
	
	/**
	 * The list that contains created invocations.
	 */
	private Combo invokeList;
	
	/**
	 * The list that contains the search results.
	 */
	private Combo resultList;
	
	/**
	 * The box that contains the invocation results.
	 */
	private Text resultText;
	
	
	/**
	 * The service registry used to perform queries.
	 */
	private SystemID systemID;
	
	/**
	 * The name shown by the element manager to identify the control.
	 */
	private String name;
	
	/**
	 * Creates a new service control using the specified element manager.
	 * 
	 * @param manager The element manager used to perform actions.
	 * @param systemID The system id of the system whose
	 * 	service registry will be used to perform queries.
	 * @param name The name of the system used to create the name
	 * 	shown by the element manager. 
	 */
	public ServiceControl(IElementManager manager, SystemID systemID, String name) {
		super(manager);
		this.systemID = systemID;
		this.name = BaseUI.getText(UI_TEXT) + " (" + name + ")";
	}
	
	/**
	 * Returns the system id of the system that is used to perform
	 * queries.
	 * 
	 * @return The system id of the system that is used to perform
	 * 	queries.
	 */
	public SystemID getSystemID() {
		return systemID;
	}

	/**
	 * Returns the human readable name that will be shown
	 * by the element manager.
	 * 
	 * @return The name of the service control.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the image that will be shown by the element
	 * manager.
	 * 
	 * @return The image of the service control.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_SERVICE);
	}

	/**
	 * Creates the control on the parent.
	 * 
	 * @param parent The parent of the control.
	 */
	public void showControl(Composite parent) {
		// create the executor and creator panes
		Composite service = new Composite(parent, SWT.NONE);
		service.setLayout(new GridLayout(4, false));
		setControl(service);
		// create search pane content
		Label searchLabel = new Label(service, SWT.NONE);
		searchLabel.setText(BaseUI.getText(UI_SEARCHES));
		searchLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));
		searchList = new Combo(service, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);
		searchList.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
		final ImageButton executeSearch = new ImageButton(service, SWT.FLAT);
		executeSearch.setEnabled(false);
		executeSearch.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_RUN));
		executeSearch.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final  ImageButton  addSearch = new ImageButton(service, SWT.FLAT);
		addSearch.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		addSearch.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final ImageButton removeSearch = new ImageButton(service, SWT.FLAT);
		removeSearch.setEnabled(false);
		removeSearch.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		removeSearch.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		// create invoke pane content
		Label invokeLabel = new Label(service, SWT.NONE);
		invokeLabel.setText(BaseUI.getText(UI_INVOCATIONS));
		invokeLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));
		invokeList = new Combo(service, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
		invokeList.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
		final ImageButton executeInvoke = new ImageButton(service, SWT.FLAT);
		executeInvoke.setEnabled(false);
		executeInvoke.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_RUN));
		executeInvoke.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final ImageButton addInvoke = new ImageButton(service, SWT.FLAT);
		addInvoke.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_ADD));
		addInvoke.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		final ImageButton removeInvoke = new ImageButton(service, SWT.FLAT);
		removeInvoke.setEnabled(false);
		removeInvoke.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		removeInvoke.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		// create search label
		Label searchResultLabel = new Label(service, SWT.NONE);
		searchResultLabel.setText(BaseUI.getText(UI_SEARCH_RESULTS));
		searchResultLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));
		resultList = new Combo(service, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
		resultList.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1));
		final ImageButton clearResult = new ImageButton(service, SWT.FLAT);
		clearResult.setEnabled(false);
		clearResult.setImage(BaseUI.getImage(BaseUI.IMAGE_BUTTON_REMOVE));
		clearResult.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		// create invoke label
		Label invokeResultLabel = new Label(service, SWT.NONE);
		invokeResultLabel.setText(BaseUI.getText(UI_INVOKE_RESULTS));
		invokeResultLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));
		// create invoke result
		resultText = new Text(service, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		resultText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true, 4, 1));
		// add all selection listeners
		searchList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = searchList.getSelectionIndex() != -1;
				removeSearch.setEnabled(enabled);
				executeSearch.setEnabled(enabled);
			}
		});
		invokeList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeInvoke.setEnabled(invokeList.getSelectionIndex() != -1);
				executeInvoke.setEnabled(invokeList.getSelectionIndex() != -1
						&& resultList.getSelectionIndex() != -1);
			};
		});
		resultList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				clearResult.setEnabled(resultList.getItemCount() > 0);
				executeInvoke.setEnabled(invokeList.getSelectionIndex() != -1
						&& resultList.getSelectionIndex() != -1);
			}
		});
		// add all button listeners
		removeSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = searchList.getSelectionIndex();
				if (idx != -1) {
					searches.removeElementAt(idx);
					updateSearches();
				}
			}
		});
		removeInvoke.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int idx = invokeList.getSelectionIndex();
				if (idx != -1) {
					invocations.removeElementAt(idx);
					updateInvokes();
				}
			}
		});
		clearResult.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				results.removeAllElements();
				updateResults();
			}
		});
		addInvoke.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AbstractElementControl[] controls = getManager().getElements();
				for (int i = 0; i < controls.length; i++) {
					if (controls[i] != null && controls[i] instanceof InvokeBuilderControl) {
						getManager().focusElement(controls[i]);
						return;
					}	
				}
				InvokeBuilderControl c = new InvokeBuilderControl(getManager());
				getManager().addElement(c);
				getManager().focusElement(c);
			}
		});
		addSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				AbstractElementControl[] controls = getManager().getElements();
				for (int i = 0; i < controls.length; i++) {
					if (controls[i] != null && controls[i] instanceof SearchBuilderControl) {
						getManager().focusElement(controls[i]);
						return;
					}	
				}
				SearchBuilderControl c = new SearchBuilderControl(getManager());
				getManager().addElement(c);
				getManager().focusElement(c);
			}
		});
		executeSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final SearchData data = (SearchData)searches.elementAt(searchList.getSelectionIndex());
				try {
					getManager().run(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask("Searching ...", 2);
							try {
								ServiceRegistryProxy proxy = new ServiceRegistryProxy();
								proxy.setTargetID(new ReferenceID(systemID, IServiceRegistry.REGISTRY_ID));
								proxy.setSourceID(new ReferenceID(SystemID.SYSTEM));
								ServiceProperties p = new ServiceProperties();
								String[] names = data.getProperites();
								for (int i = 0; i < names.length; i++) {
									p.setProperty(names[i], data.getProperty(names[i]));
								}
								monitor.worked(1);
								final Vector descriptions = proxy.lookup
									(data.getName(), data.getInterfaces(), p , data.getLookup());
								monitor.worked(1);
								getManager().run(new Runnable() {
									public void run() {
										setResults(descriptions);
										setResult(BaseUI.getText(UI_INVOKE_OK) + " (" + descriptions.size() + ")");
									}
								});
								monitor.done();
							} catch (Throwable t) {
								monitor.done();
								throw new InvocationTargetException(t);
							}
						}
					}, false);					
				} catch (InvocationTargetException ex) {
					Throwable t = ex.getCause();
					setResult(BaseUI.getText(UI_INVOKE_FAIL) + " " + t.getMessage() + " (" + t.getClass().getName() + ")");
				} catch (InterruptedException ex) {
					Logging.error(getClass(), "Search got interrupted.", ex);
				}
			}
		});
		executeInvoke.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final ServiceDescriptor desc = (ServiceDescriptor)results.elementAt(resultList.getSelectionIndex());
				final InvokeData data = (InvokeData)invocations.elementAt(invokeList.getSelectionIndex());
				try {
					getManager().run(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(UI_INVOKE_RUN + "...", 2);
							try {
								final Invocation invoke = new Invocation();
								invoke.setArguments(data.getParameters());
								invoke.setSignature(data.getSignature());
								invoke.setTarget(desc.getIdentifier());
								invoke.setSource(new ReferenceID(SystemID.SYSTEM));
								invoke.setRequirements(NFCollection.getDefault(NFCollection.TYPE_SYNCHRONOUS, true));
								monitor.worked(1);
								InvocationBroker b = InvocationBroker.getInstance();
								b.invoke(invoke);
								monitor.worked(1);
								if (invoke.getException() != null) {
									throw invoke.getException();
								} else {
									getManager().run(new Runnable() {
										public void run() {
											setResult(BaseUI.getText(UI_INVOKE_OK) + " " + invoke.getResult());
										}
									});
								}
								monitor.done();
							} catch (Throwable t) {
								monitor.done();
								throw new InvocationTargetException(t);
							}	
						}
					}, false);					
				} catch (InvocationTargetException ex) {
					Throwable t = ex.getCause();
					setResult(BaseUI.getText(UI_INVOKE_FAIL) + " " + t.getMessage() + " (" + t.getClass().getName() + ")");
				} catch (InterruptedException ex) {
					Logging.error(getClass(), "Search got interrupted.", ex);
				}
			}
		});
		
		// update all elements
		refresh();
	}
	
	/**
	 * Called to update the predefined invocations. This
	 * will adjust the contents of the combo box and it
	 * will deselect all items.
	 */
	private void updateInvokes() {
		String[] invokes = new String[invocations.size()];
		for (int i = 0; i < invokes.length; i++) {
			invokes[i] = LabelProvider.toString((InvokeData)invocations.elementAt(i));
			if (invokes[i].length() > TRIM_LENGTH) {
				invokes[i] = invokes[i].substring(0, TRIM_LENGTH - 4) + "...";
			}
		} 
		invokeList.setItems(invokes);
		invokeList.deselectAll();
		invokeList.notifyListeners(SWT.Selection, null);
	}

	/**
	 * Called to update the predefined searches. This
	 * will adjust the contents of the combo box and it
	 * will deselect all items.
	 */
	private void updateSearches() {
		String[] queries = new String[searches.size()];
		for (int i = 0; i < queries.length; i++) {
			queries[i] = LabelProvider.toString((SearchData)searches.elementAt(i));
			if (queries[i].length() > TRIM_LENGTH) {
				queries[i] = queries[i].substring(0, TRIM_LENGTH - 4) + "...";
			}
		}
		searchList.setItems(queries);
		searchList.deselectAll();
		searchList.notifyListeners(SWT.Selection, null);
	}
	
	/**
	 * Called to update the query result. This will
	 * adjust the contents of the combo box and it
	 * will deselect all items.
	 */
	private void updateResults() {
		String[] result = new String[results.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = LabelProvider.toString((ServiceDescriptor)results.elementAt(i));
			if (result[i].length() > TRIM_LENGTH) {
				result[i] = result[i].substring(0, TRIM_LENGTH - 4) + "...";
			}
		}
		resultList.setItems(result);
		resultList.deselectAll();
		resultList.notifyListeners(SWT.Selection, null);
	}
	
	/**
	 * Called to update the invoke result. This will
	 * adjust the label of the result field.
	 */
	private void updateResult() {
		resultText.setText(result);
	}
	
	/**
	 * Sets the search results to the specified result set.
	 * 
	 * @param descriptors The vector containing the service
	 * 	descriptors.
	 */
	public void setResults(Vector descriptors) {
		if (descriptors == null) descriptors = new Vector();
		results = descriptors;
		updateResults();
	}
	
	/**
	 * Sets the result text to the specified text.
	 *  
	 * @param text The text to set.
	 */
	public void setResult(String text) {
		if (text == null) text = "";
		result = text;
		updateResult();
	}
	
	/**
	 * Updates the queries and invocation list.
	 */
	public void refresh() {
		updateSearches();
		updateInvokes();
		updateResults();
		updateResult();
	}
	
	/**
	 * Called whenever the control should be disposed. Releases
	 * the sub controls and then it will release the control.
	 */
	public void disposeControl() {
		super.disposeControl();
	}

	/**
	 * Returns the menu actions for this control. At the
	 * present time this will only return the remove and
	 * the refresh action.
	 * 
	 * @return The actions of the control (remove and refresh).
	 */
	public Action[] getMenuActions() {
		return new Action[] {
			new RefreshAction(this),
			new RemoveAction(this, getManager())
		};
	}
	
}
