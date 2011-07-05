package info.pppc.base.service;

/**
 * This exception is used by the registries in order to signal
 * problems with the registration of items at the registry.
 * 
 * @author Marcus Handte
 */
public class ServiceRegistryException extends Exception {
	
	/**
     * Creates a new exception without a detail message.
     */
    public ServiceRegistryException() {
    	super();
    }
    
    /**
     * Creates a new exception with the specified detail message.
     * 
     * @param message The detail message. 
     */
    public ServiceRegistryException(String message){
        super(message);
    }

}
