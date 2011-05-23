package info.pppc.base.system.io;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
 * The object stream translator enables a more efficient serialization 
 * by assigning abbreviations to class names. It is used by the default object 
 * input and output stream of BASE. Users can register abbreviations for their 
 * serializable classes. Note that this mechanism should mainly be used
 * by system classes that are available on all systems since adding an
 * abbreviations might make debugging harder and there is no mechanism
 * that guarantees that the assigned abbreviations are unique.
 * 
 * Note that users should not use '/' as the first character for their
 * abbreviations since this is used to abbreviate classes from the
 * java language. Similarly, applications should not use ';' as first
 * character since this is used by BASE and PCOM system services and
 * data types. Typically, BASE data types will be abbreviated with
 * ';B', PCOM data types will be abbreviated with ';P'.
 * 
 * @author Marcus Handte
 */
public class ObjectStreamTranslator {

	/**
	 * The abbreviation for cyclic references within an object graph.
	 */
	public static final String ABBREVIATION_REFERENCE = "/R";
	
	/**
	 * The abbreviation for objects with the value null.
	 */
	public static final String ABBREVIATION_NULL = "/N";
	
	/**
	 * The abbreviation for classes of type Object.
	 */
	public static final String ABBREVIATION_OBJECT = "/O";

	/**
	 * The abbreviation for classes of type Integer.
	 */
	public static final String ABBREVIATION_INTEGER = "/I";
	
	/**
	 * The abbreviation for classes of type Short.
	 */
	public static final String ABBREVIATION_SHORT = "/S";
	
	/**
	 * The abbreviation for classes of type Long.
	 */
	public static final String ABBREVIATION_LONG = "/J";
	
	/**
	 * The abbreviation for classes of type Boolean.
	 */
	public static final String ABBREVIATION_BOOLEAN = "/Z";
	
	/**
	 * The abbreviation for classes of type Byte.
	 */
	public static final String ABBREVIATION_BYTE = "/B";
	
	/**
	 * The abbreviation for classes of type Character.
	 */
	public static final String ABBREVIATION_CHARACTER = "/C";
	
	/**
	 * The abbreviation for classes of type String.
	 */
	public static final String ABBREVIATION_STRING = "/T";
	
	/**
	 * The abbreviation for a one-dimensional array of type int.
	 */
	public static final String ABBREVIATION_PRIMITIVE_INT = "/[I";
	
	/**
	 * The abbreviation for a one-dimensional array of type long.
	 */	
	public static final String ABBREVIATION_PRIMITIVE_LONG = "/[J";
	
	/**
	 * The abbreviation for a one-dimensional array of type short.
	 */
	public static final String ABBREVIATION_PRIMITIVE_SHORT = "/[S";
	
	/**
	 * The abbreviation for a one-dimensional array of type boolean.
	 */
	public static final String ABBREVIATION_PRIMITIVE_BOOLEAN = "/[Z";
	
	/**
	 * The abbreviation for a one-dimensional array of type byte.
	 */
	public static final String ABBREVIATION_PRIMITIVE_BYTE = "/[B";
	
	/**
	 * The abbreviation for a one-dimensional array of type char.
	 */
	public static final String ABBREVIATION_PRIMITIVE_CHAR = "/[C";
	
	/**
	 * The abbreviation for a one-dimensional array of type Object.
	 */
	public static final String ABBREVIATION_ARRAY_OBJECT = "/LO";
	
	/**
	 * The abbreviation for a one-dimensional array of type Integer.
	 */
	public static final String ABBREVIATION_ARRAY_INTEGER = "/LI";
	
	/**
	 * The abbreviation for a one-dimensional array of type Short.
	 */
	public static final String ABBREVIATION_ARRAY_SHORT = "/LS";
	
	/**
	 * The abbreviation for a one-dimensional array of type Long.
	 */
	public static final String ABBREVIATION_ARRAY_LONG = "/LJ";
	
	/**
	 * The abbreviation for a one-dimensional array of type Boolean.
	 */
	public static final String ABBREVIATION_ARRAY_BOOLEAN = "/LZ";
	
	/**
	 * The abbreviation for a one-dimensional array of type Byte.
	 */
	public static final String ABBREVIATION_ARRAY_BYTE = "/LB";
	
	/**
	 * The abbreviation for a one-dimensional array of type Character.
	 */
	public static final String ABBREVIATION_ARRAY_CHARACTER = "/LC";
	
	/**
	 * The abbreviation for a one-dimensional array of type String.
	 */
	public static final String ABBREVIATION_ARRAY_STRING = "/LT";
	
	/**
	 * The abbreviation for a one-dimensional array of type Vector.
	 */
	public static final String ABBREVIATION_VECTOR = "/UV";
	
	/**
	 * The abbreviation for a one-dimensional array of type Stack.
	 */
	public static final String ABBREVIATION_STACK = "/US";
	
	/**
	 * The abbreviation for a one-dimensional array of type Hashtable.
	 */
	public static final String ABBREVIATION_HASHTABLE = "/UH";
	
	/**
	 * This hash table hashes abbreviations to class names.
	 */
	private static Hashtable abbreviations = new Hashtable();
	
	/**
	 * This hash table hashes class names to abbreviations.
	 */
	private static Hashtable classnames = new Hashtable();
	
	/**
	 * Initializer for default java data types and base system
	 * classes.
	 */
	static {
		// register internal identifiers used by the streams
		register(ABBREVIATION_REFERENCE, ABBREVIATION_REFERENCE); // the value representing a reference
		register(ABBREVIATION_NULL, ABBREVIATION_NULL); // the value representing a null
		// register the primitive wrapper types
		register(Object.class.getName(), ABBREVIATION_OBJECT);
		register(Integer.class.getName(), ABBREVIATION_INTEGER);
		register(Short.class.getName(), ABBREVIATION_SHORT);
		register(Long.class.getName(), ABBREVIATION_LONG);
		register(Boolean.class.getName(), ABBREVIATION_BOOLEAN);
		register(Byte.class.getName(), ABBREVIATION_BYTE);
		register(Character.class.getName(), ABBREVIATION_CHARACTER);
		register(String.class.getName(), ABBREVIATION_STRING);
		// Note on the array code: calling type[].class.getName() will
		// fail on sun spots with a class loader exception since the 
		// intrinsic class does not have a definition file. A work-around
		// is to instantiate the array type (which wastes resources). 
		// register the primitive array types (with work-around above)
		register(new int[0].getClass().getName(), ABBREVIATION_PRIMITIVE_INT);
		register(new long[0].getClass().getName(), ABBREVIATION_PRIMITIVE_LONG);
		register(new short[0].getClass().getName(), ABBREVIATION_PRIMITIVE_SHORT);
		register(new boolean[0].getClass().getName(), ABBREVIATION_PRIMITIVE_BOOLEAN);
		register(new byte[0].getClass().getName(), ABBREVIATION_PRIMITIVE_BYTE);
		register(new char[0].getClass().getName(), ABBREVIATION_PRIMITIVE_CHAR);
		// register the primitive wrapper array types (with work-around above)
		register(new Object[0].getClass().getName(), ABBREVIATION_ARRAY_OBJECT);
		register(new Integer[0].getClass().getName(), ABBREVIATION_ARRAY_INTEGER);
		register(new Short[0].getClass().getName(), ABBREVIATION_ARRAY_SHORT);
		register(new Long[0].getClass().getName(), ABBREVIATION_ARRAY_LONG);
		register(new Boolean[0].getClass().getName(), ABBREVIATION_ARRAY_BOOLEAN);
		register(new Byte[0].getClass().getName(), ABBREVIATION_ARRAY_BYTE);
		register(new Character[0].getClass().getName(), ABBREVIATION_ARRAY_CHARACTER);
		register(new String[0].getClass().getName(), ABBREVIATION_ARRAY_STRING);
		// register the util types
		register(Vector.class.getName(), ABBREVIATION_VECTOR);
		register(Hashtable.class.getName(), ABBREVIATION_HASHTABLE);
		register(Stack.class.getName(), ABBREVIATION_STACK);
	}
	
	/**
	 * Registers a certain abbreviation for the specified class name. Note
	 * that neither class names nor abbreviations are allowed to be an empty 
	 * string. If the class name has been already registered, if the 
	 * abbreviation has been used already, or if the they do not conform the 
	 * the conventions described above, this method will throw an exception. 
	 * Note that neither the class name nor the abbreviation must be null, 
	 * otherwise an exception will be thrown.
	 * 
	 * @param classname The class name that should be registered.
	 * @param abbreviation The abbreviation that should be registered.
	 * @throws IllegalArgumentException Thrown if the class name or the
	 * 	abbreviation has been registered already or if one of them
	 *  represents the empty string.
	 */
	public static void register(String classname, String abbreviation) {
		if (classname == null) 
			throw new NullPointerException("Class name must not be null.");
		if (abbreviation == null) 
			throw new NullPointerException("Abbreviation must not be null.");
		if (classname.length() == 0) 
			throw new IllegalArgumentException("Class name is malformed.");
		if (abbreviation.length() == 0) 
			throw new IllegalArgumentException("Abbreviation is malformed.");
		if (classnames.containsKey(classname) && !classnames.get(classname).equals(abbreviation)) 
			throw new IllegalArgumentException("Class name has been registered already.");
		if (abbreviations.containsKey(abbreviation) && !abbreviations.get(abbreviation).equals(classname)) 
			throw new IllegalArgumentException("Abbreviation has been registered already.");
		classnames.put(classname, abbreviation);
		abbreviations.put(abbreviation, classname);
	}
	
	/**
	 * Returns the class name for the specified abbreviation. If the
	 * abbreviation has not been registered, this method will return
	 * the abbreviation that has been passed to the method. Note that
	 * the abbreviation must not be null, otherwise an exception will
	 * be thrown.
	 * 
	 * @param abbreviation The abbreviation that should be translated
	 * 	into a class name.
	 * @return The class name for the abbreviation or the abbreviation
	 * 	itself, if the abbreviation has not been registered.
	 * @throws NullPointerException Thrown if the abbreviation is
	 * 	null.
	 */
	public static String getClassname(String abbreviation) {
		String result = (String)abbreviations.get(abbreviation);
		if (result == null) {
			return abbreviation;
		} else {
			return result;
		}
	}
	
	/**
	 * Returns the abbreviation for the specified class name. If the
	 * class name has not been registered already, this method will
	 * return the class name. Note that the class name must not be
	 * null, otherwise an exception will be thrown.
	 * 
	 * @param classname The class name whose abbreviation needs to
	 * 	be retrieved.
	 * @return The abbreviation for the class name or the class name
	 * 	itself, if the class name has not been registered.
	 * @throws NullPointerException Thrown if the class name is null.
	 */
	public static String getAbbreviation(String classname) {
		String result = (String)classnames.get(classname);
		if (result == null) {
			return classname;
		} else {
			return result;
		}
	}
	
	/**
	 * Determines whether the specified class name has been registered
	 * already. Returns true if the class name has an abbreviation,
	 * false otherwise. Note that this method will throw an exception
	 * if the class name is null.
	 * 
	 * @param classname The class name of interest.
	 * @return True if the class name has been registered, false
	 * 	otherwise.
	 * @throws NullPointerException Thrown if the class name is null.
	 */
	public static boolean containsClassname(String classname) {
		return classnames.containsKey(classname);
	}
	
	/**
	 * Determines whether the specified abbreviation has been registered
	 * already. Returns true if the abbreviation is in use, false if
	 * the abbreviation is not used. Note that this method will throw
	 * an exception if the abbreviation is null.
	 * 
	 * @param abbreviation The abbreviation of interest.
	 * @return True if the abbreviation is in use already, false otherwise.
	 * @throws NullPointerException Thrown if the abbreviation is null.
	 */
	public static boolean containsAbbreviation(String abbreviation) {
		return abbreviations.containsKey(abbreviation);
	}
	
}
