package info.pppc.base.lcdui.element;

import info.pppc.base.lcdui.form.FormItem;

import javax.microedition.lcdui.Image;

/**
 * The abstract element is the base class for all elements managed by
 * an element manager. 
 * 
 * @author Marcus Handte
 */
public abstract class AbstractElement {

	/**
	 * The manager of the element.
	 */
	private IElementManager manager;
	
	/**
	 * Creates a new element with the specified manager.
	 * 
	 * @param manager The manager of the element.
	 */
	public AbstractElement(IElementManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Returns a reference to the manager of the element.
	 * 
	 * @return The manager of the element.
	 */
	public IElementManager getManager() {
		return manager;
	}
	
	/**
	 * Returns the name of the element. This name is displayed in
	 * the user interface.
	 * 
	 * @return The name of the element.
	 */
	public abstract String getName();
	
	/**
	 * Returns the image of the element. This image is displayed in
	 * the user interface.
	 * 
	 * @return the image of the user interface.
	 */
	public abstract Image getImage();
	
	/**
	 * Returns the actions that this element contributes to the 
	 * menu.
	 * 
	 * @return The actions that this element contributes to the
	 * 	menu.
	 */
	public ElementAction[] getActions() {
		return new ElementAction[0];
	}
	
	/**
	 * Returns the items provided by the element in the order
	 * in which they should be displayed.
	 * 
	 * @return The items provided by the element.
	 */
	public abstract FormItem[] getItems();
	
	/**
	 * Called whenever the control is removed from the manager.
	 * Elements should overwrite this method if they are interested
	 * in performing cleanups, such as unregistering listeners, etc.
	 */
	public void dispose() { }
	
}
