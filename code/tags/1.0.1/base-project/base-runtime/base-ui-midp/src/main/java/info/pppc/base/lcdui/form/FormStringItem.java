package info.pppc.base.lcdui.form;

import javax.microedition.lcdui.Graphics;

/**
 * The string item displays a label and some text. The size
 * of the item depends on the size of the text. However,
 * the size can also be set directly. If the direct size
 * indicator is set to -1, -1, then the item's size depends
 * on its textual content.
 * 
 * @author Marcus Handte
 */
public class FormStringItem extends FormItem {
	
	/**
	 * The spacing required for the border.
	 */
	private static final int BORDER_SPACING = 2;
	
	/**
	 * The string that is drawn in the paint method.
	 */
	private String text;
	
	/**
	 * The sizes of the item, first (set width and height) manipulated
	 * by user, if negative use auto compute (second pair).
	 */
	private int[] size = new int[]{ -1, -1, 0, 0 };
	
	/**
	 * The lines of the text.
	 */
	private String[] lines = new String[0];
	
	/**
	 * The style of the from.
	 */
	private FormStyle style;
	
	/**
	 * A flag that indicates whether the label has the focus.
	 */
	private boolean focus = false;
		
	/**
	 * A flag that indicates whether the item has the focus.
	 */
	private boolean enabled = false;
	
	/**
	 * Creates a new string item with the specified label and
	 * string.
	 * 
	 * @param label The label of the string item.
	 * @param text The string item of the string.
	 */
	public FormStringItem(String label, String text) {
		super(label);
		style = FormStyle.getStyle();
		setText(text);
	}
	
	/**
	 * Sets a flag that indicates whether the string item
	 * can receive the focus.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		synchronized (FormDisplay.UI_LOCK) {
			this.enabled = enabled;
			if (!enabled && focus) {
				invalidate();
			}			
		}
	}
	
	/**
	 * Returns the enabled state of the item. If it is
	 * enabled, the string item can receive the focus.
	 * 
	 * @return The enabled state of the item.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Sets the text that is displayed in the label.
	 * 
	 * @param text The text of the label.
	 */
	public void setText(String text) {
		synchronized (FormDisplay.UI_LOCK) {
			this.text = text;
			recompute();
			invalidate();			
		}
	}
	
	/**
	 * Returns the text of the label.
	 * 
	 * @return The text of the label.
	 */
	public String getText() {
		synchronized (FormDisplay.UI_LOCK) {
			return text;	
		}
	}
	
	/**
	 * Recomputes the internal data structures of the
	 * label.
	 */
	private void recompute() {
		if (text != null) {
			lines = new String[] { text };
			size[2] = 0;
			for (int i = 0; i < lines.length; i++) {
				int idx = lines[i].indexOf('\n');
				if (idx != -1) {
					String[] cpy = new String[lines.length + 1];
					System.arraycopy(lines, 0, cpy, 0, lines.length - 1);
					cpy[lines.length - 1] = lines[i].substring(0, idx); 
					cpy[lines.length] = lines[i].substring
						(idx + 1, lines[i].length());
					lines = cpy;
				}
				int w = style.FONT_ITEM.stringWidth(lines[i]);
				size[2] = size[2]>w?size[2]:w;
			}
			size[3] = lines.length * style.FONT_ITEM.getHeight();
		} else {
			lines = new String[0];
			size[2] = 0;
			size[3] = 0;
		}
	}
	
	/**
	 * Sets the size of the label to the specified values.
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
	 * Returns the minimum height of the label.
	 * 
	 * @return The minimum height of the label.
	 */
	public int getMinimumHeight() {
		if (size[1] < 0) {
			return size[3] + BORDER_SPACING;
		} else {
			return size[1];
		}
	}
	
	/**
	 * Returns the maximum width of the label.
	 * 
	 * @return The minimum width of the label.
	 */
	public int getMinimumWidth() {
		if (size[0] < 0) {
			return size[2] + BORDER_SPACING;
		} else {
			return size[0];
		}
	}
	
	/**
	 * Draws the text of the label.
	 * 
	 * @param g The graphics to draw to.
	 * @param width The width of the item.
	 * @param height The height of the item.
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
		for (int i = 0, s = lines.length; i < s; i++) {
			g.drawString(text, xoff, yoff, Graphics.TOP | Graphics.LEFT);
			yoff += style.FONT_ITEM.getHeight();
		}
	}
	
	/**
	 * Called whenever the item is traversed.
	 * 
	 * @param code The key code of the traversal.
	 * @param width The width of the item.
	 * @param height The height of the item.
	 * @param visible The visible in/out parameter.
	 * @return True to keep the focus, false otherwise.
	 */
	public boolean traverse(int code, int width, int height, int[] visible) {
		if (enabled) {
			if (! focus || code == KEY_NONE) {
				visible[0] = getMinimumWidth() / 2;
				visible[1] = getMinimumHeight() / 2;
				visible[2] = 1;
				visible[3] = 1;
				if (! focus) {
					focus = true;
					repaint();
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Called whenever the focus leaves the item.
	 */
	public void traverseOut() {
		focus = false;
		repaint();
	}

}
