package info.pppc.base.system.util; 

import java.io.PrintStream;

/**
 * This class is responsible for logging. Instead using System.out.println
 * this way lets the programmer turning off logging messages more easily. The
 * logging distinguishes between several logging types. The java logging is not
 * used because of the missing classes in J2ME-API.
 * 
 * @author Marcus Handte
 */
public final class Logging {
	
	/**
	 * A list of packages or classes that should not be logged. The comparison
	 * is done by a startsWith string comparison on the class name of the 
	 * location class. Thus, "base", "pcom" will filter all classes whose name
	 * matches base* and pcom*. Note that the exclude filter does not exclude
	 * error messages these messages are logged always as long as the ERROR
	 * flag is set to true.
	 */
	private static final String[] FILTER_EXCLUDE = new String[] { }; 
	
	/**
	 * A list of packages or classes that should be logged in any case. The
	 * comparison is done by a startsWith string comparison. See the exclusion
	 * filter for details. Note that the inclusion filter superceeds the 
	 * exclusion filter. This can be used to turn on logging in some bogus class
	 * or package during debugging.
	 */
	private static final String[] FILTER_INCLUDE = new String[] { };
	
	/**
	 * Minimum verbosity disables all log messages.
	 */
	public static final int MINIMUM_VERBOSITY = 0;
	
	/**
	 * Normal verbosity contains log and error messages.
	 */
	public static final int NORMAL_VERBOSITY = 1;
	
	/**
	 * Maximum verbosity is like normal verbosity but it also includes debug messages.
	 */
	public static final int MAXIMUM_VERBOSITY = 2;
	
	/**
	 * Denotes that a log message must not contain a time stamp.
	 */
	public static final int TIME_DISABLED = 0;
	
	/**
	 * Denotes that a log message must contain an absolute time stamp.
	 * The time stamp form is hour[0-23]:minute[0-59]:second[0-59]:millis[0-999].
	 */
	public static final int TIME_ABSOLUTE = 1;
	
	/**
	 * Denotes that a logged message must contain a relative time stamp.
	 * The time stamp form hour[>=0]:minute[0-59]:second[0-59]:millis[0-999].
	 * The relative offset is the load time of the class.
	 */
	public static final int TIME_RELATIVE = 2;
	
	/**
	 * The time zone for absolute time stamps (UTC +1, Germany).
	 */
	public static final int TIME_ZONE = +1;
	
	/**
	 * The load time of the class used to calculate relative time stamps.
	 */
	public static final long TIME_BASE = System.currentTimeMillis();
	
	/**
	 * The time format for time stamps that are added to log messages.
	 * Possible values are TIME_DISABLED, TIME_ABSOLUTE, TIME_RELATIVE.
	 * This is a compile time only parameter to speed up computation.
	 */
	private static final int TIME = TIME_RELATIVE;
	
	/**
	 * Determines whether log messages are logged.
	 */
	private static boolean LOG = true;
	
	/**
	 * Determines whether error messages are logged.
	 */
	private static boolean ERROR = true;
	
	/**
	 * Determines whether debug messages are logged.
	 */
	private static boolean DEBUG = true;
	
	/**
	 * Determines whether exception traces are logged.
	 */
	private static boolean TRACE = true;
	
	/**
	 * The output stream used for logging.
	 */
	private static PrintStream out = System.out;

	/**
	 * Sets the verbosity level of the logger.
	 * 
	 * @param level The must be one of the verbosity constants.
	 */
	public static void setVerbosity(int level) {
		if (level == MINIMUM_VERBOSITY) {
			LOG = false;
			ERROR = false;
			DEBUG = false;
		} else if (level == NORMAL_VERBOSITY) {
			LOG = true;
			ERROR = true;
			DEBUG = false;	
		} else if (level == MAXIMUM_VERBOSITY) {
			LOG = true;
			ERROR = true;
			DEBUG = true;
		}
	}
	
	/**
	 * Returns the current verbosity level of the logger.
	 * 
	 * @return The current verbosity level of the logger.
	 */
	public static int getVerbosity() {
		if (LOG && DEBUG && ERROR) {
			return MAXIMUM_VERBOSITY;
		} else if (LOG && ERROR) {
			return NORMAL_VERBOSITY;
		} else {
			return MINIMUM_VERBOSITY;
		}
	}
    	
    /**
     * Enables or disables traces of exceptions. Warning: in most cases this
     * feature should not be changed. Actually, this should only be disabled
     * for demonstrations. 
     * 
     * @param trace True to enable tracing, false to disable.
     */
    public static void setTrace(boolean trace) {
    	TRACE = trace;
    }
	
	/**
	 * Sets the output of the logging facility to the specified print
	 * stream.
	 * 
	 * @param stream The stream to print to.
	 */    
    public static void setOutput(PrintStream stream) {
    	if (stream != null) {
			out = stream;	
    	}
    }

	/**
	  * Writes a log message to the specified output. Should be used if messages
	  * are necessary in normal use (like system start or end).
	  * 
	  * @param location The source of the message used for filtering.
	  * @param message The message to be logged. 
	  */
	public static void log(String location, String message) {
		if (!Logging.LOG || !accept(location)) return;
		print("LOG", location, message, null);
	}


	/**
	 * Writes a log message to the specified output. Should be used if messages
	 * are necessary in normal use (like system start or end).
	 * 
	 * @param location The source of the message used for filtering.
	 * @param message The message to be logged.
	 */
	public static void log(Class location, String message){
		log(((location == null)?null:location.getName()), message);
	}
    
	/**
	 * Writes an error message to the specified output. This method should be
	 * used instead of printStackTrace() in every try-catch block. Note that 
	 * error messages are never filtered.
	 * 
	 * @param location The source of the message.
	 * @param message The message to be logged.
	 * @param e The exception caused this message used for traces.
	 */    
    public static void error(String location, String message, Throwable e) {
		if (!Logging.ERROR) return;
    	print("ERR", location, message, e);
    }
    
 	/**
	 * Writes an error message to the specified output. This method should be
	 * used instead of printStackTrace() in every try-catch block. Note that
	 * error messages are never filtered.
	 * 
	 * @param location The source of the message.
	 * @param message The message to be logged.
	 * @param e The exception caused this message used for traces.
	 */
    public static void error(Class location, String message, Throwable e){
		error(((location == null)?null:location.getName()), message, e);
    }

	/**
	 * Write a debug message to the specified output. Use this method to state
	 * any important or unimportant messages to debug the system. The output
	 * of those messages should be toggled of while running the system outside
	 * the development area..
	 * 
	 * @param location The source of the message used for filtering.
	 * @param message The message to be logged.
	 */    	
	public static void debug(String location, String message) {
		if (!Logging.DEBUG || !accept(location)) return;	
		print("DBG", location, message, null);	
	}


	/**
	 * Write a debug message to the specified output. Use this method to state
	 * any important or unimportant messages to debug the system. The output
	 * of those messages should be toggled of while running the system outside
	 * the development area.
	 * 
	 * @param location The source of the message used for filtering.
	 * @param message The message to be logged.
	 */    
    public static void debug(Class location, String message) {
		debug(((location == null)?null:location.getName()), message);
    }

	/**
	 * Prints the message and the stack trace of the exception if the exception
	 * is not null using Gregor's pretty print format. If the location contains dots,
	 * only the chars behind the last dot will be printed as location. This will 
	 * effectively remove package names from locations.
	 * 
	 * @param type The message type to print.
	 * @param location The location of the message to print.
	 * @param message The message to print. 
	 * @param e The exception to print.
	 */
    private synchronized static void print(String type, String location, String message, Throwable e){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append('[');
		buffer.append(type);
		if (TIME != TIME_DISABLED) {
			buffer.append('|');
			buffer.append(timestamp(System.currentTimeMillis()));			
		} 
		buffer.append('|');
		if (location != null) {
			int idx = location.lastIndexOf('.');
			if (idx != -1) {
				buffer.append(location.substring(idx + 1));
			} else {
				buffer.append(location);
			}
		}
		buffer.append("] ");
		buffer.append(message);
		if (e != null) {
			buffer.append(" (");
			buffer.append(e.getMessage());
			buffer.append(')');
			buffer.append('\n');
			if (TRACE) {
				e.printStackTrace();
			}
		} else {
			buffer.append('\n');
		}
		out.print(buffer.toString());
    }
    
    /**
     * Implements the filter on source class objects using the inclusion and
     * exclusion filter. This method returns true if the location should be
     * logged.
     * 
     * @param location The location to log.
     * @return True if the location should be logged, false otherwise.
     */
    private static boolean accept(String location) {
		if (location != null) {
			boolean accept = true;
			for (int i = FILTER_EXCLUDE.length - 1; i >= 0; i--) {
				if (location.startsWith(FILTER_EXCLUDE[i])) {
					accept = false;
					break;	 
				}
			}
			for (int i = FILTER_INCLUDE.length - 1; i >= 0; i--) {
				if (location.startsWith(FILTER_INCLUDE[i])) {
					accept = true;
					break;
				}
			}
			return accept;
		}
		return true;
    }

	/**
	 * Returns a pretty printed time stamp whose format depends on the time 
	 * parameter of the logging class.
	 * 
	 * @param millis The milliseconds to pretty print.
	 * @return A pretty printed absolute or relative time stamp. If
	 * 	the time flag is set to disabled, this method will return an empty 
	 * 	string.
	 */
	public static String timestamp(long millis) {
		// perform reference adjustments
		switch (TIME) {
			case TIME_ABSOLUTE:
				// adjust time zone
				millis += (3600000 * TIME_ZONE);
				millis = millis % 86400000;
				break;
			case TIME_RELATIVE:
				millis -= TIME_BASE;
				break;
			default:
				return "";
		}
		// component calculations
		int ms = (int)(millis % 1000);
		int sc = (int)((millis % 60000) / 1000);
		int mn = (int)((millis % 3600000) / 60000);
		int hr = (int)((millis) / 3600000);
		// perform date formatting
		return ((hr<10)?"0":"") + hr 
			+ ":" + ((mn < 10)?"0":"") + mn 
			+ ":" + ((sc < 10)?"0":"") + sc + "." 
			+ ((ms < 100)?((ms < 10)?"00":"0"):"") + ms;
	}
    
}
