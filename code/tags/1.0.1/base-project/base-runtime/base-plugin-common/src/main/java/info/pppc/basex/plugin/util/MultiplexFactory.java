package info.pppc.basex.plugin.util;

import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.Packet;
import info.pppc.base.system.util.Logging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

/**
 * The multiplexer enables the creation of an arbitrary number of stream
 * and packet-based connectors using a single underlying input and output
 * stream. The typical approach to use a multiplexer is to open the 
 * streams and to connect a multiplexer to both ends of the streams.
 * 
 * @author Marcus Handte
 */
public final class MultiplexFactory {
	
	/**
	 * The buffer is used to send and receive data.
	 * 
	 * @author Marcus Handte
	 */
	private final class Buffer {
		
		/**
		 * The data contained in the buffer. The size will be the maximum
		 * of packet and stream size as the buffer is used by packets as
		 * well as streams. The +9 and +7 are due to the commands that are
		 * encoded in the buffers in order to send complete buffers and
		 * to avoid small packets that might be a result of the deactivation
		 * of nagle's algorithm.
		 */
		private final byte[] data = new byte[Math.max(STREAM_LENGTH, PACKET_LENGTH)];

		/**
		 * The offset of the data in the data buffer.
		 */
		private int offset;
		
		/**
		 * The length of the data in the data buffer.
		 */
		private int length;
		
	}

	
	/**
	 * The packet connector that is issued upon every open request.
	 * 
	 * @author Marcus Handte
	 */
	private final class PacketConnector implements IPacketConnector {

		/**
		 * The group of the packet connector.
		 */
		private short group = 0;
		
		/**
		 * The send buffer for outgoing packets.
		 */
		private Buffer[] sendBuffer = new Buffer[PACKET_BUFFER];
		
		/**
		 * The index into the send buffer that points to the next
		 * buffer entry that can be filled with a new packet.
		 */
		private int sendPut = 0;
		
		/**
		 * The index into the send buffer that points to the next
		 * buffer entry that can be sent by the writter.
		 */
		private int sendGet = 0;
		
		/**
		 * The listeners to receive incoming packets.
		 */
		private ListenerBundle listeners = new ListenerBundle(this);
		
		/**
		 * A flag that indicates whether the connector needs to be
		 * released.
		 */
		private boolean releasing = false;
		
		/**
		 * A flag that indicates whether the connector is released.
		 */
		private boolean released = false;

		/**
		 * Creates a new packet connector for the specified group.
		 * 
		 * @param group The group of the packet connector.
		 */
		public PacketConnector(short group) {
			this.group = group;
		}

		/**
		 * Creates a packet that can be used by this connector.
		 * 
		 * @return A packet that can be used with this connector.
		 */
		public IPacket createPacket() {
			return new Packet(PACKET_LENGTH);
		}
		
		/**
		 * Adds a listeners for the specified events.
		 * 
		 * @param type The type of events.
		 * @param listener The listener to add.
		 */
		public void addPacketListener(int type, IListener listener) {
			listeners.addListener(type, listener);
		}
		
		/**
		 * Removes a listener for the specified events.
		 * 
		 * @param type The type of events.
		 * @param listener The listener to remove.
		 * @return True if removed, false otherwise. 
		 */
		public boolean removePacketListener(int type, IListener listener) {
			return listeners.removeListener(type, listener);
		}

		/**
		 * Called to signal that a new packet has arrived.
		 * 
		 * @param payload The payload of the packet.
		 */
		protected void firePacketReceived(byte[] payload) {
			Packet packet = new Packet(PACKET_LENGTH);
			packet.setPayload(payload);
			listeners.fireEvent(IPacketConnector.EVENT_PACKET_RECEIVED, packet);
		}

		/**
		 * Sends the specified packet through this connector.
		 * 
		 * @param packet The packet to transfer.
		 * @throws IOException Thrown if the packet could not be
		 * 	transfered.
		 */
		public synchronized void sendPacket(IPacket packet) throws IOException {
			while (sendBuffer[sendPut] != null) {
				if (releasing || released) throw new IOException("Connector closed.");					
				try {
					this.wait();
				} catch (InterruptedException e) {
					Logging.error(getClass(), "Thread got interrupted.", e);
				}
			}
			// put packet in send buffer and adjust index accordingly
			byte[] payload = packet.getPayload();
			int payloadLength = payload.length;
			Buffer buffer = createBuffer();
			buffer.data[0] = TYPE_DATA + DATA_PACKET; 
			buffer.data[1] = (byte)((group >>> 8) & 0xFF);
	        buffer.data[2] = (byte)((group >>> 0) & 0xFF);
			buffer.data[3] = (byte)((payloadLength >>> 24) & 0xFF);
			buffer.data[4] = (byte)((payloadLength >>> 16) & 0xFF);
			buffer.data[5] = (byte)((payloadLength >>> 8) & 0xFF);
			buffer.data[6] = (byte)((payloadLength >>> 0) & 0xFF);
			System.arraycopy(payload, 0, buffer.data, 7, payloadLength);
			buffer.length = payloadLength + 7;
			sendBuffer[sendPut] = buffer;
			sendPut += 1;
			if (sendPut == PACKET_BUFFER) sendPut = 0;
			// add to ready queue
			synchronized (ready) {
				ready.addElement(this);
				ready.notify(); 
			}
		}

		/**
		 * Returns the maximum packet length of the connector.
		 * 
		 * @return The maximum packet length;
		 */
		public int getPacketLength() {
			return PACKET_LENGTH - 7;
		}
		
		/**
		 * Releases this connector.
		 */
		public synchronized void release() {
			releasing = true;
			synchronized (ready) {
				ready.addElement(this);
				ready.notify();
			}
			listeners.fireEvent(IPacketConnector.EVENT_PACKET_CLOSED);
		}

		/**
		 * Returns the plug-in of the packet connector.
		 * 
		 * @return The plug-in of the connector.
		 */
		public IPlugin getPlugin() {
			return plugin;
		}

	}

	/**
	 * The stream connector of the multiplexer that provides stream
	 * based point to point connections.
	 * 
	 * @author Marcus Handte
	 */
	private final class StreamConnector implements IStreamConnector {

		/**
		 * The input stream of the connector or null if not initialized.
		 */
		private InputStream input;
		
		/**
		 * The output stream of the connector or null if not initialized.
		 */
		private OutputStream output;

		/**
		 * The identifier of the connector used to associate two
		 * connectors on different devices.
		 */
		private int identifier;
		
		/**
		 * The send buffer of the connector.
		 */
		private Buffer[] sendBuffer = new Buffer[STREAM_BUFFER];
		
		/**
		 * The index that points to the next buffer entry that can be
		 * taken for sending.
		 */
		private int sendGet = 0;
		
		/**
		 * The index that points to the next buffer entry that can be
		 * used to put a buffer used for sending.
		 */
		private int sendPut = 0;
		
		/**
		 * The receive buffer of the connector.
		 */
		private Buffer[] receiveBuffer = new Buffer[STREAM_BUFFER];
		
		/**
		 * The index that points to the next buffer entry that can be
		 * taken for receiving.
		 */
		private int receiveGet = 0;
		
		/**
		 * The index that points to the next buffer entry that can be 
		 * taken to put received data.
		 */
		private int receivePut = 0;
		
		/**
		 * A flag that indicates whether the connector is initialized, only
		 * locally created stream connectors must be initialized.
		 */
		private boolean initialized = false;

		/**
		 * A flag that indicates whether the connector needs to be released.
		 */
		private boolean releasing = false;
		
		/**
		 * A flag that indicates whether the packet connector should be 
		 * released.
		 */
		private boolean released = false;

		/**
		 * A flag that indicates whether the connector is local or remote.
		 */
		private boolean local = false;
		
		/**
		 * Creates a new stream connector with the specified identifier
		 * and the specified send and receive buffer size.
		 * 
		 * @param identifier The identifier of the connector.
		 * @param local A flag that indicates whether the stream connector
		 * 	has been created locally or remotely.
		 */
		public StreamConnector(int identifier, boolean local) {
			this.identifier = identifier;
			this.local = local;
		}

		/**
		 * Returns the input stream of the connector.
		 * 
		 * @return The input stream of the connector.
		 * @throws IOException Thrown if the stream is closed or
		 * 	the connector is invalid.
		 */
		public InputStream getInputStream() throws IOException {
			if (input == null) {
				input = new InputStream() {
					
					/**
					 * The local buffer that is used reduce the number of
					 * synchronizations. This buffer is nulled out if the
					 * end is reached during a read operation.
					 */
					private Buffer buffer;
					
					/**
					 * Determines the number of available bytes.
					 * 
					 * @return the number of available bytes.
					 */
					public int available() throws IOException {
						if (buffer == null) {
							synchronized (StreamConnector.this) {
								buffer = receiveBuffer[receiveGet];
								if (buffer != null) {
									// retrieve available length from buffer
									receiveBuffer[receiveGet] = null;
									receiveGet += 1;
									if (receiveGet == STREAM_BUFFER) receiveGet = 0;
									StreamConnector.this.notifyAll();
									return buffer.length;
								} else if (releasing || released) {
									// throw closed exception 
									throw new IOException("Connector closed.");
								} else {
									// return 0 as there is nothing available
									return 0;
								}
							}
						} else {
							return buffer.length;
						}
					}
					
					/**
					 * Reads a single byte from the receive buffer.
					 * This method is blocking.
					 * 
					 * @return A byte from the buffer.
					 */
					public int read() throws IOException {
						if (buffer != null) {
							// performance optimized read if buffer is available
							int result = buffer.data[buffer.offset] & 0xFF;
							buffer.length -= 1;
							if (buffer.length == 0) {
								releaseBuffer(buffer);
								buffer = null;
							} else {
								buffer.offset += 1;	
							}
							return result;
						} else {
							// if buffer is not available, block in complex read
							// this will only be executed once every STREAM_BUFFER calls
							byte[] b = new byte[1];
							int result = 0;
							while ((result = read(b, 0, 1)) == 0)	;
							if (result == 1) return b[0] & 0xFF;	
							else return -1;
						}
					}
					
					/**
					 * Reads a number of bytes from the receive buffer.
					 * This method is blocking if no byte is available. 
					 * 
					 * @param b The buffer to write to.
					 * @param offset The offset in the buffer.
					 * @param length The length to read.
					 */
					public int read(byte[] b, int offset, int length) throws IOException {
						while (buffer == null) {
							synchronized (StreamConnector.this) {
								buffer = receiveBuffer[receiveGet];
								if (buffer != null) {
									receiveBuffer[receiveGet] = null;
									receiveGet += 1;
									if (receiveGet == STREAM_BUFFER) receiveGet = 0;
									StreamConnector.this.notifyAll();
								} else if (releasing || released) {
									// throw closed exception 
									return -1;
								} else {
									try {
										StreamConnector.this.wait();
									} catch (InterruptedException e) {
										Logging.error(getClass(), "Thread got interrupted.", e);
									}
								}							
							}
						}
						int read = Math.min(length, buffer.length);
						System.arraycopy(buffer.data, buffer.offset, b, offset, read);
						buffer.length -= read;
						if (buffer.length == 0) {
							releaseBuffer(buffer);
							buffer = null;
						} else {
							buffer.offset += read;
						}
						return read;
					}
				};
			}
			return input;
		}	

		/**
		 * Returns the output stream of the connector.
		 * 
		 * @return The output stream of the connector.
		 * @throws IOException Thrown if the stream is closed or the
		 * 	connector is invalid.
		 */
		public OutputStream getOutputStream() throws IOException {
			if (output == null) {
				output = new OutputStream() {
					
					/**
					 * The local buffer that is used reduce the number of
					 * synchronizations. This buffer is nulled out if the
					 * buffer is transferred to the send buffer of the socket.
					 */
					private Buffer buffer;
					
					/**
					 * Writes a single byte to the output stream.
					 * 
					 * @param oneByte The byte to write.
					 * @throws IOException Thrown if the stream is closed.
					 */
					public void write(int oneByte) throws IOException {
						if (releasing || released) throw new IOException("Connector closed.");
						if (buffer == null) {
							// optimized data path for uninitialized buffer
							buffer = createBuffer();
							if (local) {
								buffer.data[0] = TYPE_DATA + DATA_STREAM + ID_LOCAL;	
							} else {
								buffer.data[0] = TYPE_DATA + DATA_STREAM + ID_REMOTE;	
							}
					        buffer.data[1] = (byte)((identifier >>> 24) & 0xFF);
							buffer.data[2] = (byte)((identifier >>> 16) & 0xFF);
							buffer.data[3] = (byte)((identifier >>> 8) & 0xFF);
							buffer.data[4] = (byte)((identifier >>> 0) & 0xFF);
							// leave 4 bytes room for packet length (created on send)
							buffer.data[9] = (byte)oneByte;
							buffer.length = 10;
						} else if (buffer.length + 1 != STREAM_LENGTH) {
							// optimized data path if buffer must not be copied
							buffer.data[buffer.length] = (byte)oneByte;
							buffer.length += 1;
						} else {
							// complex write, will happen only once every STREAM_LENGTH
							write(new byte[] { (byte) oneByte	}, 0, 1);	
						}
					}
					
					/**
					 * Flushes the output stream.
					 * 
					 * @throws IOException Thrown if the stream is closed.
					 */
					public void flush() throws IOException {
						// add bytes, set flushstate, add to list, block
						while (buffer != null) {
							synchronized (StreamConnector.this) {
								if (sendBuffer[sendPut] == null) {
									sendBuffer[sendPut] = buffer;
									// write the data lenth into the buffer at the header position
									int total = buffer.length - 9;
									buffer.data[5] = (byte)((total >>> 24) & 0xFF);
									buffer.data[6] = (byte)((total >>> 16) & 0xFF);
									buffer.data[7] = (byte)((total >>> 8) & 0xFF);
									buffer.data[8] = (byte)((total >>> 0) & 0xFF);
									sendBuffer[sendPut] = buffer;
									buffer = null;
									sendPut += 1;
									if (sendPut == STREAM_BUFFER) sendPut = 0;
									synchronized (ready) {
										ready.addElement(StreamConnector.this);
										ready.notify();
									}
									break;
								} else {
									try {
										StreamConnector.this.wait();
										if (releasing || released) throw new IOException("Connector closed.");
									} catch (InterruptedException e) {
										Logging.error(getClass(), "Thread got interrupted.", e);
									}
								}
							}
						}
						synchronized (StreamConnector.this) {
							while (true) {
								synchronized (ready) {
									if (! ready.contains(StreamConnector.this)) {
										return;
									}
								}
								if (released) throw new IOException("Connector is closed.");
								try {
									StreamConnector.this.wait();	
								} catch (InterruptedException e) {
									Logging.error(getClass(), "Thread got interrupted.", e);
								}
							}
						}
					}
					
					/**
					 * Flushes and closes the stream.
					 * 
					 * @throws IOException Thrown if the stream cannot be flushed.
					 */
					public void close() throws IOException {
						if (releasing || released) return;
						flush();
					}
					
					/**
					 * Writes a number of bytes to the buffer.
					 * 
					 * @param b The buffer that contains the bytes.
					 * @param offset The offset in the buffer.
					 * @param count The count of bytes.
					 * @throws IOException Thrown if the stream is closed.
					 */
					public void write(byte[] b, int offset, int count) throws IOException {
						if (releasing || released) throw new IOException("Connector closed.");
						while (count != 0) {
							if (buffer == null) {
								// optimized data path for large blocks
								int write = Math.min(count, STREAM_LENGTH - 9);
								buffer = createBuffer();
								if (local) {
									buffer.data[0] = TYPE_DATA + DATA_STREAM + ID_LOCAL;	
								} else {
									buffer.data[0] = TYPE_DATA + DATA_STREAM + ID_REMOTE;	
								}
						        buffer.data[1] = (byte)((identifier >>> 24) & 0xFF);
								buffer.data[2] = (byte)((identifier >>> 16) & 0xFF);
								buffer.data[3] = (byte)((identifier >>> 8) & 0xFF);
								buffer.data[4] = (byte)((identifier >>> 0) & 0xFF);
								// leave 4 bytes room for packet length (created on send)
								buffer.length = write + 9;
								System.arraycopy(b, offset, buffer.data, 9, write);
								count -= write;
								offset += write;
							} else {
								int write = Math.min(count, STREAM_LENGTH - buffer.length);
								System.arraycopy(b, offset, buffer.data, buffer.length, write);
								buffer.length += write;
								offset += write;
								count -= write;
							}
							while (buffer.length == STREAM_LENGTH) {
								synchronized (StreamConnector.this) {
									if (sendBuffer[sendPut] == null) {
										// write the buffer size into the header
										buffer.data[5] = (byte)((STREAM_LENGTH - 9 >>> 24) & 0xFF);
										buffer.data[6] = (byte)((STREAM_LENGTH - 9 >>> 16) & 0xFF);
										buffer.data[7] = (byte)((STREAM_LENGTH - 9 >>> 8) & 0xFF);
										buffer.data[8] = (byte)((STREAM_LENGTH - 9 >>> 0) & 0xFF);
										sendBuffer[sendPut] = buffer;
										buffer = null;
										sendPut += 1;
										if (sendPut == STREAM_BUFFER) sendPut = 0;
										synchronized (ready) {
											ready.addElement(StreamConnector.this);
											ready.notify();
										}
										break;
									} else {
										try {
											StreamConnector.this.wait();
											if (releasing || released) throw new IOException("Connector closed.");
										} catch (InterruptedException e) {
											Logging.error(getClass(), "Thread got interrupted.", e);
										}
									}
								}
							}
						}
					}
				};
			}
			return output;
		}

		/**
		 * Releases the connector and closes the connectors input
		 * and output streams. This might lead to io exceptions if
		 * the streams are read or written.
		 */
		public synchronized void release() {
			if (! released) {
				releasing = true;
				synchronized (ready) {
					ready.addElement(StreamConnector.this);
					ready.notify();
				}				
			}
			notifyAll();
		}

		/**
		 * Returns the plug-in of this connector.
		 * 
		 * @return The plug-in of this connector.
		 */
		public IPlugin getPlugin() {
			return plugin;
		}
		
	}

	/**
	 * The input reader encapsulates the input stream and reads from
	 * the stream until the stream is closed and or the multiplexer
	 * is shut down.
	 * 
	 * @author Marcus Handte
	 */
	private final class InputReader implements IOperation {

		/**
		 * The input stream to read from.
		 */
		private DataInputStream stream;


		/**
		 * The monitor that is used during the close operation.
		 */
		IMonitor monitor = new NullMonitor();
		
		/**
		 * Creates a new input reader 
		 * 
		 * @param stream
		 */
		private InputReader(InputStream stream) {
			this.stream = new DataInputStream(stream);
			monitor.done();
		}

		/**
		 * Closes the input reader and waits until the thread
		 * has been closed.
		 */
		public synchronized void close() {
			try {
				stream.close();	
			} catch (IOException e) {
				Logging.debug(getClass(), "Exception while closing stream.");
			}
			try {
				monitor.join();
			} catch (InterruptedException e) {
				Logging.error(getClass(), "Could not wait for closing stream.", e);
			}
		}

		/**
		 * Reads from the stream until the steam is closed or the shutdown
		 * method is called. 
		 * 
		 * @param monitor The monitor (not used).
		 */
		public void perform(IMonitor monitor) {
			try {
				Vector packets = new Vector();
				while (true) {
					byte type = stream.readByte();	
					switch (type) {
						case TYPE_OPEN:
						{
							// handle incoming open requests
							int streamID = stream.readInt();			
							StreamConnector connector = new StreamConnector(streamID, false);
							connector.initialized = true;
							addIncoming(connector);
							plugin.acceptConnector(MultiplexFactory.this, connector);
							listeners.fireEvent(EVENT_STREAM_OPENED, connector);
							break;
						}
						case TYPE_CLOSE + ID_LOCAL:
						{
							// handle incoming close requests
							int streamID = stream.readInt();
							StreamConnector connector = getIncoming(streamID);
							if (connector != null) {
								synchronized (connector) {
									connector.released = true;
									connector.notifyAll();
								}
								removeIncoming(connector);
								listeners.fireEvent(EVENT_STREAM_CLOSED, connector);	
							}
							break;
						}
						case TYPE_CLOSE + ID_REMOTE:
						{
							int streamID = stream.readInt();
							StreamConnector connector = getOutgoing(streamID);
							if (connector != null) {
								synchronized (connector) {
									connector.released = true;
									connector.notifyAll();
								}
								removeOutgoing(connector);
								listeners.fireEvent(EVENT_STREAM_CLOSED, connector);
							}
							break;
						}
						case TYPE_DATA + DATA_PACKET:
						{
							short groupID = stream.readShort();
							int packetLength = stream.readInt();
							byte[] packetData = new byte[packetLength];
							stream.readFully(packetData);
							// deliver data packet to all connectors
							getPacket(groupID, packets);
							for (int i = packets.size() - 1; i >= 0; i--) {
								PacketConnector connector = (PacketConnector)packets.elementAt(i);
								packets.removeElementAt(i);
								// deliver packet or taildrop
								synchronized (connector) {
									connector.firePacketReceived(packetData);
									listeners.fireEvent(EVENT_PACKET_RECEIVED, connector);
								}
							}
							break;
						}
						case TYPE_DATA + DATA_STREAM + ID_LOCAL:
						{
							int streamID = stream.readInt();
							int dataLength = stream.readInt();
							Buffer buffer = createBuffer();
							stream.readFully(buffer.data, 0, dataLength);
							buffer.length = dataLength;
							StreamConnector connector = getIncoming(streamID);
							if (connector != null) {
								synchronized (connector) {
									// drop data if connector is released in order to avoid blocking
									while (connector.receiveBuffer[connector.receivePut] != null) {
										if (connector.released || connector.releasing) break;	
										try {
											connector.wait();
										} catch (InterruptedException e) {
											Logging.error(getClass(), "Thread got interrupted.", e);
										}
									}
									if (! (connector.released || connector.releasing)) {
										connector.receiveBuffer[connector.receivePut] = buffer; 
										connector.receivePut += 1;
										if (connector.receivePut == STREAM_BUFFER) connector.receivePut = 0;
										connector.notifyAll();									
									}
								}
							}
							break;
						}
						case TYPE_DATA + DATA_STREAM + ID_REMOTE:
						{
							int streamID = stream.readInt();
							int dataLength = stream.readInt();
							Buffer buffer = createBuffer();
							stream.readFully(buffer.data, 0, dataLength);
							buffer.length = dataLength;
							StreamConnector connector = getOutgoing(streamID);
							if (connector != null) {
								synchronized (connector) {
									// drop data if connector is released in order to avoid blocking
									while (connector.receiveBuffer[connector.receivePut] != null) {
										if (connector.released || connector.releasing) break;	
										try {
											connector.wait();
										} catch (InterruptedException e) {
											Logging.error(getClass(), "Thread got interrupted.", e);
										}
									}
									if (! (connector.released || connector.releasing)) {
										connector.receiveBuffer[connector.receivePut] = buffer; 
										connector.receivePut += 1;
										if (connector.receivePut == STREAM_BUFFER) connector.receivePut = 0;
										connector.notifyAll();									
									}
								}
							}
							break;
						}
						case TYPE_ACKNOWLEDGE:
						{
							synchronized (ready) {
								waitAcknowledge = false;
								ready.notify();
							}
							continue;
						}
						default:	
							throw new IOException("Illegal type found (" + type + ")");
					}
					if (useAcknowledge) {
						synchronized (ready) {
							sendAcknowledge = true;
							ready.notify();
						}						
					}
				}
			} catch (IOException e) {
				Logging.debug(getClass(), "Multiplexer closed.");
				// signal that we are through here (avoid deadlocks)
				monitor.done();
				// close the multiplex factory, if that has not been done already
				MultiplexFactory.this.close();
			}
		}
	}

	/**
	 * The output writer writes the contents of the buffers to the
	 * stream in a serialized way.
	 * 
	 * @author Marcus Handte
	 */
	private final class OutputWriter implements IOperation {
		
		/**
		 * The output stream to write to.
		 */
		private DataOutputStream stream;

		/**
		 * The monitor that is used during the close operation.
		 */
		IMonitor monitor = new NullMonitor();
		
		/**
		 * Creates a new output writer. 
		 * 
		 * @param stream The stream to write to.
		 */
		private OutputWriter(OutputStream stream) {
			this.stream = new DataOutputStream(stream);
			monitor.done();
		}

		/**
		 * Closes the output writer and waits until the thread
		 * has been closed.
		 */
		public synchronized void close() {
			try {
				stream.close();	
			} catch (IOException e) {
				Logging.debug(getClass(), "Exception while closing stream.");
			}
			try {
				monitor.cancel();
				synchronized (ready) {
					ready.notify();
				}
				monitor.join();
			} catch (InterruptedException e) {
				Logging.error(getClass(), "Could not wait for closing stream.", e);
			}
		}

		/**
		 * Writes to the stream until the stream is closed or the shutdown
		 * method is called. 
		 * 
		 * @param monitor The monitor (not used).
		 */
		public void perform(IMonitor monitor) {
			try {
				byte[] smallCommand = new byte[5]; // data structures for open/close
				while (true) {
					Object data = null;
					synchronized (ready) {
						while (ready.isEmpty() || (useAcknowledge && waitAcknowledge)) {
							if (monitor.isCanceled()) {
								throw new IOException();
							}
							if (useAcknowledge && sendAcknowledge) {
								stream.writeByte(TYPE_ACKNOWLEDGE & 0xFF);
								sendAcknowledge = false;
							}
							stream.flush();
							try {
								ready.wait();	
							} catch (InterruptedException e) {
								Logging.error(getClass(), "Thread got interrupted.", e);
							}
						}
						if (useAcknowledge && sendAcknowledge) {
							stream.writeByte(TYPE_ACKNOWLEDGE & 0xFF);
							sendAcknowledge = false;
						}						
						data = ready.elementAt(0);
						ready.removeElementAt(0);
					}
					if (data instanceof byte[]) {
						waitAcknowledge = true;
						stream.write((byte[])data);
					} else if (data instanceof PacketConnector) {
						PacketConnector connector = (PacketConnector)data;
						Buffer buffer = null;
						synchronized (connector) {
							buffer = connector.sendBuffer[connector.sendGet];
							if (buffer != null) {
								connector.sendBuffer[connector.sendGet] = null;
								connector.sendGet += 1;
								if (connector.sendGet == PACKET_BUFFER) connector.sendGet = 0;
							} else if (connector.releasing){
								connector.released = true;
								removePacket(connector);
							} else {
								Logging.debug(getClass(), "Found a packet connector and don't know what to do.");
							}
							connector.notifyAll();
						}
						if (buffer != null) {
							waitAcknowledge = true;
							stream.write(buffer.data, buffer.offset, buffer.length);	
							releaseBuffer(buffer);
						}
					} else if (data instanceof StreamConnector) {
						StreamConnector connector = (StreamConnector)data;
						boolean released, releasing;
						Buffer buffer;
						synchronized (connector) {
							released = connector.released;
							releasing = connector.releasing;
							buffer = connector.sendBuffer[connector.sendGet];
						}
						if (released) {
							synchronized (connector) {
								connector.notifyAll();
							}
						} else if (! connector.initialized) {
							smallCommand[0] = TYPE_OPEN;
							smallCommand[1] = ((byte)((connector.identifier >>> 24) & 0xFF));
							smallCommand[2] = ((byte)((connector.identifier >>> 16) & 0xFF));
							smallCommand[3] = ((byte)((connector.identifier >>> 8) & 0xFF));
							smallCommand[4] = ((byte)((connector.identifier >>> 0) & 0xFF));
							waitAcknowledge = true;
							connector.initialized = true;
							stream.write(smallCommand);
						} else if (buffer != null) {
							synchronized (connector) {
								connector.sendBuffer[connector.sendGet] = null;
								connector.sendGet += 1;
								if (connector.sendGet == STREAM_BUFFER) connector.sendGet = 0;
								connector.notifyAll();
							}
							waitAcknowledge = true;
							stream.write(buffer.data, buffer.offset, buffer.length);
							releaseBuffer(buffer);
						} else if (releasing) {
							connector.released = true;
							if (connector.local) {
								smallCommand[0] = TYPE_CLOSE + ID_LOCAL;
								removeOutgoing(connector);
							} else {
								smallCommand[0] = TYPE_CLOSE + ID_REMOTE;
								removeIncoming(connector);
							}
							smallCommand[1] = ((byte)((connector.identifier >>> 24) & 0xFF));
							smallCommand[2] = ((byte)((connector.identifier >>> 16) & 0xFF));
							smallCommand[3] = ((byte)((connector.identifier >>> 8) & 0xFF));
							smallCommand[4] = ((byte)((connector.identifier >>> 0) & 0xFF));
							waitAcknowledge = true;
							stream.write(smallCommand);
							listeners.fireEvent(EVENT_STREAM_CLOSED, connector);
						} else {
							Logging.debug(getClass(), "Found a stream connector and don't know what to do.");							
						}
					} else {
						Logging.debug(getClass(), "Unknown type found in ready queue (" + data + ").");
					}
				}
			} catch (IOException e) {
				Logging.debug(getClass(), "Multiplexer closed.");
				// signal that we are through here (avoid deadlocks)
				monitor.done();
				// close the multiplex factory, if that has not been done already
				MultiplexFactory.this.close();
			}
		}
		
	}
	
	/**
	 * This is the maximum pool size of the buffer pool. This determines
	 * the average memory consumption of the multiplexer. The estimate here
	 * is BUFFER_POOL * MAX(PACKET_LENGTH, STREAM_LENGTH). Reduce this to
	 * reduce the amount of buffers used.
	 */
	private static final int BUFFER_POOL = 10;

	/**
	 * This is the default length of the data units that are transmitted
	 * with the multiplexer atomically.
	 */
	private static final int DEFAULT_DATA_LENGTH = 2048;
	
	/**
	 * The maximum packet length for packets of the packet connector. As
	 * buffers are shared between packets and streams, it does not make
	 * a lot of sense to have different packet and stream lengths. The
	 * +7 is due to the header that is written into the buffer field.
	 */
	private int PACKET_LENGTH = DEFAULT_DATA_LENGTH + PACKET_HEADER_LENGTH;

	/**
	 * The length of the packet header. This is appended in front of
	 * every data unit that is sent for a packet transmission.
	 */
	private static final int PACKET_HEADER_LENGTH = 7;
	
	/**
	 * The size of the packet buffer for incoming and outgoing packet
	 * connectors.
	 */
	private static final int PACKET_BUFFER = 5;

	/**
	 * The size of the stream buffer for incoming and outgoing stream
	 * connectors. As buffers are shared between packets and streams, 
	 * it does not make a lot of sense to have different packet and 
	 * stream lengths.The +9 is due to the header that is written into 
	 * the buffer field.
	 */
	private int STREAM_LENGTH = DEFAULT_DATA_LENGTH + STREAM_HEADER_LENGTH;
	
	/**
	 * The length of the stream header. This is appended in front of
	 * every data unit that is sent for a stream.
	 */
	private static final int STREAM_HEADER_LENGTH = 9;
	
	/**
	 * The size of the stream buffer for incoming and outgoing stream
	 * connectors.
	 */
	private static final int STREAM_BUFFER = 5;
	
	/**
	 * The open packet type. The packet structure is as follows:
	 * TYPE_OPEN, identifier.
	 */
	private static final byte TYPE_OPEN = (byte)0;

	/**
	 * The close packet type. The packet structure is as follows:
	 * TYPE_CLOSE + (ID_LOCAL|ID_REMOTE), identifier, received
	 */	
	private static final byte TYPE_CLOSE = (byte)1;

	/**
	 * The data packet type. The packet structure is as follows:
	 * TYPE_DATA + DATA_PACKET, group, length, payload.
	 * TYPE_DATA + DATA_STREAM + (ID_LOCAL|ID_REMOTE), 
	 * 	identifier, length, payload.
	 */		
	private static final byte TYPE_DATA = (byte)2;
	
	/**
	 * The type that is used for acknowledgments.
	 */
	private static final byte TYPE_ACKNOWLEDGE = (byte)4;
	
	/**
	 * A flag that indicates that the id is local to the sender.
	 */
	private static final byte ID_LOCAL = (byte)0;
	
	/**
	 * A flag that indicates that the id is remote to the sender.
	 */
	private static final byte ID_REMOTE = (byte)8;
	
	/**
	 * A flag that indicates that the data packet is a part of
	 * a group packet.
	 */
	private static final byte DATA_PACKET = (byte)0;
	
	/**
	 * A flag that indicates that the data packet is part of a
	 * stream.
	 */
	private static final byte DATA_STREAM = (byte)16;

	
	/**
	 * The event constant that is used to signal that a new packet
	 * connector has been created. The source will be this multiplexer.
	 * The data object will be the packet connector that has been
	 * created.
	 */
	public static final int EVENT_PACKET_OPENED = 1;
	
	/**
	 * The event constant that is used to signal that a new packet
	 * has been received. The source will be this multiplexer. The 
	 * data object will be the packet connector that has received
	 * a packet.
	 */
	public static final int EVENT_PACKET_RECEIVED = 2;
	
	/**
	 * The event constant that is used to signal that a packet 
	 * connector has been closed. The source will be this multiplexer.
	 * The data object will be the packet connector that has been
	 * closed.
	 */
	public static final int EVENT_PACKET_CLOSED = 4;
	
	/**
	 * The event constant that is used to signal that a stream
	 * connector has been opened. The source will be this multiplexer.
	 * The data object will be the connector that has been opened.
	 */
	public static final int EVENT_STREAM_OPENED = 8;
	
	/**
	 * The event constant that is used to signal that a stream
	 * connector has received some data. The source will be this
	 * multiplexer. The data object will be the connector that has
	 * received some data.
	 */
	public static final int EVENT_STREAM_RECEIVED = 16;
	
	/**
	 * The event constant that is used to signal that a stream
	 * connector has been closed. The source will be this multiplexer.
	 * The data object will be the connector that has been closed.
	 */
	public static final int EVENT_STREAM_CLOSED = 32;

	/**
	 * The input reader used to receive data.
	 */
	private InputReader input;
	
	/**
	 * The output writer used to transfer data.
	 */
	private OutputWriter output;

	/**
	 * The plug-in that uses the mulitplexer.
	 */
	private IMultiplexPlugin plugin;

	/**
	 * The next identifier of the next stream connector that is locally
	 * created.
	 */
	private int identifier = 0;
	
	/**
	 * A vector of the local packet connectors.
	 */
	private Vector packets = new Vector();

	/**
	 * A vector of the stream connectors that have been opened due to
	 * remote request.
	 */
	private Vector incoming = new Vector();
	
	/**
	 * A vector of the stream connectors that have been opened do to
	 * local open requests.
	 */
	private Vector outgoing = new Vector();

	/**
	 * A vector that contains the buffers that are currently not used
	 * but could be used by streams. This is used to perform instance
	 * pooling on buffers.
	 */
	private Vector buffers = new Vector();
	
	/**
	 * A vector that contains the connectors that are ready for transmission
	 * and byte arrays that are flushes that have been requested by a remote
	 * system.
	 */
	private Vector ready = new Vector();
	
	/**
	 * The listeners that are registered for events provided by this factory.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);

	/**
	 * A flag that indicates whether the multiplexer is still running.
	 */
	private boolean running = true;
	
	/**
	 * A flag that is used for acknowledged transmission. This flag indicates
	 * that the output writer should send an acknowledge.
	 */
	private boolean sendAcknowledge = false;
	
	/**
	 * A flag that is used for acknowledged transmission. This flag indicates
	 * that the output writer should wait for an acknowledge.
	 */
	private boolean waitAcknowledge = false;
	
	/**
	 * A flag that indicates whether acknowledged transmission is used.
	 * This transmission has been added due to a flow control problem
	 * in the nokia 60er series.
	 */
	final private boolean useAcknowledge;
	
	/**
	 * Creates a new multiplexer with the specified input and output
	 * stream. Note that at the other ends of the input and output
	 * stream, there must be another multiplexer. If this constructor
	 * is used, transmissions are not acknowledged.
	 * 
	 * @param plugin The plug-in that uses the multiplexer. This plug-in
	 * 	will be issued as responsible plug-in whenever a connector is
	 * 	requested for its plug-in.
	 * @param input The input stream used by the multiplexer.
	 * @param output The output stream used by the multiplexer.
	 */
	public MultiplexFactory(IMultiplexPlugin plugin, InputStream input, OutputStream output) {
		this(plugin, input, output, false);
	}
	
	/**
	 * Creates a new multiplexer with the specified input and output
	 * stream. Note that at the other ends of the input and output
	 * stream, there must be another multiplexer. The acknowledged
	 * flag indicates whether each transmission must be acknowledged.
	 * Note that the multiplexers at both ends must be configured
	 * equally.
	 * 
	 * @param plugin The plug-in that uses the multiplexer. This plug-in
	 * 	will be issued as responsible plug-in whenever a connector is
	 * 	requested for its plug-in.
	 * @param input The input stream used by the multiplexer.
	 * @param output The output stream used by the multiplexer.
	 * @param acknowledged A flag that enables a stop and wait 
	 * 	protocol for application layer flow control on nokia series
	 * 	60 mobile phones.
	 */
	public MultiplexFactory(IMultiplexPlugin plugin, InputStream input, OutputStream output, boolean acknowledged) {
		this(plugin, input, output, acknowledged, DEFAULT_DATA_LENGTH);
	}

	/**
	 * Creates a new multiplexer with the specified input and output
	 * stream. Note that at the other ends of the input and output
	 * stream, there must be another multiplexer. The acknowledged
	 * flag indicates whether each transmission must be acknowledged.
	 * The size indicates the maximum data unit that will be transfered
	 * via the multiplexer at once. Together with the acknowledgement
	 * configuration, this enables flexible control over the buffer
	 * that is filled at most. Note that the multiplexers at both ends 
	 * must be configured equally.
	 * 
	 * @param plugin The plug-in that uses the multiplexer. This plug-in
	 * 	will be issued as responsible plug-in whenever a connector is
	 * 	requested for its plug-in.
	 * @param input The input stream used by the multiplexer.
	 * @param output The output stream used by the multiplexer.
	 * @param acknowledged A flag that enables a stop and wait 
	 * 	protocol for application layer flow control on nokia series
	 * 	60 mobile phones.
	 * @param size The size of the maximum data unit. Note that 10 additional
	 * 	bytes may be used due to the header sizes. This has been added
	 *  for sun spots as they can only buffer 255 bytes reliably. 
	 */
	public MultiplexFactory(IMultiplexPlugin plugin, InputStream input, OutputStream output, boolean acknowledged, int size) {
		PACKET_LENGTH = size + PACKET_HEADER_LENGTH;
		STREAM_LENGTH = size + STREAM_HEADER_LENGTH;
		this.input = new InputReader(input);
		this.output = new OutputWriter(output);
		this.plugin = plugin;
		this.useAcknowledge = acknowledged;
		IPluginManager manager = plugin.getPluginManager();
		manager.performOperation(this.input, this.input.monitor);
		manager.performOperation(this.output, this.input.monitor);
	}
	
	/**
	 * Adds a multiplex listener to the set of registered listeners. Possible
	 * events are defined by the event constants of this interface.
	 * 
	 * @param types The types of events to register for.
	 * @param listener The listener to register for.
	 */
	public void addMultiplexListener(int types, IListener listener) {
		listeners.addListener(types, listener);
	}
	
	/**
	 * Removes a previously registered listener from the set of registered
	 * listeners. Possible events are defined by the event constants of 
	 * this interface.
	 * 
	 * @param types The types of events to unregister.
	 * @param listener The listener to unregister.
	 * @return True if the listener has been removed, false otherwise.
	 */
	public boolean removeMultiplexListener(int types, IListener listener) {
		return listeners.removeListener(types, listener);
	}

	/**
	 * Creates a packet connector that is connected to the specified
	 * group. It is allowed to create an arbitrary number of packet
	 * connector that is connected to the same group. All registered
	 * packet connectors will receive all incoming packets.
	 * 
	 * @param group The group that the packet connector should be in.
	 * @return A packet connector that is connected to the specified
	 * 	group.
	 * @throws IOException Thrown if the connector cannot be opened,
	 * 	this indicates that the streams are malfunctioning.
	 */
	public synchronized IPacketConnector openConnector(short group) throws IOException {
		if (! running) throw new IOException("Multiplexer is not running, streams closed.");
		PacketConnector connector = new PacketConnector(group);
		addPacket(connector);
		listeners.fireEvent(EVENT_PACKET_OPENED, connector);
		return connector;			
	}
	
	/**
	 * Creates a new stream connector that is connected through a
	 * stream connector on the other end of the streams.
	 * 
	 * @return The stream connector connected through the streams.
	 * @throws IOException Thrown if the connector cannot be opened,
	 * 	this indicates that the streams are malfunctioning or that
	 * 	the receiving end of the connector did not respond.
	 */
	public synchronized IStreamConnector openConnector() throws IOException {
		if (! running) throw new IOException("Multiplexer is not running, streams closed.");
		StreamConnector connector = new StreamConnector(identifier, true);
		addOutgoing(connector);
		synchronized (ready) {
			ready.addElement(connector);
			ready.notify();
		}
		identifier += 1;
		listeners.fireEvent(EVENT_STREAM_OPENED, connector);
		return connector;
	}

	/**
	 * Closes the multiplexer and performs the necessary cleanup by
	 * shutting down the streams. If the multiplexer still has issued
	 * connectors that are still referenced, all calls to the connectors
	 * will be interrupted and will throw an exception. 
	 */
	public void close() {
		input.close();
		output.close();
		synchronized (this) {
			if (! running) return;
			running = false;			
		}
		for (int i = incoming.size() - 1; i >= 0; i--) {
			StreamConnector sc = (StreamConnector)incoming.elementAt(i);
			incoming.removeElementAt(i);
			synchronized (sc) {
				sc.released = true;
				sc.notifyAll();
			}			
			listeners.fireEvent(EVENT_STREAM_CLOSED, sc);
		}
		for (int i = outgoing.size() - 1; i >= 0; i--) {
			StreamConnector sc = (StreamConnector)outgoing.elementAt(i);
			outgoing.removeElementAt(i);
			synchronized (sc) {
				sc.released = true;
				sc.notifyAll();
			}			
			listeners.fireEvent(EVENT_STREAM_CLOSED, sc);
		}
		for (int i = packets.size() - 1; i >= 0; i--) {
			PacketConnector pc = (PacketConnector)packets.elementAt(i);
			packets.removeElementAt(i);
			synchronized (pc) {
				pc.released = true;
				pc.notifyAll();
			}
			listeners.fireEvent(EVENT_PACKET_CLOSED, pc);
		}
		plugin.closeMultiplexer(MultiplexFactory.this);
	}
	
	/**
	 * Adds the specified packet connector to the set of registered connectors.
	 * 
	 * @param connector The packet connector to add to the set.
	 */
	private void addPacket(PacketConnector connector) {
		synchronized (packets) {
			packets.addElement(connector);
		}
	}
	
	/**
	 * Puts the set of registered packet connectors into the passed vector that
	 * is registered for the specified group.
	 * 
	 * @param group The group of the packet connector.
	 * @param data The vector that will be filled with the connectors that
	 * 	are currently registered and belong to the specified group.
	 */
	private void getPacket(short group, Vector data) {
		synchronized (packets) {
			for (int i = packets.size() - 1; i >= 0; i--) {
				PacketConnector connector = (PacketConnector)packets.elementAt(i);
				if (connector.group == group) {
					data.addElement(connector);
				}
			}
			
		}
	}
	
	/**
	 * Removes the specified packet connector from the set of registered
	 * connectors.
	 * 
	 * @param connector The packet connector that should be removed.
	 */
	private void removePacket(PacketConnector connector) {
		synchronized (packets) {
			packets.removeElement(connector);
		}
	}
	
	/**
	 * Adds the specified stream connector to the list of incoming 
	 * stream connectors.
	 * 
	 * @param connector The stream connector to add.
	 */
	private void addIncoming(StreamConnector connector) {
		synchronized (incoming) {
			incoming.addElement(connector);
		}
	}
	
	/**
	 * Removes the specified stream connector from the set of incoming
	 * stream connectors.
	 * 
	 * @param connector The connector to remove.
	 */
	private void removeIncoming(StreamConnector connector) {
		synchronized (incoming) {
			incoming.removeElement(connector);
		}
	}
	
	/**
	 * Returns the stream connector with the specified identifier
	 * that might be contained in the incoming list.
	 * 
	 * @param identifier The identifier that should be retrieved 
	 * @return The incoming stream connector with the specified identifier
	 * 	or null if no such incoming stream connector exists. 
	 */
	private StreamConnector getIncoming(int identifier) {
		synchronized (incoming) {
			for (int i = incoming.size() - 1; i >= 0; i--) {
				StreamConnector connector = (StreamConnector)incoming.elementAt(i);
				if (connector.identifier == identifier) {
					return connector;
				}
			}
		}
		return null;
	}

	/**
	 * Adds the specified outgoing stream connector to the set of
	 * outgoing connectors.
	 * 
	 * @param connector The stream connector to add to the set of
	 * 	outgoing connectors.
	 */
	private void addOutgoing(StreamConnector connector) {
		synchronized (outgoing) {
			outgoing.addElement(connector);
		}
	}
	
	/**
	 * Removes the specified stream connector from the set of 
	 * outgoing stream connectors.
	 * 
	 * @param connector The stream connector that should be removed.
	 */
	private void removeOutgoing(StreamConnector connector) {
		synchronized (outgoing) {
			outgoing.removeElement(connector);
		}
	}
	
	/**
	 * Returns the outgoing stream connector with the specified
	 * identifier or null if no such stream connector exists.
	 * 
	 * @param identifier The identifier of the connector to retrieve.
	 * @return The stream connector contained in the outgoing set
	 * 	with the specified identifier or null if it cannot be found. 
	 */
	private StreamConnector getOutgoing(int identifier) {
		synchronized (outgoing) {
			for (int i = outgoing.size() - 1; i >= 0; i--) {
				StreamConnector connector = (StreamConnector)outgoing.elementAt(i);
				if (connector.identifier == identifier) {
					return connector;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns a buffer from the buffer pool or creates a new one
	 * if not enough buffers are available.
	 * 
	 * @return A new buffer or a reused buffer. The buffer will be
	 * 	initialized with a data array that has a size of maximum
	 * 	between packet and stream length.
	 */
	private Buffer createBuffer() {
		synchronized (buffers) {
			if (buffers.size() != 0) {
				Buffer result = (Buffer)buffers.elementAt(0);
				buffers.removeElementAt(0);
				return result;
			}
		}
		return new Buffer();
	}
	
	/**
	 * Releases an unused buffer and puts it back into the pool
	 * if the pool size limit has not been reached already.
	 * 
	 * @param buffer The buffer that is no longer used.
	 */
	private void releaseBuffer(Buffer buffer) {
		buffer.offset = 0;
		buffer.length = 0;
		synchronized (buffers) {
			if (buffers.size() < BUFFER_POOL) {
				buffers.addElement(buffer);
			}
		}
	}

}
