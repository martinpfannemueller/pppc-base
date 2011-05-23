package info.pppc.basex.plugin.semantic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.bouncycastle.asn1.x509.X509Name;

import info.pppc.base.system.ISession;
import info.pppc.base.system.Invocation;
import info.pppc.base.system.InvocationException;
import info.pppc.base.system.PluginDescription;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.io.ObjectInputStream;
import info.pppc.base.system.io.ObjectOutputStream;
import info.pppc.base.system.nf.NFCollection;
import info.pppc.base.system.nf.NFDimension;
import info.pppc.base.system.plugin.ISemantic;
import info.pppc.base.system.plugin.ISemanticManager;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.security.AbstractCertificate;
import info.pppc.base.system.security.KeyStore;
import info.pppc.base.system.security.StaticSecurity;
import info.pppc.base.system.security.ecc.ECCCertificate;
import info.pppc.base.system.security.sym.AESSymmetricKey;
import info.pppc.base.system.security.sym.HMACSymmetricKey;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.semantic.dh.AbstractExchange;

/**
 * A semantic plug-in that implements a Diffie Hellmann key exchange on top
 * of a secure three way handshake that is authenticated using certificates.
 * 
 * @author Mac
 */
public class ExchangeSemantic implements ISemantic {

	/**
	 * The ability of the plug-in. [5][2].
	 */
	public static final short PLUGIN_ABILITY = 0x0502;
	
	/**
	 * The nonce has the size of a 64-Bit integer
	 */
	private static final short NONCE_SIZE=8;
	
	/**
	 * The maximum time drift that may occur between two devices, 10 minutes
	 */
	private static final int MAXIMUM_TIME_DRIFT=1000*60*10;
	
	/**
	 * The property key of the elliptic curve oid, set in the plug-in descriptor
	 */
	private static final String CURVE_PROPERTY="curveId";
	
	/**
	 * The plug-in description of the ip plug-in.
	 */
	private final PluginDescription description;
	
	/**
	 * A flag that indicates whether the plug-in has been started already
	 * or whether it is currently stopped.
	 */
	private boolean started = false;

	/**
	 * The local semantic plug-in manager. Used to synchronize invocations
	 * that do not use the same semantic plug-in.
	 */
	private ISemanticManager manager;

	/**
	 * The key store to update.
	 */
	private KeyStore store = KeyStore.getInstance();
	
	/**
	 * Creates a new exchange semantic plug-in.
	 */
	public ExchangeSemantic() {
		PluginDescription pd=new PluginDescription(PLUGIN_ABILITY,EXTENSION_SEMANTIC);
		AbstractCertificate cert=store.getCertificate(SystemID.SYSTEM);
		if(cert instanceof ECCCertificate)
		{
			ECCCertificate ecc=(ECCCertificate)cert;
			String eccCurveId=ecc.getCurveId();
			if(eccCurveId!=null)
			{
				pd.setProperty(CURVE_PROPERTY, eccCurveId, false);
			}
		}
		this.description=pd;
	}
	
	
	/**
	 * Negotiates the configuration for the plug-in.
	 * 
	 * @param d The description of the remote plug-in.
	 * @param c The nf collection with requirements.
	 * @param s The session object to store the configuration.
	 */
	public boolean prepareSession(PluginDescription d, NFCollection c, ISession s) {
		checkPlugin();
		String curveProperty=(String)d.getProperty(CURVE_PROPERTY);
		String ownProperty=(String)description.getProperty(CURVE_PROPERTY);
		if(ownProperty!=null && curveProperty!=null && curveProperty.equals(ownProperty))
		{
			if(ownProperty.equals(ECCCertificate.SUN_ALGORITHM_ID))
			{
				s.setRemote(new byte[]{AbstractExchange.PERFORM_FAST_ECDH});
			}
			else
			{
				s.setRemote(new byte[]{AbstractExchange.PERFORM_ECDH});
			}
		}
		else
		{
			s.setRemote(new byte[]{AbstractExchange.PERFORM_NORMAL_DH});
		}
		// semantic must be key exchange, otherwise return that the requirements are not 
		// met by this semantic plug-in.
		NFDimension dim = c.getDimension(EXTENSION_SEMANTIC, NFDimension.IDENTIFIER_TYPE);
		if (dim.getHardValue().equals(new Short((short)NFCollection.TYPE_EXCHANGE))) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Called whenever an incoming connection is forwarded.
	 * 
	 * @param connector The connector for the new connection.
	 * @param session The session of the new connection.
	 */
	public void deliverIncoming(IStreamConnector connector, ISession session) {
		checkPlugin();
		AbstractExchange exchange=AbstractExchange.createInstance(session.getRemote()[0]);
		try {
			byte[] local = exchange.createLocalSecret();
			ObjectOutputStream out = new ObjectOutputStream(connector.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(connector.getInputStream());
			ByteArrayOutputStream bos=new ByteArrayOutputStream(2*SystemID.LENGTH+String.valueOf(Long.MIN_VALUE).getBytes().length+local.length);
			SystemID system = (SystemID)in.readObject();
			if(!system.equals(session.getTarget()))
			{
				Logging.debug(getClass(), "Wrong remote device in request.");
				throw new IOException("Wrong remote device in request.");
			}
			bos.write(system.getBytes());
			
			byte[] nonce=(byte[])in.readObject();
			exchange.setNonce(nonce);
			bos.write(nonce);
			
			SystemID mySystem=(SystemID)in.readObject();
			if(!SystemID.SYSTEM.equals(mySystem))
			{
				Logging.debug(getClass(), "Wrong target device in request.");
				throw new IOException( "Wrong target device in request.");
			}
			bos.write(mySystem.getBytes());
			
			long time = in.readLong();
			long currentTime=System.currentTimeMillis();
			if(Math.abs(currentTime-time)>MAXIMUM_TIME_DRIFT)
			{
				Logging.debug(getClass(), "Exchange request is too old (>"+MAXIMUM_TIME_DRIFT+"ms).");
				throw new IOException("Exchange request is too old (>"+MAXIMUM_TIME_DRIFT+"ms).");
			}
			bos.write((String.valueOf(time)).getBytes());
			
			byte[] remote = (byte[])in.readObject();
			bos.write(remote);
			
			byte[] signature=(byte[])in.readObject();
			
			//Check the signature:
			AbstractCertificate remoteCert=store.getCertificate(system); //Check if the remote certificate is locally available
			int trustLevel=store.getCertificateLevel(remoteCert);
			Vector certificates=(Vector)in.readObject();
			if(remoteCert==null)
			{
				if(certificates.size()<=0)
				{
					Logging.debug(getClass(), "Certificate list is invalid.");
					throw new IOException("Certificate list is invalid.");
				}
				AbstractCertificate[] certChain=new AbstractCertificate[certificates.size()];
				for(int i=0;i<certChain.length;i++)
				{
					certChain[i]=store.createCertificate((byte[])certificates.get(i));
				}
				trustLevel=store.verifyCertificate(certChain);
				remoteCert=certChain[0];
			}
			//Compare fingerprint and SystemID
			byte[] remoteSystem=system.getBytes();
			byte[] fingerprint=remoteCert.getFingerprint();
			for(int i=0;i<remoteSystem.length;i++)
			{
				if(remoteSystem[i]!=fingerprint[i])
				{
					Logging.debug(getClass(), "Device id differs from fingerprint.");
					throw new IOException("Device id differs from fingerprint.");
				}
			}
			
			if(!remoteCert.verifySignature(bos.toByteArray(), signature))
			{
				Logging.debug(getClass(), "Asymmetric signature is invalid.");
				throw new IOException("Asymmetric signature is invalid.");
			}
			bos.reset();
			
			out.writeObject(SystemID.SYSTEM);
			bos.write(SystemID.SYSTEM.getBytes());
			out.writeObject(nonce);
			bos.write(nonce);
			out.writeObject(system);
			bos.write(system.getBytes());
			out.writeObject(local);
			bos.write(local);
			
			AbstractCertificate cert=store.getCertificate(SystemID.SYSTEM);
			out.writeObject(cert.sign(bos.toByteArray()));
			Vector certificateVector=new Vector();
			while(cert!=null)
			{
				X509Name oldSubject=cert.getSubject();
				certificateVector.addElement(cert.toByteArray());
				cert=store.getCertificate(cert.getIssuer());
				if(cert.getSubject().equals(oldSubject))
				{
					break; //CA certificate found
				}
			}
			out.writeObject(certificateVector); //Add the certificate chain to the message
			out.flush();
			byte[] secret = exchange.getSharedSecret(remote, remoteCert);
			byte[] aes = new byte[16];
			byte[] hmac = new byte[16];
			for (int i = 0; i < 16; i++) {
				aes[i] = secret[i];
				hmac[i] = secret[i + 16];
			}
			store.addKey(system, new HMACSymmetricKey(hmac), new AESSymmetricKey(aes), time, trustLevel);
		} catch (IOException e) {
			Logging.error(getClass(), "Exception during key exchange.", e);
			connector.release();
		}
	}
	
	/**
	 * Called whenever an outgoing invocation shall be delivered.
	 * 
	 * @param invocation The invocation.
	 * @param session The session configuration.
	 */
	public void performOutgoing(Invocation invocation, ISession session) {
		checkPlugin();
		AbstractExchange exchange=AbstractExchange.createInstance(session.getRemote()[0]);
		try {
			byte[] local = exchange.createLocalSecret();
			long time = System.currentTimeMillis();
			ISession prepared = manager.prepareSession(session, invocation.getRequirements());
			IStreamConnector c = manager.openSession(prepared);
			try {
				ObjectOutputStream out = new ObjectOutputStream(c.getOutputStream());
				ByteArrayOutputStream bos=new ByteArrayOutputStream(2*SystemID.LENGTH+String.valueOf(Long.MIN_VALUE).getBytes().length+local.length);
				ObjectInputStream in = new ObjectInputStream(c.getInputStream());
				bos.write(SystemID.SYSTEM.getBytes()); //My SystemID
				out.writeObject(SystemID.SYSTEM);
				
				byte[] nonce=new byte[NONCE_SIZE];
				StaticSecurity.getSecureRandom().nextBytes(nonce);
				bos.write(nonce); //Nonce
				exchange.setNonce(nonce);
				out.writeObject(nonce);
				
				bos.write(session.getTarget().getBytes());
				out.writeObject(session.getTarget());
				
				bos.write(String.valueOf(time).getBytes()); //Timestamp
				out.writeLong(time);
				
				bos.write(local); //DHMessage
				out.writeObject(local);
				
				AbstractCertificate cert=store.getCertificate(SystemID.SYSTEM);
				bos.close();
				out.writeObject(cert.sign(bos.toByteArray())); //Write the signature
				Vector certificateVector=new Vector();
				while(cert!=null)
				{
					X509Name oldSubject=cert.getSubject();
					certificateVector.addElement(cert.toByteArray());
					cert=store.getCertificate(cert.getIssuer());
					if(cert.getSubject().equals(oldSubject))
					{
						break; //CA certificate found
					}
				}
				out.writeObject(certificateVector); //Add the certificate chain to the message

				out.flush();
				
				//Response:
				bos.reset();
				SystemID remoteSystem=(SystemID)in.readObject();
				if(!remoteSystem.equals(session.getTarget()))
				{
					Logging.debug(getClass(), "Wrong target device in response.");
					throw new IOException("Wrong target device in response.");
				}
				bos.write(remoteSystem.getBytes());
				byte[] nonceResponse=(byte[])in.readObject();
				bos.write(nonceResponse);
				for(int i=0;i<nonce.length;i++)
				{
					if(nonce[i]!=nonceResponse[i])
					{

						Logging.debug(getClass(), "Nounce is not identical.");
						throw new IOException("Nounce is not identical.");
					}
				}
				SystemID mySystem=(SystemID)in.readObject();
				bos.write(mySystem.getBytes());
				if(!mySystem.equals(SystemID.SYSTEM))
				{
					Logging.debug(getClass(), "Message was not sent to this system.");
					throw new IOException("Message was not sent to this system.");
				}
				
				byte[] remote = (byte[])in.readObject();
				bos.write(remote);
				
				byte[] signature=(byte[])in.readObject();
				
				AbstractCertificate remoteCert=store.getCertificate(remoteSystem); //Check if the remote certificate is locally available
				int trustLevel=store.getCertificateLevel(remoteCert);
				Vector certificates=(Vector)in.readObject();
				if(remoteCert==null)
				{
					if(certificates.size()<=0)
					{
						Logging.debug(getClass(), "Invalid certificate list.");
						throw new IOException("Invalid certificate list");
					}
					AbstractCertificate[] certChain=new AbstractCertificate[certificates.size()];
					for(int i=0;i<certChain.length;i++)
					{
						certChain[i]=store.createCertificate((byte[])certificates.get(i));
					}
					trustLevel=store.verifyCertificate(certChain);
					remoteCert=certChain[0];
				}
				//Compare fingerprint and SystemID
				byte[] remoteSystemBytes=remoteSystem.getBytes();
				byte[] fingerprint=remoteCert.getFingerprint();
				for(int i=0;i<remoteSystemBytes.length;i++)
				{
					if(remoteSystemBytes[i]!=fingerprint[i])
					{
						Logging.debug(getClass(), "Device system id differs from fingerprint.");
						throw new IOException("Device system id differs from fingerprint.");
					}
				}
				//Check the signature
				if(!remoteCert.verifySignature(bos.toByteArray(), signature))
				{
					Logging.debug(getClass(), "Asymmetric signature is invalid.");
					throw new IOException("Asymmetric signature is invalid.");
				}
				byte[] secret = exchange.getSharedSecret(remote, remoteCert);
				byte[] aes = new byte[16];
				byte[] hmac = new byte[16];
				for (int i = 0; i < 16; i++) {
					aes[i] = secret[i];
					hmac[i] = secret[i + 16];
				}
				store.addKey(invocation.getTarget().getSystem(), new HMACSymmetricKey(hmac), new AESSymmetricKey(aes), time, trustLevel);				
			} catch (IOException e) {
				c.release();
				throw e;
			}
		} catch (IOException e) {
			invocation.setException(new InvocationException(e.getMessage()));
		}
		
	}
		
	/**
	 * Sets the semantic manager.
	 */
	public void setSemanticManager(ISemanticManager manager) {
		this.manager = manager;
	};
	
	/**
	 * Stops the plug-in.
	 */
	public void start() {
		started = true;
	}
	
	/**
	 * Starts the plug-in.
	 */
	public void stop() {
		started = false;
	}

	/**
	 * Returns the plug-in description of the exchange semantic.
	 * 
	 * @return The description of the plug-in.
	 */
	public PluginDescription getPluginDescription() {
		return description;
	}
	
	/**
	 * Validates whether the plug-in can open a connection and respond to
	 * connection requests. This method throws an exception if the current
	 * state of the plug-in does not allow the initialization or a 
	 * connector.
	 */
	private void checkPlugin() {
		if (manager == null) throw new RuntimeException("Manager not set.");
		if (! started) throw new RuntimeException("Plugin not started.");
	}

}
