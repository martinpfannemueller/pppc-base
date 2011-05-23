package info.pppc.base.swtui.console;

import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.event.IListener;

import java.io.IOException;
import java.io.InputStream;

/**
 * The console provider starts a thread that reads from
 * a stream and provides the read contents as a string. The string
 * is automatically pruned if the size exceeds a predefined maximum.
 * 
 * @author Marcus Handte
 */
public class ConsoleContentProvider implements Runnable {
	
	/**
	 * The event that is fired whenever the content changes. The 
	 * event object will be the stream provider and the data object
	 * will be null.
	 */
	public static final int EVENT_CONTENT_CHANGED = 1;	
	
	/**
	 * The maximum size of the content. If the data provided by the
	 * stream exceeds this length, the stream will be pruned.
	 */
	private static final int MAXIMUM_CONTENT = 10240;
		
	/**
	 * The size of the buffer that is read at once. This can be 
	 * enlarged in order to speed up processing, however the value
	 * must not exceed the size of the maximum content.
	 */
	private static final int BUFFER_SIZE = 1024; 
	
	/**
	 * The actual content that has been read from the stream so far.
	 */
	private StringBuffer content = new StringBuffer();
	
	/**
	 * The input stream to read from.
	 */
	private InputStream input;
	
	/**
	 * A bundle of listeners that need to be informed whenever the content
	 * changes.
	 */
	private ListenerBundle bundle = new ListenerBundle(this);
	
	/**
	 * Creates a new console provider for the specified stream.
	 * 
	 * @param input The input stream to read from.
	 */
	public ConsoleContentProvider(InputStream input) {
		this.input = input;
	}
	
	/**
	 * Adds a content listener that listens to content changes. The supported
	 * events are currently EVENT_CONTENT_CHANGED. The data object of this
	 * event will be null.
	 * 
	 * @param type The type to register for.
	 * @param listener The listener to register.
	 */
	public void addContentListener(int type, IListener listener) {
		bundle.addListener(type, listener);
	}
	
	/**
	 * Removes a previously registered listener that listens to content
	 * changes.
	 * 
	 * @param type The type to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been added, false otherwise.
	 */
	public boolean removeContentListener(int type, IListener listener) {
		return bundle.removeListener(type, listener);
	}
	
	/**
	 * Continuously reads from the input stream and creates the content model.
	 */
	public void run() {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			while (true) {
				int length = Math.min(Math.max(input.available(), 1), BUFFER_SIZE);
				int position = 0;
				while (position != length) {
					int read = input.read(buffer, position, length - position);
					if (read == -1) {
						content.append("\nEnd of content reached.");
						return;
					}
					position += read;
				}
				synchronized (content) {
					content.append(new String(buffer, 0, length));				
					if (content.length() > MAXIMUM_CONTENT) {
						content.delete(0, content.length() - MAXIMUM_CONTENT);
						for (int i = 0, l = content.length(); i < l; i++) {
							if (content.charAt(i) == '\n') {
								content.delete(0, i + 1);
								break;
							}
						}
					}					
				}
				bundle.fireEvent(EVENT_CONTENT_CHANGED);
			}
		} catch (IOException e) {
			content.append("\nInput redirection failed:\n" + e.getMessage());
		}
	}
	
	/**
	 * Returns the content of the content provider.
	 * 
	 * @return The content of the content provider.
	 */
	public String getContent() {
		synchronized (content) {
			return content.toString();	
		}
	}

}
