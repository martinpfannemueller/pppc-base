package info.pppc.base.system.event;

/**
 * The basic interface for all listeners used by BASE. A listener
 * is some class that receives handle event calls that are 
 * associated with some event. This interface can also be used by
 * application developers. Using this enables developers to reuse
 * the listener bundle implementation which provides basic event
 * support.
 * 
 * @author Marcus Handte
 */
public interface IListener {

	/**
	 * Called by some event source whenever an event occurs.
	 * 
	 * @param event The event that has occurred.
	 */
	public void handleEvent(Event event);

}
