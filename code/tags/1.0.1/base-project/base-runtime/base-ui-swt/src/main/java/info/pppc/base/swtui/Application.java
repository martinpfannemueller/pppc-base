package info.pppc.base.swtui;

import info.pppc.base.swtui.action.AutoRefreshAction;
import info.pppc.base.swtui.action.ConsoleAction;
import info.pppc.base.swtui.action.ExitAction;
import info.pppc.base.swtui.console.ConsoleBuffer;
import info.pppc.base.swtui.console.ConsoleControl;
import info.pppc.base.swtui.element.AbstractElementControl;
import info.pppc.base.swtui.element.IElementManager;
import info.pppc.base.swtui.element.IRefreshable;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This is the main class of the base swt/jface ui. Typically, this
 * class is not accessed directly. Instead, users should call the
 * get instance method at the base ui class to create an instance of
 * the application window that can be shared between users.
 * 
 * @author Marcus Handte
 */
public class Application extends ApplicationWindow implements IElementManager {

	/**
	 * The resource identifier for the text of the system menu.
	 */
	private static final String UI_SYTEM = "info.pppc.base.swtui.Application.SYSTEM";
	
	/**
	 * The resource identifier for the text of the view menu.
	 */
	private static final String UI_VIEW = "info.pppc.base.swtui.Application.VIEW";

	/**
	 * The resource identifier for the text of the view menu.
	 */
	private static final String UI_TITLE = "info.pppc.base.swtui.Application.TITLE";


	/**
	 * A flag that indicates whether the console should be displayed
	 * on startup. If the flag is set, it is displayed. This flag is
	 * mutex with hide console.
	 */
	public static final int SHOW_CONSOLE = 1;
	
	/**
	 * A flag that indicates whether the console should be displayed
	 * on startup. If the flag is set, it is not displayed. This flag
	 * is mutex with show console.
	 */
	public static final int HIDE_CONSOLE = 2;
	
	/**
	 * A flag that indicates whether the base default logger should be
	 * replaced by the console. If this flag is set, the logger is 
	 * replaced and the console will output the logged data. This flag
	 * is mutex with disable console.
	 */
	public static final int ENABLE_CONSOLE = 4;
	
	/**
	 * A flag that indicates whether the base default logger should be
	 * replaced by the console. If this flag is set, the logger is not
	 * changed and the console will not contain any content. This flag
	 * is mutex with enable console.
	 */
	public static final int DISABLE_CONSOLE = 8;
	
	/**
	 * The system action group name.
	 */
	public static final String MENU_GROUP_SYSTEM = "SYSTEM";
	
	/**
	 * The non-system action group name.
	 */
	public static final String MENU_GROUP_ACTION = "ACTION";
	
	/**
	 * The default modifiers of all applications windows.
	 */
	protected static int modifiers = SHOW_CONSOLE | ENABLE_CONSOLE;
	
	/**
	 * The application window that is initialized by a call
	 * to the get instance method.
	 */
	private static Application instance = null;
	
	/**
	 * The base composite of the view of this application
	 * window. It holds a sash form that is used to display
	 * the tabbed view and the console. The contents can
	 * be switched in and out dynamically.
	 */
	private SashForm parentView;

	/**
	 * The tab folder that contains system and application
	 * specific views as tabs.
	 */
	private CTabFolder contentView;
	
	/**
	 * The composite that holds the console. The console is a
	 * snap in replacement that replaces the default base logger.
	 */
	private Composite consoleView;

	/**
	 * The console control that is used to create a console view.
	 * It reads data from a stream and displays the data in a 
	 * text widget.
	 */
	private ConsoleControl consoleControl;

	/**
	 * The console buffer that is used to redirect logging if it
	 * is enabled.
	 */
	private ConsoleBuffer consoleBuffer = new ConsoleBuffer(10240);

	/**
	 * A flag that indicates whether the console is currently
	 * shown as an element of the view. 
	 */
	private boolean console;

	/**
	 * A flag that indicates whether all refreshable views will be
	 * automatically refreshed.
	 */
	private boolean refresh;

	/**
	 * A vector that contains the element controls. Element controls are 
	 * shown within the tabbed view. Each tab item used to display the
	 * element is held in the pages vector.
	 */
	private Vector elements = new Vector();

	/**
	 * The pages that are used to display the elements. The indexes of 
	 * both vectors are kept in sync, thus accessing the i-th entry 
	 * will access the page of the i-th control.
	 */
	private Vector pages = new Vector();

	/**
	 * The menu manager of the menu bar. This menu is created whenever the
	 * window is opened and it is disposed whenever the window is closed.
	 * The content of this menu bar is currently statically determined on
	 * startup. The menu contains the system menu and the view menu.
	 */
	private MenuManager menuBar;

	/**
	 * The menu manager of the system menu. The menu is created whenever the
	 * window is opened and it is disposed whenever the window is closed.
	 * The content of this menu is currently statically determined on startup.
	 */
	private MenuManager systemMenu;

	/**
	 * The menu manager of the view menu. This menu is created whenever the
	 * window is opened and it is disposed whenever the window is closed.
	 * The content is adjusted based on the current selected view.
	 */
	private MenuManager viewMenu;
	
	/**
	 * Creates the application on a new shell with the default modifiers.
	 */
	public Application() {
		this(null);
	}

	/**
	 * Creates the application on a new shell with the specified modifiers.
	 * 
	 * @param modifiers The modifiers to create the application.
	 */
	public Application(int modifiers) {
		this(null, modifiers);
	}


	/**
	 * Creates a new application window with the specified parent
	 * shell and the default settings form the graphical representation
	 * of the initial window.
	 * 
	 * @param shell The shell used as parent for the window.
	 */
	public Application(Shell shell) {
		this(shell, modifiers);
	}

	/**
	 * Creates a new application window with the specified
	 * parent shell. The flags can be used to change the 
	 * graphical representation and operation of the application
	 * at the moment, supported flags are DISPLAY_CONSOLE to
	 * enable the console right from the start and ENABLE_CONSOLE
	 * to redirect the default logging to the console.
	 * 
	 * @param shell The parent shell of the window.
	 * @param flags The flags used to control the graphical 
	 * 	representation.
	 */
	public Application(Shell shell, int flags) {
		super(shell);
		// check for illegal flag specification
		if (((flags & SHOW_CONSOLE) != 0 && (flags & HIDE_CONSOLE) != 0) ||
			((flags & ENABLE_CONSOLE) != 0 && (flags & DISABLE_CONSOLE) != 0)) {
			throw new RuntimeException("Illegal flag specification.");
		} 
		// show or hide console
		if ((flags & SHOW_CONSOLE) != 0) {
			console = true;
		}
		InputStream stream = null;
		// redirect logging or just leave it
		if ((flags & DISABLE_CONSOLE) != 0) {
			byte[] warning = new String("Logging redirection disabled.").getBytes();
			stream = new ByteArrayInputStream(warning);
		} else {
			Logging.setOutput(new PrintStream(consoleBuffer.getOutputStream()));
			stream = consoleBuffer.getInputStream();
		}
		consoleControl = new ConsoleControl(stream);
		// this hack is required if the shell is set to null
		// we must create a display before we load an image registry
		Display.getDefault();
		// add the menu bar
		addMenuBar();
		addStatusLine();
		setShellStyle((isWindowsCE())?SWT.RESIZE:SWT.SHELL_TRIM);
	}
	
	/**
	 * Determines whether the platform is a pocket pc.
	 * (Tested with windows mobile 6.1 as well.)
	 * 
	 * @return True if running on a pocket pc, false otherwise.
	 */
	protected boolean isWindowsCE() {
		return "Windows CE".equals(System.getProperty("os.name"));
	}

	/**
	 * Called by swt whenever the application window is 
	 * created. This method initializes the contents of the
	 * window.
	 * 
	 * @param parent The parent composite of the window used to
	 * 	display contents.
	 * @return The control that contains the window contents.
	 */
	protected Control createContents(Composite parent) {
		// set the text and image of the shell
		getShell().setText(BaseUI.getText(UI_TITLE));
		getShell().setImage(BaseUI.getImage(BaseUI.IMAGE_LOGO));
		// create basic layout
		parentView = new SashForm(parent, SWT.VERTICAL | SWT.NULL);
		contentView = new CTabFolder(parentView, SWT.TOP);
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				synchronized (Application.this) {
					Logging.setOutput(System.out);
					consoleBuffer.close();
					consoleControl.disposeControl();
					for (int i = 0; i < elements.size(); i++) {
						AbstractElementControl element = 
							(AbstractElementControl)elements.elementAt(i);
						element.disposeControl();
					}
					menuBar = null;
					systemMenu = null;
					viewMenu = null;
				}
			}
		});
		showConsole(console);
		// add element plug-ins
		for (int i = 0; i < elements.size(); i++) {
			AbstractElementControl element = 
				(AbstractElementControl)elements.elementAt(i);
			CTabItem item = new CTabItem(contentView, SWT.NULL);
			item.setText(element.getName());
			item.setImage(element.getImage());
			element.showControl(contentView);
			item.setControl(element.getControl());
			pages.addElement(item);			
		}
		// selection listener that will perform auto-refresh on refreshable elements.
		contentView.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (contentView != null && ! contentView.isDisposed()) {
					if (refresh) {
						int index = contentView.getSelectionIndex();
						if (index != -1 && elements.elementAt(index) instanceof IRefreshable) {
							IRefreshable r = (IRefreshable)elements.elementAt(index);
							r.refresh();
						}
					}
					// update dynamic menu
					updateElement();					
				}
			}
		});
		// update dynamic menu
		updateElement();
		return parentView;
	}
	
	/**
	 * Create the menu manager for the application.
	 * 
	 * @return The newly created menu manager.
	 */
	protected MenuManager createMenuManager() {
		menuBar = new MenuManager(new String());
		// static main menu
		systemMenu = new MenuManager(BaseUI.getText(UI_SYTEM));
		systemMenu.add(new GroupMarker(MENU_GROUP_ACTION));
		systemMenu.add(new GroupMarker(MENU_GROUP_SYSTEM));
		systemMenu.appendToGroup(MENU_GROUP_SYSTEM, new Separator());
		systemMenu.appendToGroup(MENU_GROUP_SYSTEM, new AutoRefreshAction(this, !refresh));
		systemMenu.appendToGroup(MENU_GROUP_SYSTEM, new ConsoleAction(this, !console));
		systemMenu.appendToGroup(MENU_GROUP_SYSTEM, new ExitAction(this));
		menuBar.add(systemMenu);
		// dynamic view menu
		viewMenu = new MenuManager(BaseUI.getText(UI_VIEW));		
		menuBar.add(viewMenu);
		return menuBar;
	}

	
	/**
	 * Returns the display that is currently used or null if the display is
	 * not determined yet.
	 * 
	 * @return The display that is currently used.
	 */
	protected Display getDisplay() {
		Shell shell = getShell();
		if (shell != null) {
			return shell.getDisplay();
		} else {
			return null;
		}
	}
	
	/**
	 * Enables or disables the console view depending on the flag. Set the
	 * flag to true to show the console and to false to disable the control.
	 * 
	 * @param visible True if the console should be shown, false if the 
	 * 	console should not be shown.
	 */
	public void showConsole(boolean visible) {
		console = visible;
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (parentView != null && ! parentView.isDisposed()) {
						if (console && consoleView == null) {
							consoleView = new Composite(parentView, SWT.NULL);
							consoleView.setLayout(new FillLayout());
							consoleControl.showControl(consoleView);
							parentView.layout(true);
						} else if (!console && consoleView != null) {
							consoleControl.disposeControl();
							consoleView.dispose();
							consoleView = null;
							parentView.layout(true);
						}
					}
				}
			});
		}
		
	}
	
	/**
	 * A flag that indicates whether the auto refresh feature is enabled.
	 * 
	 * @param enabled True to enable an automated refresh of refreshable
	 * 	elements whenever they are activated.
	 */
	public void setAutoRefresh(boolean enabled) {
		refresh = enabled;
	}
	
	/**
	 * Opens a blocking or non-blocking version of the gui. Note that if
	 * the gui is opened non-blocking that the opening thread must execute 
	 * the swt event lookup or it must call the block method of the application. 
	 * 
	 * @param blocking True to open a blocking version of the gui, false to
	 * 	open a non-blocking version. 
	 */
	public void open(boolean blocking) {
		setBlockOnOpen(blocking);
		open();
	}
	
	/**
	 * If the application has been opened in non-blocking mode, a call to 
	 * this method will executed the swt event loop until the application
	 * is closed.
	 */
	public void block() {
		Shell shell = getShell();
		Display display = getDisplay();
		if (display != null && shell != null) {
			while(!shell.isDisposed()){
				try {
					if(!display.readAndDispatch())
						display.sleep();				
				} catch (SWTException e) { 
					Logging.error(getClass(), 
						"Caught exception in swt event loop.", e);
				}
			}			
		}
		display.dispose();
	}
	

// implementation of the element manager interface

	/**
	 * Adds the specified element to the view and returns whether the addition
	 * was successful. An element is not added if it does not have a name, i.e.
	 * if its name is null or if it was already registered.
	 * 
	 * @param element The element to add to the view.
	 * @return True if the element has been added, false if the 
	 * 	element could not be added.
	 */
	public synchronized boolean addElement(final AbstractElementControl element) {
		if (element != null && ! elements.contains(element) && element.getName() != null) {
			elements.addElement(element);
			// add the element if it the application is already shown
			Display display = getDisplay();
			if (display != null) {
				display.syncExec(new Runnable() {
					public void run() {
						if (contentView != null && ! contentView.isDisposed()) {
							CTabItem item = new CTabItem(contentView, SWT.NULL);
							item.setText(element.getName());
							item.setImage(element.getImage());
							element.showControl(contentView);
							item.setControl(element.getControl());
							pages.addElement(item);
							contentView.layout(true);
							updateElement();					
						}
					}
				});				
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a working copy of the elements that are currently registed.
	 * 
	 * @return An array of the currently registered element controls.
	 */
	public synchronized AbstractElementControl[] getElements() {
		return (AbstractElementControl[])elements.toArray
			(new AbstractElementControl[elements.size()]);
	}

	/**
	 * This a convenience method that returns the abstract element control 
	 * with the specified name or null if such a control is not registered 
	 * at this application.
	 * 
	 * @param name The name of the element to retrieve.
	 * @return The abstract control of the element with the specified name or
	 * 	null if such an element does not exist.
	 */
	public synchronized AbstractElementControl getElement(String name) {
		for (int i = 0; i < elements.size(); i++) {
			AbstractElementControl element = (AbstractElementControl)elements.elementAt(i);
			if (element.getName().equals(name)) {
				return element;	
			}
		}
		return null;
	}

	/**
	 * Removes the specified element control from the set of registered controls.
	 * 
	 * @param element The element to remove.
	 * @return True if the specified element was found and has been removed,
	 * 	false if the specified element was not registered.
	 */
	public synchronized boolean removeElement(final AbstractElementControl element) {
		final int index = elements.indexOf(element);
		if (index != -1) {
			elements.removeElementAt(index);
			Display display = getDisplay();
			if (display != null) {
				display.syncExec(new Runnable() {
					public void run() {
						if (contentView != null && ! contentView.isDisposed()) {
							element.disposeControl();
							CTabItem item = (CTabItem)pages.elementAt(index);
							item.dispose();
							pages.removeElementAt(index);
							contentView.layout(true);
							updateElement();
						}
					}
				});				
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds a contribution item to the system menu bar. The contribution
	 * will be added before the exit, refresh and console actions.
	 * 
	 * @param contribution The contribution that should be added.
	 */
	public synchronized void addContribution(final IContributionItem contribution) {
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (systemMenu != null) {
						systemMenu.appendToGroup(MENU_GROUP_ACTION, contribution);
						systemMenu.markDirty();
						systemMenu.updateAll(true);
					}
				}
			});
		} else {
			Logging.debug(getClass(), "Display is not set in add contribution.");
		}
	}
	
	/**
	 * Removes a contribution item from the system menu bar. 
	 * 
	 * @param contribution The contribution item that should be removed.
	 */
	public synchronized void removeContribution(final IContributionItem contribution) {
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (systemMenu != null) {
						systemMenu.remove(contribution);
						systemMenu.markDirty();
						systemMenu.updateAll(true);
					}
				}
			});
		}		
	}
	
	/**
	 * Sets the focus to the specified element.
	 * 
	 * @param element The element to focus on.
	 */
	public synchronized void focusElement(final AbstractElementControl element) {
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (contentView != null && ! contentView.isDisposed()) {
						int index = elements.indexOf(element);
						if (index != -1) {
							contentView.setSelection(index);
							updateElement();
						}
					}
				}
			});
		}
	}

	/**
	 * A call to this method will update the view menu with the
	 * actions provided by the currently focused element.
	 */
	public void updateElement() {
		Display display = getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					viewMenu.removeAll();
					if (contentView != null) {
						int index = contentView.getSelectionIndex();
						if (index >= 0 && index < elements.size()) {
							AbstractElementControl element = 
								(AbstractElementControl)elements.elementAt(index);
							Action[] actions = element.getMenuActions();
							for (int i = 0; i < actions.length; i++) {
								if (actions[i] != null) {
									viewMenu.add(actions[i]);
								} else {
									viewMenu.add(new Separator());
								}
							}
						}
						viewMenu.setVisible(viewMenu.getItems().length > 0);
						viewMenu.update(true);
						viewMenu.getParent().update(true);
					}					
				}
			});
		}
	}

	/**
	 * Executes the specified runnable within the gui thread.
	 * 
	 * @param run The runnable to execute.
	 */
	public void run(Runnable run) {
		Display d = getDisplay();
		if (d != null) {
			d.syncExec(run);
		} else {
			run.run();
		}
	}

	/**
	 * A call to this method will execute the runnable in a dialog box with an 
	 * extra thread. Note that this method should only be called from the gui thread.
	 * 
	 * @param runnable The runnable that should be executed.
	 * @param cancel 
	 * @throws InvocationTargetException Thrown if the runnable has a problem.
	 * @throws InterruptedException Thrown if the execution gets interrupted.
	 */
	public void run(final IRunnableWithProgress runnable, final boolean cancel) 
		throws InvocationTargetException, InterruptedException {
		run(true, cancel, runnable);	
	}
	
	/**
	 * Returns the instance of the application. If the application does
	 * not exist already, the method will spawn a new thread that executes
	 * the event handler. This event handler will block until the application
	 * window is closed. Note that this event handler will be the main
	 * gui thread. If the main gui thread already exists, this method will
	 * return null.
	 * 
	 * @return The current instance of the application.
	 */
	public synchronized static Application getInstance() {
		if (instance == null) {
			final boolean[] done = new boolean[] { false };
			IOperation operation = new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					synchronized (done) {
						try {
							instance = new Application();
							instance.open(false);
						} catch (Throwable t) {
							Logging.error(Application.class, "Could not create application.", t);
						} finally {
							done[0] = true;
							done.notify();			
						}
					}
					if (instance != null) {
						instance.block();
						InvocationBroker.getInstance().shutdown();
						instance = null;						
					}
				}
			};
			InvocationBroker broker = InvocationBroker.getInstance();
			NullMonitor monitor = new NullMonitor();
			synchronized (done) {
				broker.performOperation(operation, monitor);
				while (! monitor.isDone() && ! done[0]) {
					try {
						done.wait();	
					} catch (InterruptedException e) {
						Logging.error(Application.class, "Thread got interrupted.", e);	
					}
				}
			}
		}
		return instance;
	}
	
}
