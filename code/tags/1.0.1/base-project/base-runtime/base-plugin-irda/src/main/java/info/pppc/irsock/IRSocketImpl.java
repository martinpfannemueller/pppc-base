package info.pppc.irsock;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This implements the native socket api for IRDA protocol. Every instance of
 * this class holds a native file handle for access to the native socket api.
 * Note: This class must not be modified as it is referenced by native code.
 * 
 * @author bator
 */
public class IRSocketImpl{

	/**
	 * Loads the library upon startup.
	 */
	static{
		try {
			System.loadLibrary("pppcir_ce");	
		} catch (UnsatisfiedLinkError e) {
			System.loadLibrary("pppcir_xp");
		}
	}

	/**
	 * A constant that represents an uninitialized file descriptor.
	 */
	private static final int UNSET = -1;
	
	/**
	 * The native file descriptor.
	 */
	private int	descriptor = UNSET;
	
	/**
	 * Creates a new socket implementation. This constructor
	 * is called by native code.
	 */
	protected IRSocketImpl(){
		super();
	}
	
	/**
	 * Initializes a new native socket and sets the file
	 * descriptor.
	 * 
	 * @throws IOException Thrown if the socket could not
	 * 	be initialized.
	 */
	protected void create() throws IOException {
		descriptor=nativeCreate();
	}

	
	/**
	 * Performs a remote discovery and returns an array of 
	 * available devices.
	 * 
	 * @return Returns a list of discovered devices.
	 * @throws IOException Thrown if the discovery fails.
	 */
	protected static IRDevice[] discover() throws IOException {
		return nativeDiscover();
	}

	/**
	 * Connect to a remote IRDA device using the discovered device 
	 * id and a known service name.
	 * 
	 * @param deviceID The four byte address of the device.
	 * @param serviceName service name (analog to a port number)
	 * @throws IOException Thrown if the device cannot be connected.
	 */
	protected void connect(byte[] deviceID, String serviceName) throws IOException {
		nativeConnect(descriptor,deviceID,serviceName.getBytes());
	}
	
	/**
	 * Bind the internal file handle to the service name.
	 * 
	 * @param serviceName service to provide
	 * @throws IOException Thrown if the socket could not be bound.
	 */
	protected void bind(String serviceName) throws IOException {
		nativeBind(this.descriptor, serviceName.getBytes());		
	}
	
	/**
	 * Listen for incoming connections.
	 * 
	 * @param backlog The backlog for incoming connections.
	 * @throws IOException Thrown if the call fails.
	 */
	protected void listen(int backlog) throws IOException {
		nativeListen(descriptor,backlog);
	}
	
	/**
	 * Accept an incoming connection and blocks, until 
	 * a client connects to it.
	 * 
	 * @param client gives the given socket impl a new file handle
	 * @throws IOException Thrown if the accept cal fails.
	 */
	protected void accept(IRSocketImpl client) throws IOException {
		int tmpfd = nativeAccept(this.descriptor);
		client.descriptor = tmpfd;   
	}
	
	/**
	 * Returns the input stream of this native socket.
	 * 
	 * @return The input stream for the native socket.
	 * @throws IOException Thrown if the stream could not be created.
	 */
	protected InputStream getInputStream() throws IOException {		
		return new IRInputStream(this);
	}
	
	/**
	 * Returns the output stream of this native socket.
	 * 
	 * @return output The output stream for the native socket.
	 * @throws IOException Thrown if the stream could not be created.
	 */
	protected OutputStream getOutputStream() throws IOException {
		return new IROutputStream(this);
	}
	
	/**
	 * Returns the available bytes in the input stream. Note that
	 * this method is not implemented on windows ce. 
	 * 
	 * @return The number of available bytes. 
	 * @throws IOException Thown by the underlying implementation.
	 */
	protected int available() throws IOException {
		return nativeAvailable(this.descriptor);
	}
	
	/**
	 * Closes the native socket.
	 * 
	 * @throws IOException Thrown by the underlying implementation. 
	 */
	protected void close() throws IOException {
	   if (this.descriptor != UNSET){
			nativeClose(descriptor);   	
			descriptor = UNSET;
	    }	
	}
	
	/**
	 * Read a single byte from native socket. If the end of the 
	 * stream is read this method will return -1.
	 * 
	 * @return The byte read or  -1 if the end is reached.
	 * @throws IOException Thrown by the underlying implementation.
	 */
	public int read()throws IOException {
		byte[] buf = new byte[1];
		int count;
		while (true) {
			count = nativeRead(descriptor, buf, 0, 1);
			if (count == 1) {
				return buf[0] & 0xFF;
			} else if (count == -1) {
				return -1;
			} 
		}		
	}
	
	/**
	 * Read number of bytes into the given buffer.
	 * 
	 * @param b The buffer to write to.
	 * @param off Offset in the buffer.
	 * @param len Lenght in the buffer.
	 * @return The number of bytes read.
	 * @throws IOException Thrown by the underlying implementation.
	 */
	public int read(byte[] b, int off, int len) throws IOException{
		return nativeRead(descriptor,b,off,len);
	}
	
	/**
	 * Writes a number of bytes to the socket.
	 * 
	 * @param b The buffer to write to. 
	 * @param off The offset in buffer.
	 * @param len The length to write.
	 * @throws IOException Thrown if the buffer could not be written.
	 */
	public void write(byte[] b, int off, int len) throws IOException{
		nativeWrite(descriptor,b,off,len);
	}
	 
	/**
	 * Write a single byte to socket.
	 * 
	 * @param data The byte to write.
	 * @throws IOException Thrown if the buffer could not be written.
	 */
	public void write(int data)  throws IOException{
		byte[] buf=new byte[1];
	 	buf[0]=(byte)data;
		nativeWrite(descriptor,buf,0,1);
	}

	// native implementations for the methods of this class
	private native static IRDevice[] nativeDiscover() throws IOException; 
	private native static int nativeCreate()throws IOException;
	private native static void nativeBind(int fd, byte[] serviceName) throws IOException;
 	private native static void nativeListen(int fd, int backlog) throws IOException;
	private native static int nativeAccept(int fd) throws IOException;
	private native static void nativeConnect(int fd, byte[] deviceID,byte[] serviceName)throws IOException;
	private native static int nativeAvailable(int fd)throws IOException;
	private native static void nativeClose(int fd)throws IOException;
	private native static int nativeRead(int fd,byte[] b, int off, int len) throws IOException;
	private native static void nativeWrite(int fd, byte[] b, int off, int len)throws IOException;




 
}
