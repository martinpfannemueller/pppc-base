package info.pppc.base.system.event;

/**
 * An event is an object with a source (its creator), some user specified data
 * that is event specific, a type (that is typically globally known). There are
 * two types of events: undoable events and non undoable events. Undoable events
 * support an undo operation that will lead to a roll back. Non undoable events 
 * will be performed no matter what status the system is in. The semantic of a
 * specific event depends on the event source. This includes the point in time
 * in which an event is reported (i.e. whether is reported synchronously, 
 * asynchronously, before the change or after the change). This means that users
 * should read the comments.
 * Note that an event type should be modeled by a single bit (although it is
 * possible to use multiple bit masks to denote one event, it is discouraged 
 * in general).
 * 
 * @author Marcus Handte
 */
public class Event {

	/**
	 * A constant that denotes no type. This constant is the default value
	 * for an event.
	 */
	public static int EVENT_NOTHING    = 0;
	
	/**
	 * A constant that denotes all types. This constant can be used to subscribe
	 * to all event types.
	 */
	public static int EVENT_EVERYTHING = ~0;	
	
	/**
	 * The type of the event.
	 */
	private int type = 0;

	/**
	 * The source of the event.
	 */
	private Object source = null;
	
	/**
	 * Some user specified data that is propagated by the event.
	 */
	private Object data = null;
	
	/**
	 * A flag that indicates whether this is an undo event.
	 */
	private boolean undo = false;
	
	/**
	 * A flag that indicates whether the event can be undone.
	 */
	private boolean undoable = false;

	/**
	 * Creates a new event with the specified type, source and user data.
	 * The undoable flag determines whether the event can be undone.
	 * 
	 * @param type The type of the event. It is convention that an event
	 *  type should only contain one active bit.
	 * @param source The source of the event.
	 * @param data The data of the event.
	 * @param undoable A flag that indicates whether the 
	 * 	event can be aborted.
	 */
	public Event(int type, Object source, Object data, boolean undoable) {
		this.type = type;
		this.source = source;
		this.data = data;
		this.undoable = undoable;
	}
	
	/**
	 * Returns the type of the event as an integer constant.
	 * 
	 * @return The type of the event.
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Determines whether the event is also of the specified type, i.e.
	 * if the bitmask to test is completely contained in the bitmask
	 * that is used by the event.
	 * 
	 * @param t The type to test for.
	 * @return True if the type is completely contained in the specified
	 * 	bitmask type definition.
	 */
	public boolean isType(int t) {
		return ((t & type) == type);
	}
	
	/**
	 * Returns the source object that created the event.
	 * 
	 * @return The source object that created the event.
	 */
	public Object getSource() {
		return source;
	}
	
	/**
	 * Returns the user data object contained in the event.
	 * 
	 * @return The user data object of the event.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Determines whether the specified event is undoable. Returns true
	 * if the event can be undone and false if it cannot. If it cannot be
	 * undone, it is illegal to set the perform flag.
	 * 
	 * @return True if the event can be undone, false if it cannot.
	 */
	public boolean isUndoable() {
		return undoable;
	}

	/**
	 * Determines whether this is an undo event. An undo event is created
	 * from an undoable event when the event is undone by some listener.
	 * At that point an undo event will be created and typically it will be
	 * sent to all listeners that have already processed the event.
	 * 
	 * @return Determines whether this is the undo operation of some event
	 * 	that has been created and processed earlier.
	 */
	public boolean isUndo() {
		return undo;
	}

	/**
	 * Cancels the event. A typical event source will inform all listeners that
	 * have already been notified that this event should be canceled.
	 * 
	 * @throws RuntimeException Thrown if the event is not undoable or if
	 * 	the event is an undo event.
	 */
	public void undo() {
		if (undoable) {
			if (!undo) {
				undo = true;	
			} else {
				throw new RuntimeException("Illegal perform state for an undo event.");	
			}
		} else {
			throw new RuntimeException("The event does not support undo.");
		}
	}
	
	/**
	 * Returns a human readable string representation of this event.
	 * 
	 * @return A human readable string representation of the event.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("TYPE <");
		b.append(type);
		b.append("> UNDOABLE <");
		b.append((undoable)?"YES":"NO");
		b.append("> UNDO <");
		b.append((undo)?"YES":"NO");
		b.append("> SOURCE <");
		b.append((source == null)?"NULL":source.toString());
		b.append("> DATA <");
		b.append((data == null)?"NULL":data.toString());
		b.append(">");
		return b.toString();
	}
	
}
