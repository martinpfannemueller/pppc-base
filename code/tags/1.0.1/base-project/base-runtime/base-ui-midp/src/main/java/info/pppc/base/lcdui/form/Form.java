package info.pppc.base.lcdui.form;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

/**
 * Implements a simple form that can deal with string and image items.
 * 
 * @author Marcus Handte
 */
public class Form extends Canvas implements CommandListener {

	/**
	 * The spacing required for the border.
	 */
	private static final int BORDER_SPACING = 4;
	
	/**
	 * The spacing required for the label.
	 */
	private int labelHeight;
	
	/**
	 * The point of the form that is displayed at the top left
	 * corner of the screen. 
	 */
	private int[] viewport = new int[] { 0, 0 };
	
	/**
	 * The items contained in the form.
	 */
	private Vector items = new Vector();
	
	/**
	 * A vector that contains integer arrays of length 4 that
	 * describe the positions and dimensions of the items.
	 */
	private Vector locations = new Vector();

	/**
	 * The commands that are currently registered at the form.
	 */
	private Vector formCommands = new Vector();
	
	/**
	 * A flag that indicates whether the internal state of
	 * the form is valid, if it is not valid, the state must
	 * be recomputed before the item can be drawn. 
	 */
	private boolean valid = false;
	
	/**
	 * The commands that come from the focused item.
	 */
	private Vector focusCommands = new Vector();
	
	/**
	 * The default command of the item that has the focus.
	 */
	private Command focusCommand = null;
	
	/**
	 * The item that is focused.
	 */
	private FormItem focusItem = null;
	
	/**
	 * The command listener that has been added to the form.
	 */
	private CommandListener commandListener = null;
	
	/**
	 * The size of the form, that is visible.
	 */
	private int[] size = new int[] { getWidth(), getHeight() };
	
	/**
	 * Creates a new off screen image that is used for
	 * safe rendering of items.
	 */
	private Image image = null;
	
	/**
	 * The form style used by this form.
	 */
	private FormStyle style;
	
	/**
	 * The total height of the content.
	 */
	private int contentHeight = 0;
	
	/**
	 * The display used to deliver events of commands.
	 */
	private FormDisplay display;
	
	/**
	 * Creates a new form with the specified title.
	 * 
	 * @param display The form display used to dispatch events.
	 * @param label The title of the form.
	 */
	public Form(FormDisplay display, String label) {
		setTitle(label);
		setFullScreenMode(true);
		this.display = display;
		style = FormStyle.getStyle();
		labelHeight = style.FONT_LABEL.getHeight() + 2;
		super.setCommandListener(this);
	}
	
	/**
	 * Adds an item to the form.
	 * 
	 * @param item The item to add to the form.
	 */
	public void append(FormItem item) {
		synchronized (FormDisplay.UI_LOCK) {
			items.addElement(item);
			item.setForm(this);
			valid = false;			
		}
	}
	
	/**
	 * Deletes all items from the form.
	 */
	public void deleteAll() {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = items.size() - 1; i >= 0; i--) {
				delete(i);
			}			
		}
	}
	
	/**
	 * Deletes the item at the specified index from
	 * the form.
	 * 
	 * @param index The index of the item to delete.
	 */
	public void delete(int index) {
		synchronized (FormDisplay.UI_LOCK) {
			FormItem item = (FormItem)items.elementAt(index);
			items.removeElementAt(index);
			if (item == focusItem) {
				setFocusItem(-1);
			}
			item.setForm(null);
			valid = false;			
		}
	}
	
	/**
	 * Inserts an item at the specified index.
	 * 
	 * @param index The index of the item to insert.
	 * @param item The item that should be inserted.
	 */
	public void insert(int index, FormItem item) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = items.size() - 1; i >= 0; i--) {
				if (item == items.elementAt(i)) {
					delete(index);
					break;
				}
			}
			items.insertElementAt(item, index);
			item.setForm(this);
			valid = false;			
		}
	}
	
	/**
	 * Returns the item at the specified index.
	 * 
	 * @param index The index of the item to return.
	 * @return The item at the specified index.
	 */
	public FormItem get(int index) {
		synchronized (FormDisplay.UI_LOCK) {
			return (FormItem)items.elementAt(index);	
		}
	}

	/**
	 * Deletes the specified form item from the form.
	 * 
	 * @param item The form item to delete.
	 */
	public void delete(FormItem item) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = items.size() - 1; i >= 0; i--) {
				if (item == items.elementAt(i)) {
					delete(i);
					break;
				}
			}			
		}
	}
	
	
	/**
	 * Returns the number of items currently contained
	 * in the form.
	 * 
	 * @return The items contained in the form.
	 */
	public int size() {
		synchronized (FormDisplay.UI_LOCK) {
			return items.size();	
		}
	}
	
	/**
	 * Adds a certain command to the form.
	 * 
	 * @param command The command to add.
	 */
	public void addCommand(Command command) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = formCommands.size() - 1; i >= 0; i--) {
				if (command == formCommands.elementAt(i)) {
					return;
				}
			}
			formCommands.addElement(command);
			super.addCommand(command);			
		}
	}
	
	/**
	 * Removes a certain command from the form.
	 * 
	 * @param command The command to remove.
	 */
	public void removeCommand(Command command) {
		synchronized (FormDisplay.UI_LOCK) {
			for (int i = formCommands.size() - 1; i >= 0; i--) {
				if (command == formCommands.elementAt(i)) {
					formCommands.removeElementAt(i);
					super.removeCommand(command);
					return;
				}
			}			
		}
 	}

	/**
	 * Sets the command listener to the specified listener
	 * 
	 * @param listener The command listener to set.
	 */
	public void setCommandListener(CommandListener listener) {
		synchronized (FormDisplay.UI_LOCK) {
			commandListener = listener;	
		}
	}
	
	/**
	 * Called whenever a command has been executed.
	 * 
	 * @param command The command that has been pushed.
	 * @param displayable The displayable that received the command
	 */
	public void commandAction(final Command command, final Displayable displayable) {
		synchronized (FormDisplay.UI_LOCK) {
			if (focusItem != null) {
				final FormCommandListener listener = focusItem.getItemCommandListener();
				if (listener != null) {
					for (int i = focusCommands.size() - 1; i >= 0; i--) {
						if (command == focusCommands.elementAt(i)) {
							final FormItem item = focusItem;
							display.runAsync(new Runnable() {
								public void run() {
									listener.commandAction(command, item);
								}
							});
							return;
						}
					}
				}				
			}
			if (commandListener == null || displayable != this) return;
			for (int i = formCommands.size() - 1; i >= 0; i--) {
				if (command == formCommands.elementAt(i)) {
					final CommandListener listener = commandListener;
					display.runAsync(new Runnable() {
						public void run() {
							listener.commandAction(command, displayable);
						}
					});
					return;
				}
			}			
		}
	}

	/**
	 * Called whenever the form should be drawn on the specified graphics
	 * context.
	 * 
	 * @param g The graphics context to draw to.
	 */
	protected void paint(Graphics g) {
		synchronized (FormDisplay.UI_LOCK) {
			if (! valid) {
				// create a new layout
				recompute();
				// create a new off screen image
				int w = BORDER_SPACING;
				int h = BORDER_SPACING + labelHeight;
				for (int i = locations.size() - 1; i >= 0; i--) {
					int[] location = (int[])locations.elementAt(i);
					w = w>location[2]?w:location[2];
					h = h>location[3]?h:location[3];
				}
				image = Image.createImage(w, h);
				valid = true;
			}
			// clear the canvas
			g.setColor(style.COLOR_BACKGROUND);
			g.fillRect(0, 0, size[0], size[1]);
			// transform the coordinate system
			g.translate(-viewport[0], -viewport[1]);
			// set the font to draw labels
			g.setFont(style.FONT_LABEL);
			// draw each item, first in off screen image and then onto screen
			for (int i = locations.size() - 1; i >= 0; i--) {
				int[] location = (int[])locations.elementAt(i);
				FormItem item = (FormItem)items.elementAt(i);
				String label = item.getLabel();
				int lheight = 0;
				if ((label != null && label.length() > 0)) {
					Graphics li = image.getGraphics();
					li.setColor(style.COLOR_LABEL_BACKGROUND);
					li.fillRect(0, 0, location[2] - BORDER_SPACING, labelHeight);
					li.setColor(style.COLOR_LABEL_FOREGROUND);
					li.drawString(label, BORDER_SPACING / 2, 0, Graphics.TOP | Graphics.LEFT);
					g.drawRegion(image, 0, 0, location[2] - BORDER_SPACING, labelHeight, 
						Sprite.TRANS_NONE, location[0] + BORDER_SPACING / 2, 
						location[1] + BORDER_SPACING / 2, Graphics.TOP | Graphics.LEFT);
					lheight = labelHeight;
				}
				Graphics pg = image.getGraphics();
				int w = location[2] - BORDER_SPACING;
				int h = location[3] - lheight - BORDER_SPACING;
				int x = location[0] + BORDER_SPACING / 2;
				int y = location[1] + BORDER_SPACING / 2 + lheight;
				pg.setClip(0, 0, w, h);
				item.paint(pg, w, h);
				g.drawRegion(image, 0, 0, w, h, Sprite.TRANS_NONE, 
					x, y, Graphics.TOP | Graphics.LEFT);
				if (item == focusItem) {
					g.setColor(style.COLOR_ACTIVE_BORDER);
					g.drawRect(location[0], location[1], location[2] - 1, location[3] - 1);
				}
			}
		}
	}

	/**
	 * Called whenever the size of the form changes.
	 * 
	 * @param width The new width of the form.
	 * @param height The new height of the form.
	 */
	protected void sizeChanged(int width, int height) {
		synchronized (FormDisplay.UI_LOCK) {
			size = new int[] { width, height };
			super.sizeChanged(width, height);			
		}
	}
	
	/**
	 * Computes the layout, the view port and the selected item.
	 */
	private void recompute() {
		// throw away the size and location of items
		locations.removeAllElements();
		// throw away the current focused item
		if (focusItem != null) {
			setFocusItem(-1);
		}
		// compute which item fits in what line
		boolean newline = false;
		int width = 0;
		int height = 0;
		int yoffset = 0;
		Vector line = new Vector(); // the vector for line 0
		Vector lines = new Vector(); // a vector of vectors
		for (int i = 0, s = items.size(); i < s; i++) {
			FormItem item = (FormItem)items.elementAt(i);
			int[] location = new int[4];
			locations.addElement(location);
			int w = item.getMinimumWidth() + BORDER_SPACING;
			w = (w > size[0])?size[0]:w;
			int h = item.getMinimumHeight() + BORDER_SPACING;
			if (item.getLabel() != null && item.getLabel().length() > 0) {
				h += labelHeight;
			}
			newline = newline | item.hasFlag(FormItem.LAYOUT_LINE_BEFORE);
			if (line.size() == 0 || (width + w < size[0] && ! newline)) {
				height = (h > height)?h:height;
				location[0] = width;
				location[1] = yoffset;
				location[2] = w;
				location[3] = h;
				line.addElement(item);
				width += w;
			} else {
				lines.addElement(line);
				line = new Vector();
				line.addElement(item);
				yoffset += height;
				location[0] = 0;
				location[1] = yoffset;
				location[2] = w;
				location[3] = h;
				height = h;
				width = w;
			}
			newline = item.hasFlag(FormItem.LAYOUT_LINE_AFTER);
		}
		if (line.size() > 0) {
			contentHeight = yoffset += height;
			lines.addElement(line);
		} else {
			contentHeight = yoffset;
		}
		// now we have an approximate location and height that does
		// not consider the expand states and anchors
		int index = 0;
		for (int i = 0, ls = lines.size(); i < ls; i++) {
			line = (Vector)lines.elementAt(i);
			int lineHeight = 0;
			int lineExpand = 0;
			int lineWidth = 0;
			for (int j = line.size() - 1; j >= 0; j--) {
				int[] location = (int[])locations.elementAt(index);
				FormItem item = (FormItem)items.elementAt(index);
				lineHeight = lineHeight>location[3]?lineHeight:location[3];
				lineWidth += location[2];
				if (item.hasFlag(FormItem.LAYOUT_EXPAND) 
						|| item.hasFlag(FormItem.LAYOUT_RIGHT) 
							|| item.hasFlag(FormItem.LAYOUT_CENTER)) {
					lineExpand += 1;
				}
				index += 1;
			}
			index -= line.size();
			int lineStretch = 0;
			int remaining = 0;
			if (lineExpand != 0) {
				int w = size[0] - lineWidth;
				lineStretch = w / lineExpand;
				remaining = w % lineExpand;
			}
			int lineShift = 0;
			for (int j = line.size() - 1; j >= 0; j--) {
				int[] location = (int[])locations.elementAt(index);
				FormItem item = (FormItem)items.elementAt(index);
				location[0] += lineShift;
				if (item.hasFlag(FormItem.LAYOUT_EXPAND)) {
					// expand by stretch (+) pixels
					location[2] += lineStretch;
					lineShift += lineStretch;
					if (remaining > 0) {
						location[2] += 1;
						lineShift += 1;
						remaining -= 1;
					}
				} else if (item.hasFlag(FormItem.LAYOUT_RIGHT)) {
					location[0] += lineStretch;
					lineShift += lineStretch;
					if (remaining > 0) {
						location[0] += 1;
						lineShift += 1;
						remaining -= 1;
					}
				} else if (item.hasFlag(FormItem.LAYOUT_CENTER)) {
					location[0] += lineStretch / 2;
					lineShift += lineStretch;
					if (remaining > 0) {
						location[0] += 1;
						lineShift += 1;
						remaining -= 1;
					}
				}
				if (item.hasFlag(FormItem.LAYOUT_VEXPAND)) {
					location[3] = lineHeight;
				} else {
					if (item.hasFlag(FormItem.LAYOUT_VBOTTOM)) {
						location[1] += lineHeight - location[3];
					} else if (item.hasFlag(FormItem.LAYOUT_VCENTER)) {
						location[1] += ((lineHeight - location[3]) / 2);
					}
				}
				index += 1;
			}
		}
		// compute the first item that we could focus
		setFocusItem(-1);
		viewport[0] = 0;
		viewport[1] = 0;
	}
	
	/**
	 * Called by an item on this form to tell the form
	 * that the commands have been changed.
	 * 
	 * @param item The item whose commands have been changed.
	 */
	protected void updateCommands(FormItem item) {
		synchronized (FormDisplay.UI_LOCK) {
			if (item == focusItem) {
				while (focusCommands.size() > 0) {
					Command command = (Command)focusCommands.elementAt(0);
					super.removeCommand(command);
					focusCommands.removeElementAt(0);
				}
				Command[] cs = item.getCommands();
				focusCommand = item.getDefaultCommand();
				for (int i = 0, s = cs.length; i < s; i++) {
					super.addCommand(cs[i]);
					focusCommands.addElement(cs[i]);
				}
			}			
		}
	}
	
	/**
	 * Invalidates the layout of the form.
	 */
	protected void invalidate() {
		synchronized (FormDisplay.UI_LOCK) {
			valid = false;	
		}
	}
	
	/**
	 * Called whenever a key is pressed.
	 * 
	 *  @param k The key code of the pressed key.
	 */
	protected void keyPressed(int k) {
		final int key = getGameAction(k);
		display.runAsync(new Runnable() {
			public void run() {
				if (! valid) return;
				synchronized (FormDisplay.UI_LOCK) {
					if (key == FIRE) {
						final FormCommandListener listener = focusItem.getItemCommandListener();
						final FormItem item = focusItem;
						final Command command = focusCommand;
						if (listener != null && item != null && command != null) {
							display.runAsync(new Runnable() {
								public void run() {
									listener.commandAction(command, item);	
								}
							});							
						}
					}  else {		
						int[] transform = new int[4];
						if (key == DOWN || key == LEFT || key == RIGHT || key == UP) {
							int code = 0;
							switch (key) {
								case DOWN: code = FormItem.KEY_DOWN; break;
								case UP: code = FormItem.KEY_UP; break;
								case LEFT: code = FormItem.KEY_LEFT; break;
								case RIGHT: code = FormItem.KEY_RIGHT; break;
								default:
									return;
							}
							int index = -1;
							if (focusItem != null) {
								boolean keepfocus = focusItem.traverse(code, size[0], size[1], transform);
								if (! keepfocus) {
									if (key == UP) {
										index = focusPrevious(transform);
									} else if (key == DOWN) {
										index = focusNext(transform);
									}
								} else {
									index = getFocusIndex();
								}
							}  else {
								if (key == UP) {
									index = focusPrevious(transform);
								} else if (key == DOWN) {
									index = focusNext(transform);	
								}
							}
							// logic begins only when the screen is too small
							if (index != -1 && contentHeight > size[1]) {
								// check whether the item is completely contained on screen
								int[] location = (int[])locations.elementAt(index);
								int y1 = location[1] - viewport[1];
								int y2 = location[3] + y1;
								if ( y1 < 0 ||  y2 > size[1]) {
									int shift = location[1];
									String l = focusItem.getLabel();
									if (l != null && l.length() > 0) {
										shift += labelHeight + BORDER_SPACING + BORDER_SPACING / 2;
									}
									// center the desired focus point on the screen
									shift += transform[1];
									viewport[1] = shift - (size[1] / 2);
									if (viewport[1] < 0) {
										viewport[1] = 0;
									} else if (viewport[1] + (size[1] / 2) > contentHeight) {
										viewport[1] = contentHeight - size[1];
									}
								} else {
									// keep view port
								}
							} else {
								// keep view port
							}
							repaint();
						} else {
							return;
						}
						// apply the new transform for the new focus item
					}
				}
			
				
			}
		});
	}

	/**
	 * Focuses the next focus item and returns the index of
	 * the item that has been selected as well as the transform
	 * in out parameter.
	 * 
	 * @param transform The transform in out parameter.
	 * @return The index of the selected item or -1 if none is
	 * 	selected.
	 */
	private int focusNext(int[] transform) {
		int start = getFocusIndex() + 1;
		int next = -1;
		for (int i = start, s = items.size(); i < s; i++) {
			FormItem item = (FormItem)items.elementAt(i);
			if (item.traverse(FormItem.KEY_NONE, size[0], size[1], transform)) {
				next = i;
				break;
			}
		}
		setFocusItem(next);
		return next;
	}
	
	/**
	 * Focuses the previous focus item and returns the index
	 * of the item that has been selected as well as the transform
	 * in out parameter.
	 * 
	 * @param transform The transform in out parameter.
	 * @return The index of the item or -1 if none is selected.
	 */
	private int focusPrevious(int[] transform) {
		int start = items.size() - 1;
		if (focusItem != null) {
			start = getFocusIndex() - 1;	
		}
		int next = -1;
		for (int i = start; i >= 0; i--) {
			FormItem item = (FormItem)items.elementAt(i);
			if (item.traverse(FormItem.KEY_NONE, size[0], size[1], transform)) {
				next = i;
				break;
			}
		}
		setFocusItem(next);
		return next;
	}
	
	/**
	 * Determines the item index of the current focus item and
	 * returns it. If no item is focused, this method will return
	 * -1.
	 * 
	 * @return The index of the current focus item or -1 if there
	 * 	is no focused item.
	 */
	private int getFocusIndex() {
		int index = -1;
		if (focusItem != null) {
			if (focusItem != null) {
				for (int i = items.size() - 1; i >= 0; i--) {
					if (focusItem == items.elementAt(i)) {
						index = i;
						break;
					}
				}
			}
		}
		return index;
	}
	
	/**
	 * Sets the specified item as new focus item, thereby cleaning
	 * all mess left behind from the last focused item.
	 * 
	 * @param index The index of the item to set or -1 if the new
	 * 	item is null.
	 */
	private void setFocusItem(int index) {
		if (focusItem != null) {
			// clear the current focus item
			while (focusCommands.size() > 0) {
				Command command = (Command)focusCommands.elementAt(0);
				super.removeCommand(command);
				focusCommands.removeElementAt(0);
			}
			focusItem.traverseOut();
			focusCommand = null;
			focusItem = null;
		}	
		if (index != -1) {
			focusItem = (FormItem)items.elementAt(index);
			focusCommand = focusItem.getDefaultCommand();
			Command[] cs = focusItem.getCommands();
			for (int i = 0, s = cs.length; i < s; i++) {
				focusCommands.addElement(cs[i]);
				super.addCommand(cs[i]);
			}
		} 
	}
}
