package info.pppc.base.lcdui.form;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * The form image item lets the user place an image into a form.
 * 
 * @author Marcus Handte
 */
public class FormImageItem extends FormItem {
	
	/**
	 * The spacing required for the border.
	 */
	private static final int BORDER_SPACING = 2;
	
	/**
	 * The image that is drawn in the paint method.
	 */
	private Image image;
	
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
	 * @param image The image of the item.
	 */
	public FormImageItem(String label, Image image) {
		super(label);
		style = FormStyle.getStyle();
		setImage(image);
	}
	
	/**
	 * Sets a flag that indicates whether the string item
	 * can receive the focus.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (!enabled && focus) {
			invalidate();
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
	 * Sets the image that is painted.
	 * 
	 * @param image The image that is painted.
	 */
	public void setImage(Image image) {
		synchronized (FormDisplay.UI_LOCK) {
			this.image = image;
			recompute();
			invalidate();			
		}
	}
	
	/**
	 * Returns the text of the label.
	 * 
	 * @return The text of the label.
	 */
	public Image getImage() {
		synchronized (FormDisplay.UI_LOCK) {
			return image;	
		}
	}
	
	/**
	 * Recomputes the internal data structures of the
	 * label.
	 */
	private void recompute() {
		if (image != null) {
			size[2] = image.getWidth();
			size[3] = image.getHeight();
		} else {
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
	 * Draws the image of the item.
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
		if (image != null) {
			int w = image.getWidth();
			int h = image.getHeight();
			int x = (width - w) / 2;
			int y = (height - h) / 2;
			g.drawImage(image, x, y, Graphics.TOP | Graphics.LEFT);	
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
