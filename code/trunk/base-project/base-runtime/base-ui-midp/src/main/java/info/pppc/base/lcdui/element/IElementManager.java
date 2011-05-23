package info.pppc.base.lcdui.element;

import info.pppc.base.system.operation.IOperation;

/**
 * The element manager interface is used to support different managers for elements. 
 * The default manager is the application elements that contain other elements are 
 * free to provide this interface to support more specific behavior.
 * 
 * @author Marcus Handte
 */
public interface IElementManager {

	/**
	 * Returns the currently registered elements in the order in
	 * which they have been registered.
	 * 
	 * @return The currently registered elements.
	 */
	public AbstractElement[] getElements();

	/**
	 * Returns the first element that has the specified name.
	 * 
	 * @param name The name to lookup.
	 * @return The first element that has the specified name. This
	 * 	will be the element that has been registered first with the
	 * 	specified name.
	 */
	public AbstractElement getElement(String name);
	
	/**
	 * Sets the focus to the specified element. Note that an element
	 * should never call this by itself. This is solely intended for
	 * communicating elements, i.e. if the focus of one element is
	 * passed to another element.
	 * 
	 * @param element The element to focus on.
	 */
	public void focusElement(AbstractElement element);
	
	/**
	 * This method adds the specified element to the set of registered
	 * elements. This method is intended for elements that have 
	 * temporary subviews.
	 * 
	 * @param element The element to add to the view.
	 * @return True if the addition was successful, false otherwise. An
	 * 	addition is unsuccessful if the element name is empty or if the
	 * 	element is already registered.
	 */
	public boolean addElement(AbstractElement element);
	
	/**
	 * This method removes a previously registered element from the view.
	 * Note that this method should only be called on element that have
	 * been registered by the element that has added the element. 
	 * 
	 * @param element The element to remove.
	 * @return True if the element has been removed, false if the element
	 * 	has not been registered.
	 */
	public boolean removeElement(AbstractElement element);
	
	/**
	 * This method updates the view menu shown in the menu bar with the
	 * actions of the currently active element. This method should be
	 * called whenever the set of actions that should be shown in the
	 * menu bar of the element has changed. 
	 * 
	 * @param element The element that wants to be updated. If the element
	 * 	is not the active element that is currently shown, then the update
	 * 	is ignored.
	 */
	public void updateElement(AbstractElement element);	
	
	/**
	 * Runs an operation ui modal and presents a status bar that shows
	 * the progress. The status bar can be controlled using the monitor
	 * that is passed into the operation.
	 * 
	 * @param operation The operation that should be executed.
	 * @param cancel Sets a flag that indicates whether the operation can
	 * 	be aborted by the user. True if the operation can be aborted, false
	 * 	otherwise.
	 * @throws InterruptedException Thrown if the thread cannot wait for
	 * 	the operation to finish. 
	 */
	public void run(IOperation operation, boolean cancel) throws InterruptedException;
	
	/**
	 * Runs the specified runnable in the gui thread whenever the
	 * gui thread enters the dispatch loop again. This can be 
	 * used to create animations. This method returns immediately.
	 * 
	 * @param runnable The runnable to execute.
	 */
	public void run(Runnable runnable);
	
	/**
	 * Returns the display with of the device.
	 * 
	 * @return The display with of the device.
	 */
	public int getDisplayWidth();
	
	/**
	 * Returns the display height of the device.
	 * 
	 * @return The display height of the device.
	 */
	public int getDisplayHeight();
	
}
