package info.pppc.base.lcdui;

import info.pppc.base.lcdui.console.ConsoleItem;
import info.pppc.base.lcdui.console.ConsoleOutputStream;
import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.form.Form;
import info.pppc.base.lcdui.form.FormDisplay;
import info.pppc.base.lcdui.form.FormImageItem;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.form.FormSpacer;
import info.pppc.base.lcdui.form.FormStringItem;
import info.pppc.base.lcdui.menu.MenuElement;
import info.pppc.base.lcdui.status.StatusItem;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.util.Logging;

import java.io.PrintStream;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

/**
 * The application is the main class of the base lcdui.
 * 
 * @author Marcus Handte
 */
public class Application implements IElementManager, CommandListener {

	/**
	 * The title of the application as shown in the ui.
	 */
	private static final String UI_TITLE = "info.pppc.base.lcdui.Application.TITLE";

	/**
	 * The title of the console command as shown in the menu.
	 */
	private static final String UI_CONSOLE = "info.pppc.base.lcdui.Application.CONSOLE";

	/**
	 * The title of the exit command as shown in the menu.
	 */
	private static final String UI_EXIT = "info.pppc.base.lcdui.Application.EXIT";

	/**
	 * The title of the menu command as shown in the menu.
	 */
	private static final String UI_MENUE = "info.pppc.base.lcdui.Application.MENUE";

	/**
	 * The instance of the application.
	 */
	private static Application instance;
	
	/**
	 * The console item that is used to print base logging output.
	 */
	private ConsoleItem consoleItem;
	
	/**
	 * The command that is used to show and hide the console.
	 */
	private Command consoleCommand;
	
	/**
	 * The command that is used to shutdown the system.
	 */
	private Command exitCommand;
	
	/**
	 * The command that is used to bring up the main menu.
	 */
	private Command menuCommand;
	/**
	 * A flag that indicates whether the console is currently shown.
	 */
	private boolean showConsole;
	
	/**
	 * The vector of elements in the order in which they have been added.
	 */
	private Vector elements = new Vector();

	/**
	 * The commands that are currently registered the contents are in sync
	 * with the actions vector.
	 */
	private Vector activeCommands = new Vector();
	
	/**
	 * The actions that are currently registered the contents are in sync with
	 * the commands vector.
	 */
	private Vector activeActions = new Vector();
	
	/**
	 * The abstract element that is currently active.
	 */
	private AbstractElement activeElement = null;
	
	/**
	 * The display item that is used internally to schedule gui events.
	 */
	private Display display;
	
	/**
	 * The menu element that is used to let the user select one of the
	 * elements.
	 */
	private MenuElement menuElement;
	
	/**
	 * The midlet that uses the user interface and is used to stop the system.
	 */
	private MIDlet midlet;
	
	/**
	 * The invocation broker that is used to shutdown the system.
	 */
	private InvocationBroker broker;

	/**
	 * The active form that is displayed.
	 */
	private Form activeForm;
	
	/**
	 * A flag that indicates whether the progress bar is shown at the moment.
	 */
	private boolean showStatus = false;
		
	/**
	 * The form display used for threading.
	 */
	private FormDisplay formDisplay;
	
	/**
	 * Creates an application using the specified midlet and invocation
	 * broker.
	 * 
	 * @param midlet The midlet that is used to retrieve midlet specific data
	 * 	and to request a stop.
	 */
	public Application(MIDlet midlet) {
		if (midlet == null) throw new NullPointerException("Midlet must not be null.");
		this.midlet = midlet;
		this.display = Display.getDisplay(this.midlet);
		this.formDisplay = FormDisplay.getCurrent(true);
		consoleItem = new ConsoleItem(this);
		Logging.setOutput(new PrintStream(new ConsoleOutputStream(formDisplay, consoleItem)));
		this.broker = InvocationBroker.getInstance();
		menuElement = new MenuElement(this);
		activeElement = menuElement;
		menuCommand = new Command(BaseUI.getText(UI_MENUE), Command.SCREEN, 2);
		consoleCommand = new Command(BaseUI.getText(UI_CONSOLE), Command.SCREEN, 2);
		exitCommand = new Command(BaseUI.getText(UI_EXIT), Command.EXIT, 2);
		activeForm = new Form(formDisplay, BaseUI.getText(UI_TITLE));
		activeForm.setCommandListener(this);
		updateElement(menuElement);	
	}
	
	/**
	 * Called whenever the command is executed.
	 * 
	 * @param command The command that has been executed.
	 * @param displayable The displayable that was selected.
	 */
	public void commandAction(Command command, Displayable displayable) {
		if (command == exitCommand) {
			if (activeElement != menuElement) {
				activeElement = menuElement;
				updateElement(menuElement);
			}
			broker.shutdown();
			formDisplay.dispose();
			midlet.notifyDestroyed();
		} else if (command == consoleCommand) {
			showConsole(! showConsole);				
		} else if (command == menuCommand) {
			activeElement = menuElement;
			updateElement(menuElement);	
		} else {
			for (int i = activeCommands.size() - 1; i >= 0; i--) {
				if (activeCommands.elementAt(i) == command) {
					final ElementAction action = (ElementAction)activeActions.elementAt(i);
					final AbstractElement element = activeElement;
					action.run(Application.this, element);
					break;
				}
			}
		}
	}
	
	/**
	 * Displays the application's main form.
	 */
	public void show() {
		display.setCurrent(activeForm);
	}
	
	/**
	 * Enables or disables the console, depending on the state.
	 * 
	 * @param enabled A flag that indicates whether the console is shown.
	 */
	public void showConsole(boolean enabled) {
		checkAccess();
		if (enabled != showConsole) {
			showConsole = enabled;
			updateElement(activeElement);
		}
	}
	
	/**
	 * Adds the specified action to the main menu actions.
	 * 
	 * @param action The action that should be added.
	 */
	public void addAction(ElementAction action) {
		checkAccess();
		menuElement.addAction(action);
		if (activeElement == menuElement) {
			Command command = new Command(action.getLabel(), Command.SCREEN, 1);
			activeCommands.addElement(command);
			activeActions.addElement(action);
			activeForm.addCommand(command);
		}
	}
	
	/**
	 * Removes the specified action from the main menu actions and
	 * returns true if removed, false otherwise.
	 * 
	 * @param action The action that should be removed.
	 * @return True if the action has been removed, false otherwise.
	 */
	public boolean removeAction(ElementAction action) {
		checkAccess();
		boolean val = menuElement.removeAction(action);
		if (val && activeElement == menuElement) {
			for (int i = activeActions.size() - 1; i >= 0; i--) {
				if (activeActions.elementAt(i) == action) {
					activeCommands.removeElementAt(i);
					activeActions.removeElementAt(i);
					break;
				}
			}
		}
		return val;
	}
	
	/**
	 * Dispatches display events and blocks until they are done.
	 */
	public void block() {
		formDisplay.dispatch(false);
	}
	
	/**
	 * Adds the specified element to the set of elements and returns
	 * the addition status.
	 * 
	 * @param element The element to add.
	 * @return True if successful, false otherwise. 
	 */
	public boolean addElement(AbstractElement element) {
		checkAccess();
		if (element == null || element.getName() == null) return false;
		for (int i = elements.size() - 1; i >= 0; i--) {
			if (elements.elementAt(i) == element) {
				return false;
			}
		}
		elements.addElement(element); 
		menuElement.addElement(element);
		if (activeElement == menuElement) updateElement(activeElement);
		return true;
	}
	
	/**
	 * Puts the specified element into focus if the element is
	 * contained in the set of available elements or does nothing
	 * if the element does not exists or is null.
	 * 
	 * @param element The element that should be focused.
	 */
	public void focusElement(AbstractElement element) {
		checkAccess();
		if (element == null) return;
		for (int i = elements.size() - 1; i >= 0; i--) {
			if (elements.elementAt(i) == element) {
				if (element != activeElement) {
					activeElement = element;
					updateElement(element);
				}
			}
		}
	}
	
	/**
	 * Returns the first element with the specified name or
	 * null if no such element exists.
	 * 
	 * @param name The name of the element to lookup.
	 * @return The first element with the specified name or
	 * 	null if no such element exists.
	 */
	public AbstractElement getElement(String name) {
		checkAccess();
		if (name != null) {
			for (int i = 0, s = elements.size(); i < s; i++) {
				AbstractElement element = (AbstractElement)elements.elementAt(i);
				if (name.equals(element.getName())) {
					return element;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the set of elements that is currently registered at the
	 * manager in the order in which they have been registered.
	 * 
	 * @return The elements in the order in which they have been
	 * 	registered.
	 */
	public AbstractElement[] getElements() {
		checkAccess();
		AbstractElement[] result = new AbstractElement[elements.size()];
		for (int i = elements.size() - 1; i >= 0; i--) {
			result[i] = (AbstractElement)elements.elementAt(i);
		}
		return result;
 	}

	/**
	 * Removes the specified element from the current view and
	 * returns true if the item has been removed or false if the
	 * item was not registered.
	 * 
	 * @param element The element to remove.
	 * @return True if successful, false if not found.
	 */
	public boolean removeElement(AbstractElement element) {
		checkAccess();
		if (element == null) return false;
		for (int i = elements.size() - 1; i >= 0; i--) {
			if (elements.elementAt(i) == element) {
				elements.removeElementAt(i);
				menuElement.removeElement(element);
				if (element == activeElement) {
					activeElement = menuElement;
					updateElement(menuElement);						
				}
				element.dispose();
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the menues and the display using the passed element
	 * if the passed element is not the active element, the method
	 * simply returns.
	 * 
	 * @param element The element that should be updated.
	 */
	public void updateElement(AbstractElement element) {
		checkAccess();
		synchronized (FormDisplay.UI_LOCK) { // lock the form to stop repaints
			if (element != activeElement || showStatus) return;
			while (! activeCommands.isEmpty()) {
				Command c = (Command)activeCommands.elementAt(0);
				activeCommands.removeElementAt(0);
				activeForm.removeCommand(c);
			}
			activeForm.removeCommand(exitCommand);
			activeForm.removeCommand(menuCommand);
			activeForm.removeCommand(consoleCommand);
			activeForm.deleteAll();
			activeCommands.removeAllElements();
			activeActions.removeAllElements();
			// add the console if it should be shown
			if (showConsole) {
				activeForm.append(consoleItem);
				FormSpacer lineSpacer = new FormSpacer(0, 5);
				lineSpacer.setLayout(FormItem.LAYOUT_LINE_BEFORE | FormItem.LAYOUT_LINE_AFTER);
				activeForm.append(lineSpacer);
			}
			// add the title and title image
			String name = activeElement.getName();
			Image image = activeElement.getImage();
			FormItem[] items = activeElement.getItems();
			if (image == null) image = BaseUI.getImage(BaseUI.IMAGE_LOGO);
			FormImageItem imageItem = new FormImageItem("", image);
			imageItem.setLayout(FormItem.LAYOUT_LINE_BEFORE | FormItem.LAYOUT_CENTER | FormItem.LAYOUT_VCENTER);
			activeForm.append(imageItem);
			FormSpacer imageSpacer = new FormSpacer(5, 0);
			imageSpacer.setLayout(FormItem.LAYOUT_CENTER);
			activeForm.append(imageSpacer);
			FormStringItem labelItem = new FormStringItem("", name);
			labelItem.setLayout(FormItem.LAYOUT_LINE_AFTER | FormItem.LAYOUT_CENTER | FormItem.LAYOUT_VCENTER);
			activeForm.append(labelItem);
			FormSpacer lineSpacer = new FormSpacer(0, 5);
			lineSpacer.setLayout(FormItem.LAYOUT_LINE_BEFORE | FormItem.LAYOUT_LINE_AFTER);
			activeForm.append(lineSpacer);
			// add all items of the active element
			for (int i = 0, s = items.length; i < s; i++) {
				activeForm.append(items[i]);
			}
			// add the default action set
			activeForm.addCommand(exitCommand);
			activeForm.addCommand(consoleCommand);
			if (activeElement != menuElement) {
				activeForm.addCommand(menuCommand);
			}
			// add all global actions of the element
			ElementAction[] elementActions = activeElement.getActions();
			for (int i = 0, s = elementActions.length; i < s; i++) {
				ElementAction action = elementActions[i];
				Command command = new Command(action.getLabel(), Command.SCREEN, 1);
				activeForm.addCommand(command);
				activeCommands.addElement(command);
				activeActions.addElement(action);
			}
			
		}
		// change fullscreen mode to fix bug in j2me wtk 3.0
		activeForm.setFullScreenMode(false);
		activeForm.repaint();
	}
	
	/**
	 * Runs the specified operation modal, shows a status bar and possibly a 
	 * cancel button.
	 * 
	 * @param operation The operation that should be executed.
	 * @param cancel The flag that indicates whether the operation does support
	 * 	a user-defined cancel.
	 * @throws InterruptedException Thrown if the thread could not wait on
	 * 	the operation.
	 */
	public void run(final IOperation operation, final boolean cancel) throws InterruptedException {
		checkAccess();
		final boolean[] running = new boolean[] { true };
		if (showStatus) {
			NullMonitor monitor = new NullMonitor();
			monitor.addMonitorListener(Event.EVENT_EVERYTHING, new IListener() {
				public void handleEvent(Event event) {
					if (event.getType() == IMonitor.EVENT_MONITOR_DONE) {
						run(new Runnable() {
							public void run() {
								running[0] = false;
							};
						});
					}
				}
			});
			broker.performOperation(operation, monitor);
			synchronized (formDisplay) {
				while (running[0]) {
					if (formDisplay.read()) {
						formDisplay.dispatch(true);
					} else {
						formDisplay.wait();
					}
				}	
			}
		} else {
			showStatus = true;
			try {
				
				for (int i = activeCommands.size() - 1; i >= 0; i--) {
					activeForm.removeCommand((Command)activeCommands.elementAt(i));
				}
				activeForm.removeCommand(menuCommand);
				activeForm.removeCommand(consoleCommand);
				activeForm.deleteAll();
				activeCommands.removeAllElements();
				activeActions.removeAllElements();
				activeForm.addCommand(exitCommand);
				activeForm.setCommandListener(Application.this);
				// add the console if it should be shown
				if (showConsole) {
					activeForm.append(consoleItem);
					FormSpacer lineSpacer = new FormSpacer(0, 5);
					lineSpacer.setLayout(FormItem.LAYOUT_LINE_BEFORE | FormItem.LAYOUT_LINE_AFTER);
					activeForm.append(lineSpacer);
				}
				final StatusItem status = new StatusItem(Application.this);
				status.setCancelable(cancel);
				final NullMonitor monitor = new NullMonitor();
				status.addStatusListener(StatusItem.EVENT_STATUS_CANCELED, new IListener() {
					public void handleEvent(Event event) {
						monitor.cancel();
					}
				});
				monitor.addMonitorListener(Event.EVENT_EVERYTHING, new IListener() {
					public void handleEvent(final Event event) {
						run(new Runnable() {
							public void run() {
								switch (event.getType()) {
									case IMonitor.EVENT_MONITOR_NAME:
										status.setTaskname((String)event.getData());
										break;
									case IMonitor.EVENT_MONITOR_START:
										status.setTotal(((Integer)event.getData()).intValue());
										break;
									case IMonitor.EVENT_MONITOR_STEP:
										int step = ((Integer)event.getData()).intValue();
										status.setWorked(status.getWorked() + step);
										break;
									case IMonitor.EVENT_MONITOR_DONE:
										running[0] = false;
										break;
									default:
										// nothing to be done
								}
							}
						});
					}
				});
				activeForm.append(status);
				broker.performOperation(operation, monitor);
				synchronized (formDisplay) {
					while (running[0]) {
						if (formDisplay.read()) {
							formDisplay.dispatch(true);
						} else {
							formDisplay.wait();
						}
					}	
				}
			} finally {
				showStatus = false;
				// restore the current view
				updateElement(activeElement);
			}
		}
	}
	
	
	

	/**
	 * Executes the specified runnable in the display thread after
	 * it has reentered its event dispatch.
	 * 
	 * @param runnable The runnable to execute.
	 */
	public void run(Runnable runnable) {
		formDisplay.runSync(runnable);
	}
	
	/**
	 * Returns the height of the display.
	 * 
	 * @return The height of the display.
	 */
	public int getDisplayHeight() {
		return activeForm.getHeight();
	}
	
	/**
	 * Returns the width of the display.
	 * 
	 * @return The width of the display.
	 */
	public int getDisplayWidth() {
		return activeForm.getWidth();
	}

	/**
	 * Checks whether the current thread is the ui thread and
	 * throws an exception if not.
	 */
	private void checkAccess() {
		if (Thread.currentThread() != formDisplay.getThread()) {
			throw new RuntimeException("Illegal thread access.");
		}
	}
	
	/**
	 * Creates a new application and returns it. If an application
	 * exists already, this method will return the existing 
	 * application.
	 * 
	 * @param midlet The midlet.
	 * @return The newly created application.
	 */
	public static Application getInstance(final MIDlet midlet) {
		if (instance == null) {
			final boolean[] done = new boolean[] { false };
			IOperation operation = new IOperation() {
				public void perform(IMonitor monitor) throws Exception {
					synchronized (done) {
						try {
							instance = new Application(midlet);
							instance.show();
						} catch (Throwable t) {
							Logging.error(Application.class, "Could not create application.", t);
						} finally {
							done[0] = true;
							done.notify();			
						}
					}
					if (instance != null) {
						instance.block();
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
