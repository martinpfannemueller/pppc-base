package info.pppc.base.lcdui.console;

import info.pppc.base.lcdui.form.FormDisplay;

import java.io.OutputStream;

/**
 * The console output stream is an output stream that writes it's output
 * as lines into a console.
 * 
 * @author Marcus Handte
 */
public class ConsoleOutputStream extends OutputStream {

	/**
	 * The console that is used for writing.
	 */
	private ConsoleItem console;
	
	/**
	 * The form display used to post update events.
	 */
	private FormDisplay display;
	
	/**
	 * The string buffer that contains the last line. 
	 */
	private StringBuffer buffer = new StringBuffer();
	
	/**
	 * Creates a new console output stream for the specified console.
	 * 
	 * @param display The display used to post the console update events.
	 * @param console The console that will receive the output this
	 * 	must not be null.
	 * @throws NullPointerException Thrown if the console is null.
	 */
	public ConsoleOutputStream(FormDisplay display, ConsoleItem console) {
		if (console == null) throw new NullPointerException("Console is null.");
		this.console = console;
		this.display = display;
	}
	
	/**
	 * Adds the specified line to the console item.
	 * 
	 * @param string The string to add.
	 */
	private void addLine(final String string) {
		display.runAsync(new Runnable() {
			public void run() {
				console.addLine(string);
			}
		});
	}
	
	/**
	 * Writes the specified byte to the console. If the end of the
	 * line is reached a new line is added.
	 * 
	 * @param write The char that should be written.
	 */
	public void write(int write) {
		char c = (char)write;
		if (c == '\r') return;
		if (c == '\n') {
			addLine(buffer.toString());
			buffer = new StringBuffer();
		} else{
			buffer.append(c);
		}
	}
	
	/**
	 * Flushes the stream to the console.
	 */
	public void flush() {
		if (buffer.length() > 0) {
			addLine(buffer.toString());
			buffer = new StringBuffer();
		}
	}
	
}
