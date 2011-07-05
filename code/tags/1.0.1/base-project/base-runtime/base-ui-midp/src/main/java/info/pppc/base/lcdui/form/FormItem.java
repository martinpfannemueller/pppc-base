package info.pppc.base.lcdui.form;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Graphics;

/**
 * The base class for all items placed on a form.
 * 
 * @author Marcus Handte
 */
public class FormItem {

	/**
	 * The key code for no specific key.
	 */
	public static final int KEY_NONE = 0;
	
	/**
	 * The key code for key up.
	 */
	public static final int KEY_UP = 1;
	
	/**
	 * The key code for key down.
	 */
	public static final int KEY_DOWN = 2;
	
	/**
	 * The key code for key left.
	 */
	public static final int KEY_LEFT = 3;
	
	/**
	 * The key code for key right.
	 */
	public static final int KEY_RIGHT = 4;
	
	/**
	 * A constant indicating that the width can be expanded.
	 */
	public static final int LAYOUT_EXPAND = 1;
	
	/**
	 * A constant indicating that the height can be expanded.
	 */
	public static final int LAYOUT_VEXPAND = 2;
	
	/**
	 * A constant indicating that the item should be centered horizontally.
	 */
	public static final int LAYOUT_CENTER = 4;
	
	/**
	 * A constant indicating that the item should be placed at the left edge.
	 */
	public static final int LAYOUT_LEFT = 8;
	
	/**
	 * A constant indicating that the item should be placed that the right edge.
	 */
	public static final int LAYOUT_RIGHT = 16;
	
	/**
	 * A constant indicating that the item should be centred vertically.
	 */
	public static final int LAYOUT_VCENTER = 32;
	
	/**
	 * A constant indicating that the item should be placed at the top.
	 */
	public static final int LAYOUT_VTOP = 64;
	
	/**
	 * A constant indicating that the item should be placed at the bottom.
	 */ 
	public static final int LAYOUT_VBOTTOM = 128;
	
	/**
	 * A constant indicating that the line should be broken before the item.
	 */
	public static final int LAYOUT_LINE_BEFORE = 256;
		
	/**
	 * A constant indicating that the line should be broken after the item.
	 */
	public static final int LAYOUT_LINE_AFTER = 512;
	
	/**
	 * The label of the item, can be null or the empty string to singal
	 * no label.
	 */
	private String label;

	/**
	 * The layout hints of the item.
	 */
	private int layout = LAYOUT_LEFT | LAYOUT_VTOP;
	
	/**
	 * The form used by the item.
	 */
	private Form form = null;
	
	/**
	 * The commands contained in the item.
	 */
	private Vector commands = new Vector();
	
	/**
	 * The default command of the item.
	 */
	private Command defaultCommand = null;
	
	/**
	 * The item command listener of the item.
	 */
	private FormCommandListener itemCommandListener = null;

	
	/**
	 * Creates a new item with the specified label.
	 * 
	 * @param label The label of the item.
	 */
	public FormItem(String label) {
		this.label = label;
	}
	
	/**
	 * Adds the specified command to the commands of the item.
	 * 
	 * @param command The command to add.
	 */
	public void addCommand(Command command) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = commands.size() - 1; i >= 0; i--) {
				if (command == commands.elementAt(i)) {
					return;
				}
			}
			commands.addElement(command);
			updateCommands();			
		}
	}
	
	/**
	 * Removes the specified command from the commands of the
	 * item.
	 * 
	 * @param command The command that should be removed.
	 */
	public void removeCommand(Command command) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = commands.size() - 1; i >= 0; i--) {
				if (command == commands.elementAt(i)) {
					commands.removeElementAt(i);
					if (command == defaultCommand) {
						defaultCommand = null;
					}
					updateCommands();
					return;
				}
			}
		}
	}
	
	/**
	 * Returns the commands that are available for the item.
	 * 
	 * @return The commands available for the item.
	 */
	public Command[] getCommands() {
		synchronized (FormDisplay.UI_LOCK) {
			Command[] cs = new Command[commands.size()];
			for (int i = commands.size() - 1; i >= 0; i--) {
				cs[i] = (Command)commands.elementAt(i);
			}
			return cs;
		}
	}
	
	/**
	 * Sets the default command of the item. The command must
	 * be added already.
	 * 
	 * @param command The command to add.
	 */
	public void setDefaultCommand(Command command) {
		synchronized (FormDisplay.UI_LOCK) {
			if (command == defaultCommand) return;
			for (int i = commands.size() - 1; i >= 0; i--) {
				if (command == commands.elementAt(i)) {
					defaultCommand = command;
					updateCommands();
					break;
				}
			}			
		}
	}
	
	/**
	 * Returns the default command of the item.
	 * 
	 * @return The default command of the item.
	 */
	public Command getDefaultCommand() {
		synchronized (FormDisplay.UI_LOCK) {
			return defaultCommand;	
		}
	}
	
	/**
	 * Sets the item command listener of the item.
	 * 
	 * @param listener The new item command listener.
	 */
	public void setItemCommandListener(FormCommandListener listener) {
		synchronized (FormDisplay.UI_LOCK) {
			itemCommandListener = listener;
		}
	}
	
	/**
	 * Returns the item command listener of the item.
	 * 
	 * @return The item command listener of the item.
	 */
	public FormCommandListener getItemCommandListener() {
		synchronized (FormDisplay.UI_LOCK) {
			return itemCommandListener;
		}
	}
	
	
	/**
	 * Returns the label of the item.
	 * 
	 * @return The label of the item.
	 */
	public String getLabel() {
		synchronized (FormDisplay.UI_LOCK) {
			return label;
		}
	}
	
	/**
	 * Sets the label of the form item.
	 * 
	 * @param label The new label of the item.
	 */
	public void setLabel(String label) {
		synchronized (FormDisplay.UI_LOCK) {
			this.label = label;
			invalidate();
		}
	}
	
	/**
	 * Returns the layout constants of the item.
	 * 
	 * @return The layout constants of the item.
	 */
	public int getLayout() {
		synchronized (FormDisplay.UI_LOCK) {
			return layout;
		}
	}
	
	/**
	 * Sets the layout constants of the item.
	 * 
	 * @param layout The layout constants of the item.
	 */
	public void setLayout(int layout) {
		synchronized (FormDisplay.UI_LOCK) {
			this.layout = layout;
			invalidate();
		}
	}
	
	/**
	 * Called whenever the item needs to be painted.
	 * 
	 * @param g The graphic object to paint.
	 * @param width The width of the item.
	 * @param height The height of the item.
	 */
	public void paint(Graphics g, int width, int height) { }
	
	/**
	 * Called whenever the item is traversed.
	 * 
	 * @param code The key code of the traversal, up, down, left, right, none.
	 * @param width The maximum visible width of the item.
	 * @param height The maximum visible height of the item.
	 * @param visible The visible in out parameter that tells the parent
	 * 	form where to focus.
	 * @return True to keep the focus, false to release the focus.
	 */
	public boolean traverse(int code, int width, int height, int[] visible) {
		return false;
	}

	/**
	 * Called whenever the focus leaves the item.
	 */
	public void traverseOut() {	}

	/**
	 * Returns the minimum width of the item.
	 * 
	 * @return The minimum width of the item.
	 */
	public int getMinimumWidth() {
		return 0;
	}

	/**
	 * Returns the minimum height of the item.
	 * 
	 * @return The minimum height of the item.
	 */
	public int getMinimumHeight() {
		return 0;
	}
	
	/**
	 * Tells the form of the item that the item wants to
	 * be repained.
	 */
	public final void repaint() {
		synchronized (FormDisplay.UI_LOCK) {
			if (form != null) {
				form.repaint();
			}
		}
	}
	
	/**
	 * Tells the form of the item that the item is invalid.
	 */
	public final void invalidate() {
		synchronized (FormDisplay.UI_LOCK) {
			if (form != null) {
				form.invalidate();
			}
		}
	}
	
	/**
	 * Tells the current form that the commands have been 
	 * changed.
	 */
	private final void updateCommands() {
		synchronized (FormDisplay.UI_LOCK) {
			if (form != null) {
				form.updateCommands(this);
			}
		}
	}
	
	/**
	 * Sets the form of the item.
	 * 
	 * @param form The form of the item.
	 */
	protected void setForm(Form form) {
		synchronized (FormDisplay.UI_LOCK) {
			if (form != this.form && this.form != null) {
				this.form.delete(this);
			}
			this.form = form;
		}	
	}
	
	/**
	 * Returns the form of the item.
	 * 
	 * @return The form of the item.
	 */
	protected final Form getForm() {
		synchronized (FormDisplay.UI_LOCK) {
			return form;	
		}
	}
	
	/**
	 * Determines whether the item has the specified layout flag set.
	 * 
	 * @param flag The layout flag to check.
	 * @return True if the flag is set, false otherwise.
	 */
	protected boolean hasFlag(int flag) {
		return ((layout & flag) == flag);
	}
}
