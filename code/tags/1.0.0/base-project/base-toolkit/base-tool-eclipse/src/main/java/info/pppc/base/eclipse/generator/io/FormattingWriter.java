/*
 * CVS Information
 * 
 * File:     $RCSfile: FormattingWriter.java,v $
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:33 $
 * 
 */
package info.pppc.base.eclipse.generator.io;

import java.io.IOException;
import java.io.Writer;

/**
 * The formatting writter knows how to indent java source code. The indentation
 * is based on the number of opening and closing brackets.
 * 
 * Be aware that this implementation will be 'really' slow.
 * 
 * @author Mac
 */
public class FormattingWriter extends Writer {

	/**
	 * The underlying writer implementation.
	 */
	private Writer writer = null;
	
	/**
	 * An index used to determine the format for new lines.
	 */
	private int formatIndex = 0; 

	/**
	 * Determines whether the next char starts at a new line.
	 */
	private boolean newLine = true;

	/**
	 * Creates a new formatting writer from the specified writer. The first
	 * char will be dealt with as if it would begin at a new line.
	 * 
	 * @param writer The writer to write to.
	 */
	public FormattingWriter(Writer writer) {
		this(writer, true);
	}

	/**
	 * Creates a new formatting writer from the specified writer.
	 * 
	 * @param writer The writer to write to.
	 * @param newLine Determines whether the first char begins at a new line.
	 */
	public FormattingWriter(Writer writer, boolean newLine) {
		super(writer);
		this.writer = writer;
		this.newLine = newLine;
	}	


	/**
	 * Write some chars and keep the format.
	 * 
	 * @see java.io.Writer#write(char[], int, int)
	 * @param arg0 The array containing the chars to write.
	 * @param arg1 The start index.
	 * @param arg2 The end index.
	 */
	public void write(char[] arg0, int arg1, int arg2) throws IOException {
		for (int i = arg1; i < arg2; i++) {
			// do the formatting, baby
			if (arg0[i] == '}') {
				formatIndex--;
			}
			if (newLine) {	
				for (int j = 0; j < formatIndex; j++) {
					writer.write('\t');
				}
				newLine = false;
			}
			if (arg0[i] == '{') {
				formatIndex++;
			}
			// write the char
			writer.write(arg0[i]);
			// handle new lines
			if (arg0[i] == '\n') {
				newLine = true;
				if (arg2 < (i + 1) && arg0[i + 1] == '\r') {
					writer.write('\r');
					i += 1;
				}
			}
		}
		
	}	
	
	/**
	 * Flushes the underlying writer implementation.
	 * 
	 * @see java.io.Writer#flush()
	 * @throws IOException Thrown by the underlying implementation.
	 */
	public void flush() throws IOException {
		writer.flush();
	}

	/**
	 * Closes the underlying writer implementation.
	 * 
	 * @see java.io.Writer#close()
	 * @throws IOException Thrown by the underlying implementation.
	 */
	public void close() throws IOException {
		writer.close();
	}

}
