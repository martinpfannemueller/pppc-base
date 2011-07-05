package info.pppc.base.system.event;

import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * A bundle is a helper class for managing bundles of listeners. Users can add
 * and remove listeners and use convenient messages to create and fire events.
 * This includes undo support and type management. Note that each listener can
 * only be registered once per event type.
 * 
 * @author Marcus Handte
 */
public final class ListenerBundle {

	/**
	 * The listeners that have been registered for some events.
	 */
	private Vector listeners = new Vector();

	/**
	 * The types for which the corresponding listener (i.e. the listener at the
	 * same index in the listener vector) is registered.
	 */
	private Vector types = new Vector();

	/**
	 * The default source object that will be passed to the event constructor
	 * in cases where the event is created using the convenience methods.
	 */
	private Object source = null;

	/**
	 * Creates a new bundle that does not have any registered listeners that
	 * uses the specified default source to create events.
	 * 
	 * @param source The source of the events used for events that are created
	 * 	with the convenience methods. It is legal to set the source to null
	 * 	and to use the generic fire event method.
	 */
	public ListenerBundle(Object source) {
		this.source = source;
	}
	
	/**
	 * Registers a specified listener for the specified types. Note that if the
	 * listener is already registered for one of the types it will not be registered
	 * for the type again. So each listener will be notified only once per type.
	 * 
	 * @param type The types of the events that the listener will receive.
	 * @param listener The listener to add.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public void addListener(int type, IListener listener) throws NullPointerException {
		if (type == Event.EVENT_NOTHING) return;
		if (listener != null) {
			synchronized (listeners) {
				int index = listeners.indexOf(listener);
				if (index != -1) {
					int registered = ((Integer)types.elementAt(index)).intValue();
					types.removeElementAt(index);
					types.addElement(new Integer(registered | type));
				} else {
					listeners.addElement(listener);
					types.addElement(new Integer(type));
				}
			}
		} else {
			throw new RuntimeException("Listener must not be null.");
		}
	}
	
	/**
	 * Removes the specified listener from the specified types. If the listener
	 * is no longer registered after the
	 * 
	 * @param type The types of the events that the listener will receive.
	 * @param listener The listener that will be removed.
	 * @return True if the listener is no longer registered for an event and thus
	 * 	it has been removed.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public boolean removeListener(int type, IListener listener) throws NullPointerException {
		if (type == Event.EVENT_NOTHING) return false;
		if (listener != null) {
			int index = listeners.indexOf(listener);
			if (index != -1) {
				// invert the types and change registration or unregister
				int registered = ((Integer)types.elementAt(index)).intValue();
				int newType = ~type & registered;
				if (newType == Event.EVENT_NOTHING) {
					types.removeElementAt(index);
					listeners.removeElementAt(index);
					return true;
				} else {
					types.removeElementAt(index);
					types.addElement(new Integer(newType));
					return false;
				}
			} else {
				return false;
			}
		} else {
			throw new RuntimeException("Listener must not be null.");
		}
	}
	
	/**
	 * Removes the specified listener from all registrations. This will free the
	 * listener and completely release it (if it is registered at this bundle).
	 * 
	 * @param listener The listener to remove.
	 * @return True if the listener has been removed, false otherwise.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public boolean removeListener(IListener listener) throws NullPointerException {
		if (listener != null) {
			int index = listeners.indexOf(listener);
			if (index != -1) {
				listeners.removeElementAt(index);
				types.removeElementAt(index);
				return true;
			} else {
				return false;
			}
		} else {
			throw new RuntimeException("Listener must not be null.");
		}
	}

	/**
	 * Returns the types for which a listener is registered. If the listener
	 * is not registered a TYPE_NOTHING will be returned.
	 * 
	 * @param listener The listener that has been registered.
	 * @return The types for which the listener is currently registered.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public int getTypes(IListener listener) throws NullPointerException {
		if (listener != null) {
			int index = listeners.indexOf(listener);
			if (index != -1) {
				return ((Integer)types.elementAt(index)).intValue();
			} else {
				return Event.EVENT_NOTHING;
			}
		} else {
			throw new RuntimeException("Listener must not be null.");
		}
	}

	/**
	 * Determines whether the specified listener is registered at this
	 * bundle.
	 * 
	 * @param listener The listener to lookup.
	 * @return True if the listener is registered, false if it is not
	 * 	registered.
	 * @throws NullPointerException Thrown if the listener is null.
	 */
	public boolean containsListener(IListener listener) throws NullPointerException {
		if (listener != null) {
			return listeners.contains(listener);	
		} else {
			throw new RuntimeException("Listener must not be null.");
		}
	}
	
	/**
	 * Removes all listeners from this bundle.
	 */
	public void clearListeners() {
		synchronized (listeners) {
			listeners.removeAllElements();
			types.removeAllElements();			
		}
	}

	/**
	 * Returns all listeners that are at least registered for the specified
	 * types. If the type query is nothing, no listener will be returned.
	 * 
	 * @param type The types to check.
	 * @return The listeners that are registered for all types specified
	 * 	by the query. 
	 */
	public IListener[] getListeners(int type) {
		if (type == Event.EVENT_NOTHING) {
			return new IListener[0];
		} else {
			Vector temp = new Vector();
			synchronized (listeners) {
				for (int i = 0, length = listeners.size(); i < length; i++) {
					int registered = ((Integer)types.elementAt(i)).intValue();
					if ((registered & type) == type) {
						temp.addElement(listeners.elementAt(i));	
					}
				}
			}
			IListener[] result = new IListener[temp.size()];
			for (int i = 0, length = temp.size(); i < length; i++) {
				result[i] = (IListener)temp.elementAt(i);
			}
			return result;
		}
	}
	
	/**
	 * Returns all listeners that are registered at this bundle. If this bundle
	 * does not contain any listener an array of size 0 will be returned.
	 * 
	 * @return An array that contains all listeners that are currently registered
	 * 	at this bundle.
	 */
	public IListener[] getListeners() {
		synchronized (listeners) {
			IListener[] result = new IListener[listeners.size()];
			for (int i = 0, length = result.length; i < length; i++) {
				result[i] = (IListener)listeners.elementAt(i);
			}
			return result;
		}
	}
	
	/**
	 * Sends the specified event to all listeners. If the event is undoable
	 * the method will return the undo status of the event after the listeners
	 * have been called. If an undoable operation is aborted by some listener
	 * all listeners that have already processed the event will receive the
	 * undo operation. The flag can be used to control the ordering.
	 * 
	 * @param event The event to send to all listeners.
	 * @param reverse A flag that indicates whether the listeners should be notified
	 * 	in the order in which they have been registered or in the reverse order.
	 *  True for reverse order, false for normal order.
	 * @return The status of the operation after it has been processed. This
	 * 	method will return true if the operation has not been aborted and
	 * 	false if the operation has been aborted.
	 * @throws NullPointerException Thrown if the event is null.
	 */
	public boolean fireEvent(Event event, boolean reverse) throws NullPointerException {
		if (event != null) {
			if (! event.isUndo()) {
				int type = event.getType();
				if (type == Event.EVENT_NOTHING) return true;
				Vector notify = new Vector(); 
				synchronized (listeners) {
					for (int i = 0, length = listeners.size(); i < length; i++) {
						int registered = ((Integer)types.elementAt(i)).intValue();
						if ((registered & type) == type) {
							if (reverse) {
								notify.insertElementAt(listeners.elementAt(i), 0);
							} else {
								notify.addElement(listeners.elementAt(i));	
							}
						}
					}
				}
				for (int i = 0, length = notify.size(); i < length; i++) {
					IListener listener = (IListener)notify.elementAt(i);
					try {
						listener.handleEvent(event);
					} catch (Throwable t) {
						Logging.error(getClass(), "Exception while delivering event.", t);	
					}
					// test whether undo has been called
					if (event.isUndo()) {
						// notify all previously notified listeners	
						for (int j = i - 1; j >= 0; j++) {
							listener.handleEvent(event);
						}
						// event has been aborted by some listener
						return false;
					}
				}
				// event delivered successfully
				return true;			
			} else {
				// event has been aborted before it has been delivered
				return false;
			}
		} else {
			throw new RuntimeException("Event must not be null.");
		}
	}

	/**
	 * Sends the specified event to all listeners. If the event is undoable
	 * the method will return the undo status of the event after the listeners
	 * have been called. If an undoable operation is aborted by some listener
	 * all listeners that have already processed the event will receive the
	 * undo operation.
	 * 
	 * @param event The event to send to all listeners.
	 * @return The status of the operation after it has been processed. This
	 * 	method will return true if the operation has not been aborted and
	 * 	false if the operation has been aborted.
	 * @throws NullPointerException Thrown if the event is null.
	 */
	public boolean fireEvent(Event event) throws NullPointerException {
		return fireEvent(event, false);
	}
		
	/**
	 * Fires an event that cannot be undone that is created from the default source
	 * with the specified type and without a data object. The event will be delivered
	 * to all listeners that are registered for this event.
	 * 
	 * @param type The type of the event to create and fire.
	 */
	public void fireEvent(int type) {
		fireEvent(type, false);
	}

	/**
	 * Fires an event that cannot be undone that is created from the default source
	 * with the specified type and without a data object. The event will be delivered
	 * to all listeners that are registered for this event.
	 * 
	 * @param type The type of the event to create and fire.
	 * @param reverse A flag that indicates whether the listeners should be notified
	 * 	in the order in which they have been registered or in the reverse order.
	 */
	public void fireEvent(int type, boolean reverse) {
		fireEvent(new Event(type, source, null, false), reverse);
	}
	
	/**
	 * Fires an event that cannot be undone that is created from the default source
	 * with the specified data object and type. The event will be delivered to all
	 * listeners that are registered for the event.
	 * 
	 * @param type The type of the event to create and fire.
	 * @param data The data of the event.
	 */
	public void fireEvent(int type, Object data) {
		fireEvent(type, data, false);
	}

	/**
	 * Fires an event that cannot be undone that is created from the default source
	 * with the specified data object and type. The event will be delivered to all
	 * listeners that are registered for the event.
	 * 
	 * @param type The type of the event to create and fire.
	 * @param data The data of the event.
	 * @param reverse A flag that indicates whether the listeners should be notified
	 * 	in the order in which they have been registered or in the reverse order.
	 */
	public void fireEvent(int type, Object data, boolean reverse) {
		fireEvent(new Event(type, source, data, false), reverse);
	}

	
	/**
	 * Fires and undoable event and returns the status of the operation as result.
	 * The return value will be true, if the operation has not been aborted and
	 * false if it has been aborted. In this case the appropriate undo operation
	 * will be sent to all listeners that have already accepted the operation.
	 * The event that is fired will carry the specified type as well
	 * as the default source.
	 * 
	 * @param type The type of the event to create.
	 * @return True if the event has not been aborted, false if the event has been
	 * 	aborted by some listener.
	 */
	public boolean fireUndoableEvent(int type) {
		return fireUndoableEvent(type, false);
	}
	
	/**
	 * Fires and undoable event and returns the status of the operation as result.
	 * The return value will be true, if the operation has not been aborted and
	 * false if it has been aborted. In this case the appropriate undo operation
	 * will be sent to all listeners that have already accepted the operation.
	 * The event that is fired will carry the specified type as well
	 * as the default source.
	 * 
	 * @param type The type of the event to create.
	 * @param reverse A flag that indicates whether the listeners should be notified
	 * 	in the order in which they have been registered or in the reverse order.
     * @return True if the event has not been aborted, false if the event has been
	 * 	aborted by some listener.
	 */
	public boolean fireUndoableEvent(int type, boolean reverse) {
		return fireEvent(new Event(type, source, null, true), reverse);
	}
	
	/**
	 * Fires and undoable event and returns the status of the operation as result.
	 * The return value will be true, if the operation has not been aborted and
	 * false if it has been aborted. In this case the appropriate undo operation
	 * will be sent to all listeners that have already accepted the operation.
	 * The event that is fired will carry the specified type and user data as well
	 * as the default source.
	 * 
	 * @param type The type of the event to create and fire.
	 * @param data The user data of the event.
	 * @return True if the event has not been aborted, false if the event has been
	 * 	aborted by some listener.
	 */
	public boolean fireUndoableEvent(int type, Object data) {
		return fireUndoableEvent(type, data, false);
	}
	
	/**
	 * Fires and undoable event and returns the status of the operation as result.
	 * The return value will be true, if the operation has not been aborted and
	 * false if it has been aborted. In this case the appropriate undo operation
	 * will be sent to all listners that have already accepted the operation.
	 * The event that is fired will carry the specified type and user data as well
	 * as the default source.
	 * 
	 * @param type The type of the event to create and fire.
	 * @param data The user data of the event.
	 * @param reverse A flag that indicates whether the listeners should be notified
	 * 	in the order in which they have been registered or in the reverse order.
	 * @return True if the event has not been aborted, false if the event has been
	 * 	aborted by some listener.
	 */
	public boolean fireUndoableEvent(int type, Object data, boolean reverse) {
		return fireEvent(new Event(type, source, data, true), false);
	}
}
