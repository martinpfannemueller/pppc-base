package info.pppc.base.lcdui.util;

import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * The resource bundle is a simple replacement for the standard java resource
 * bundle. It does not support localization and other nifty features. It can
 * simply be used to externalize strings.
 * 
 * @author Marcus Handte
 */
public class ResourceBundle {

	/**
	 * The char that separates the name from the value.
	 */
	private static final char SEPARATOR = '=';
	
	/**
	 * The char that is used within comments.
	 */
	private static final char COMMENT = '#';
	
	/**
	 * The line delimiter used to find a single name value pair.
	 */
	private static final char DELIMITER = '\n';
	
	/**
	 * The file name extension for property files.
	 */
	private static final String EXTENSION = ".properties";
	
	/**
	 * The hash table that contains the keys.
	 */
	private Hashtable table = new Hashtable();

	/**
	 * Returns the resource bundle located at the specified folder.
	 * The folder is relative to the class path, e.g. info/pppc/fobar.
	 * 
	 * @param string The folder that contains the file.
	 * @return The resource bundle for the specified file.
	 */
	public static ResourceBundle getBundle(String string) {
		InputStream in = ResourceBundle.class.getResourceAsStream
			("/" + string + EXTENSION);
		ResourceBundle res = new ResourceBundle(in);
		return res;
	}
	
	/**
	 * Creates an empty resource bundle without any strings.
	 */
	protected ResourceBundle() {
		super();
	}

	/**
	 * Creates a resource bundle from the specified input stream.
	 * 
	 * @param in The input stream to read from.
	 */
	protected ResourceBundle(InputStream in) {
		InputStreamReader r = new InputStreamReader(in);
		String line;
		try {
			while ((line = readLine(r)) != null) {
				line = line.trim();
				if (line.length() <= 1)
					continue;
				if (line.charAt(0) == COMMENT)
					continue;
				int index = line.indexOf(SEPARATOR);
				if (index == -1)
					continue;
				String name = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				table.put(name, value);
			}
			r.close();
		} catch (IOException e) {
			Logging.error(getClass(), "Exception while parsing resource bundle.", e);
		}
	}

	/**
	 * Helper method that reads a single line from the input stream.
	 * 
	 * @param in The input stream to read from.
	 * @return The next line.
	 * @throws IOException Thrown by the underlying stream.
	 */
	private String readLine(InputStreamReader in) throws IOException {
		StringBuffer res = new StringBuffer();
		int c = in.read();
		if (c == -1)
			return null;
		while (c != -1) {
			if ((char) c == DELIMITER)
				return res.toString();
			res.append((char) c);
			c = in.read();
		}
		return res.toString();
	}

	/**
	 * Returns the string for the specified key.
	 * 
	 * @param id The key to retrieve.
	 * @return  The string for the specified key.
	 * @throws NullPointerException Thrown if the key is null.
	 * @throws RuntimeException Thrown if the key does not exist.
	 */
	public String getString(String id) {
		if (! table.containsKey(id))
			throw new RuntimeException("Resource missing: " + id);
		return (String) table.get(id);
	}

}
