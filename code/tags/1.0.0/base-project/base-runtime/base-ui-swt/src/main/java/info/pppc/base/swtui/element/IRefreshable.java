package info.pppc.base.swtui.element;

/**
 * The refreshable interface is used to simplify the development of refreshable
 * element controls. It is used by the refresh action and the element manager.
 * Whenever the element manager focuses on the element control, it determines
 * whether the element control is refreshable if it is refreshable it calls the
 * refresh method.
 * 
 * @author Marcus Handte
 */
public interface IRefreshable {

	/**
	 * Signals that the element should be refreshed.
	 */
	public void refresh();

}
