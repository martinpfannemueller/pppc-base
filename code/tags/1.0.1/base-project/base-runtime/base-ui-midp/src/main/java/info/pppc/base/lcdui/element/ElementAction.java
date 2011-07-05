package info.pppc.base.lcdui.element;

/**
 * The element action is the base class for all actions
 * that are shown in the menu of the mobile phone whenever
 * the element is displayed.
 * 
 * @author Marcus Handte
 */
public class ElementAction {

	/**
	 * The label of the action that is shown in the user interface.
	 */
	private String label;
	
	/**
	 * The enabled flag that indicates whether the action is enabled.
	 */
	private boolean enabled;
	
	/**
	 * Creates a new enabled action with the specified label. 
	 * 
	 * @param label The label of the action that is shown in the
	 * 	user interface.
	 */
	public ElementAction(String label) {
		this(label, true);
	}
	
	/**
	 * Creates a new action with the specified label and the
	 * specified enabled state.
	 * 
	 * @param label The label of the action as shown in the ui.
	 * @param enabled The enabled state of the action.
	 */
	public ElementAction(String label, boolean enabled) {
		this.label = label;
		this.enabled = enabled;
	}
	
	/**
	 * Returns the label of the action.
	 * 
	 * @return The label of the action.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Returns a flag that indicates whether the action is enabled. 
	 * 
	 * @return A flag that indicates whether the action is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Sets the enabled flag of the action. If an action is disabled,
	 * it will not be executed.
	 * 
	 * @param enabled True if the action is enabled, false if the
	 * 	action is disabled.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * Called whenever the action should be executed. A subclass 
	 * can either overwrite this method or the run method without
	 * manager and element if it does not need to have this information.
	 * 
	 * @param manager The manager that called the action.
	 * @param element The element that was shown when the action
	 * 	was called.
	 */
	public void run(IElementManager manager, AbstractElement element) {
		run();
	}
	
	/**
	 * Called whenever the action should be executed.
	 */
	public void run() {
		// overwrite in a subclass.
	}
	
}
