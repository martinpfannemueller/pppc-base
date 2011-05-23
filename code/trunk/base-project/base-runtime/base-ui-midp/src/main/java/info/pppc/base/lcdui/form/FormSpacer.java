package info.pppc.base.lcdui.form;

/**
 * A simple spacer that can be configured with a certain height and or width.
 * 
 * @author Marcus Handte
 */
public class FormSpacer extends FormItem {

	/**
	 * The width of the spacer.
	 */
	private int width;
	
	/**
	 * The height of the spacer.
	 */
	private int height;
	
	/**
	 * Creates a new spacer with the specified width and height.
	 * 
	 * @param width The width of the spacer.
	 * @param height The height of the spacer.
	 */
	public FormSpacer(int width, int height) {
		super(null);
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Returns the minimum height of the spacer.
	 * 
	 * @return The height.
	 */
	public int getMinimumHeight() {
		return height;
	}
	
	/**
	 * Returns the minimum width of the spacer.
	 * 
	 * @return The width of the spacer.
	 */
	public int getMinimumWidth() {
		return width;
	}
	
}
