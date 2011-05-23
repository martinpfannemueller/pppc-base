package info.pppc.basex.plugin.transceiver.emulator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * The scenario parser is used to read a scenario from a
 * file. It solely uses CLCD methods, so it implements 
 * its own (maybe buggy) string handling.
 * 
 * @author Marcus Handte
 */
public class Parser {
	
	/**
	 * This is the vector of devices that have been parsed.
	 */
	private Vector devices = new Vector();
	
	/**
	 * This is the vector of connections that have been parsed.
	 */
	private Vector connections = new Vector();
	
	/**
	 * The following constants define parts of the language.
	 */
	private String DIRECTIVE_COMMENT = "#";
	
	/**
	 * Identifier for the device directive. 
	 */
	private String DIRECTIVE_DEVICE = "device.";
	
	/**
	 * Identifier for the connect directive.
	 */
	private String DIRECTIVE_CONNECTION = "connect";
	
	/**
	 * Identifier for the set operator.
	 */
	private char DIRECTIVE_SET = '=';
	
	/**
	 * Identifier for the sub operator.
	 */
	private char DIRECTIVE_SUB = ':';
	
	/**
	 * The input stream to read from.
	 */
	private InputStream stream;

	/**
	 * The current line number while parsing 
	 */
	private int number = 0;
	
	/**
	 * Creates a new scenario parser that uses the specified
	 * stream to read a scenario file.
	 * 
	 * @param stream The stream to read from.
	 */
	public Parser(InputStream stream) {
		this.stream = stream;
	}
	
	/**
	 * Parses the file, after this method has been called
	 * successfully, the device and connection information
	 * is stored in this parser.
	 * 
	 * @throws IOException Thrown if the stream fails or if
	 * 	there are syntax errors while parsing.
	 */
	public void parse() throws IOException {
		if (stream == null) throw new IOException("Scenario stream is null.");
		String line = null;
		while ((line = readLine()) != null) {
			number +=  1;
			if (line.startsWith(DIRECTIVE_COMMENT)) {
				continue; // ignore comments
			} else if (line.startsWith(DIRECTIVE_DEVICE)) {
				Device d = createDevice(line);
				if (d.name.length() == 0) {
					throw new IOException("Error at line " + number + ", illegal device.");
				}
				for (int i = 0; i < devices.size(); i++) {
					Device d2 = (Device)devices.elementAt(i);
					if (d2.name.equals(d.name)) {
						throw new IOException("Error at line " + number + ", duplicate device definition.");
					}
					if (d2.port == d.port && d2.host.equals(d.host)) {
						throw new IOException("Error at line " + number + ", duplicate port definition.");
					}
				}
				devices.addElement(d);
			} else if (line.startsWith(DIRECTIVE_CONNECTION)) {
				Connection c = createConnection(line);
				if (c.source.equals(c.target) || c.source.length() == 0 | c.target.length() == 0) {
					throw new IOException("Error at line " + number + ", illegal connection.");
				}
				boolean source = false;
				boolean target = false;
				for (int i = 0; i < devices.size(); i++) {
					Device d = (Device)devices.elementAt(i);
					source = source || d.name.equals(c.source);
					target = target || d.name.equals(c.target);
				}
				if (! source || ! target) {
					throw new IOException("Error at line " + number + ", illegal connection.");
				}
				for (int i = 0; i < connections.size(); i++) {
					Connection c2 = (Connection)connections.elementAt(i);
					if ((c.source.equals(c2.source) && c.target.equals(c2.target)) ||
						(c.source.equals(c2.target) && c.target.equals(c2.source))) {
						throw new IOException("Error at line " + number + ", duplicate connection definition.");
					}
				}
				connections.addElement(c);
			} else if (line.length() == 0){
				continue; // ignore blank lines
			} else {
				// not blank, not comment, but also not directive, what is it?
				throw new IOException("Syntax error at line " +  number + ", no directive found.");
			}
		}
		stream = null;
	}
	
	/**
	 * This method creates a device from a line that represents a device
	 * directive.
	 * 
	 * @param directive The directive to parse.
	 * @return The device that has been parsed.
	 * @throws IOException Thrown if the directive is malformed.
	 */
	private Device createDevice(String directive) throws IOException {
		Device device = new Device();
		String[] split = splitDirective(directive.substring(DIRECTIVE_DEVICE.length()), DIRECTIVE_SET);
		device.name = split[0];
		if (split[1].indexOf(DIRECTIVE_SUB) < 0) {
			device.host = "";
		}  else {
			String[] subsplit = splitDirective(split[1], DIRECTIVE_SUB);
			device.host = subsplit[0];
			split[1] = subsplit[1];
		}
		try {
			device.port = Integer.parseInt(split[1]);
		} catch (Throwable t) {
			throw new IOException("Syntax error at line " + number + ", expecting port number.");
		}
		return device;
	}
	
	/**
	 * This method creates a connection from a connection directive.
	 * 
	 * @param directive The directive to parse.
	 * @return The connection that has been parsed.
	 * @throws IOException Thrown if the directive contains syntax errors.
	 */
	private Connection createConnection(String directive) throws IOException {
		Connection result = new Connection();
		directive = splitDirective(directive, DIRECTIVE_SET)[1];
		String[] names = splitDirective(directive, DIRECTIVE_SUB);
		result.source = names[0];
		result.target = names[1];
		return result;
	}
	
	/**
	 * This helper method returns two trimmed strings from one string
	 * that is separated at the first occurrence of a splitter char.
	 * 
	 * @param directive The directive to split.
	 * @param splitter The splitter char.
	 * @return Two trimmed strings.
	 * @throws IOException Thrown if the splitter char is not contained
	 * 	in the string.
	 */
	private String[] splitDirective(String directive, char splitter) throws IOException {
		String[] result = new String[2];
		int index = directive.indexOf(splitter);
		if (index < 0) throw new IOException("Syntax error at line " + number + ", expecting " + splitter);
		result[0] = directive.substring(0, index).trim();
		result[1] = directive.substring(index + 1, directive.length()).trim();
		return result;
	}
	
	/**
	 * Reads a line from the input stream of the parser.
	 * 
	 * @return A line of the input stream or null, if the
	 * 	stream has reached its end.
	 * @throws IOException Thrown if the stream read fails.
	 */
	private String readLine() throws IOException {
		boolean isRead = false;
		StringBuffer b = new StringBuffer();
		int read;
		while ((read = stream.read()) > 0) {
			isRead = true;
			if (read == '\n') {
				return b.toString();
			} else if (read == '\r') {
				continue;
			} else {
				b.append((char)read);
			}
		}
		if (isRead) {
			return b.toString().trim();	
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the vector of parsed devices.
	 * 
	 * @return The vector of parsed devices.
	 */
	public Vector getDevices() {
		return devices;
	}
	
	/**
	 * Returns the vector of connections.
	 * 
	 * @return The vector of connections.
	 */
	public Vector getConnections() {
		return connections;
	}		
	
}
