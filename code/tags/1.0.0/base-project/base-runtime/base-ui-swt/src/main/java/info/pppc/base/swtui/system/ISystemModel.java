package info.pppc.base.swtui.system;

/**
 * This interface defines the types of the browser model. The tree model
 * will be structured as follows:
 * - DEVICE
 * -- IDENTIFIER
 * --- PLUGINS
 * ---- PLUGIN
 * ----- ABILITY
 * ----- PROPERTY
 * -- SERVICES
 * --- SERVICE
 * 
 * @author Marcus Handte
 */
public interface ISystemModel {

	/**
	 * The null type denotes the root node. The data object will be null.
	 */
	public static final short TYPE_NULL = 0;

	/**
	 * The node id of a node that denotes a device. The data object will
	 * be null.
	 */
	public static final short TYPE_DEVICE = 1;
	
	/**
	 * The node id of a node that denotes the device name. The data object
	 * will contain a string with a maximum length of 10.
	 */
	public static final short TYPE_NAME = 2;
	
	/**
	 * The node id of a node that denotes the device type. The data object
	 * will contain a short that corresponds to one of the device type
	 * constants defined by the device description.
	 */
	public static final short TYPE_TYPE = 3;
	
	/**
	 * The node id of a note that denotes a system id. The data object will
	 * be the system id.
	 */
	public static final short TYPE_IDENTIFIER = 4;
	
	/**
	 * The node id of a node that denotes a set of well known services. 
	 * The data object will be null.
	 */
	public static final short TYPE_SERVICES = 5;
	
	/**
	 * The node id of a node that denotes a single service. The data object
	 * will contain the object id of the service.
	 */
	public static final short TYPE_SERVICE = 6;
	
	/**
	 * The node id of a node that denotes a set of plug-ins. The data object
	 * will be null.
	 */
	public static final short TYPE_PLUGINS = 7;
	
	/**
	 * The node id of a node that denotes a single plug-in. The data object
	 * contains the plug-in extensions.
	 */
	public static final short TYPE_PLUGIN = 8;
	
	/**
	 * The node id of a node that denotes a plug-in ability. The data object
	 * will be a short denoting the ability.
	 */
	public static final short TYPE_ABILITY = 9;
	
	/**
	 * The node id of a node that contains a plug-in property. The data object
	 * will contain a string that denotes the represented property.
	 */
	public static final short TYPE_PROPERTY = 10;

}
