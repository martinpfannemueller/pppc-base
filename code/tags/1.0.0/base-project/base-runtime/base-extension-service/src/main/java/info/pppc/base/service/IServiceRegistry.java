package info.pppc.base.service;

import info.pppc.base.system.InvocationException;
import info.pppc.base.system.ObjectID;

import java.util.Vector;


/**
 * The remotely accessible interface of registries. This interface enables
 * other registries to perform queries for services that are exported by the
 * registry. The remote registry is a well known service that is exported 
 * using the registry id specified in this interface.
 *  
 * @author Marcus Handte
 */
public interface IServiceRegistry {
	
	/**
	 * The well known object id of all remote registries. The id 
	 * defaults to 1.
	 */
	public static final ObjectID REGISTRY_ID = new ObjectID(2);
 	
	/**
	 * Returns a vector of service descriptors of services that are exported
	 * on the local device. The query can be parameterized by service name,
	 * service interfaces or properties. If one of the parameters is set to
	 * null, the parameter is set to a wild card that matches all services 
	 * available on the device.
	 * 
	 * @param name The name of the service, or null if any name should be
	 * 	returned.
	 * @param interfaces The interfaces provided by the service, or null if
	 * 	any interface should be returned.
	 * @param properties The properties specified by the service, or null if
	 * 	any property should be returned.
	 * @param scope The scope of the query. This must be one of the scope
	 * 	constants.
	 * @return A vector that contains services exported by the registry.
	 * @throws InvocationException Thrown by base if the remote call failed.
	 */
	public Vector lookup(String name, String[] interfaces, ServiceProperties properties, int scope)
		throws InvocationException;

}
