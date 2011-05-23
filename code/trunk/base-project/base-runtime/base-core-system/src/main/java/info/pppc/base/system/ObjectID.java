package info.pppc.base.system;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;

/**
 * The ObjectID identifies a unique object on a certain system. This
 * means that ObjectIDs are used to identify a globally unique object. 
 * 
 * @author Marcus Handte
 */
public final class ObjectID implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BO";
	
	/**
	 * Digits used to create a string representation.
	 */
	private static final char digits[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'a', 'b', 'c', 'd', 'e', 'f'
    };
	
	/**
	 * This is the local counter for object ids.
	 */
	private static long COUNTER = 0;

    /**
     * This field denotes the creator of the object id. Together with
     * the count and the current epoche, this field identifies an object
     * uniquely.
     */
    private SystemID creator = SystemID.SYSTEM;

    /**
     * A locally unique and epoche independent count. For the current
     * epoche on the creator system, this value identifies an object
     * uniquely. 
     */
    private long count;

	/**
	 * Creates a new globally unique object id.
	 * 
	 * @return A new globally unique object id.
	 */
	public static ObjectID create() {
		ObjectID id = new ObjectID();
		synchronized (ObjectID.class) {
			COUNTER--;
			id.count = COUNTER;
		}
		return id;
	}

    /**
     * Generates a new uninitialized object id. This constructor is
     * intended for deserialization only. It must not be called 
     * from user code. To create a new and globally unique id, a 
     * user must call the create method. 
     */
    public ObjectID() {
		super();
    }  

	/**
	 * Generates a new well known object id. The value set as parameter
	 * must be non-negative, otherwise this method will throw an illegal
	 * argument exception. Search for references to this constructor to 
	 * find the well known object ids that have been assigned already.
	 * 
	 * @param value The id of the well known object id.
	 */
	public ObjectID(long value) {
		if (value >= 0) {
			creator = null;
			count = value;
		} else {
			throw new IllegalArgumentException
				("Well known object ids must be positive.");
		}
	}

	/**
	 * Returns the hash code of the id. 
	 * 
	 * @return The hash code of the id.
	 */
	public int hashCode() {
		if (creator != null) {
			return (int) count + (int)creator.hashCode();	
		} else {
			return (int) count;
		}
	}

	/**
	 * Determines whether this object id represents a well known object id.
	 * 
	 * @return True if the object id is a well known object id.
	 */
	public boolean isKnown() {
		return creator == null;
	}

	/**
	 * Compares the specified object with this id for equality.
	 * 
	 * @param object The object to compare to.
	 * @return True if the object id equals the given object. False
	 * 	otherwise.
	 */
	public boolean equals(Object object) {
		if (object != null && object.getClass() == getClass()) {
			ObjectID oid = (ObjectID)object;
			if (creator != null) {
				return (creator.equals(oid.creator) &&
						count == oid.count);
			} else {
				return (oid.creator == null &&
						count == oid.count);
			}
		} else {
			return super.equals(object);
		}
	}

    /**
     * Returns a string representation of this <code>UID</code>.
     *
     * @return	a string representation of this <code>UID</code>
     */
    public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (creator != null) {
			buffer.append(creator);
			buffer.append(" ");
		}
		long cid = count;
		int j = 64;
        final int k = 1 << 4;
        final long l1 = k - 1;
        char ac[] = new char[64];
        do {
            ac[--j] = digits[(int)(cid & l1)];
            cid >>>= 4;
        } while(cid != 0L);     
		buffer.append(new String(ac, j, 64 - j));
		return buffer.toString();
    }

	/**
	 * Writes the ObjectID to the given stream.
	 * 
	 * @param stream The stream to write to.
	 * @throws IOException If writing to the stream caused an exception.
	 */
	public void writeObject(IObjectOutput stream) throws IOException {
		stream.writeLong(count);
		if (count < 0) {
			stream.writeObject(creator);
		}
	} 	

	/**
	 * Reads the ObjectID from a stream.
	 * 
	 * @param input The stream to read from.
	 * @throws IOException Thrown if reading caused this exception.
	 */
	public void readObject(IObjectInput input) throws IOException {
		count = input.readLong();
		if (count < 0) {
			creator =(SystemID)input.readObject();
		} else {
			creator = null;
		}
	}
	
}


