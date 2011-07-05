package info.pppc.base.system.nf;

import info.pppc.base.system.IExtension;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The non-functional collection is used to represent requirements on
 * the communication stack. The collection consists of several dimensions
 * that specify the individual requirements. Convenience methods are 
 * provided for typical compositions. These can be overwritten in 
 * proxies to define specific requirements.
 * 
 * @author Marcus Handte
 */
public final class NFCollection implements ISerializable, IExtension {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BC";

	
	/**
	 * A type that can be used to create the default collection for
	 * an invocation that establishes a session key using Diffie
	 * Hellmann over an authenticated 3-way handshake.
	 */
	public static final short TYPE_EXCHANGE = 3;

	/**
	 * A type that can be used to create the default collection for
	 * an invocation that opens a stream. The stream will bypass
	 * most of base's robustness mechanisms, however, it will be 
	 * faster if large amounts of data need to be transferred.
	 */
	public static final short TYPE_STREAM = 2;
	
	/**
	 * A type that can be used to create the default collection for an 
	 * invocation that is a asynchronous. An asynchronous invocation
	 * does not have a return value and thus, it can be sent best effort
	 * without signaling.
	 */
	public static final short TYPE_ASYNCHRONOUS = 1;
	
	/**
	 * A type that can be used to create the default collection for an 
	 * invocation that is asynchronous. A synchronous invocation or
	 * a deferred synchronous invocation do have results and thus they
	 * must be delivered with signaling.
	 */
	public static final short TYPE_SYNCHRONOUS = 0;

	/**
	 * A factory method that creates the default non-functional 
	 * collections for invocations delivered with statically generated
	 * proxies.
	 * 
	 * @param gateway The type that indicates whether routing should
	 * 	use remote gateways to connect.
	 * @param type The type of collection to create. This can be
	 * 	one of the type constants specified in this class.
	 * @return An collection that will enforce the default usage for
	 * 	the specified type.
	 */
	public static NFCollection getDefault(short type, boolean gateway) {
		NFCollection collection = new NFCollection();
		// state that the semantic is required
 		NFDimension required = new NFDimension(
			NFDimension.IDENTIFIER_REQUIRED,
			new Boolean(true)
		);
		collection.addDimension(EXTENSION_SEMANTIC, required);
		// adjust semantic type according to the type
		NFDimension typed = null;
		switch (type) {
			case TYPE_EXCHANGE:
			case TYPE_SYNCHRONOUS:
		 	case TYPE_ASYNCHRONOUS:
		 	case TYPE_STREAM:
		 		typed = new NFDimension(NFDimension.IDENTIFIER_TYPE, new Short(type));
		 		break;
		 	default:
				throw new IllegalArgumentException("Illegal collection type.");
		}
		collection.addDimension(EXTENSION_SEMANTIC, typed);
		// set the remote routing attribute accordingly
		NFDimension gwd = new NFDimension(NFDimension.IDENTIFIER_GATEWAY, new Boolean(gateway));
		collection.addDimension(EXTENSION_ROUTING, gwd);
		return collection;
		
	}
	
	

	/**
	 * A hash table that contains vectors for each extension layer. The
	 * vector contains the dimensions that have been specified for the
	 * extension.
	 */
	private Hashtable extensions = new Hashtable();


	/**
	 * Creates a new collection with no properties set.
	 */
	public NFCollection() {
		super();
	}
	
	/**
	 * Adds a dimension to the specified extension.
	 * 
	 * @param extension The extension to add the dimension.
	 * @param dimension The dimension that is added to the extension.
	 * @return The dimension that got replaced if any, else null.
	 */
	public synchronized NFDimension addDimension(short extension, NFDimension dimension) {
		Vector dims = (Vector)extensions.get(new Short(extension));
		NFDimension replaced = null;
		if (dims == null) {
			dims = new Vector();
			extensions.put(new Short(extension), dims);
		}
		for (int i = dims.size() - 1; i >= 0; i--) {
			NFDimension dim = (NFDimension)dims.elementAt(i);
			if (dim.getIdentifier() == dimension.getIdentifier()) {
				replaced = dim;
				dims.removeElementAt(i);
				break;
			}
		}
		dims.addElement(dimension);
		return replaced;
		
	}

	/**
	 * Returns the dimension from the specified extension with the specified
	 * dimension identifier.
	 * 
	 * @param extension The extension to search for.
	 * @param dimension The identifier of the dimension to lookup
	 * @return The dimension or null if it doesn't exist.
	 */
	public synchronized NFDimension getDimension(short extension, short dimension) {
		Vector dims = (Vector)extensions.get(new Short(extension));
		if (dims == null) {
			return null;	
		} else {
			for (int i = dims.size() - 1; i >= 0; i--) {
				NFDimension dim = (NFDimension)dims.elementAt(i);
				if (dim.getIdentifier() == dimension) {
					return dim;
				}
			}
			return null;
		}
	}

	/**
	 * Returns all dimensions that have been specified for the specified
	 * extension.
	 * 
	 * @param extension The extension to lookup.
	 * @return The dimensions that have been specified for the extension.
	 */
	public synchronized NFDimension[] getDimensions(short extension) {
		Vector dims = (Vector)extensions.get(new Short(extension));
		if (dims == null) {
			return new NFDimension[0];
		} else {
			NFDimension[] result = new NFDimension[dims.size()];
			for (int i = dims.size() - 1; i >= 0; i--) {
				result[i] = (NFDimension)dims.elementAt(i);
			}
			return result;
		}
	}
	
	/**
	 * Removes the specified dimension from the specified extension requirement.
	 * 
	 * @param extension The extension point to consider during removal.
	 * @param dimension The dimension to remove from the specified extension.
	 */
	public synchronized void removeDimension(short extension, short dimension) {
		Vector dims = (Vector)extensions.get(new Short(extension));
		if (dims != null) {
			for (int i = dims.size() - 1; i >= 0; i--) {
				NFDimension dim = (NFDimension)dims.elementAt(i);
				if (dim.getIdentifier() == dimension) {
					dims.removeElementAt(i);
					break;
				}
			}
			if (dims.size() == 0) {
				extensions.remove(new Short(extension));
			}
		}
	}

	/**
	 * Deserializes the object from the stream.
	 * 
	 * @param input The input to read from.
	 * @throws IOException Thrown if the deserialization failed.
	 */
	public synchronized void readObject(IObjectInput input) throws IOException {
		extensions = (Hashtable)input.readObject();
	}

	/**
	 * Serializes the object to the stream.
	 * 
	 * @param output The output to write to.
	 * @throws IOException Thrown if the serialization failed.
	 */
	public synchronized void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(extensions);
	}

	/**
	 * Creates a copy of this collection of parameters. The flag indicates
	 * whether the returned copy is deep or shallow. If a deep copy is
	 * created, all dimensions are copied, too.
	 * 
	 * @param deep Set to true to create a deep copy, set to false to create
	 * 	a shallow copy.
	 * @return A copy of the collection of non-functional parameters.
	 */
	public synchronized NFCollection copy(boolean deep) {
		NFCollection collection = new NFCollection();
		Enumeration e = extensions.keys();
		while (e.hasMoreElements()) {
			Short ext = (Short)e.nextElement();
			Vector dims = (Vector)extensions.get(ext);
			if (dims != null) {
				Vector copy = new Vector();
				for (int i = 0; i < dims.size(); i++) {
					NFDimension dim = (NFDimension)dims.elementAt(i);
					if (deep) {
						dim = dim.copy();
					}
					copy.addElement(dim);
				}
				collection.extensions.put(ext, copy);									
			}
		}
		return collection;
	}

	/**
	 * Returns a string representation of the collection.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		Enumeration e = extensions.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Vector v = (Vector)extensions.get(key);
			for (int i = 0; i < v.size(); i++) {
				Object o = v.elementAt(i);
				b.append("EXTENSION <");
				b.append(key.toString());
				b.append("> DIMENSION <");
				if (o != null) {
					b.append(o.toString());
				} else {
					b.append("NULL");
				}
				b.append("> ");
			}
			b.append("\n");
		}
		return b.toString();
	}


}
