package info.pppc.basex.plugin.routing;

import java.util.Vector;

/**
 * The routing filter is used to determine possible gateways
 * and to express preferences over their usage. The filter
 * is called during gateway selection and may adapt the
 * gateway set.
 * 
 * @author Mac
 */
public interface IRoutingFilter {

	/**
	 * Called whenever the routing plug-in selects a gateway.
	 * The routing plug-in will pass in a vector of known
	 * gateways by reference. The gateways may be adapted
	 * according to the preference by means of reordering
	 * or they may be removed. In addition it is possible
	 * to add gateways, however, in general, this does not
	 * make sense since the initial set is determined on the
	 * basis of availability.
	 * 
	 * @param systems A vector of system ids of possible
	 * 	gateways. The vector contents may be manipulated
	 * 	to express preferences or remove candidate gateways. 
	 */
	public void getGateways(Vector systems);
	
}
