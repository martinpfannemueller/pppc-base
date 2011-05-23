package info.pppc.base.service;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A properties object contains a list of name value pairs or so-called
 * properties. Properties are used to model non-functional parameters. 
 * 
 * @author Marcus Handte
 */
public class ServiceProperties implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BT";
	
	/**
	 * The properties contained in this property object.
	 */
	private Hashtable properties;
	
	/**
	 * Creates a new set of properties. The properties are 
	 * initially empty.
	 */
	public ServiceProperties() {
		properties = new Hashtable();	
	}

	/**
	 * Sets the specified property to the specified value.
	 * The value and the name must not be null, otherwise
	 * this method will raise a null pointer exception.
	 * 
	 * @param name The name of the property.
	 * @param value The value of the property. 
	 */
	public void setProperty(String name, String value) {
		if (name == null || value == null) {
			throw new NullPointerException("Null is not a valid property.");
		}
		properties.put(name, value);
	}
	
	/**
	 * Determines whether the given property is available.
	 * 
	 * @param name The name of the property to lookup.
	 * @return True if the property exists, false otherwise.
	 */
	public boolean hasProperty(String name) {
		return properties.containsKey(name);
	}
	
	/**
	 * Returns the value of the property with the specified name.
	 * 
	 * @param name The name to lookup.
	 * @return The value of the specified property.
	 */
	public String getProperty(String name) {
		return (String)properties.get(name);
	}
	
	/**
	 * Removes the property with the specified name.
	 * 
	 * @param name The name of the property to remove.
	 */
	public void removeProperty(String name) {
		properties.remove(name);
	}
	
	/**
	 * Returns the names of the properties that are encapsulated
	 * in this object.
	 * 
	 * @return The properties of this object.
	 */
	public Enumeration getProperties() {
		return properties.keys();
	}
	
	/**
	 * Determines whether the passed properties are completely
	 * contained within is set of properties.
	 * 
	 * @param props The properties that should be compared
	 * 	to this properties. If the properties passed to this
	 * 	method are null, a null pointer exception will be 
	 * 	raised.
	 * @return True if the passed properties are completely
	 * 	contained in the set of properties denoted by this
	 * 	property object. The comparison of properties is 
	 * 	done using the built in equals operator.
	 */
	public boolean contains(ServiceProperties props) {
		Enumeration e = props.getProperties();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = props.getProperty(key);
			String value2 = getProperty(key);
			if (! value.equals(value2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines whether the property object equals another
	 * object. Note that this comparison performs a deep
	 * equals based on the content of the property object.
	 * 
	 * @param o The object to compare to.
	 * @return True if the objects are the same, false otherwise.
	 */
	public boolean equals(Object o) {
		if (o != null && o.getClass() == getClass()) {
			ServiceProperties p = (ServiceProperties)o;
			Vector v = new Vector();
			Enumeration e = getProperties();
			while (e.hasMoreElements()) {
				v.addElement(e.nextElement());
			}
			e = p.getProperties();
			while (e.hasMoreElements()) {
				String key = (String)e.nextElement();
				if (v.contains(key)) {
					v.removeElement(key);
					String value = p.getProperty(key);
					String value2 = getProperty(key);
					if (value.equals(value2)) {
						continue;
					}
				}
				return false;
			}
			return v.isEmpty();
		} 
		return false;
	}
	
	/**
	 * Returns the hash code of this property set.
	 * 
	 * @return The hash code.
	 */
	public int hashCode() {
		int code = 0;
		Enumeration e = getProperties();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			code += key.hashCode();
			String value = getProperty(key);
			code += value.hashCode();
		}
		return code;
	}


	/**
	 * Reads the properties from a stream.
	 * 
	 * @param input The input stream to read from.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void readObject(IObjectInput input) throws IOException {
		properties = (Hashtable)input.readObject();
	}

	/**
	 * Writes the properties to the output stream.
	 * 
	 * @param output The output stream to write to.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(properties);
	}

	/**
	 * Returns a string representation of the properties
	 * encapsulated by this object.
	 * 
	 * @return The string representation of the properties.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		Enumeration e = getProperties();
		while (e.hasMoreElements()) {
			String name = (String)e.nextElement();
			buffer.append(name);
			buffer.append("=");
			buffer.append(getProperty(name));	
			if (e.hasMoreElements()) {
				buffer.append(", ");
			}
		}
		return buffer.toString();
	}

}
