package info.pppc.base.swtui.service.data;

import java.util.Vector;

/**
 * The invoke is a data object that encapsulates an invocation
 * with a name.
 * 
 * @author Marcus Handte
 */
public class InvokeData {

	/**
	 * The type constant for void return values.
	 */
	public static final int TYPE_VOID = 0;
	
	/**
	 * The type constant for string parameters and return types.
	 */
	public static final int TYPE_STRING = 1;
	
	/**
	 * The type constant for integer parameters and return types.
	 */
	public static final int TYPE_INTEGER = 2;
	
	/**
	 * The name of the invoke data object used in the user interface
	 * to represent this object.
	 */
	private String name = "";
	
	/**
	 * The return type of the invocation.
	 */
	private int returnType = TYPE_VOID;
	
	/**
	 * The parameters of the invocation.
	 */
	private Vector parameters = new Vector();
	
	/**
	 * Creates a new invoke data object that is empty.
	 */
	public InvokeData() {
		super();
	}
	
	/**
	 * Sets the name of the invoke data object as used in the user
	 * interface.
	 * 
	 * @param name The name of the invoke data object.
	 */
	public void setName(String name) {
		if (name != null) {
			this.name = name;	
		}
	}
	
	/**
	 * Returns the name of the invoke data object.
	 * 
	 * @return The name of the invoke data object.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns a human readable string representation of the invoke
	 * object.
	 * 
	 * @return A human readable string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("NAME (");
		b.append(getName());
		b.append(")");
		return b.toString();
	}

	/**
	 * Sets the return type of the invocation. This must
	 * be one of the type constants.
	 * 
	 * @param type The return type of the invocation.
	 */
	public void setReturnType(int type) {
		if (type == TYPE_INTEGER || type == TYPE_STRING || type == TYPE_VOID) {
			returnType = type;	
		}
	}
	
	/**
	 * Returns the return type of the invocation. This
	 * will be one of the type constants.
	 * 
	 * @return The return type of the method.
	 */
	public int getReturnType() {
		return returnType;
	}
	
	/**
	 * Adds the specified parameter. The parameter
	 * must be of type integer or type string.
	 * 
	 * @param parameter The parameter to add.
	 */
	public void addParameter(Object parameter) {
		if (parameter == null) return;
		if (parameter instanceof String || parameter instanceof Integer) {
			parameters.addElement(parameter);	
		}
	}
	
	/**
	 * Removes the parameter at the specified index.
	 * 
	 * @param idx The index of the parameter to remove.
	 */
	public void removeParameter(int idx) {
		parameters.removeElementAt(idx);
	}
	
	/**
	 * Moves the parameter at the index from to the 
	 * index to.
	 * 
	 * @param from The index of the element to move.
	 * @param to The index to move the element to.
	 */
	public void moveParameter(int from, int to) {
		if (from < 0 || from >= parameters.size()) return;
		if (to < 0 || from >= parameters.size()) return;
		Object param = parameters.elementAt(from);
		parameters.removeElementAt(from);
		parameters.insertElementAt(param, to);
	}
	
	/**
	 * Returns the ordered list of parameters.
	 * 
	 * @return The ordered list of parameters.
	 */
	public Object[] getParameters() {
		Object[] ps = new Object[parameters.size()];
		for (int i = 0; i < ps.length; i++) {
			ps[i] = parameters.elementAt(i);
		}
		return ps;
	}
	
	/**
	 * Returns the signature for the current invocation.
	 * 
	 * @return The signature for the current invocation.
	 */
	public String getSignature() {
		StringBuffer b = new StringBuffer();
		switch (returnType) {
			case TYPE_VOID:
				b.append("void ");
				break;
			case TYPE_INTEGER:
				b.append("int ");
				break;
			case TYPE_STRING:
				b.append("java.lang.String ");
				break;
			default:
				// will never happen
		}
		b.append(name);
		b.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			Object param = parameters.elementAt(i);
			if (param instanceof String) {
				b.append("java.lang.String");
			} else if (param instanceof Integer) {
				b.append("int");
			} else {
				// will never happen
			}
			if (i != parameters.size() - 1) {
				b.append(", ");
			}
		}
		b.append(")");
		return b.toString();
	}
	
}
