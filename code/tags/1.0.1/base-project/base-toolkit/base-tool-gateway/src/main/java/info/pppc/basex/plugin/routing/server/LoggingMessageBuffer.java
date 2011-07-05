package info.pppc.basex.plugin.routing.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A minimal implementtion of a message buffer for the BASE logging
 * facility.
 * 
 * @author Mac
 *
 */
public class LoggingMessageBuffer extends java.io.PrintStream {

	/**
	 * The maximum size of log messages.
	 */
	private int size;
	
	/**
	 * The last n log messages.
	 */
	private LinkedList<String> buffer = new LinkedList<String>();
	
	/**
	 * Creates a new message buffer with the specified maximum size.
	 * 
	 * @param size The size.
	 */
	public LoggingMessageBuffer(int size) {
		super(new OutputStream() {
			@Override
			public void write(int arg0) throws IOException {
				// do nothing here, will never be called.
			}
		});
		this.size = size;
	}
	
	/**
	 * Called by the logging facility of base.
	 *
	 * @param string The string to print.
	 */
	@Override
	public synchronized void print(String string) {
		buffer.add(string);
		while (buffer.size() > size) {
			buffer.remove();
		}
	}
	
	/**
	 * Returns the last n logged messages.
	 * 
	 * @return The last n logged messages.
	 */
	public synchronized ArrayList<String> getMessages() {
		ArrayList<String> lines = new ArrayList<String>();
		lines.addAll(buffer);
		return lines;
	}
	
}
