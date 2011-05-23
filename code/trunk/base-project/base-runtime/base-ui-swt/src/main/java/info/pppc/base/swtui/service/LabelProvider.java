package info.pppc.base.swtui.service;

import info.pppc.base.service.ServiceDescriptor;
import info.pppc.base.service.ServiceProperties;
import info.pppc.base.swtui.service.data.InvokeData;
import info.pppc.base.swtui.service.data.SearchData;

import java.util.Enumeration;

/**
 * This class provides utilities for the service pane.
 * 
 * @author Marcus Handte
 */
public class LabelProvider {
	
	/**
	 * Returns a string for the specified service descriptor.
	 * 
	 * @param d The service descriptor for which a string
	 * 	needs to be generated.
	 * @return The string for the service descriptor.
	 */
	public static String toString(ServiceDescriptor d) {
		StringBuffer b = new StringBuffer();
		b.append("(");
		b.append(d.getName());
		b.append(") (");
		String[] ifaces = d.getInterfaces();
		for (int i = 0; i < ifaces.length; i++) {
			b.append(ifaces[i]);
			if (i != ifaces.length - 1) {
				b.append(", ");	
			}
		}
		b.append(") (");
		ServiceProperties props = d.getProperties();
		Enumeration e = props.getProperties();
		while (e.hasMoreElements()) {
			String prop = (String)e.nextElement();
			String val = props.getProperty(prop);
			b.append(prop);
			b.append("=");
			b.append(val);
			if (e.hasMoreElements()) {
				b.append(", ");	
			}				 
		}
		b.append(") (");
		b.append(d.getIdentifier());
		b.append(")");
		return b.toString();
	}
	
	/**
	 * Returns the user interface string for the specified search data.
	 * 
	 * @param d The search data whose string needs to be retrieved.
	 * @return The string for the specified search data.
	 */
	public static String toString(SearchData d) {
		StringBuffer b = new StringBuffer();
		switch (d.getLookup()) {
			case SearchData.LOOKUP_BOTH:
				b.append("BOTH"); break;
			case SearchData.LOOKUP_LOCAL_ONLY:
				b.append("LOCAL"); break;
			case SearchData.LOOKUP_REMOTE_ONLY:
				b.append("REMOTE"); break;
			default:
				// will never happen
		}
		b.append(";");
		b.append(d.getName());
		b.append(";");
		String[] ifaces = d.getInterfaces();
		for (int i = 0; i < ifaces.length; i++) {
			b.append(ifaces[i]);
			if (i != ifaces.length - 1) {
				b.append(", ");	
			}
		}
		b.append(";");
		String[] props = d.getProperites();
		for (int i = 0; i < props.length; i++) {
			String prop = props[i];
			String val = d.getProperty(prop);
			b.append(prop);
			b.append("=");
			b.append(val);
			if (i != props.length - 1) {
				b.append(", ");	
			}
		}
		return b.toString();
	}
	
	/**
	 * Returns the string for the specified invoke data.
	 * 
	 * @param d The invoke data whose string needs to be
	 * 	retrieved.
	 * @return The user interface string for the invoke data.
	 */
	public static String toString(InvokeData d) {
		StringBuffer b = new StringBuffer();
		switch (d.getReturnType()) {
			case InvokeData.TYPE_VOID:
				b.append("void ");
				break;
			case InvokeData.TYPE_INTEGER:
				b.append("int ");
				break;
			case InvokeData.TYPE_STRING:
				b.append("String ");
				break;
			default:
				// will never happen
		}
		b.append(d.getName());
		b.append("(");
		Object[] parameters = d.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Object param = parameters[i];
			if (param instanceof String) {
				b.append("String");
			} else if (param instanceof Integer) {
				b.append("int");
			} else {
				// will never happen
			}
			b.append("=");
			b.append(param.toString());
			if (i != parameters.length - 1) {
				b.append(", ");
			}
		}
		b.append(")");
		return b.toString();
	}
}
