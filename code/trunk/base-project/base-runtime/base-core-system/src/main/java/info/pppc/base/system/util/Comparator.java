package info.pppc.base.system.util;

/**
 * This class provides helper methods to enable comparisons and type operations
 * on J2ME CLDC configurations. Specifically it allows to transform strings into
 * various types and it enables comparisons between wrapper classes of basic
 * types. 
 * 
 * @author Marcus Handte
 */
public final class Comparator {
	
	/**
	 * Determines whether the first object is smaller than the second object
	 * passed to the method. This implementation works with Byte, Short,
	 * Integer, Long and String types.
	 * 
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return True if the first object is smaller than the second object.
	 * @throws IllegalArgumentException Thrown if one of the objects is 
	 * 	illegal.
	 */
	public static boolean isLess(Object o1, Object o2) throws IllegalArgumentException {
		Long l1 = null;
		Long l2 = null;
		String s1 = null;
		String s2 = null;
		if (o1 instanceof Byte) l1 = new Long(((Byte)o1).byteValue());
		if (o1 instanceof Short) l1 = new Long(((Short)o1).shortValue());
		if (o1 instanceof Integer) l1 = new Long(((Integer)o1).intValue());
		if (o1 instanceof Long) l1 = (Long)o1;
		if (o1 instanceof String) s1 = (String)o1;
		if (o2 instanceof Byte) l2 = new Long(((Byte)o2).byteValue());
		if (o2 instanceof Short) l2 = new Long(((Short)o2).shortValue());
		if (o2 instanceof Integer) l2 = new Long(((Integer)o2).intValue());
		if (o2 instanceof Long) l2 = (Long)o2;
		if (o2 instanceof String) s2 = (String)o2;
		if ((l1 != null) && (l2 != null))
			return l1.longValue() < l2.longValue();
		if ((s1 != null) && (s2 != null))
			return s1.compareTo(s2) < 0;
		throw new IllegalArgumentException("Improper arguments for comparison.");
	}

	/**
	 * Determines whether the first object is smaller or equal than the second 
	 * object passed to the method. This implementation works with Byte, Short,
	 * Integer, Long and String types.
	 * 
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return True if the first object is smaller than or larger as the second 
	 * 	object.
	 * @throws IllegalArgumentException Thrown if one of the objects is 
	 * 	illegal.
	 */
	public static boolean isLessOrEqual(Object o1, Object o2) throws IllegalArgumentException{
		Long l1 = null;
		Long l2 = null;
		String s1 = null;
		String s2 = null;
		if (o1 instanceof Byte) l1 = new Long(((Byte)o1).byteValue());
		if (o1 instanceof Short) l1 = new Long(((Short)o1).shortValue());
		if (o1 instanceof Integer) l1 = new Long(((Integer)o1).intValue());
		if (o1 instanceof Long) l1 = (Long)o1;
		if (o1 instanceof String) s1 = (String)o1;
		if (o2 instanceof Byte) l2 = new Long(((Byte)o2).byteValue());
		if (o2 instanceof Short) l2 = new Long(((Short)o2).shortValue());
		if (o2 instanceof Integer) l2 = new Long(((Integer)o2).intValue());
		if (o2 instanceof Long) l2 = (Long)o2;
		if (o2 instanceof String) s2 = (String)o2;
		if ((l1 != null) && (l2 != null))
			return l1.longValue() <= l2.longValue();
		if ((s1 != null) && (s2 != null))
			return s1.compareTo(s2) <= 0;
		throw new IllegalArgumentException("Improper arguments for comparison.");
	}


	/**
	 * Determines whether the first object is larger than the second object
	 * passed to the method. This implementation works with Byte, Short,
	 * Integer, Long and String types.
	 * 
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return True if the first object is larger than the second object.
	 * @throws IllegalArgumentException Thrown if one of the objects is 
	 * 	illegal.
	 */
	public static boolean isMore(Object o1, Object o2) throws IllegalArgumentException {
		Long l1 = null;
		Long l2 = null;
		String s1 = null;
		String s2 = null;
		if (o1 instanceof Byte) l1 = new Long(((Byte)o1).byteValue());
		if (o1 instanceof Short) l1 = new Long(((Short)o1).shortValue());
		if (o1 instanceof Integer) l1 = new Long(((Integer)o1).intValue());
		if (o1 instanceof Long) l1 = (Long)o1;
		if (o1 instanceof String) s1 = (String)o1;
		if (o2 instanceof Byte) l2 = new Long(((Byte)o2).byteValue());
		if (o2 instanceof Short) l2 = new Long(((Short)o2).shortValue());
		if (o2 instanceof Integer) l2 = new Long(((Integer)o2).intValue());
		if (o2 instanceof Long) l2 = (Long)o2;
		if (o2 instanceof String) s2 = (String)o2;
		if ((l1 != null) && (l2 != null))
			return l1.longValue() > l2.longValue();
		if ((s1 != null) && (s2 != null))
			return s1.compareTo(s2) > 0;
		throw new IllegalArgumentException("Improper arguments for comparison.");
	}

	/**
	 * Determines whether the first object is larger than the second object
	 * passed to the method. This implementation works with Byte, Short,
	 * Integer, Long and String types.
	 * 
	 * @param o1 The first object to compare.
	 * @param o2 The second object to compare.
	 * @return True if the first object is larger than or equal to the second object.
	 * @throws IllegalArgumentException Thrown if one of the objects is 
	 * 	illegal.
	 */
	public static boolean isMoreOrEqual(Object o1, Object o2) throws IllegalArgumentException {
		Long l1 = null;
		Long l2 = null;
		String s1 = null;
		String s2 = null;
		if (o1 instanceof Byte) l1 = new Long(((Byte)o1).byteValue());
		if (o1 instanceof Short) l1 = new Long(((Short)o1).shortValue());
		if (o1 instanceof Integer) l1 = new Long(((Integer)o1).intValue());
		if (o1 instanceof Long) l1 = (Long)o1;
		if (o1 instanceof String) s1 = (String)o1;
		if (o2 instanceof Byte) l2 = new Long(((Byte)o2).byteValue());
		if (o2 instanceof Short) l2 = new Long(((Short)o2).shortValue());
		if (o2 instanceof Integer) l2 = new Long(((Integer)o2).intValue());
		if (o2 instanceof Long) l2 = (Long)o2;
		if (o2 instanceof String) s2 = (String)o2;
		if ((l1 != null) && (l2 != null))
			return l1.longValue() >= l2.longValue();
		if ((s1 != null) && (s2 != null))
			return s1.compareTo(s2) >= 0;
		throw new IllegalArgumentException("Improper arguments for comparison.");
	}


	/**
	 * Converts the string representation of a boolean into a boolean.
	 * 
	 * @param string The string representation of the boolean.
	 * @return True if the string representation is a boolean true, false if
	 * 	it is a boolean false.
	 * @throws IllegalArgumentException Thrown if the string does not specify
	 * 	a valid boolean representation.
	 */
	public static boolean getBoolean(String string) throws IllegalArgumentException {
		String tstring = String.valueOf(true);
		String fstring = String.valueOf(false);
		if (tstring.equals(string)) {
			return true;
		} else if (fstring.equals(string)) {
			return false;
		} else {
			throw new IllegalArgumentException("Improper arguments for conversion.");
		}
	}

}
