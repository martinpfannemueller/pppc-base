package info.pppc.base.system;

/**
 * Constant definitions for extension points that are part of the plug-in manager.
 * The constants identify different plug-in types. A plug-in must identify itself
 * using these types.
 * 
 * @author Marcus Handte
 */
public interface IExtension {

	/**
	 * The layer value that indicates that the plug-in is a discovery plug-in.
	 * All discovery plug-ins must support the discovery interface that is
	 * defined in the plug-in package.
	 */
	public final static short EXTENSION_DISCOVERY = 1;
	
	/**
	 * The layer value that indicates that this plug-in provides a communication
	 * transceiver. All communication transceivers must support the transceiver
	 * interface defined in the plug-in package.
	 */
	public final static short EXTENSION_TRANSCEIVER = 2;

	/**
	 * The layer value that indicates that this plug-in is an encryption plug-in.
	 * All encryption plug-ins must support the modifier interface defined in 
	 * the plug-in package.
	 */
	public final static short EXTENSION_ENCRYPTION = 4;
	
	/**
	 * The layer value that indicates that this plug-in is a compression plug-in.
	 * All compression plug-ins must support the modifier interface defined in
	 * the plug-in package.
	 */
	public final static short EXTENSION_COMPRESSION = 8;

	/**
	 * The layer value that indicates that this plug-in is a routing plug-in.
	 * The routing plug-ins must support the routing interface defined in
	 * the plug-in package.
	 */
	public final static short EXTENSION_ROUTING = 16;
	
	/**
	 * The layer value that indicates that this plug-in is a serialization plug-in.
	 * All serializer plug-ins must support the modifier interface defined in
	 * the plug-in package. In addition, the streams provided by this plug-in 
	 * must conform to the base object input and output stream interface 
	 * definitions.
	 */
	public final static short EXTENSION_SERIALIZATION = 32;
	
	/**
	 * The layer value that indicates that this plug-in is a semantic plug-in.
	 * All semantic plug-ins must support the semantic interface defined in
	 * the plug-in package.
	 */
	public final static short EXTENSION_SEMANTIC = 64;

}
