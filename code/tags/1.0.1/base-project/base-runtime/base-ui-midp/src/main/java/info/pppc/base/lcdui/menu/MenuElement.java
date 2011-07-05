package info.pppc.base.lcdui.menu;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.AbstractElement;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.lcdui.element.IElementManager;
import info.pppc.base.lcdui.form.FormCommandListener;
import info.pppc.base.lcdui.form.FormImageItem;
import info.pppc.base.lcdui.form.FormItem;
import info.pppc.base.lcdui.form.FormItemGroup;
import info.pppc.base.lcdui.form.FormStringItem;

/**
 * The menu element manages a menu for a list of elements.
 * 
 * @author Marcus Handte
 */
public class MenuElement extends AbstractElement implements FormCommandListener {

	/**
	 * The key for the title of the menu.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.menu.MenuElement.TEXT";

	/**
	 * The key for the select command text.
	 */
	private static final String UI_SELECT = "info.pppc.base.lcdui.menu.MenuElement.SELECT";
	
	/**
	 * The entries contained in the menu.
	 */
	private Vector entries = new Vector();

	/**
	 * The actions that have been added externally.
	 */
	private Vector actions = new Vector();
	
	/**
	 * The default command for each menu entry (select and focus).
	 */
	private Command select = new Command(BaseUI.getText(UI_SELECT), Command.ITEM, 1);
	
	/**
	 * Creates a new menu element.
	 * 
	 * @param manager The manager of the element.
	 */
	public MenuElement(IElementManager manager) {
		super(manager);
	}

	/**
	 * Returns the name of the menu.
	 * 
	 * @return The name of the menu.
	 */
	public String getName() {
		return BaseUI.getText(UI_TEXT);
	}

	/**
	 * Returns the image of the menu.
	 * 
	 * @return The image of the menu.
	 */
	public Image getImage() {
		return BaseUI.getImage(BaseUI.IMAGE_LOGO);
	}

	/**
	 * Adds a menu entry for the specified element to the menu.
	 * 
	 * @param element The element whose entry should be added.
	 */
	public void addElement(AbstractElement element) {
		String name = element.getName();
		Image image = element.getImage();
		if (image == null) image = BaseUI.getImage(BaseUI.IMAGE_LOGO);
		FormStringItem stringItem = new FormStringItem(null, name);
		FormImageItem imageItem = new FormImageItem(null, image);
		FormItemGroup group = new FormItemGroup("", imageItem, 
			stringItem, FormItemGroup.POSITION_HORIZONTAL);
		group.setLayout(FormItem.LAYOUT_CENTER | FormItem.LAYOUT_VTOP);
		group.addCommand(select);
		group.setDefaultCommand(select);
		group.setEnabled(true);
		group.setItemCommandListener(this);
		MenuEntry entry = new MenuEntry(element, group);
		entries.addElement(entry);
	}
	
	/**
	 * Removes the specified element from the menu.
	 * 
	 * @param element The element that should be removed.
	 * @return True if the element has been removed, false otherwise.
	 */
	public boolean removeElement(AbstractElement element) {
		for (int i = 0; i < entries.size(); i++) {
			MenuEntry entry = (MenuEntry)entries.elementAt(i);
			if (entry.getElement() == element) {
				entries.removeElement(entry);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the items of the menu.
	 * 
	 * @return The items of the menu.
	 */
	public FormItem[] getItems() {
		FormItem[] items = new FormItem[entries.size()];
		for (int i = entries.size() - 1; i >=0; i--) {
			MenuEntry entry = (MenuEntry)entries.elementAt(i);
			items[i] = entry.getItem();
		}
		return items;
	}
	
	/**
	 * Called whenever a command on some item has been executed.
	 * 
	 * @param command The command that has been executed.
	 * @param item The item that executed the command.
	 */
	public void commandAction(Command command, FormItem item) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			final MenuEntry entry = (MenuEntry)entries.elementAt(i);
			if (entry.getItem() == item && command == select) {
				getManager().focusElement(entry.getElement());
				return;
			}
		}
	}
	
	/**
	 * Adds the specified action to the set of menu actions.
	 * 
	 * @param action The action that should be added.
	 */
	public void addAction(ElementAction action) {
		if (action != null && ! actions.contains(action)) {
			actions.addElement(action);
		}
	}
	
	/**
	 * Removes a previously added action from the set of menu actions.
	 * 
	 * @param action The action that should be removed.
	 * @return True if the action has been removed, false otherwise.
	 */
	public boolean removeAction(ElementAction action) {
		if (action != null) {
			return actions.removeElement(action);
		}
		return false;
	}
	
	/**
	 * Returns the actions that are available while the menu
	 * is displayed. These are the actions that are added to
	 * the application using the add action method that have
	 * not been removed, yet.
	 * 
	 * @return The actions that are currently available.
	 */
	public ElementAction[] getActions() {
		ElementAction[] a = new ElementAction[actions.size()];
		for (int i = actions.size() - 1; i >= 0; i--) {
			a[i] = (ElementAction)actions.elementAt(i);
		}
		return a;
	}
}
