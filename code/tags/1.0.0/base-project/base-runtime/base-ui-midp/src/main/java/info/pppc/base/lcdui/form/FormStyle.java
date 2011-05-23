package info.pppc.base.lcdui.form;

import javax.microedition.lcdui.Font;

/**
 * The form color class provides an easy way to customize
 * the look and feel of the form ui package.
 * 
 * @author Marcus Handte
 */
public class FormStyle {

	// Eclipse SWT color definitions
	//	white      =  255, 255, 255
	//	lightGray  =  192, 192, 192
	//	gray       =  128, 128, 128
	//	darkGray   =   64,  64,  64
	//	black      =    0,   0,   0
	//	red        =  255,   0,   0
	//	orange     =  255, 196,   0
	//	yellow     =  255, 255,   0
	//	green      =    0, 255,   0
	//	lightGreen =   96, 255,  96
	//	darkGreen  =    0, 127,   0
	//	cyan       =    0, 255, 255
	//	lightBlue  =  127, 127, 255
	//	blue       =    0,   0, 255
	//	darkBlue   =    0,   0, 127

	/**
	 * The system specific form style.
	 */
	private static FormStyle style;
	
	/**
	 * The background color for things that are not active.
	 */
	public final int COLOR_BACKGROUND;
	
	/**
	 * The foreground color for things that are not active.
	 */
	public final int COLOR_FOREGROUND;
	
	/**
	 * The color for non-active borders.
	 */
	public final int COLOR_BORDER;
	
	/**
	 * The background color for active things.
	 */
	public final int COLOR_ACTIVE_BACKGROUND;
	
	/**
	 * The foreground color for active things.
	 */
	public final int COLOR_ACTIVE_FOREGROUND;

	/**
	 * The border color for non-active things.
	 */
	public final int COLOR_ACTIVE_BORDER;
	
	/**
	 * The label background color.
	 */
	public final int COLOR_LABEL_BACKGROUND;
	
	/**
	 * The label foreground color.
	 */
	public final int COLOR_LABEL_FOREGROUND;
	
	/**
	 * The default font for items.
	 */
	public final Font FONT_ITEM;
	
	/**
	 * The default font for active items.
	 */
	public final Font FONT_ACTIVE_ITEM;
	
	/**
	 * The default font for labels.
	 */
	public final Font FONT_LABEL;
	
	/**
	 * Creates a new form style using the specified default values.
	 * 
	 * @param background The background color.
	 * @param foreground The foreground color.
	 * @param border The border color.
	 * @param hbackground The active background color.
	 * @param hforeground The active foreground color.
	 * @param hborder The active border color.
	 * @param lbackground The label background color.
	 * @param lforeground The label foreground color.
	 * @param item The item font.
	 * @param hitem The active item font.
	 * @param label The label font.
	 */
	public FormStyle(int background, int foreground, int border, 
			int hbackground, int hforeground, int hborder, int lbackground, int lforeground, 
			Font item, Font hitem, Font label) {
		COLOR_BACKGROUND = background;
		COLOR_FOREGROUND = foreground;
		COLOR_BORDER = border;
		COLOR_ACTIVE_BACKGROUND = hbackground;
		COLOR_ACTIVE_FOREGROUND = hforeground;
		COLOR_ACTIVE_BORDER = hborder;
		COLOR_LABEL_BACKGROUND = lbackground;
		COLOR_LABEL_FOREGROUND = lforeground;
		FONT_ITEM = item;
		FONT_ACTIVE_ITEM = hitem;
		FONT_LABEL = label;
	}
	
	/**
	 * Sets the form style to the specified style. Only new
	 * forms and items are affected by this method. 
	 * 
	 * @param newStyle The new style to set.
	 */
	public static void setStyle(FormStyle newStyle) {
		style = newStyle;
	}
	
	/**
	 * Retrieves the current form style. If no form style
	 * is set, a default new form style will be created.
	 * 
	 * @return The new form style to retrieve.
	 */
	public static FormStyle getStyle() {
		if (style == null) {
			Font labelFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			Font itemFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
			Font activeFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_UNDERLINED, Font.SIZE_MEDIUM);
			style = new FormStyle
				(0x00FFFFFF, 0x00000000, 0x00000000, 
					0x007F7FFF, 0x00FFFFFF, 0x007F7FFF, 
						0x0000007F, 0x00FFFFFF, itemFont, activeFont, labelFont);
		}
		return style;
	}	

}
