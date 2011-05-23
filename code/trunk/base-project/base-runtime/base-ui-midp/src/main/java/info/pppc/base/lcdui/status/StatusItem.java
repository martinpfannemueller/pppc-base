package info.pppc.base.lcdui.status;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.form.FormCommandListener;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.form.FormStyle;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * The status item presents a progress bar that can potentially be canceled.
 * 
 * @author Marcus Handte
 */
public class StatusItem extends FormItem implements FormCommandListener {

	/**
	 * The title of the status item.
	 */
	private static final String UI_TITLE = "info.pppc.base.lcdui.status.StatusItem.TITLE";
	
	/**
	 * The name of the command used to select the cancel button.
	 */
	private static final String UI_SELECT = "info.pppc.base.lcdui.status.StatusItem.SELECT";
	
	/**
	 * The cancel button used to cancel a cancelable operation.
	 */
	private static final String UI_CANCEL = "info.pppc.base.lcdui.status.StatusItem.CANCEL";
	
	/**
	 * The height of the progress bar in pixels (including border).
	 */
	private static final int BAR_HEIGHT = 12;
	
	/**
	 * The event constant that signals that the status bar has been canceled.
	 * The event source will be the status. The data object will be null.
	 */
	public static final int EVENT_STATUS_CANCELED = 1;
	
	/**
	 * The manager used for width computations.
	 */
	private IElementManager manager;
	
	/**
	 * A flag that indicates whether the status item is
	 * cancelable.
	 */
	private boolean cancelable = false;
	
	/**
	 * A flag that indicates whether the status item has the focus.
	 */
	private boolean focus = false;
	
	/**
	 * The name of the task that is currently drawn or null if none.
	 */
	private String taskname = null;
	
	/**
	 * The total amount of work to do always larger than 0 and always
	 * larger or equal to worked. 
	 */
	private int total = 1;
	
	/**
	 * The amount of work that has been done always larger or equal to
	 * 0 and always smaller than or equal to total.
	 */
	private int worked = 0;
	
	/**
	 * The command that can be used to cancel the status item.
	 */
	private Command cancelCommand;
	
	/**
	 * The listeners that are registered for the click to the cancel button.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The style of the status item.
	 */
	private FormStyle style;
	
	/**
	 * Creates a new status item that can be used to display a progress bar.
	 * 
	 * @param manager The element manager used to compute the size of the widget.
	 */
	public StatusItem(IElementManager manager) {
		super(BaseUI.getText(UI_TITLE));
		this.manager = manager;
		style = FormStyle.getStyle();
		cancelCommand = new Command(BaseUI.getText(UI_SELECT), Command.ITEM, 0);
		setItemCommandListener(this);
		setLayout(LAYOUT_LINE_AFTER | LAYOUT_LINE_BEFORE);
	}
	
	/**
	 * Adds the specified status listener to the set of registered listeners.
	 * 
	 * @param types The types of the listener to register for. Defined by the
	 * 	event constants in this class.
	 * @param listener The listener that should be registered.
	 */
	public void addStatusListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}
	
	/**
	 * Removes the specified listener from the set of registered listeners.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to remove.
	 * @return True if the listener has been removed, false otherwise.
	 */
	public boolean removeStatusListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Sets the flag that determines whether the status item is cancelable.
	 * 
	 * @param cancelable The flag that indicates whether the status item
	 * 	is cancelable.
	 */
	public void setCancelable(boolean cancelable) {
		if (cancelable != this.cancelable) {
			if (this.cancelable) {
				removeCommand(cancelCommand);
			} else {
				addCommand(cancelCommand);
				setDefaultCommand(cancelCommand);
			}
			this.cancelable = cancelable;
			invalidate();
			repaint();
		}
	}
	
	/**
	 * Returns the flag that indicates whether the status item is cancelable.
	 * 
	 * @return The flag that indicates whether the item is cancelable.
	 */
	public boolean isCancelable() {
		return cancelable;
	}
	
	/**
	 * Sets the task name to the specified string or null if no taskname
	 * should be displayed.
	 * 
	 * @param taskname The task name to display.
	 */
	public void setTaskname(String taskname) {
		this.taskname = taskname;
		invalidate();
		repaint();
	}
	
	/**
	 * Returns the current task name or null if none is displayed.
	 * 
	 * @return The task name or null if none is set.
	 */
	public String getTaskname() {
		return taskname;
	}

	/**
	 * Returns the content width. This will be the screen width.
	 * 
	 * @return The content width.
	 */
	public int getMinimumWidth() {
		return manager.getDisplayWidth() - 2;
	}

	/**
	 * Sets the total amount of work to be done. If the total
	 * is smaller than 1, it will be set to 1. If the total
	 * is smaller than the worked, the worked will be reduced.
	 * 
	 * @param total The total amount of work to be done.
	 */
	public void setTotal(int total) {
		if (total < 1) {
			this.total = 1;
		} else {
			this.total = total;	
		}
		if (this.worked > total) {
			this.worked = total;
		}
		repaint();
	}

	/**
	 * Returns the amount of work that has been done.
	 * 
	 * @return The amount of work that has been done.
	 */
	public int getWorked() {
		return worked;
	}

	/**
	 * Sets the amount of work that has been done. If the
	 * worked is smaller than 0 or larger than total it will
	 * be moved to the according boundaries.
	 * 
	 * @param worked The amount of work that has been done.
	 */
	public void setWorked(int worked) {
		if (worked > total) {
			this.worked = total;
		} if (worked < 0) {
			this.worked = 0;
		} else {
			this.worked = worked;	
		}
		repaint();
	}
	
	/**
	 * Returns the content height. This will be the height of the
	 * bar plus the height of the cancel label.
	 * 
	 * @return The content height.
	 */
	public int getMinimumHeight() {
		int height = BAR_HEIGHT + 2;
		if (cancelable) {
			height += style.FONT_ITEM.getHeight() + 1;
		}
		if (taskname != null) {
			height += style.FONT_ITEM.getHeight() + 1;
		}
		return height;
	}

	/**
	 * Draws the status bar using the specified content.
	 * 
	 * @param graphics The graphics to draw.
	 * @param width The width of the bar.
	 * @param height The height of the bar.
	 */
	public void paint(Graphics graphics, int width, int height) {
		// clear screen
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_BACKGROUND);
		} else {
			graphics.setColor(style.COLOR_BACKGROUND);	
		}
		graphics.fillRect(0, 0, width, height);
		int y = 1;
		// draw the task name if there is one
		if (taskname != null) {
			if (focus) {
				graphics.setColor(style.COLOR_ACTIVE_FOREGROUND);	
			} else {
				graphics.setColor(style.COLOR_FOREGROUND);
			}
			Font f = style.FONT_ITEM;
			graphics.setFont(f);
			String name = taskname;
			if (f.stringWidth(name) > width) {
				while (name.length() > 0 && f.stringWidth(name + "...") > width - 2) {
					name = name.substring(0, name.length() - 1);
				}				
				name += "...";
			}
			graphics.drawString(name, 0, y, Graphics.TOP | Graphics.LEFT);
			y += f.getHeight() + 1;
		}
		// draw the bar according to the fill state
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_BORDER);	
		} else {
			graphics.setColor(style.COLOR_BORDER);
		}
		graphics.drawRect(0, y, width - 2, BAR_HEIGHT - 1);
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_FOREGROUND);
		} else {
			graphics.setColor(style.COLOR_FOREGROUND);
		}
		int pixels = ((width - 5) * worked) / total;
		graphics.fillRect(2, y + 2, pixels, BAR_HEIGHT - 4);
		y += BAR_HEIGHT + 1;
		// draw the cancel button if available
		if (cancelable) {
			graphics.setFont(focus?style.FONT_ACTIVE_ITEM:style.FONT_ITEM);
			graphics.drawString(BaseUI.getText(UI_CANCEL), width - 1, y, Graphics.RIGHT | Graphics.TOP);
		}
	}

	/**
	 * Called whenever the cancel command is executed.
	 * 
	 * @param command The cancel command.
	 * @param item The selected item.
	 */
	public void commandAction(Command command, FormItem item) {
		listeners.fireEvent(EVENT_STATUS_CANCELED);
	}
	
	/**
	 * Called whenever the focus enters the status item.
	 * 
	 * @param type The type of key that caused the focus.
	 * @param w The width that is visible.
	 * @param h The height that is visible.
	 * @param rect The focus rectangle.
	 * @return True to keep the status, false otherwise.
	 */
	public boolean traverse(int type, int w, int h, int[] rect) {
		if (! cancelable || focus) { 
			return false;
		}
		else {
			rect[0] = 0;
			rect[1] = 0;
			rect[2] = 1;
			rect[3] = 1;
			focus = true;
			repaint();
			return true;
		}
	}
	
	/**
	 * Called whenever the focus leaves the status item.
	 */
	public void traverseOut() {
		focus = false;
		repaint();
	}

	/**
	 * Returns the total amount of work to be done.
	 * 
	 * @return The total amount of work to be done.
	 */
	public int getTotal() {
		return total;
	}
	
}
