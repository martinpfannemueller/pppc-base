package info.pppc.base.lcdui.tree;

import java.util.Vector;

import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.form.FormCommandListener;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.form.FormStyle;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;

/**
 * The tree item is a item that can display a tree of tree nodes.
 * 
 * @author Marcus Handte
 */
public class TreeItem extends FormItem implements FormCommandListener {

	/**
	 * The event that is fired whenever the selected tree node is selectable
	 * and the user pressed the select button. The source will be this tree 
	 * item. The data object will be the selected node. The data object will
	 * never be null.
	 */
	public static final int EVENT_SELECTION_FIRED = 1;
	
	/**
	 * The event that is fired whenever the selected tree node changes. The
	 * source will be this tree item. The data object will be the selected
	 * node. The data object might be null, if no item is selected.
	 */
	public static final int EVENT_SELECTION_CHANGED = 2;
	
	/**
	 * The default number of lines that will be visible in the tree.
	 */
	public static final int LINES_DEFAULT = 7;

	/**
	 * The line height that is used by the widget.
	 */
	public static final int LINE_HEIGHT = 16;
		
	/**
	 * The root node of the content.
	 */
	private TreeNode content;
	
	/**
	 * A flag that indicates whether the tree item has the focus.
	 */
	private boolean focus;
	
	/**
	 * The tree node that is currently selected or null if none.
	 */
	private TreeNode selected;
	
	/**
	 * A flag that indicates whether the root node is visible.
	 */
	private boolean visible;
	
	/**
	 * The x offset that is used to draw the tree.
	 */
	private int offsetX;
	
	/**
	 * The y offset that is used to draw the tree.
	 */
	private int offsetY;
	
	/**
	 * The lines that are visible in the control.
	 */
	private int lines = LINES_DEFAULT;
	
	/**
	 * The element manager of the tree item.
	 */
	private IElementManager manager;
	
	/**
	 * The select command that is used to detect button presses on
	 * a selected item.
	 */
	private Command selectCommand;
	
	/**
	 * The listeners that are registered for tree events.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The style of the tree item.
	 */
	private FormStyle style;
	
	/**
	 * Creates a new tree item using the specified label.
	 * 
	 * @param manager The element manager that uses the tree item.
	 * @param label The label of the tree item.
	 * @param command The command name of the selection command.
	 */
	public TreeItem(IElementManager manager, String label, String command) {
		super(label);
		this.manager = manager;
		this.style = FormStyle.getStyle();
		selectCommand = new Command(command, Command.ITEM, 1);
		setItemCommandListener(this);
		setLayout(Item.LAYOUT_SHRINK | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
	}

	/**
	 * Returns the element manager of the tree item.
	 * 
	 * @return The element manager of the tree item.
	 */
	public IElementManager getManager() {
		return manager;
	}
	
	/**
	 * Adds the specified selection listener to the set of registered selection
	 * listeners for the specified set of events.
	 * 
	 * @param types The types of events to register for, see event constants for
	 * 	a definition.
	 * @param listener The listener that should be registered.
	 */
	public void addSelectionListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}
	
	/**
	 * Removes a previously added selection listener from the set of registered
	 * listeners for the set of events.
	 * 
	 * @param types The types of events to unregister for.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been remove, false if not.
	 */
	public boolean removeSelectionListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}
	
	/**
	 * Sets the content of the tree. The passed tree node will
	 * be interpreted as root of the tree, no matter whether 
	 * this node has a parent.
	 * 
	 * @param content The content of the tree node.
	 */
	public void setContent(TreeNode content) {
		this.content = content;
		setSelected(null);
		offsetX = 0;
		offsetY = 0;
		repaint();
	}
	
	/**
	 * Returns a flag that indicates whether the root node is visible.
	 * 
	 * @return True if the root node is visible, false otherwise.
	 */
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Sets a flag that indicates whether the root node is visible.
	 * 
	 * @param visible True to draw the root node, false otherwise.
	 */
	public void setVisible(boolean visible) {
		if (this.visible != visible) {
			this.visible = visible;
			setSelected(null);
			repaint();
		}
	}
	
	
	/**
	 * Returns the minimum content width. This will be the screen width.
	 * 
	 * @return The minimum content width.
	 */
	public int getMinimumWidth() {
		return manager.getDisplayWidth() - 2;
	}

	/**
	 * Returns the minimum content height. This will be the height according
	 * to the height specification.
	 * 
	 * @return The minimum content height.
	 */
	public int getMinimumHeight() {
		return lines * LINE_HEIGHT;
	}

	/**
	 * Draws the tree on the specified graphics object.
	 * 
	 * @param graphics The graphics object used to draw.
	 * @param width The width of the item.
	 * @param height The height of the item.
	 */
	public void paint(Graphics graphics, int width, int height) {
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_BACKGROUND);	
		} else {
			graphics.setColor(style.COLOR_BACKGROUND);
		}
		graphics.fillRect(0, 0, width, height);
		graphics.translate(-offsetX, -offsetY);
		if (lines == 0 || content == null) return;
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_FOREGROUND);	
		} else {
			graphics.setColor(style.COLOR_FOREGROUND);
		}
		if (visible) {
			paint(graphics, content);
		} else {
			int drawn = 0;
			Vector children = content.getChildren();
			for (int i = 0, s = children.size(); i < s; i++) {
				TreeNode child = (TreeNode)children.elementAt(i);
				int number = paint(graphics, child);
				drawn += number;
				graphics.translate(0, LINE_HEIGHT * number);
			}
			graphics.translate(0, -LINE_HEIGHT * drawn);
			if (drawn > 1) {
				int fs = LINE_HEIGHT;
				int stroke = graphics.getStrokeStyle();
				int line = LINE_HEIGHT * (drawn) - fs/2;
				graphics.setStrokeStyle(Graphics.DOTTED);
				graphics.drawLine(fs/2*3, fs, fs/2*3, line);
				graphics.setStrokeStyle(stroke);
			}

		}
	}
	
	/**
	 * Draws the subtree on the graphics object and returns the number
	 * of additional lines that have been filled by the tree.
	 * 
	 * @param graphics The graphics object used to draw the tree.
	 * @param node The tree node that should be drawn.
	 * @return The number of elements that have been painted.
	 */
	private int paint(Graphics graphics, TreeNode node) {
		if (selected == node) {
			graphics.setFont(style.FONT_ACTIVE_ITEM);
		} else {
			graphics.setFont(style.FONT_ITEM);
		}
		int fs = LINE_HEIGHT;
		if (node.hasChildren()) {
			if (node.isExpanded()) {
				graphics.drawRect(3, 3, fs - 6, fs - 6);	
			} else {
				graphics.fillRect(4, 4, fs - 7, fs - 7);
			}
		} else {
			graphics.fillRect(6, 6, fs - 11, fs - 11);
		}
		Image image = node.getImage();
		if (image != null) {
			graphics.drawImage(image, fs, 0, Graphics.TOP | Graphics.LEFT);
		}
		graphics.drawString(node.getLabel(), fs * 2 + 3, 
			(fs - style.FONT_ITEM.getHeight())/2 + 1, Graphics.LEFT | Graphics.TOP);
		int xoff = LINE_HEIGHT;
		graphics.translate(xoff, LINE_HEIGHT);
		int drawn = 1;
		if (node.isExpanded()) {
			Vector children = node.getChildren();
			for (int i = 0, s = children.size(); i < s; i++) {
				TreeNode child = (TreeNode)children.elementAt(i);
				int number = paint(graphics, child);
				drawn += number;
				graphics.translate(0, LINE_HEIGHT * number);
			}			
		}
		graphics.translate(-xoff, -LINE_HEIGHT * drawn);
		if (drawn > 1) {
			int stroke = graphics.getStrokeStyle();
			int line = LINE_HEIGHT * (drawn) - fs/2;
			graphics.setStrokeStyle(Graphics.DOTTED);
			graphics.drawLine(fs/2*3, fs, fs/2*3, line);
			graphics.setStrokeStyle(stroke);			
		}
		return drawn;
	}
	
	
	/**
	 * Sets the number of lines that should be visible. If the number
	 * is negative, it will be adjusted to 0.
	 * 
	 * @param lines The lines that should be visible.
	 */
	public void setLines(int lines) {
		if (lines < 0) lines = 0;
		if (this.lines != lines) {
			invalidate();
		}
	}
	
	/**
	 * Returns the lines that are visible.
	 * 
	 * @return The lines that are visible.
	 */
	public int getLines() {
		return lines;
	}
	
	/**
	 * Sets the node that is currently selected to the specified node.
	 * 
	 * @param node The node that is currently selected.
	 */
	protected void setSelected(TreeNode node) {
		if (node != selected) {
			removeCommand(selectCommand);
			selected = node;
			listeners.fireEvent(EVENT_SELECTION_CHANGED, selected);
			if (selected != null && selected.isSelectable()) {
				addCommand(selectCommand);
				setDefaultCommand(selectCommand);
			}
		}
	}
	
	/**
	 * Returns the node that is currently selected. If no node is
	 * selected, this will be null.
	 * 
	 * @return The node that is currently selected or null.
	 */
	public TreeNode getSelected() {
		return selected;
	}
	
	
	/**
	 * Called whenever a focus event reaches the tree item. The tree
	 * item will consume the key event if the ends are not reached.
	 * 
	 * @param code The key code of the traversal event (up,down,left,right,none).
	 * @param w The width of the item.
	 * @param h The height of the item.
	 * @param visible The part of the item that should be made visible.
	 * @return True if the focus should be kept, false otherwise.
	 */
	public boolean traverse(int code, int w, int h, int[] visible) {
		boolean keepfocus = true;
		if (focus) {
			switch (code) {
				case KEY_LEFT:
					if (selected != null && selected.isExpanded()) {
						selected.setExpanded(false);
					}
					break;
				case KEY_RIGHT:
					if (selected != null && ! selected.isExpanded()) {
						selected.setExpanded(true);
					}
					break;
				case KEY_UP:
					TreeNode previous = findPrevious();
					if (previous != null) {
						setSelected(previous);
					} else {
						keepfocus = false;
					}
					break;
				case KEY_DOWN:
					TreeNode next = findNext();
					if (next != null) {
						setSelected(next);
					} else {
						keepfocus = false;
					}
					break;
				default:
					// fall through
			}
		} else {
			focus = true;
		}
		centerCurrent(visible);
		if (keepfocus) repaint();
		return keepfocus;
	}
	
	/**
	 * Called whenever the focus is moved on to another item.
	 * This will reset the internal focus state of the item
	 */
	public void traverseOut() {
		focus = false;
		repaint();
	}
	
	/**
	 * Finds the previous node using the current selection and content.
	 * 
	 * @return The previous node using the current selection and content
	 * 	or null if there is no previous node.
	 */
	private TreeNode findPrevious() {
		if (content == null) return null;
		if (selected == null) {
			if (visible) {
				return content;
			}
			else {
				if (content.hasChildren()) {
					Vector children = content.getChildren();
					return (TreeNode)children.elementAt(0);
				} else {
					return null;
				}
			}
		} else if (selected == content){
			return null;
		} else {
			TreeNode node = selected.getParent();
			if (node == null) return null;
			Vector children = node.getChildren();
			int index = children.indexOf(selected);
			if (index > 0) {
				node = (TreeNode)children.elementAt(index - 1);
				while (node.isExpanded()) {
					if (node.hasChildren()) {
						Vector cs = node.getChildren();
						node = (TreeNode)cs.elementAt(cs.size() - 1);
					} else {
						break;	
					}
				}
			}
			if (! visible && node == content) {
				return null;
			} else {
				return node;
			}
		}
	}
	
	/**
	 * Finds the next node using the current selection and content.
	 * 
	 * @return The next node using the current selection and content.
	 */
	private TreeNode findNext() {
		if (content == null) return null;
		if (selected == null) {
			if (visible) {
				return content;
			}
			else {
				if (content.hasChildren()) {
					Vector children = content.getChildren();
					return (TreeNode)children.elementAt(0);
				} else {
					return null;
				}
			}
		} else {
			if (selected.isExpanded() && selected.hasChildren()) {
				return (TreeNode)selected.getChildren().elementAt(0);
			} else {
				TreeNode sel = selected;
				TreeNode par = sel.getParent();
				while (true) {
					if (par == null || sel == content) return null;	
					int index = par.getChildren().indexOf(sel);
					if (index != -1 && index < par.getChildren().size() - 1) {
						TreeNode next = (TreeNode)par.getChildren().elementAt(index + 1);
						return next;
					} else {
						sel = par;
						par = sel.getParent();
					}
				}
			}
		}
	}
	
	/**
	 * Computes the x and y offset and the view port using the
	 * passed array.
	 * 
	 * @param visible The view port that needs to be visible.
	 */
	private void centerCurrent(int[] visible) {
		visible[0] = 0;
		visible[1] = 0;
		visible[2] = 1;
		visible[3] = LINE_HEIGHT - 1;
		offsetX = 0;
		offsetY = 0;
		int totalLines = computeTotalLines();
		if (totalLines == -1) return;
		int selectedLine = computeSelectedLine();
		if (selectedLine == -1) return;
		if (totalLines < lines) {
			visible[1] = selectedLine;
		} else if (selectedLine < lines / 2) {
			visible[1] = selectedLine;
			offsetY = 0;
		} else if (totalLines - selectedLine <= lines / 2) {
			visible[1] = lines - (totalLines - selectedLine);
			offsetY = totalLines - lines;
		} else {
			visible[1] = lines / 2;
			offsetY = selectedLine - (lines / 2);
		}
		visible[1] = visible[1] * LINE_HEIGHT;
		offsetY = offsetY * LINE_HEIGHT;
		TreeNode node = selected;
		int depth = this.visible?0:-1;
		while (node != content && node != null) {
			depth += 1;
			node = node.getParent();
		}
		offsetX = depth * LINE_HEIGHT;
	}
	
	/**
	 * Computes the line number of the selected node and returns
	 * it. If no node is selected or the selected node is not 
	 * visible, -1 is returned.
	 * 
	 * @return The line number of the selected node or -1.
	 */
	private int computeSelectedLine() {
		if (selected == null || content == null) return -1;
		Vector visit = new Vector();
		visit.addElement(content);
		int line = visible?0:-1;
		while (! visit.isEmpty()) {
			TreeNode next = (TreeNode)visit.elementAt(0);
			visit.removeElementAt(0);
			if (next == selected) {
				return line;
			}
			line += 1;
			if (next.isExpanded() || (next == content && !visible)) {
				Vector children = next.getChildren();
				for (int i = children.size() - 1; i >= 0; i--) {
					visit.insertElementAt(children.elementAt(i), 0);
				}
			}
		}
		return -1;
	}

	/**
	 * Computes the total number of visible lines and returns them.
	 * If no content is visible, -1 will be returned.
	 * 
	 * @return The total number of lines or -1.
	 */
	private int computeTotalLines() {
		if (content == null) return -1;
		Vector visit = new Vector();
		visit.addElement(content);
		int line = visible?0:-1;
		while (! visit.isEmpty()) {
			TreeNode next = (TreeNode)visit.elementAt(0);
			visit.removeElementAt(0);
			line += 1;
			if (next.isExpanded() || (next == content && !visible)) {
				Vector children = next.getChildren();
				for (int i = children.size() - 1; i >= 0; i--) {
					visit.insertElementAt(children.elementAt(i), 0);
				}
			}
		}
		return line;
	}

	/**
	 * Called whenever an action is executed on the tree item.
	 * 
	 * @param command The command that was executed.
	 * @param item The item that received the command.
	 */
	public void commandAction(Command command, FormItem item) {
		if (command == selectCommand) {
			final TreeNode fire = selected;
			listeners.fireEvent(EVENT_SELECTION_FIRED, fire);	
		}
	}
	
}
