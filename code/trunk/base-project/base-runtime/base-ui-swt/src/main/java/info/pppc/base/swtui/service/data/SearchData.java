package info.pppc.base.swtui.service.data;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The search object is used to encapsulate a service search.
 * 
 * @author Marcus Handte
 */
public class SearchData {

	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on the local device only.
	 */
	public static final int LOOKUP_LOCAL_ONLY = 1;

	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on the remote devices only.
	 */
	public static final int LOOKUP_REMOTE_ONLY = 2;
	
	/**
	 * A constant used within the lookup methods to determine that the lookup
	 * should be performed on both, local and remote devices.
	 */
	public static final int LOOKUP_BOTH = 4;	
	
	/**
	 * The scope of the lookup. This will be one of the constants
	 * defined above.
	 */
	private int lookup = LOOKUP_LOCAL_ONLY;
	
	/**
	 * The name of the service to search.
	 */
	private String name;
	
	/**
	 * The interfaces of the service to search.
	 */
	private Vector interfaces = new Vector();
	
	/**
	 * The properties of the service to search.
	 */
	private Hashtable properties = new Hashtable();
	
	/**
	 * The search data object is used to represent a service 
	 * search that has a certain name.
	 */
	public SearchData() {
		super();
	}
	
	/**
	 * Returns the name of the service to retrieve.
	 * 
	 * @return The name of the service to retrieve.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the service to retrieve.
	 * 
	 * @param name The name of the service to retrieve.
	 */
	public void setName(String name) {
		this.name = name;
	}

	
	/**
	 * Adds the specified interface.
	 * 
	 * @param iface The interface to add.
	 */
	public void addInterface(String iface) {
		if (! interfaces.contains(iface)) {
			interfaces.addElement(iface);
		}
	}
	
	/**
	 * Removes the interface with the specified name.
	 * 
	 * @param iface The name of the interface to remove.
	 */
	public void removeInterface(String iface) {
		interfaces.removeElement(iface);
	}
	
	/**
	 * Returns the specified interfaces.
	 * 
	 * @return The interfaces contained in the query.
	 */
	public String[] getInterfaces() {
		String[] s = new String[interfaces.size()];
		for (int i = 0; i < interfaces.size(); i++) {
			s[i] = (String)interfaces.elementAt(i);
		} 
		return s;
	}
	
	/**
	 * Sets the property with the specified name to the specified
	 * value.
	 * 
	 * @param name The name of the property.
	 * @param value The value of the property.
	 */
	public void addProperty(String name, String value) {
		properties.put(name, value);
	}
	
	/**
	 * Removes the specified property for the set of properties.
	 * 
	 * @param name The name of the property to remove.
	 */
	public void removeProperty(String name) {
		properties.remove(name);
	}
	
	/**
	 * Returns the properties that are specified in the
	 * query.
	 * 
	 * @return The properties specified in the query.
	 */
	public String[] getProperites() {
		String[] result = new String[properties.size()];
		Enumeration e = properties.keys();
		for (int i = 0; i < result.length; i++) {
			result[i] = (String)e.nextElement();
		}
		return result;
	}
	
	/**
	 * Returns the value of the property with the specified
	 * name.
	 * 
	 * @param name The name of the property to retrieve.
	 * @return The value or null if the property does not
	 * 	exist.
	 */
	public String getProperty(String name) {
		return (String)properties.get(name);
	}

	/**
	 * Returns the lookup value. This will be one of the
	 * constants.
	 * 
	 * @return Returns the lookup value.
	 */
	public int getLookup() {
		return lookup;
	}

	/**
	 * Sets the lookup value to the specified value.
	 * 
	 * @param lookup The lookup to set.
	 */
	public void setLookup(int lookup) {
		if (lookup == LOOKUP_BOTH || lookup == LOOKUP_LOCAL_ONLY 
				|| lookup == LOOKUP_REMOTE_ONLY) {
			this.lookup = lookup;	
		}
	}
	
	/**
	 * Returns a human readable string representation of the search
	 * data object.
	 * 
	 * @return A human readable string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("LOOKUP (");
		switch (getLookup()) {
			case SearchData.LOOKUP_BOTH:
				b.append("BOTH"); break;
			case SearchData.LOOKUP_LOCAL_ONLY:
				b.append("LOCAL"); break;
			case SearchData.LOOKUP_REMOTE_ONLY:
				b.append("REMOTE"); break;
			default:
				// will never happen
		}
		b.append(") NAME (");
		b.append(getName());
		b.append(") INTERFACES (");
		String[] ifaces = getInterfaces();
		for (int i = 0; i < ifaces.length; i++) {
			b.append(ifaces[i]);
			if (i > ifaces.length - 1) {
				b.append(", ");	
			}
		}
		b.append(") PROPERTIES (");
		String[] props = getProperites();
		for (int i = 0; i < props.length; i++) {
			String prop = props[i];
			String val = getProperty(prop);
			b.append(prop);
			b.append("=");
			b.append(val);
			if (i > props.length - 1) {
				b.append(", ");	
			}
		}
		b.append(")");
		return b.toString();
	}
	
}
