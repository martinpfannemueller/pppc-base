package info.pppc.base.lcdui.form;

import info.pppc.base.system.util.Logging;

import java.util.Vector;

/**
 * The form display is used to manager the user interface thread
 * that is used by items. Since we cannot guarantee, that the jvm
 * implementation does only use one thread, we use an ui-lock to
 * synchronize all items with the repaint and key event methods.
 * 
 * @author Marcus Handte
 */
public class FormDisplay {

	/**
	 * The global lock for all user interface elements.
	 */
	public static final Object UI_LOCK = new Object();
	
	/**
	 * The displays that are available and have not been disposed yet.
	 */
	protected static final Vector displays = new Vector();
	
	/**
	 * Returns the display for the current thread. The create
	 * flag determines whether a new one should be created if
	 * none exists.
	 * 
	 * @param create The create flag, set to true to create a
	 * 	new display if there is none for this thread.
	 * @return The form display for the thread or null if the
	 * 	create flag is set to false and the current thread does	
	 *  not have a display associated.
	 */
	public static FormDisplay getCurrent(boolean create) {
		Thread t = Thread.currentThread();
		for (int i = displays.size() - 1; i >= 0; i--) {
			FormDisplay d = (FormDisplay)displays.elementAt(i);
			if (d.getThread() == t) {
				return d;
			}
		}
		if (create) {
			FormDisplay d = new FormDisplay();
			displays.addElement(d);
			return d;
		} else {
			return null;
		}
	}
	
	/**
	 * The reference to the display thread.
	 */
	private Thread thread;
	
	/**
	 * The runnables that have not or are in the process of
	 * being dispatched.
	 */
	private Vector runnables = new Vector();
	
	/**
	 * A flag that indicates whether the display has been
	 * disposed.
	 */
	private boolean disposed = false;
	
	/**
	 * The current dispatch depth.
	 */
	private int depth = -1;
	
	/**
	 * Creates a new display and makes the current thread
	 * the display thread.
	 */
	protected FormDisplay() {
		thread = Thread.currentThread();
	}
	
	/**
	 * Returns the display thread that is responsible
	 * for this display.
	 * 
	 * @return The thread responsible for the display.
	 */
	public Thread getThread() {
		return thread;
	}
	
	/**
	 * Determines whether there is an event to dispatch.
	 * 
	 * @return True if there is an event to dispatch, false
	 * 	otherwise.
	 */
	public synchronized boolean read() {
		return (runnables.size() > depth + 1);
	}
	
	/**
	 * Dispatches the runnables posted to the display and returns
	 * whenever the display is disposed.
	 * 
	 * @param single A flag that indicates whether only a single
	 * 	event should be dispatched.
	 */
	public void dispatch(boolean single) {
		checkAccess();
		depth += 1;
		dispatch: while (true) {
			Runnable runnable;
			synchronized (this) {
				while (runnables.size() <= depth) {
					if (disposed) { 
						break dispatch;
					} else {
						try {
							wait();	
						} catch (InterruptedException e) {
							Logging.error(getClass(), "Form display got interrupted.", e);
						}
					}
				}
				runnable = (Runnable)runnables.elementAt(depth);
			}
			try {
				runnable.run();
			} catch (Throwable t) {
				Logging.error(getClass(), "Exception in ui runnable.", t);
			}
			synchronized (this) {
				runnables.removeElementAt(depth);
				notifyAll();
			}
			if (single) {
				break dispatch;
			}
		}
		depth -= 1;
	}
	
	/**
	 * Disposes the display.
	 */
	public synchronized void dispose() {
		disposed = true;
		notifyAll();
		synchronized (getClass()) {
			for (int i = displays.size() - 1; i >= 0; i--) {
				FormDisplay d = (FormDisplay)displays.elementAt(i);
				if (d == this) {
					displays.removeElementAt(i);
					return;
				}
			}
		}
	}
	
	/**
	 * Determines whether the display is disposed.
	 * 
	 * @return True if the display is disposed, false otherwise.
	 */
	public synchronized boolean isDisposed() {
		return false;
	}
	
	/**
	 * Runs the specified runnable within the display thread synchronously.
	 * 
	 * @param runnable The runnable to run synchronously.
	 */
	public void runSync(Runnable runnable) {
		if (disposed) throw new RuntimeException("Form display disposed.");
		if (Thread.currentThread() == thread) {
			runnable.run();
		} else {
			synchronized (this) {
				runnables.addElement(runnable);
				notifyAll();
				while (true) {
					boolean done = true;
					for (int i = runnables.size() - 1; i >= 0; i--) {
						if (runnables.elementAt(i) == runnable) {
							done = false;
							break;
						}
					}
					if (done) return;
					try {
						wait();	
					} catch (InterruptedException e) {
						Logging.error(getClass(), "Thread got interrupted.", e);
					}
				}
			}
		}
	}
	
	/**
	 * Executes the specified runnable with the display thread
	 * asynchronously.
	 * 
	 * @param runnable The runnable to execute asynchronously.
	 */
	public synchronized void runAsync(Runnable runnable) {
		if (disposed) throw new RuntimeException("Form display disposed.");
		runnables.addElement(runnable);
		notifyAll();
	}
	
	/**
	 * Determines whether the current thread is the display
	 * thread and whether the display has been disposed already.
	 */
	private synchronized void checkAccess() {
		if (Thread.currentThread() != thread) {
			throw new RuntimeException("Illegal thread access.");
		} else if (disposed) {
			throw new RuntimeException("Form display disposed.");
		}
	}
	
}
