package info.pppc.base.lcdui.menu;

import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.form.FormItem;

/**
 * The menu entry is an abstract data structure that associates
 * an element with an item.
 * 
 * @author Marcus Handte
 */
public class MenuEntry {

	/**
	 * The element of the entry.
	 */
	private AbstractElement element;
	
	/**
	 * The item of the entry.
	 */
	private FormItem item;
	
	/**
	 * Creates a new menu entry with the specified element and
	 * item.
	 * 
	 * @param element The element represented by this entry.
	 * @param item The item that represents the element.
	 */
	public MenuEntry(AbstractElement element, FormItem item) {
		this.element = element;
		this.item = item;
	}
	
	/**
	 * Returns the element represented by the entry.
	 * 
	 * @return The element represented by the entry.
	 */
	public AbstractElement getElement() {
		return element;
	}
	
	/**
	 * Returns the item that represents the element.
	 * 
	 * @return The item of the entry that represents the
	 * 	element.
	 */
	public FormItem getItem() {
		return item;
	}

}
