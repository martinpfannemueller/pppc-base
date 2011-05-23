package info.pppc.base.system.nf;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;
import info.pppc.base.system.util.Comparator;

import java.io.IOException;

/**
 * This class represents an nonfunctional attribute or dimension including a
 * tolerance window and some more features. Instances of this class are used
 * to represent individual requirements on the communication stack composition.
 * 
 * @author Marcus Handte
 */
public final class NFDimension implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BN";
	
	/**
	 * This is a marker dimension that denotes that the specified extension
	 * is required. The values of this dimension are booleans. If the hard
	 * value is true, the extension is required (i.e., no valid communication
	 * session can be created without the extension). If the hard value is
	 * false and the soft value is true, the selection strategy decides 
	 * whether the extension can be created.
	 */
	public final static short IDENTIFIER_REQUIRED = 1;
	
	/**
	 * This is an identifier that denotes that the specified extension should
	 * have the specified ability. The value objects are shorts, just like
	 * any ability. This dimension does only support a hard value.
	 */
	public final static short IDENTIFIER_ABILITY = 2;
	
	/**
	 * This is an identifier that denotes a specific type. At the present time
	 * this is solely used as part of semantic plug-ins. The only values supported
	 * are the short value 0 to denote a synchronous invocation and the short
	 * value 1 to denote asynchronous invocations. This value denotes a remote 
	 * invocation. This dimension does only support a hard value.
	 */
	public final static short IDENTIFIER_TYPE = 3;

	/**
	 * This is an identifier that denotes whether routing should use gateways
	 * This dimension only supports hard values and the value must be a boolean
	 * set to true if gateways shall be used and false if gateways shall not be
	 * used.
	 */
	public final static short IDENTIFIER_GATEWAY = 4;
	
	
	/**
	 * Constant that defines the ordering to be ascending. Ascending means
	 * that a larger value is better.
	 */
	public final static short ORDERING_ASCENDING = -1;

	/**
	 * Constant that defines the ordering to be descending. Descending means
	 * that a smaller value is better.
	 */	
	public final static short ORDERING_DESCENDING = 1;

	/**
	 * The identifier of the dimension.
	 */
	private short identifier;

	/**
	 * A boolean that indicates whether the dimension is ordered.
	 */
	private boolean ordered;

	/**
	 * A boolean that indicates the orientation of the dimension.
	 */
	private short orientation;

	/**
	 * The hard value requirement of the dimension.
	 */
	private Object hardValue;

	/**
	 * The soft value desired by this dimension.
	 */
	private Object softValue;

	/**
	 * This constructor is solely used during serialization, do not
	 * call this constructor from user code.
	 */
	public NFDimension() {}

	/**
	 * Creates a dimension that is not ordered and that denotes a single hard value.
	 * This is the simplest form of a dimension. It states "this value must be set"
	 * for this dimension identifier.
	 * 
	 * @param identifier The id of the dimension.
	 * @param hardValue The value of this dimension.
	 */
	public NFDimension(short identifier, Object hardValue) {
		this.identifier = identifier;
		this.ordered = false;
		this.hardValue = hardValue;
		checkDimension();
	}


	/**
	 * Creates a dimension that is ordered with the specified orientation and a single
	 * hard value. This form of dimension denotes that "a value must be less than
	 * the specified hard value" (descending) or "more than the specified hard value"
	 * (ascending).
	 * 
	 * @param identifier The identifier of the dimension.
	 * @param hardValue The hard value of the dimension.
	 * @param orientation The orientation of the dimension. The orientation must
	 * 	either be ascending or descending.
	 */
	public NFDimension(short identifier, short orientation, Object hardValue)  {
		this.identifier = identifier;
		this.ordered = true;
		this.orientation = orientation;
		this.hardValue = hardValue;
		checkDimension();
	}

	/**
	 * Creates a dimension that is ordered with the specified orientation under the
	 * specified hard and soft value. This form of dimension denotes that "a value must
	 * be less than the specified hard value and the soft value would be best fit" (descending)
	 * or "more than the specified hard value and the soft value would be best fit"
	 * (ascending).
	 * 
	 * @param identifier The identifier of the dimension.
	 * @param hardValue The hard value of the dimension.
	 * @param orientation The orientation of the dimension.
	 * @param softValue The soft value of the dimension.
	 */
	public NFDimension(short identifier, short orientation, Object hardValue, Object softValue) {
		this(identifier, orientation, hardValue);
		this.softValue = softValue;
		this.checkDimension();
	}


	/**
	 * Ensures that the given parameters of this object make sense.
	 * 
	 * @throws IllegalArgumentException Thrown if given parameters make no sense.
	 */
	private void checkDimension() throws IllegalArgumentException {
		if (hardValue == null) {
			throw new IllegalArgumentException("Hard value must be set.");
		}
		if (this.ordered && this.softValue != null) {
			// Check depending on order orientation that hard and soft values
			// are not logically swapped.
			if (this.orientation == ORDERING_ASCENDING) {
				if (Comparator.isLess(hardValue, softValue)) {
					throw new IllegalArgumentException("Hard and soft values mismatch orientation.");
				}
			} else if (this.orientation == ORDERING_DESCENDING) {
				if (Comparator.isMore(hardValue, softValue)) {
					throw new IllegalArgumentException("Hard and soft values mismatch orientation.");
				}
			} else {
				throw new IllegalArgumentException("Orientation must be ascending or descending.");
			}
		}
	}

	/**
	 * Returns the identifier of the dimension.
	 * 
	 * @return The identifier of the dimension.
	 */
	public short getIdentifier() {
		return identifier;
	}

	/**
	 * Returns if the dimension is ordered.
	 * 
	 * @return True, if ordered, else false.
	 */
	public boolean isOrdered() {
		return ordered;
	}

	/**
	 * Returns the hard value.
	 * 
	 * @return The hard value.
	 */
	public Object getHardValue() {
		return hardValue;
	}

	/**
	 * Returns the orientation of the dimension. See static 
	 * constants of the class for possible values.
	 * 
	 * @return The orientation.
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * Returns the soft value.
	 * 
	 * @return The soft value.
	 */
	public Object getSoftValue() {
		return softValue;
	}

	/**
	 * Deserializes the object from the stream.
	 * 
	 * @param input The input to read from.
	 * @throws IOException Thrown if the deserialization failed.
	 */
	public void readObject(IObjectInput input) throws IOException {
		this.identifier = input.readShort();
		this.ordered = input.readBoolean();
		this.orientation = input.readShort();
		this.hardValue = input.readObject();
		this.softValue = input.readObject();
	}

	/**
	 * Serializes the object to the stream.
	 * 
	 * @param output The output to write to.
	 * @throws IOException Thrown if the serialization failed.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeShort(identifier);
		output.writeBoolean(ordered);
		output.writeShort(orientation);
		output.writeObject(this.hardValue);
		output.writeObject(this.softValue);
	}
	
	/**
	 * Creates a copy of this dimension. The parameters are assumed
	 * to be immutable and thus they are not copied.
	 * 
	 * @return A copy of this dimension.
	 */
	public NFDimension copy() {
		NFDimension dimension = new NFDimension();
		dimension.identifier = identifier;
		dimension.ordered = ordered;
		dimension.orientation = orientation;
		dimension.hardValue = hardValue;
		dimension.softValue = softValue;
		return dimension;
	}

	/**
	 * Returns a string representation of this dimension.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("ID <");
		b.append(identifier);
		b.append("> ORDERED <");
		b.append(ordered?"TRUE":"FALSE");
		b.append("> ORIENTATION <");
		b.append(orientation);
		b.append("> HARD <");
		b.append(hardValue);
		b.append("> SOFT <");
		b.append(softValue);
		b.append(">");
		return b.toString();
	}


}
