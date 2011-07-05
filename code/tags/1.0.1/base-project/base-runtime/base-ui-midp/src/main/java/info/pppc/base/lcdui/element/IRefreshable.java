package info.pppc.base.lcdui.element;

/**
 * The refreshable interface is used to simplify the development of refreshable
 * element controls. It is used by the refresh action to refresh and redraw
 * elements that change over time.
 * 
 * @author Marcus Handte
 */
public interface IRefreshable {

	/**
	 * Signals that the element should be refreshed.
	 */
	public void refresh();

}
