/**
 * 
 */
package info.pppc.base.system.util;

/**
 * A helper class for static object references to avoid 
 * overhead from garbage collection.
 * 
 * @author Mac
 */
public class Static {

	/**
	 * A static boolean object representing true.
	 */
	public static final Boolean TRUE = new Boolean(true);
	
	/**
	 * A static boolean object representing false.
	 */
	public static final Boolean FALSE = new Boolean(false);
	
}
