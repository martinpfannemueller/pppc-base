package info.pppc.base.lcdui.console;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.form.FormDisplay;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.form.FormStyle;

import javax.microedition.lcdui.Graphics;

/**
 * The console is a custom item that enables a user to view the output generated
 * by base.
 * 
 * @author Marcus Handte
 */
public class ConsoleItem extends FormItem {

	/**
	 * The title of the console as shown in the ui.
	 */
	private static final String UI_TITLE = "info.pppc.base.lcdui.console.ConsoleItem.TITLE";
	

	/**
	 * The number of lines that will be stored in the console.
	 */
	public static final int LINES_MAXIMUM = 50;
	
	/**
	 * The default number of lines that will be shown in the console.
	 */
	public static final int LINES_DEFAULT = 5;
	
	/**
	 * The lines that are currently contained in the console.
	 */
	private int lineCount = 0;

	/**
	 * The lines that should be shown. 
	 */
	private String[] lineTexts = new String[LINES_MAXIMUM];
	
	/**
	 * The widths of the lines that are currently displayed.
	 */
	private int[] lineWidths = new int[LINES_MAXIMUM];
	
	/**
	 * The x offset from the upper left corner of what is currently displayed in the
	 * console.
	 */
	private int x = 0;
	
	/**
	 * The y offset from the upper left corner of what is currently displyed in the
	 * console.
	 */
	private int y = 0;
	
	/**
	 * The number of visible lines that are shown to the user.
	 */
	private int lines = LINES_DEFAULT;
	
	/**
	 * A flag that indicates whether this component already has the focus.
	 */
	private boolean focus = false;
	
	/**
	 * A reference to the element manager that uses the console item.
	 */
	private IElementManager manager;
	
	/**
	 * The form style of the item.
	 */
	private FormStyle style;
	
	/**
	 * Creates a new console using the specified element manager.
	 * 
	 * @param manager The element manager that uses the console.
	 */
	public ConsoleItem(IElementManager manager) {
		super(BaseUI.getText(UI_TITLE));
		this.manager = manager;
		this.style = FormStyle.getStyle();
		setLayout(LAYOUT_EXPAND | LAYOUT_LINE_AFTER | LAYOUT_LINE_BEFORE);
	}

	/**
	 * Returns the minimum width. The console always requests the full
	 * screen width. 
	 * 
	 * @return The minimum width.
	 */
	public int getMinimumWidth() {
		return manager.getDisplayWidth() - 2;
	}

	/**
	 * Returns the minimum height. The console always requests the line
	 * count specified as lines.
	 *
	 * @return The minimum height.
	 */
	public int getMinimumHeight() {
		return style.FONT_ITEM.getHeight() * lines;
	}

	/**
	 * Called whenever the console needs to be painted.
	 * 
	 * @param graphics The graphics object used to draw the console.
	 * @param w The width of the item.
	 * @param h The height of the item.
	 */
	public void paint(Graphics graphics, int w, int h) {
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_BACKGROUND);
		} else {
			graphics.setColor(style.COLOR_BACKGROUND);	
		}
		graphics.fillRect(0, 0, w, h);
		graphics.translate(-x, 0);
		if (focus) {
			graphics.setColor(style.COLOR_ACTIVE_FOREGROUND);
		} else {
			graphics.setColor(style.COLOR_FOREGROUND);
		}
		graphics.setFont(style.FONT_ITEM);
		for (int i = computeLineOffset(), ypos = 0; i < lineCount; i ++) {
			String line = lineTexts[i];
			if (focus && i == y) {
				graphics.setFont(style.FONT_ACTIVE_ITEM);
				graphics.drawString(line, 0, ypos, Graphics.TOP | Graphics.LEFT);
				graphics.setFont(style.FONT_ITEM);
			} else {
				graphics.drawString(line, 0, ypos, Graphics.TOP | Graphics.LEFT);	
			}
			ypos += style.FONT_ITEM.getHeight();
		}
	}
	
	/**
	 * Computes the current line offset based on the selected line
	 * and the lines that are displayed.
	 * 
	 * @return The current line offset based on the selected line and
	 * 	the displayed lines.
	 */
	private int computeLineOffset() {
		int offset = 0;
		if (lineCount - lines > 0) {
			if (y + lines < lineCount) {
				offset = y;
			} else {
				offset = lineCount - lines;
			}
		}
		return offset;
	}
	
	/**
	 * Adds a lines to the console. If the console's line maximum has
	 * been reached, the first row will be replaced (fifo).
	 * 
	 * @param string The string that makes up the line.
	 */
	public void addLine(String string) {
		synchronized (FormDisplay.UI_LOCK) {
			if (lineCount != LINES_MAXIMUM) {
				lineTexts[lineCount] = string;
				lineWidths[lineCount] = style.FONT_ITEM.stringWidth(string);
				lineCount += 1;
			} else {
				System.arraycopy(lineTexts, 1, lineTexts, 0, LINES_MAXIMUM - 1);
				System.arraycopy(lineWidths, 1, lineWidths, 0, LINES_MAXIMUM - 1);
				lineTexts[LINES_MAXIMUM - 1] = string;
				lineWidths[LINES_MAXIMUM - 1] = style.FONT_ITEM.stringWidth(string);
			}
			repaint(); 		
		}
	}
	
	/**
	 * Sets the number of lines that should be visible. If the number of 
	 * lines exceeds the maximum lines, the method will throw an illegal
	 * argument exception.
	 * 
	 * @param lines The lines that should be visible.
	 * @throws IllegalArgumentException Thrown if the lines exceed the
	 * 	maximum line count of the console.
	 */
	public void setLines(int lines) {
		if (lines > LINES_MAXIMUM) 
			throw new IllegalArgumentException("Lines exceed maximum.");
		this.lines = lines;
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
	 * Called whenever a focus event reaches the console item. The console
	 * item will consume the key event if the ends are not reached.
	 * 
	 * @param code The key code of the traversal event (up,down,left,right,none).
	 * @param w The width of the item.
	 * @param h The height of the item.
	 * @param visible The part of the item that should be made visible.
	 * @return True if the focus should be keept, false otherwise.
	 */
	public boolean traverse(int code, int w, int h, int[] visible) {
		boolean keepfocus = true;
		if (focus) {
			switch (code) {	
				case KEY_LEFT:
					x -= manager.getDisplayWidth() / 2;
					if (x < 0) x = 0;
					break;
				case KEY_RIGHT:
					int wmax = 0;
					for (int i = 0; i < lineCount; i++) {
						if (lineWidths[i] > wmax) wmax = lineWidths[i];
					}
					x += manager.getDisplayWidth() / 2;
					if (x > wmax) x = wmax;
					break;
				case KEY_UP:
					y -= 1;
					if (y < 0) {
						y = 0;
						keepfocus = false;
					}
					break;
				case KEY_DOWN:
					y += 1;
					if (y >= lineCount) {
						y = lineCount - 1;
						keepfocus = false;
					}
					break;
				default:
					// fall through
			}
		} else {
			focus = true;
		}
		
		int offset = (y - computeLineOffset()) * style.FONT_ITEM.getHeight();
		visible[0] = 0;
		visible[1] = offset;
		visible[2] = 0;
		visible[3] = style.FONT_ITEM.getHeight();
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
	
}
