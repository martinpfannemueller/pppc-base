package info.pppc.base.system;

/**
 * The result is the container that is transfered
 * between skeletons and proxies.
 * 
 * @author Marcus Handte
 */
public final class Result {

	/**
	 * The return value of a method.
	 */
	private Object value;
	
	/**
	 * The exception of a method.
	 */
	private Throwable exception;
	
	/**
	 * Creates a new result with the specified return
	 * value and exception.
	 * 
	 * @param value The return value.
	 * @param exception The exception.
	 */
	public Result(Object value, Throwable exception) {
		this.value = value;
		this.exception = exception;
	}
	
	/**
	 * Determines whether the service result has an exception.
	 * 
	 * @return True if the exception is not set to null.
	 */
	public boolean hasException() {
		return exception != null;
	}
	
	/**
	 * Returns the value of the exception.
	 * 
	 * @return The value of the exception.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * Returns the return value of the service result.
	 * 
	 * @return The service result's return value.
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Returns a string representation of the result.
	 * 
	 * @return A string representation.
	 */
	public String toString() {
		return "Result: value is " + value + ", exception is " + exception + ".";
	}

}