package info.pppc.base.lcdui.form;

import javax.microedition.lcdui.Graphics;

/**
 * The form item group enables a user to group two items and
 * make them look like one. The only thing that is reused
 * from the items are the paint methods and the size computation.
 * 
 * @author Marcus Handte
 */
public class FormItemGroup extends FormItem {

	/**
	 * A layout constant that is used to signal that the item
	 * 1 should be placed left of item2.
	 */
	public static final int POSITION_VERTICAL = 1;
	
	/**
	 * A layout constant that is used to signal that the item
	 * 1 should be placed right of item2.
	 */
	public static final int POSITION_HORIZONTAL = 2;
	
	/**
	 * A layout constant that is used to signal that the content
	 * of the items should be expanded.
	 */
	public static final int POSITION_EXPAND = 4;
	
	/**
	 * A layout constant that is used to signal that the content
	 * of the items should be centered.
	 */
	public static final int POSITION_CENTER = 8;
	
	/**
	 * The sizes of the item, first (set width and height) manipulated
	 * by user, if negative use auto compute (second pair).
	 */
	private int[] size = new int[]{ -1, -1, 0, 0 };
	
	/**
	 * A flag that indicates whether the label has the focus.
	 */
	private boolean focus = false;
		
	/**
	 * A flag that indicates whether the item has the focus.
	 */
	private boolean enabled = false;
	
	/**
	 * The left or top from item.
	 */
	private FormItem item1 = null;
	
	/**
	 * The right or bottom item.
	 */
	private FormItem item2 = null;
	
	/**
	 * A flag that indicates the position of the item.
	 */
	private int position;
	
	/**
	 * The style of the from.
	 */
	private FormStyle style;
	
	/**
	 * The form item group can be used to group two items 
	 * together.
	 * 
	 * @param label The label of the item.
	 * @param item1 The first item.
	 * @param item2 The second item.
	 * @param position Layout constant, horizontally to place 
	 * 	item1 left of item2 or vertically to place item1 on
	 * 	top of item2.
	 */
	public FormItemGroup(String label, FormItem item1, FormItem item2, int position) {
		super(label);
		this.item1 = item1;
		this.item2 = item2;
		this.position = position;
		this.style = FormStyle.getStyle();
		recompute();
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
	 * Sets the item1.
	 * 
	 * @param item1 The new item1 to set.
	 */
	public void setItem1(FormItem item1) {
		synchronized (FormDisplay.UI_LOCK) {
			if (this.item1 != null) item1.setForm(null);
			this.item1 = item1;
			if (item1 != null) item1.setForm(getForm());
			recompute();
			invalidate();			
		}
	}

	/**
	 * Returns the item1.
	 * 
	 * @return The item 1.
	 */
	public FormItem getItem1() {
		synchronized (FormDisplay.UI_LOCK) {
			return item1;	
		}
		
	}
	
	/**
	 * Returns the item2.
	 * 
	 * @return The item2.
	 */
	public FormItem getItem2() {
		synchronized (FormDisplay.UI_LOCK) {
			return item2;	
		}
	}
	
	/**
	 * Sets the item2.
	 * 
	 * @param item2 The item2 to set.
	 */
	public void setItem2(FormItem item2) {
		synchronized (FormDisplay.UI_LOCK) {
			if (this.item2 != null) item2.setForm(null);
			this.item2 = item2;
			if (item2 != null) item2.setForm(getForm());
			recompute();
			invalidate();			
		}
	}
	
	
	
	/**
	 * Recomputes the internal data structures of the
	 * label.
	 */
	private void recompute() {
		size[2] = 0;
		size[3] = 0;
		if (item1 != null) {
			size[2] += item1.getMinimumWidth();
			size[3] += item1.getMinimumHeight();
		}
		if (item2 != null) {
			if ((position & POSITION_HORIZONTAL) == POSITION_HORIZONTAL) {
				size[2] += item2.getMinimumWidth();
				size[3] = size[3]>item2.getMinimumHeight()?
						size[3]:item2.getMinimumHeight();
			} else {
				size[2] = size[2]>item2.getMinimumWidth()?
						size[2]:item2.getMinimumWidth();
				size[3] += item2.getMinimumHeight();
			}
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
			return size[3];
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
			return size[2];
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
		if ((position & POSITION_HORIZONTAL) == POSITION_HORIZONTAL) {
			int w1 = 0;
			int w2 = 0;
			if (item1 != null) {
				w1 = item1.getMinimumWidth();
			}
			if (item2 != null) {
				w2 = item2.getMinimumWidth();
			}
			if (width > w1 + w2) {
				if (w1 == 0) {
					w2 = width;
				} else if (w2 == 0) {
					w1 = width;
				} else {
					int excess = width - (w1 + w2);
					int add = excess / 2;
					w1 += add;
					w2 += add;
					if (add * 2 != excess) {
						w1 += 1;
					}
				}
			}
			if (item1 != null) {
				if ((position & POSITION_CENTER) == POSITION_CENTER) {
					int w = item1.getMinimumWidth();
					int h = item1.getMinimumHeight();
					int tx = (w1 - w)/2;
					int ty = (height - h)/2;
					g.translate(tx, ty);
					item1.paint(g, w, h);
					g.translate(-tx,-ty);					
				} else {
					item1.paint(g, w1, height);	
				}
			}
			g.translate(w1, 0);
			if (item2 != null) {
				if ((position & POSITION_CENTER) == POSITION_CENTER) {
					int w = item2.getMinimumWidth();
					int h = item2.getMinimumHeight();
					int tx = (w2 - w)/2;
					int ty = (height - h)/2;
					g.translate(tx, ty);
					item2.paint(g, w, h);
					g.translate(-tx,-ty);
				} else {
					item2.paint(g, w2, height);	
				}
			}
		} else {
			int h1 = 0;
			int h2 = 0;
			if (item1 != null) {
				h1 = item1.getMinimumHeight();
			}
			if (item2 != null) {
				h2 = item2.getMinimumHeight();
			}
			if (height > h1 + h2) {
				if (h1 == 0) {
					h2 = height;
				} else if (h2 == 0) {
					h1 = height;
				} else {
					int excess = height - (h1 + h2);
					int add = excess / 2;
					h1 += add;
					h2 += add;
					if (add * 2 != excess) {
						h1 += 1;
					}
				}
			}
			if (item1 != null) {
				if ((position & POSITION_CENTER) == POSITION_CENTER) {
					int w = item1.getMinimumWidth();
					int h = item1.getMinimumHeight();
					int tx = (width - w)/2;
					int ty = (h1 - h)/2;
					g.translate(tx, ty);
					item1.paint(g, w, h);
					g.translate(-tx,-ty);
				} else {
					item1.paint(g, width, h1);	
				}
			}
			g.translate(0, h1);
			if (item2 != null) {
				if ((position & POSITION_CENTER) == POSITION_CENTER) {
					int w = item2.getMinimumWidth();
					int h = item2.getMinimumHeight();
					int tx = (width - w)/2;
					int ty = (h2 - h)/2;
					g.translate(tx, ty);
					item2.paint(g, w, h);
					g.translate(-tx,-ty);
				} else {
					item2.paint(g, width, h2);
				}
			}
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
				if (! focus) {
					focus = true;
					if (item1 != null) item1.traverse(code, width, height, visible);
					if (item2 != null) item2.traverse(code, width, height, visible);
					repaint();
				}
				visible[0] = getMinimumWidth() / 2;
				visible[1] = getMinimumHeight() / 2;
				visible[2] = 1;
				visible[3] = 1;
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
		if (item1 != null) item1.traverseOut();
		if (item2 != null) item2.traverseOut();
		repaint();
	}
	
	/**
	 * Called whenever the form is updated.
	 * 
	 * @param form The form of the item.
	 */
	protected void setForm(Form form) {
		if (item1 != null) item1.setForm(form);
		if (item2 != null) item2.setForm(form);
		super.setForm(form);
	}
}
