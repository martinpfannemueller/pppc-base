package info.pppc.base.service;

import info.pppc.base.system.ReferenceID;
import info.pppc.base.system.io.IObjectInput;
import info.pppc.base.system.io.IObjectOutput;
import info.pppc.base.system.io.ISerializable;

import java.io.IOException;


/**
 * This class implements the service descriptor interface. It delivers the
 * functionality needed to describe a service within remote and local
 * service requests.
 * 
 * @author Marcus Handte
 */
public class ServiceDescriptor implements ISerializable {

	/**
	 * The abbreviation used for this class during serialization.
	 */
	public static final String ABBREVIATION = ";BS";
	
	/**
	 * The identifier that points to the service.
	 */
	private ReferenceID identifier;

	/**
	 * The interfaces exported by the service.
	 */
	private String[] interfaces;

	/**
	 * The name of the service.
	 */
	private String name;

	/**
	 * The properties of the service.
	 */
	private ServiceProperties properties;

	/**
	 * Creates a new service descriptor with empty identifier,
	 * interfaces, name and properties.
	 */
	public ServiceDescriptor() {
		super();
	}

	/**
	 * Returns the identifier of the service.
	 * 
	 * @return The identifier of the service. 
	 */
	public ReferenceID getIdentifier() {
		return identifier;
	}

	/**
	 * Returns the interfaces of the service.
	 * 
	 * @return The interfaces of the service.
	 */
	public String[] getInterfaces() {
		return interfaces;
	}

	/**
	 * Returns the name of the service.
	 * 
	 * @return The name of the service.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the properties of the service.
	 * 
	 * @return The properties of the service.
	 */
	public ServiceProperties getProperties() {
		return properties;
	}

	/**
	 * Sets the identifier of the service.
	 * 
	 * @param referenceID The identifier of the service.
	 */
	protected void setIdentifier(ReferenceID referenceID) {
		identifier = referenceID;
	}

	/**
	 * Sets the exported interfaces of the service.
	 * 
	 * @param strings The exported interfaces.
	 */
	protected void setInterfaces(String[] strings) {
		interfaces = strings;
	}

	/**
	 * Sets the name of the service.
	 * 
	 * @param string The name of the service.
	 */
	protected void setName(String string) {
		name = string;
	}

	/**
	 * Sets the properties of the service.
	 * 
	 * @param properties The properties of the service.
	 */
	protected void setProperties(ServiceProperties properties) {
		this.properties = properties;
	}

	/**
	 * Deserializes the service descriptor.
	 * 
	 * @param input the input to read from.
	 * @throws IOException Thrown by the input.
	 */
	public void readObject(IObjectInput input) throws IOException {
		identifier = (ReferenceID)input.readObject();
		interfaces = (String[])input.readObject();
		name = input.readUTF();
		properties = (ServiceProperties)input.readObject();
	}

	/**
	 * Serializes the service descriptor.
	 * 
	 * @param output The output to write to.
	 * @throws IOException Thrown by the output.
	 */
	public void writeObject(IObjectOutput output) throws IOException {
		output.writeObject(identifier);
		output.writeObject(interfaces);
		output.writeUTF(name);
		output.writeObject(properties);
	}
	
	/**
	 * Returns a human readable string representation of
	 * the service descriptor.
	 * 
	 * @return A human readable string representation.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("IDENTIFIER (");
		buffer.append(identifier);
		buffer.append(") NAME (");
		buffer.append(name);
		buffer.append(") INTERFACES (");
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				buffer.append(interfaces[i]);
				if (i + 1 != interfaces.length) 
					buffer.append(", ");
			}			
		}
		buffer.append(") PROPERTIES (");
		buffer.append(properties);
		buffer.append(")");
		return buffer.toString();
	}
}