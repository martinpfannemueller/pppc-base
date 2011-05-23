package info.pppc.base.system;

import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;
import info.pppc.base.system.nf.NFCollection;

import java.io.IOException;

/**
 * Implementation of an invocation that is used for data exchange between
 * local and remote objects. Note that the semantic of the type field of the 
 * invocation can be defined by semantic plug-ins. However, note that all
 * types below 1024 are reserved for the broker, values above 1024 can be
 * user defined. 
 * 
 * @author Marcus Handte
 */
public final class Invocation implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BI";
	
	/**
	 * Constant value that indicates the initial type when a invocation is created.
	 */
	public final static short TYPE_UNDEFINED = 0;
	
	/**
	 * Constant value that indicates that the invocation carries a remote call that
	 * must be performed.
	 */
	public final static short TYPE_INVOKE  = 1;
	
	/**
	 * Constant value that indicates that the invocation carries a result that must
	 * be delivered. 
	 */
	public final static short TYPE_RESULT  = 2;

	/**
	 * Constant value that indicates that the invocation result on the receiver side
	 * of an invoke method can be cleared.
	 */
	public final static short TYPE_REMOVE  = 4;

	/**	
	 * The id of the (corresponding) invoke message. This id is created once for 
	 * every invocation and is locally unique.
	 */
	private Integer id = null;

	/**
	 * The target of the invocation (for invoke, reply messages invert
	 * source and target).
	 */
	private ReferenceID target;

	/**
	 * The source of the invocation (for invoke, reply messages invert
	 * source and target).
	 */
	private ReferenceID source;

	/**
	 * The method signature of the referenced method.
	 */
	private String signature = null;

	/**
	 * The arguments of the referenced method.
	 */
	private Object[] arguments = null;

	/**
	 * The return value of the referenced method.
	 */
	private Object result = null;

	/**
	 * The type of the invocation (invoke/reply). This field is used by
	 * the broker to determine whether the invocation is a invocation that
	 * must be performed or a result that must be mapped into the table
	 * of outgoing invocations. This field therefore defines the semantic
	 * of the method call. It is legal for semantic plug-ins to change the
	 * content of this field, but they must ensure that they will NEVER
	 * deliver a message with a type that is not set to invoke or result.
	 */
	private short type = TYPE_UNDEFINED;

	/**
	 * The exception thrown by 1. the invocation broker or 2. the
	 * implementation (if exception type != invocation exception)
	 */
	private Throwable exception = null;

	/**
	 * The requirements of the invocation. The requirements are used
	 * by the strategy to select the right plug-in for every layer.
	 */
	private NFCollection requirements;

	/**
	 * Creates a new invocation. This method should only be used
	 * for deserialization purposes.
	 */
	public Invocation() {
		super();
	}
	
	/**
	 * Creates a new invocation with the specified source, 
	 * target, method and arguments.
	 * 
	 * @param source The source object.
	 * @param target The target object.
	 * @param method The method signature.
	 * @param args  The method arguments.
	 */
	public Invocation(ReferenceID source, ReferenceID target, String method, Object[] args) {
		this.source = source;
		this.target = target;
		signature = method;
		arguments = args;
	}

	/**
	 * Returns the id of the invocation. This id is
	 * created by the kernel on the source system.
	 * 
	 * @return The invocation's id.
	 */
	public Integer getID() {
		return id;
	}

	/**
	 * Returns the method's arguments.
	 * 
	 * @return The method's arguments.
	 */
	public Object[] getArguments() {
		return arguments;
	}

	/**
	 * Returns the return value of the method call.
	 * 
	 * @return The return value of the method call.
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * Returns the signature of the method.
	 * 
	 * @return The signature of the method.
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Returns the source of the invocation.
	 * 
	 * @return The invocation's source.
	 */
	public ReferenceID getSource() {
		return source;
	}

	/**
	 * Returns the target of the invocation.
	 * 
	 * @return The invocation's target.
	 */
	public ReferenceID getTarget() {
		return target;
	}

	/**
	 * Returns the message type. 
	 * 
	 * @return The messages type.
	 */
	public short getType() {
		return type;
	}

	/**
	 * Returns the exception state of this method.
	 * 
	 * @return Null if no exception has been thrown,
	 * 	otherwise the exception.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * Returns the nonfunctional requirements associated with this invocation.
	 * The non-functional requirements are used by the selection strategy to
	 * determine the plug-in that should be used to perform the next transformation
	 * step. It is possible and valid to modify the requirements during the
	 * call. I.e. higher layers might remove, add or modify non-functional 
	 * requirements. 
	 * 
	 * @return The non-functional requirements of the invocation.
	 */
	public NFCollection getRequirements() {
		return requirements;
	}

	/**
	 * Sets the id of the invocation.
	 * 
	 * @param id The id to set.
	 */
	public void setID(Integer id) {
		this.id = id;
	}

	/**
	 * Sets the method arguments of the invocation.
	 * 
	 * @param arguments The method arguments to set.
	 */
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * Sets the result of the invocation.
	 * 
	 * @param result The result to set.
	 */
	public void setResult(Object result) {
		this.result = result;
	}

	/**
	 * Sets the signature of the method.
	 * 
	 * @param signature The signature to set.
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * Sets the invocation's target.
	 * 
	 * @param target The invocation's target.
	 */
	public void setTarget(ReferenceID target) {
		this.target = target;
	}

	/**
	 * Sets the invocation's source.
	 * 
	 * @param source The invocation's source.
	 */
	public void setSource(ReferenceID source) {
		this.source = source;
	}

	/**
	 * Sets the type of the invocation.
	 * 
	 * @param type The type to set.
	 */
	public void setType(short type) {
		this.type = type;
	}


	/**
	 * Sets the exception of the invocation.
	 * 
	 * @param exception The exception to set.
	 */
	public void setException(Throwable exception) {
		this.exception = exception;
	}


	/**
	 * Associates nonfunctional requirements with this invocation. The non-
	 * functional requirements are typically used to select a plug-in within
	 * the plug-in selection strategy.
	 * 
	 * @param collection The new non-functional requirements of the 
	 * 	invocation.
	 */
	public void setRequirements(NFCollection collection) {
		requirements = collection;
	}

	/**
	 * Deserializes the invocation from the given stream.
	 * 
	 * @param stream The stream to read from.
	 * @throws IOException Thrown if the deserialization fails.
	 */	
	public void readObject(IObjectInput stream) throws IOException {
		id = (Integer) stream.readObject();
		target = (ReferenceID) stream.readObject();
		source = (ReferenceID) stream.readObject();
		signature = (String) stream.readObject();
		arguments = (Object[]) stream.readObject();
		result = stream.readObject();
		type = ((Short) stream.readObject()).shortValue();
		exception = (Throwable) stream.readObject();
		requirements = (NFCollection) stream.readObject();
	}

	/**
	 * Serializes the invocation to the given stream.
	 * 
	 * @param stream The stream to write to.
	 * @throws IOException Thrown if the deserialization fails.
	 */
	public void writeObject(IObjectOutput stream) throws IOException {
		stream.writeObject(id);
		stream.writeObject(target);
		stream.writeObject(source);
		stream.writeObject(signature);
		stream.writeObject(arguments);
		stream.writeObject(result);
		stream.writeObject(new Short(type));
		stream.writeObject(exception);
		stream.writeObject(requirements);
	}
	
	/**
	 * Returns a string representation of this invocation.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("ID <");
		b.append(id);
		b.append("> TYPE <");
		b.append(type);
		b.append("> SOURCE <");
		b.append(source);
		b.append("> TARGET <");
		b.append(target);
		b.append("> SIGNATURE <");
		b.append(signature);
		b.append("> # ARGUMENTS <");
		if (arguments == null) {
			b.append("NULL");
		} else {
			b.append(arguments.length);
		}
		b.append("> RESULT <");
		b.append(result);
		b.append("> EXCEPTION <");
		b.append(exception);
		b.append("> NF <");
		b.append(requirements);
		b.append(">");
		return b.toString();
	}
}