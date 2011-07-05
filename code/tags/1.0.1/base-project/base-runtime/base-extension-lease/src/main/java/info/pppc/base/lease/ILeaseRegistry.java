/*
 * Revision: $Revision: 1.3 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/21 12:19:28 $ 
 */
package info.pppc.base.lease;

import info.pppc.base.system.InvocationException;
import info.pppc.base.system.ObjectID;
import info.pppc.base.system.SystemID;

import java.util.Vector;

/**
 * The remote interface of the lease registry. This interface is for
 * internal use only. It provides methods to refresh the leases on
 * the local system and methods to signal a remote release of a 
 * certain lease.
 *  
 * @author Mac
 */
public interface ILeaseRegistry {

	/**
	 * The object id under which the remote lease registry will be 
	 * exported.
	 */
	public ObjectID REGISTRY_ID = new ObjectID(1);

	/**
	 * Called by a remote lease registry. This method refreshes
	 * the leases with the specified local leases passed as parameter
	 * to this method. The return value will be a vector of leases
	 * that could not be refreshed because they have timed out.
	 * 
	 * @param system The system that is refreshing the leases.
	 * @param leases The leases whose lease should be extended.
	 * @return The leases that could not be extended since they have 
	 *  been removed locally.
	 * @throws InvocationException Thrown if the call failed.
	 */
	public Vector update(SystemID system, Vector leases) throws InvocationException;

	/**
	 * Tells the local lease listener that the specified system
	 * has unhooked the lease and that it might be released.
	 * 
	 * @param system The system that is making the unhook call.
	 * @param lease The lease that has been unhooked by the remote system.
	 * @throws InvocationException Thrown if the call failed.
	 */
	public void unhook(SystemID system, Lease lease) throws InvocationException;

	/**
	 * Releases the observer that observes the specified object
	 * identifier on the specified system.
	 * 
	 * @param system The source system that released the lease. 
	 * @param lease The lease that has been removed on the originating
	 * 	system.
	 * @throws InvocationException Thrown if the call failed.
	 */
	public void remove(SystemID system, Lease lease) throws InvocationException;

}
