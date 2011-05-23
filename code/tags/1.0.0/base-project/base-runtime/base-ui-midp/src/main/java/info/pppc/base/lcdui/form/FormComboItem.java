package info.pppc.base.lcdui.form;

import info.pppc.base.system.event.ListenerBundle;

import java.util.Vector;

import javax.microedition.lcdui.Graphics;

/**
 * The form combo item lets a user select an item from a 
 * list of items. The user will only see one item at the
 * time.
 * 
 * @author Marcus Handte
 */
public class FormComboItem extends FormItem {

	/**
	 * The spacing required for the border.
	 */
	private static final int BORDER_SPACING = 2;
	
	/**
	 * The event that is fired, whenever the selection changes.
	 * The source object of the event will be the combo that
	 * received the event. The data object will be an integer
	 * that denotes the new selected index.
	 */
	public static final int EVENT_SELECTION_CHANGED = 1;
	
	/**
	 * The model contents that are currently contained
	 * in this form item.
	 */
	private Vector elements = new Vector();
	
	/**
	 * The item that is currently selected.
	 */
	private int element = -1;
	
	/**
	 * A flag that indicates whether the item has the focus.
	 */
	private boolean focus = false;
	
	/**
	 * A flag that indicates whether the item is enabled.
	 */
	private boolean enabled = false;
	
	/**
	 * The listeners that are contained in the combo.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * The sizes of the item, first (set width and height) manipulated
	 * by user, if negative use auto compute (second pair).
	 */
	private int[] size = new int[]{ -1, -1, 0, 0 };
	
	/**
	 * The style of the from.
	 */
	private FormStyle style;
	
	/**
	 * Creates a new form combo item without any items.
	 * 
	 * @param label The label of the combo.
	 */
	public FormComboItem(String label) {
		super(label);
		style = FormStyle.getStyle();
		size[3] = style.FONT_ITEM.getHeight();
	}
	
	/**
	 * Adds an element at the end of the combo. The
	 * element must not be null.
	 * 
	 * @param element The element that should be added.
	 */
	public void append(String element) {
		if (element == null) throw new NullPointerException("Element is null.");
		synchronized (FormDisplay.UI_LOCK) {
			elements.addElement(element);
		}
	}
	
	/**
	 * Inserts an element at the specified index. The 
	 * element must not be null and the index must be
	 * in the element range.
	 * 
	 * @param element The element that should be added.
	 * @param index The index where to add.
	 */
	public void insert(String element, int index) {
		if (element == null) throw new NullPointerException("Element is null.");
		synchronized (FormDisplay.UI_LOCK) {
			elements.insertElementAt(element, index);
			if (index >= this.element) {
				this.element += 1;
			}
		}
		
	}
	
	/**
	 * Removes the element at the specified index.
	 * The index must point to an element.
	 * 
	 * @param index The index of the element to
	 * 	remove.
	 */
	public void remove(int index) {
		synchronized (FormDisplay.UI_LOCK) {
			elements.removeElementAt(index);
			if (element >= elements.size()) {
				select(elements.size() - 1);
				repaint();
			}
		}
	}
	
	/**
	 * Retrieves the element at the specified index.
	 * 
	 * @param index The index to retrieve.
	 * @return The element at the index.
	 */
	public String get(int index) {
		synchronized (FormDisplay.UI_LOCK) {
			return (String)elements.elementAt(index);
		}
	}
	
	/**
	 * Returns all elements contained in the combo.
	 * 
	 * @return The elements contained in the combo.
	 */
	public String[] get() {
		synchronized (FormDisplay.UI_LOCK) {
			String[] elems = new String[elements.size()];
			for (int i = elements.size() - 1; i >= 0; i--) {
				elems[i] = (String)elements.elementAt(i);
			}
			return elems;
		}
	}
	
	/**
	 * Selects the item at the specified index. The index must
	 * point to a valid item. Set to -1 to deselect all.
	 * 
	 * @param index The index that points to the selected item.
	 */
	public void select(int index) {
		synchronized (FormDisplay.UI_LOCK) {
			if (index == element) return;
			if (index < -1 || index >= elements.size()) 
				throw new IndexOutOfBoundsException("Index is out of bounds.");
			this.element = index;
			listeners.fireEvent(EVENT_SELECTION_CHANGED, new Integer(index));
			repaint();			
		}
	}
	
	/**
	 * Returns the index of the current selection or -1 if
	 * nothing has been selected.
	 * 
	 * @return The current selection index or -1 if none.
	 */
	public int getSelection() {
		synchronized (FormDisplay.UI_LOCK) {
			return element;
		}
	}
	
	/**
	 * Enables the form item or disables it depending on the flag.
	 * If disabled, the item cannot be selected (i.e. it cannot
	 * retrieve the focus).
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		synchronized (FormDisplay.UI_LOCK) {
			this.enabled = enabled;
			if (! enabled && focus) {
				invalidate();
			}
		}
	}
	
	/**
	 * Determines whether the item is enabled. True if it is
	 * enabled, false otherwise.
	 * 
	 * @return True if the item is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		synchronized (FormDisplay.UI_LOCK) {
			return enabled;
		}
	}
	
	/**
	 * Returns the minimum height of the form combo.
	 * 
	 * @return The minimum height.
	 */
	public int getMinimumHeight() {
		if (size[1] < 0) {
			return size[3] + BORDER_SPACING;
		} else {
			return size[1];
		}
	}
	
	/**
	 * Returns the minimum width of the form combo.
	 * 
	 * @return The minimum width.
	 */
	public int getMinimumWidth() {
		if (size[0] < 0) {
			return size[2] + BORDER_SPACING;
		} else {
			return size[0];
		}
	}
	
	/**
	 * Sets the size of the combo to the specified values.
	 * If the values are negative, the corresponding dimension
	 * will be auto-computed depending on the content.
	 * 
	 * @param width The width of the label.
	 * @param height The height of the label.
	 */
	public void setSize(int width, int height) {
		synchronized (FormDisplay.UI_LOCK) {
			size[0] = width;
			size[1] = height;
			invalidate();			
		}
	}
	
	/**
	 * Called whenever the traversal happens.
	 * 
	 * @param code The key code.
	 * @param width The item width.
	 * @param height The item height.
	 * @param visible The visible rectangle.
	 * @return True to keep focus, false to release.
	 */
	public boolean traverse(int code, int width, int height, int[] visible) {
		if (!enabled) return false;
		boolean keepfocus = false;
		if (focus) {
			switch (code) {
				case KEY_DOWN:
				case KEY_UP:
					keepfocus = false;
				case KEY_LEFT:
					if (element > -1) {
						select(element -= 1);
					} 
					keepfocus = true;
					break;
				case KEY_RIGHT:
					if (element < elements.size() - 1) {
						select(element += 1);
					}
					keepfocus = true;
					break;
				default:
					keepfocus = true;
			}
		} else {
			focus = true;
			keepfocus = true;
		}
		if (keepfocus) {
			repaint();
			visible[0] = 0;
			visible[1] = 0;
			visible[2] = getMinimumWidth();
			visible[3] = getMinimumHeight();
		}
		return keepfocus;
	}
	
	/**
	 * Called whenever the focus leaves the item.
	 */
	public void traverseOut() {
		if (focus) {
			focus = false;
			repaint();
		}
	}
	
	/**
	 * Draws the form combo item using the specified width
	 * and height.
	 * 
	 * @param g The graphics object used to draw.
	 * @param width The width used to draw.
	 * @param height The height used to draw.
	 */
	public void paint(Graphics g, int width, int height) {
		if (focus) {
			g.setColor(style.COLOR_ACTIVE_BACKGROUND);	
		} else {
			g.setColor(style.COLOR_BACKGROUND);
		}
		g.fillRect(0, 0, width, height);
		g.setFont(style.FONT_ITEM);
		if (focus) {
			g.setColor(style.COLOR_ACTIVE_FOREGROUND);	
		} else {
			g.setColor(style.COLOR_FOREGROUND);
		}
		int yoff = BORDER_SPACING / 2;
		int xoff = BORDER_SPACING / 2;
		int w = style.FONT_ITEM.charWidth('<');
		if (element != -1) {
			g.drawChar('<', xoff, yoff, Graphics.TOP | Graphics.LEFT);
			String s = get(element);
			int sw = width - (xoff * 3) + (w * 2);
			while (sw < style.FONT_ITEM.stringWidth(s)) {
				s = s.substring(0, s.length() - 1);
			}
			g.drawString(s, xoff * 2 + w * 2, yoff, Graphics.TOP | Graphics.LEFT);	
		}
		if (element < elements.size() - 1) {
			g.drawChar('>', xoff + w, yoff, Graphics.TOP | Graphics.LEFT);
		}
	}
	


}
