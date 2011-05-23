package info.pppc.basex.plugin.transceiver;

import info.pppc.base.system.event.Event;
import info.pppc.base.system.plugin.IPluginManager;
import info.pppc.basex.plugin.transceiver.spot.ShieldedInputStream;
import info.pppc.basex.plugin.transceiver.spot.ShieldedOutputStream;
import info.pppc.basex.plugin.util.MultiplexFactory;
import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.io.j2me.radiostream.RadiostreamConnection;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import info.pppc.base.system.IExtension;
import info.pppc.base.system.ISession;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.event.ListenerBundle;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.plugin.IPacket;
import info.pppc.base.system.plugin.IPacketConnector;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.plugin.ITransceiver;
import info.pppc.base.system.plugin.ITransceiverManager;
import info.pppc.base.system.plugin.Packet;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.util.IMultiplexPlugin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This plug-in implements a transceiver for the sun spot sensor nodes. To implement
 * a single group connector, this plug-in must use two different group sockets as
 * it is not possible to receive on a broadcast socket and it is not possible to
 * send broadcasts on a bound socket. Thus, the plug-in creates two sockets for each
 * group connector. 
 * In order to implement streams, this plug-in reuses the radio stream implementation
 * that comes with sun spots. Unfortunately, this implementation requires the sender
 * and receiver to open sockets at the same time. Thus, the plug-in implements a
 * very basic handshake protocol to open connections. To do this, it uses
 * 
 * @author Marcus Handte
 */
public class MxSpotTransceiver implements ITransceiver, IListener, IMultiplexPlugin {
	
	/**
	 * The packet connector for datagram connectors. 
	 * 
	 * @author Marcus Handte
	 */
	private class PacketConnector implements IPacketConnector, IOperation {

		/**
		 * The listener bundle with listeners for incoming
		 * packets.
		 */
		private ListenerBundle listeners = new ListenerBundle(this);
		
		/**
		 * The radiogram connector of this plug-in to send broadcasts.
		 */
		private RadiogramConnection sender;
        /**
         ** The radiogram connector of this plug-in to receive broadcasts.
         */
		private RadiogramConnection receiver;
		/**
		 * The maximum packet length.
		 */
		private int length;
		
		/**
		 * The monitor used to cancel the reception.
		 */
		private IMonitor monitor;
		
		/**
		 * Creates a new packet connector from a given radiogram
		 * connectors for sending and receiving.
		 * 
		 * @param sender The radiogram sender connector.
		 * @param receiver The radiogram receiver connector.
		 */
		public PacketConnector(RadiogramConnection sender,
                RadiogramConnection receiver) throws IOException {
			this.sender = sender;
            this.receiver = receiver;
			this.length = sender.getMaximumLength();
			sender.setMaxBroadcastHops(1);
            receiver.setMaxBroadcastHops(1);
		}
		
		/**
		 * Adds a packet listener to the bundle.
		 * 
		 * @param type The type of events.
		 * @param listener The listener to add.
		 */
		public void addPacketListener(int type, IListener listener) {
			listeners.addListener(type, listener);
		}

		/**
		 * Creates a packet.
		 * 
		 * @return The packet.
		 */
		public IPacket createPacket() {
			return new Packet(length);
		}

		/**
		 * Returns the maximum packet length.
		 * 
		 * @return The maximum packet length.
		 */
		public int getPacketLength() {
			return length;
		}

		/**
		 * Removes the specified packet listener.
		 * 
		 * @param type The type of event.
		 * @param listener The listener to remove.
		 */
		public boolean removePacketListener(int type, IListener listener) {
			return listeners.removeListener(type, listener);
		}

		/**
		 * Called to send a packet via the connector.
		 * 
		 * @param packet The packet to send.
		 */
		public synchronized void sendPacket(IPacket packet) throws IOException {
			byte[] payload = packet.getPayload();
			Datagram d = sender.newDatagram(length);
            d.write(payload, 0, payload.length);
			sender.send(d);
		}

		/**
		 * Returns a reference to the transceiver plug-in.
		 * 
		 * @return This transceiver plug-in.
		 */
		public IPlugin getPlugin() {
			return MxSpotTransceiver.this;
		}

		/**
		 * Called to release the 
		 */
		public void release() {
			monitor.cancel();
			try {
				sender.close();
			} catch (IOException e) {
				Logging.debug(getClass(), "Could not close connector.");
			}
			try {
				receiver.close();	
			} catch (IOException e) {
				Logging.debug(getClass(), "Could not close connector.");
			}
            synchronized (monitor) {
				try {
					monitor.join();	
				} catch (InterruptedException e) {
					Logging.debug(getClass(), "Thread got interrupted.");
				}
			}
			listeners.fireEvent(EVENT_PACKET_CLOSED);
		}
		
		/**
		 * Runs the receive loop.
		 * 
		 * @param monitor The monitor to cancel the loop.
		 */
		public void perform(IMonitor monitor) throws Exception {
			this.monitor = monitor;
			Datagram d = receiver.newDatagram(length);
			while (! monitor.isCanceled()) {
                Packet p = new Packet(length);
				receiver.receive(d);
				if (d.getLength() > 0) {
                    byte[] payload = new byte[d.getLength()];
					System.arraycopy(d.getData(), 0, payload, 0, payload.length);
					p.setPayload(payload);
					listeners.fireEvent(EVENT_PACKET_RECEIVED, p);
				}

			}
		}		
	}
	
	
	/**
	 * Handshakes are simple objects that are transferred back
	 * and forth to request connections on other sun spots.
	 * 
	 * @author Marcus Handte
	 */
    private class Message {
    	/**
    	 * The message type that requests a new connection.
    	 */
        private static final byte SYN = 0;
        /**
         * The message type that acknowledges a connection.
         */
        private static final byte ACK = 1;
        /**
         * The type of message that this message represents.
         */
        private byte type = 0;
        /**
         * The sender of the message.
         */
        private String sender = null;
        /**
         * The receiver of the message.
         */
        private String receiver = null;
    }

    /**
     * A helper class for multiplexer entries. These entries are
     * stored in the multiplexer list.
     * 
     * @author Marcus Handte
     */
    private class Multiplexer {
    	/**
    	 * A flag that indicates whether the multiplexer is 
    	 * actually opened. An opened will never be closed
    	 * but a multiplexer might be in an intermediate state
    	 * not open and not closed during connection setup.
    	 */
    	private boolean opened = false;
    	/**
    	 * A flag that indicates whether the multiplexer has
    	 * been closed. A closed will never be open, if closed
    	 * is true, the multiplexer is doomed forever.
    	 */
    	private boolean closed = false;
    	/**
    	 * The address of the system that is connected to the
    	 * multiplex factory.
    	 */
    	private String address;
    	/**
    	 * The multiplex factory that represents the connection.
    	 */
    	private MultiplexFactory factory;
    	/**
    	 * The underlying connection that provides the input
    	 * and output streams to the factory.
    	 */
    	private RadiostreamConnection connection;
    }  

    /**
     * The radio channel id.
     */
    private static final int RADIO_CHANNEL = IProprietaryRadio.DEFAULT_CHANNEL;

    /**
     * The pan identifier.
     */
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;

	/**
	 * The ability of the plug-in [1][8].
	 */
	public static short PLUGIN_ABILITY = 0x0108;
	
	/**
	 * The extension layer of the plug-in (transceiver).
	 */
	public static short PLUGIN_EXTENSION = IExtension.EXTENSION_TRANSCEIVER;
	
	/**
	 * The port on which the group connector will send and receive 
	 * announcements.
	 */
	public static short BROADCAST_PORT = 32;
	
	/**
	 * The port on which the server will listen for incoming connections.
	 */
	public static short SERVER_PORT = 33;
	
	/**
	 * The port on which the streams to other remote systems will be 
	 * established.
	 */
	public static short STREAM_PORT = 34;
	
	/**
	 * The timeout for outgoing connection requests.
	 */
	public static int TIMEOUT_PERIOD = 1000;
	
	/**
	 * The plug-in description property that defines the address.
	 */
	public static String PROPERTY_ADDRESS = "AD";
	
	/**
	 * The plug-in description. The actual configuration is done 
	 * during start up of the plug-in.
	 */
	private PluginDescription descripition = new PluginDescription(PLUGIN_ABILITY, PLUGIN_EXTENSION);
	
	/**
	 * The transceiver listeners that listen for enable/disable events.
	 */
	private ListenerBundle listeners = new ListenerBundle(this);
	
	/**
	 * A flag to indicate whether the plug-in is enabled or disabled.
	 */
	private boolean enabled = false;

	/**
	 * The reference to the transceiver manager.
	 */
	private ITransceiverManager manager;
	
    /**
     * The packet connector that is used for connection establishment.
     */
    private IPacketConnector connector;


	/**
	 * Creates a new sun spot transceiver.
	 */
	public MxSpotTransceiver() { 
		  descripition.setProperty(PROPERTY_ADDRESS, 
				  System.getProperty("IEEE_ADDRESS"), true);
	}
	
	/**
	 * Adds a transceiver listener to the bundle.
	 * 
	 * @param type The type of events.
	 * @param listener The listener to add.
	 */
	public void addTransceiverListener(int type, IListener listener) {
		listeners.addListener(type, listener);
	}

	/**
	 * Removes a previously registered listener from the bundle.
	 * 
	 * @param type The types of events to remove.
	 * @param listener The listener to remove.
	 * @return True if removed, false otherwise.
	 */
	public boolean removeTransceiverListener(int type, IListener listener) {
		return listeners.removeListener(type, listener);
	}

	/**
	 * Returns the enabled state.
	 * 
	 * @return True if enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Called to enable or disable the transceiver.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		if (enabled != this.enabled) {
			this.enabled = enabled;
			if (enabled) {
				// turn on the thing
				IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
                rpm.setChannelNumber(RADIO_CHANNEL);
                rpm.setPanId(PAN_ID);
                rpm.setOutputPower(31);
                try {
                	synchronized (this) {
                        connector = openGroup(SERVER_PORT);
                        connector.addPacketListener(IPacketConnector.EVENT_PACKET_RECEIVED, this);
                	}
                	listeners.fireEvent(EVENT_TRANCEIVER_ENABLED);     
                } catch (IOException e) {
                    Logging.debug(getClass(), "Could not open server port.");
                    connector = null;
                }
			} else {
				// turn of the thing
				synchronized (this) {
					if (connector != null) {
	                    connector.removePacketListener(IPacketConnector.EVENT_PACKET_RECEIVED, this);
	                    connector.release();
	                    connector = null;	                   
					}					
					// TODO cleanup multiplexers
					Vector mxs = new Vector();
					Enumeration e = multiplexers.elements();
					while (e.hasMoreElements()) {
						mxs.addElement(e.nextElement());
					}
					int length = mxs.size();
					while (length > 0) {
						length -= 1;
						Multiplexer mx = (Multiplexer)mxs.elementAt(length);
						mxs.removeElementAt(length);
						mx.factory.close();
					}
				}
				listeners.fireEvent(EVENT_TRANCEIVER_DISABLED);
			}
		}
	}

	/**
	 * Called by the manager to set the backward reference.
	 * 
	 * @param manager The manager reference for internal use.
	 */
	public void setTransceiverManager(ITransceiverManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the plug-in description of the plug-in.
	 * 
	 * @return A reference to the plug-in description.
	 */
	public PluginDescription getPluginDescription() {
		return descripition;
	}

	/**
	 * Called by the manager to start the plug-in.
	 * This enables the transceiver.
	 */
	public void start() {
		setEnabled(true);
	}

	/**
	 * Called by the manager to stop the plug-in.
	 * This disables the transceiver.
	 */
	public void stop() {
		setEnabled(false);
	}

    private Message unserialize(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Message message = new Message();
        message.type = (byte)(ois.readByte() & 0xff);
        message.sender = ois.readUTF();
        message.receiver = ois.readUTF();
        return message;
    }

    private byte[] serialize(Message message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(bos);
        ous.writeByte(message.type);
        ous.writeUTF(message.sender);
        ous.writeUTF(message.receiver);
        ous.flush();
        return bos.toByteArray();
    }

    



    /**
     * A hash table that hashes multiplexers by address.
     */
    private Hashtable multiplexers = new Hashtable();
    

    /**
     * Called to wait until a multiplexer has been opened
     * or until a timeout has been expired. 
     * 
     * @param multiplexer The multiplexer to wait on.
     * @return True if the multiplexer is open, false if
     * 	the timeout has expired.
     */
    private boolean wait(Multiplexer multiplexer) {
    	synchronized (multiplexer) {
        	if (multiplexer.opened) return true;
        	long start = System.currentTimeMillis();
        	long now = start;
        	long remainder = TIMEOUT_PERIOD;
        	while (! multiplexer.opened && ! multiplexer.closed) {
        		try {
        			multiplexer.wait(remainder);
        		} catch (InterruptedException e) { }
        		now = System.currentTimeMillis();
        		remainder = start + TIMEOUT_PERIOD - now; 
        		if (remainder <= 0) {
        			Logging.debug(getClass(), "Connection attempt timed out.");
        			multiplexer.closed = true;
        			multiplexer.notifyAll();
        		}
        	}
    	}
    	if (multiplexer.closed) {
   			multiplexer.factory.close();
    	}
    	return multiplexer.opened;
    }


	/**
	 * Called by the thing to open a session.
	 */
	public IStreamConnector openSession(ISession session) throws IOException {
		Multiplexer multiplexer = null;
		synchronized (this) {
			multiplexer = (Multiplexer)multiplexers.get(session.getLocal());
			if (multiplexer == null) {
				// create a new multiplexer
				multiplexer = new Multiplexer();
				multiplexer.connection = (RadiostreamConnection)
                	Connector.open("radiostream://" + session.getLocal().toString() 
                			+ ":" + STREAM_PORT);
				multiplexer.address = (String)session.getLocal();
				// send the connection request
				try {
					multiplexer.factory = new MultiplexFactory
						(this, new ShieldedInputStream(multiplexer.connection.openInputStream()), 
							new ShieldedOutputStream(multiplexer.connection.openOutputStream()));
					Message s = new Message();
	                s.type = Message.SYN;
	                s.sender = (String)System.getProperty("IEEE_ADDRESS");
	                s.receiver = (String)session.getLocal();
	                IPacket out = connector.createPacket();
	                out.setPayload(serialize(s));
	                connector.sendPacket(out);					
				} catch (IOException e) {
					Logging.debug(getClass(), "Could not initialize streams.");
					multiplexer.connection.close();
					throw e;
				}
				// add the multiplexer to the table for other requests
				multiplexers.put(session.getLocal(), multiplexer);
			}
		}
		if (wait(multiplexer)) {
			return multiplexer.factory.openConnector();
		} else {
			throw new IOException("Could not initate connection.");
		}
	}

	/**
	 * Called whenever the server socket receives an incoming packet.
	 * 
	 * @param event The event that includes the packet that has been
	 * 	received.
	 */
    public void handleEvent(Event event) {
        try {
            IPacket p = (IPacket)event.getData();
            Message s = unserialize(p.getPayload());
            if (s.receiver.equals(System.getProperty("IEEE_ADDRESS"))) {
	            switch (s.type) {
	                case Message.SYN: {                  
	                    // retrieve the current multiplexer or create a new one
	                    Multiplexer multiplexer = null;
	                    synchronized (this) {
	                    	multiplexer = (Multiplexer)multiplexers.get(s.sender);
	                    	if (multiplexer == null) {
	                    		Logging.debug(getClass(), "Creating new connection for incoming request.");
	            				// create a new multiplexer
	            				multiplexer = new Multiplexer();
	            				try {
		            				multiplexer.connection = (RadiostreamConnection)
	                            		Connector.open("radiostream://" + s.sender + ":" + STREAM_PORT);
		            				multiplexer.address = s.sender;
		            				try {
		            					multiplexer.factory = new MultiplexFactory
		            						(this, new ShieldedInputStream(multiplexer.connection.openInputStream()), 
		            								new ShieldedOutputStream(multiplexer.connection.openOutputStream()));
		            				} catch (IOException e) {
		            					Logging.debug(getClass(), "Could not initialize streams.");
		            					multiplexer.connection.close();
		            					return;
		            				}
	            				} catch (IOException e) {
	            					Logging.debug(getClass(), "Could not open connection.");
	            					return;
	            				}
	            				multiplexers.put(s.sender, multiplexer);
	                    	}
	                    	synchronized (multiplexer) {
	                    		if (! multiplexer.opened && ! multiplexer.closed) {
	                    			Logging.debug(getClass(), "Activating connection.");
	                    			multiplexer.opened = true;
	                    			multiplexer.notifyAll();
	                    			// send reply back
	                    			Message m = new Message();
	                    			m.sender = s.receiver;
	                    			m.receiver = s.sender;
	                    			m.type = Message.ACK;
	                    			try {
		                    			IPacket out = connector.createPacket();
		            	                out.setPayload(serialize(m));
		            	                connector.sendPacket(out);				                    				
	                    			} catch (IOException e) {
	                    				Logging.debug(getClass(), "Could not send ack.");
	                    			}
	                    		} else if (multiplexer.opened && ! multiplexer.closed) {
	                    			multiplexer.closed = true;
	                    			multiplexer.opened = false;
	                    			multiplexer.factory.close();
	                    			// here we could directly open a new one 
	                    			// to improve the performance
	                    		} 
	                    	}
	                    }
	                    break;
	                }
	                case Message.ACK: {
	                    Multiplexer multiplexer = null;
	                    synchronized (this) {
	                    	multiplexer = (Multiplexer)multiplexers.get(s.sender);
	                    }
	                    if (multiplexer != null) {
	                    	synchronized (multiplexer) {
	                    		if (! multiplexer.opened && ! multiplexer.closed) {
	                    			multiplexer.opened = true;
	                    			multiplexer.notifyAll();
	                    		} 
	                    	}
	                    } else {
	                    	Logging.debug(getClass(), "Could not find multiplexer for ack.");
	                    }
	                    break;
	                }
	                default: {
	                	Logging.debug(getClass(), "Received unknown packet from: " + s.sender);
	                	break;
	                }
	            }
	        }
        } catch (IOException e) {
            Logging.debug(getClass(), "Received malformed packet.");
        }
    }

	/**
	 * Called to prepare a session for remote communication.
	 * 
	 * @param d The remote plug-in description.
	 * @param c The collection of properties.
	 * @param s The session.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		try {
			String address = (String)d.getProperty(PROPERTY_ADDRESS);
			s.setLocal(address);
			return (address != null);
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Opens a group connector for unreliable broadcast communication.
	 * 
	 * @return A packet connector for unreliable broadcast communication.
     * @throws IOException Thrown if the port cannot be opened.
	 */
	public IPacketConnector openGroup() throws IOException {
		return openGroup(BROADCAST_PORT);
    }
	
	/**
	 * Opens a group connector for unreliable broadcast communication
	 * on the specified port.
	 * 
	 * @param port The port to open the connector.
	 * @return The group connector.
	 * @throws IOException Thrown if the port cannot be opened.
	 */
	private IPacketConnector openGroup(short port) throws IOException {
		Connection sender = Connector.open("radiogram://broadcast:" + port);
		try {
	        Connection receiver = Connector.open("radiogram://:" + port);
	        PacketConnector p = new PacketConnector
	        	((RadiogramConnection)sender, (RadiogramConnection) receiver);
	        manager.performOperation(p);
	        return p;				
		} catch (IOException e) {
			sender.close();
			throw e;
		}
	}

    /**
     * Returns the plug-in manager that is used to execute operations
     * in the multiplexer.
     */
    public IPluginManager getPluginManager() {
        return manager;
    }

    /**
     * Called whenever a multiplexer receives an incoming connection.
     * 
     * @param source The source of the request.
     * @param connector The connector provided by the factory.
     */
    public void acceptConnector(MultiplexFactory source, IStreamConnector connector) {
        manager.acceptSession(connector);
    }

    /**
     * Called whenever a multiplexer is closed.
     * 
     * @param factory The multiplexer that has been closed.
     */
    public void closeMultiplexer(MultiplexFactory factory) {
    	Multiplexer multiplexer = null;
    	synchronized (this) {
    		Enumeration e = multiplexers.elements();
    		while (e.hasMoreElements()) {
    			Multiplexer m = (Multiplexer)e.nextElement();
    			if (m.factory == factory) {
    				multiplexer = m;
    				break;
    			}
    		}
    	}
    	if (multiplexer != null) {
    		synchronized (multiplexer) {
    			multiplexer.closed = true;
    			multiplexer.opened = false;
    			try {
    				multiplexer.connection.close();
    			} catch (IOException e) { }
    			multiplexer.notifyAll();
    		}
    		synchronized (this) {
    			if (multiplexers.get(multiplexer.address) == multiplexer) {
    				multiplexers.remove(multiplexer.address);
    			}
    		}
    	} 
    }

    





	
}
