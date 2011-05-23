package info.pppc.base.swtui.element;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * The element manager enables element controls to access other element
 * controls directly. This access can be used to enable communication
 * between different views.
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
	public AbstractElementControl[] getElements();

	/**
	 * Returns the first element that has the specified name.
	 * 
	 * @param name The name to lookup.
	 * @return The first element that has the specified name. This
	 * 	will be the element that has been registered first with the
	 * 	specified name.
	 */
	public AbstractElementControl getElement(String name);
	
	/**
	 * Sets the focus to the specified element. Note that an element
	 * should never call this by itself. This is solely intended for
	 * communicating elements, i.e. if the focus of one element is
	 * passed to another element.
	 * 
	 * @param element The element to focus on.
	 */
	public void focusElement(AbstractElementControl element);
	
	/**
	 * This method adds the specified element to the set of registered
	 * elements. This method is intended for elements that have 
	 * temporary sub views.
	 * 
	 * @param element The element to add to the view.
	 * @return True if the addition was successful, false otherwise. An
	 * 	addition is unsuccessful if the element name is empty or if the
	 * 	element is already registered.
	 */
	public boolean addElement(AbstractElementControl element);
	
	/**
	 * This method removes a previously registered element from the view.
	 * Note that this method should only be called on element that have
	 * been registered by the element that has added the element. 
	 * 
	 * @param element The element to remove.
	 * @return True if the element has been removed, false if the element
	 * 	has not been registered.
	 */
	public boolean removeElement(AbstractElementControl element);
	
	/**
	 * This method updates the view menu shown in the menu bar with the
	 * actions of the currently active element. This method should be
	 * called whenever the set of actions that should be shown in the
	 * menu bar of the element has changed. You should not call this 
	 * method if your element is not active. Note that this method
	 * can be automatically called by the element listener if you register
	 * it for a control that changes its selection.
	 */
	public void updateElement();

	/**
	 * Executes the specified runnable synchronously within the gui
	 * thread.
	 * 
	 * @param runable The runnable to execute.
	 */
	public void run(Runnable runable);

	/**
	 * Runs a runnable with a progress indicator. The flag indicates
	 * whether a cancel button is displayed. The runnable is executed
	 * in a child thread that is forked. The method returns when
	 * the child stopped executing. Note that this method must be
	 * called from the gui thread (this can be ensured by using the
	 * run method above).
	 * 
	 * @param runnable The runnable to execute.
	 * @param cancel Determines whether the runnable can be canceled.
	 * @throws InvocationTargetException Thrown by the invocation
	 * 	target.
	 * @throws InterruptedException Thrown if the thread is interrupted.
	 */
	public void run(IRunnableWithProgress runnable, boolean cancel)
		throws InvocationTargetException, InterruptedException;

}
