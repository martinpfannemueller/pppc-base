/*
 * Revision: $Revision: 1.3 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/05/20 16:02:35 $ 
 */
package info.pppc.base.lease;

import java.io.IOException;

import info.pppc.base.system.ObjectID;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

/**
 * The lease is used by the lease registry to maintain listeners
 * and to perform remote and local signaling. Users can pass the
 * lease to remote systems to register remote listeners that listen
 * to changes of a locally created lease. Users should never cerate
 * a lease using the constructor instead they should create the
 * lease through the lease registry. A lease created through the 
 * default constructor of this class will lead to failures.
 * 
 * @author Mac
 */
public final class Lease implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BL";
	
	/**
	 * The id of the lease as created by the system that
	 * created the lease.
	 */
	private ObjectID identifier;

	/**
	 * The system id of the system that created the lease.
	 */
	private SystemID creator;
	
	/**
	 * The timeout of the lease as specified by the creator.
	 */
	private long timeout;
	
	/**
	 * Creates a new uninitialized lease. This method is not
	 * intended to be called by user code. It solely for 
	 * deserialization.
	 */
	public Lease() {
		super();
	}

	/**
	 * Creates a new unique lease with the specified timeout value.
	 * The lease creator system will be the local system and the lease
	 * identifier will be a new locally unique object identifier. The
	 * timeout value will be set to the specified value.
	 * 
	 * @param timeout The timeout value of the lease.
	 * @return A new lease for the local system with the specified
	 * 	timeout value.
	 */
	protected static final Lease create(long timeout) {
		Lease lease = new Lease();
		lease.identifier = ObjectID.create();
		lease.creator = SystemID.SYSTEM;
		lease.timeout = timeout;
		return lease;
	}

	/**
	 * Returns the identifier of the lease.
	 * 
	 * @return The identifier of the lease.
	 */
	public ObjectID getIdentifier() {
		return identifier;
	}
	
	/**
	 * Returns the creator system of the lease.
	 * 
	 * @return The creator system of the lease.
	 */
	public SystemID getCreator() {
		return creator;
	}
	
	/**
	 * Returns the timeout period of the lease.
	 * 
	 * @return The timeout period of the lease.
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Deserializes the lease from the given input.
	 * 
	 * @param input The input to read from.
	 * @throws IOException Thrown by the underlying input.
	 */
	public void readObject(IObjectInput input) throws IOException {
		identifier = (ObjectID)input.readObject();
		creator = (SystemID)input.readObject();
		timeout = input.readLong();
	}

	/**
	 * Serializes the lease to the specified output.
	 * 
	 * @param output The output to write to.
	 * @throws IOException Thrown by the underlying output.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(identifier);
		output.writeObject(creator);
		output.writeLong(timeout);
	}
	
	/**
	 * Determines whether this lease is structurally equivalent
	 * to the specified object.
	 * 
	 * @param o The object to compare to.
	 * @return True if the lease equals the specified object, false
	 * 	otherwise.
	 */
	public boolean equals(Object o) {
		if (o != null && o.getClass() == getClass()) {
			Lease ls = (Lease)o;
			return (identifier.equals(ls.identifier) &&
					creator.equals(ls.creator) &&
					timeout == ls.timeout);
		}
		return false;
	}

	/**
	 * Returns a content-based hashcode for the lease.
	 * 
	 * @return A content-based hashcoode.
	 */
	public int hashCode() {
		return (int)timeout + identifier.hashCode() + creator.hashCode();
	}

	/**
	 * Returns a human readable string representation of the lease.
	 * 
	 * @return A human readable string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("(");
		b.append(creator);
		b.append("|");
		b.append(identifier);
		b.append("|");
		b.append(timeout);
		b.append(")");
		return b.toString();
	}


}
