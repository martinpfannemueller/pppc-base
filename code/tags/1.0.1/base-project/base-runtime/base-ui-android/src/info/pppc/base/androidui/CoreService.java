package info.pppc.base.androidui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.DeviceRegistry;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import info.pppc.base.system.plugin.IPlugin;
import info.pppc.base.system.util.Logging;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.routing.ProactiveRoutingGateway;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.transceiver.MxBluetoothTransceiver;
import info.pppc.basex.plugin.transceiver.MxIPMulticastTransceiver;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * The core service executes the middleware in the background.
 * 
 * @author Mac
 */
public class CoreService extends Service {

	/**
	 * The log tag used to denote entries from the core service.
	 */
	public static final String LOG_TAG = "info.pppc.base";
	
	/**
	 * The boolean extra in the start intent that determines whether bluetooth shall be installed.
	 */
	public static final String EXTRA_INSTALL_BLUETOOTH = "info.pppc.base.bluetooth";
	
	/**
	 * The boolean extra in the start intent that determines whether wifi shall be installed. 
	 */
	public static final String EXTRA_INSTALL_WIFI = "info.pppc.base.wifi";
	
	/**
	 * The maximum number of lines contained in the message log.
	 */
	private static final int MAX_LOG_MESSAGES = 200;
	
	/**
	 * The local binder that enables activities
	 * to access the middleware functionality.
	 * 
	 * @author Mac
	 */
	public class CoreBinder extends Binder {
		/**
		 * Returns a reference to the service.
		 *  
		 * @return A reference to the service.
		 */
		public CoreService getCore() {
			return CoreService.this;
		}
	}
	
	/**
	 * The invocation broker.
	 */
	protected InvocationBroker invocationBroker;
	
	/**
	 * The message log that caches log entries.
	 */
	protected LinkedList<String> messageLog = new LinkedList<String>();
	
	/**
	 * The bluetooth receiver that enables and disables the bluetooth plugin depending
	 * on the state (on/off) of the bluetooth adapter.
	 */
	protected BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			MxBluetoothTransceiver bluetooth = (MxBluetoothTransceiver)getPlugin(MxBluetoothTransceiver.class);
			if (bluetooth == null) { return; };
			if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (state) {
					case BluetoothAdapter.STATE_ON:
						if (! bluetooth.isEnabled()) {
							Logging.log(getClass(), "Enabling bluetooth transceiver.");
							bluetooth.setEnabled(true);
							Logging.log(getClass(), "Enabling bluetooth transceiver (done).");
						}
							
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
					case BluetoothAdapter.STATE_OFF:
					case BluetoothAdapter.ERROR:
						if (bluetooth.isEnabled()) {
							Logging.log(getClass(), "Disabling bluetooth transceiver.");
							bluetooth.setEnabled(false);
							Logging.log(getClass(), "Disabling bluetooth transceiver (done).");
						}	
						break;
					default:
						// nothing to be done.
				}
			}
		}
	};
	
	/**
	 * The wifi receiver that enables and disables the ip transceiver depending
	 * on the wifi state (connected/disconnected).
	 */
	protected BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MxIPMulticastTransceiver transceiver = (MxIPMulticastTransceiver)getPlugin(MxIPMulticastTransceiver.class);
			if (transceiver == null) return;
			if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected() && ! transceiver.isEnabled()) {
					WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
					WifiInfo wifiInfo = wifi.getConnectionInfo();
					Logging.debug(getClass(), "Enabling wifi.");
					wifiLock.acquire();
					int ip = wifiInfo.getIpAddress();
					transceiver.setAddress(new byte[] { (byte)(ip & 0xff), (byte)(ip >> 8 & 0xff), (byte)(ip >> 16 & 0xff), (byte)(ip >> 24 & 0xff) });
					transceiver.setEnabled(true);
					Logging.debug(getClass(), "Enabling wifi (done).");
				} 
				if (!networkInfo.isConnected() && transceiver.isEnabled()) {
					Logging.debug(getClass(), "Disabling wifi.");
					wifiLock.release();
					transceiver.setEnabled(false);
					Logging.debug(getClass(), "Disabling wifi (done).");
				}
			} else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				switch (state) {
					case WifiManager.WIFI_STATE_DISABLED:
					case WifiManager.WIFI_STATE_DISABLING:
						if (transceiver.isEnabled()) {
							Logging.debug(getClass(), "Disabling wifi.");
							wifiLock.release();
							transceiver.setEnabled(false);
							Logging.debug(getClass(), "Disabling wifi (done).");
						}
						break;
					default:
				}
			}
		}
	};
	
	/**
	 * The multicast lock that is acquired and held when the wifi plugin
	 * is enabled to allow the reception of multicast packets.
	 */
	protected WifiManager.MulticastLock wifiLock;
	
	/**
	 * A flag to determine whether bluetooth is installed.
	 */
	protected boolean bluetoothInstalled = false;
	
	/**
	 * A flag to determine whether wifi is installed.
	 */
	protected boolean wifiInstalled = false;
	
	/**
	 * Creates a new core service.
	 */
	public CoreService() { }

	/**
	 * Returns a binder to the core service.
	 * 
	 * @param arg0 The intent.
	 * @return The binder to the service.	 
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return new CoreBinder();
	}
	
	/**
	 * Returns a reference to the invocation broker.
	 * 
	 * @return A reference to the broker.
	 */
	public InvocationBroker getInvocationBroker() {
		return invocationBroker;
	}

	/**
	 * Returns the messages contained in the message log.
	 * 
	 * @return The messages contained in the log.
	 */
	public List<String> getMessageLog() {
		return new LinkedList<String>(messageLog);
	}
	
	/**
	 * Called if a start service intent is received. If the broker is already
	 * running, this method does nothing. If the broker is not running, the method
	 * creates a new broker depending on the intent extras, it also installs a
	 * bluetooth and/or an ip plug-in.
	 * 
	 * @param intent The intent.
	 * @param flags The flags.
	 * @param startId The id.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int i = super.onStartCommand(intent, flags, startId);
		if (invocationBroker == null) {
			System.setProperty("info.pppc.name", android.os.Build.DEVICE);
			System.setProperty("info.pppc.type", Short.toString(DeviceDescription.TYPE_PHONE));
			// redirect logging to ddms
			Logging.setOutput(new PrintStream(new OutputStream() {
				@Override
				public void write(int oneByte) throws IOException { }
			}) {
				@Override
				public synchronized void print(String str) {
					Log.d(LOG_TAG, str);
					messageLog.add(str);
					if (messageLog.size() > MAX_LOG_MESSAGES) {
						messageLog.removeFirst();
					}
				}
			});
			// TODO: initialize the key store here
			// create invocation broker
			invocationBroker = InvocationBroker.getInstance();
			// add debug output for device detection
			invocationBroker.getDeviceRegistry().addDeviceListener(Event.EVENT_EVERYTHING, new IListener() {
				public void handleEvent(Event event) {
					switch (event.getType()) {
						case DeviceRegistry.EVENT_DEVICE_ADDED:
							Logging.log(getClass(), "Device added: " + event.getData());
							break;
						case DeviceRegistry.EVENT_DEVICE_REMOVED:
							Logging.log(getClass(), "Device removed: " + event.getData());
							break;
						default:
					}
				}
			});
			// install bluetooth if requested
			if (intent.getBooleanExtra(EXTRA_INSTALL_BLUETOOTH, false)) {
				installBluetooth();
			}
			// install wifi if requested
			if (intent.getBooleanExtra(EXTRA_INSTALL_WIFI, false)) {
				installWifi();
			}
			// TODO: add further plug-ins here
			invocationBroker.getPluginManager().addPlugin(new ProactiveDiscovery());
			invocationBroker.getPluginManager().addPlugin(new ProactiveRoutingGateway(false));
			invocationBroker.getPluginManager().addPlugin(new RmiSemantic());	
			// TODO: initialize peces here
		}
		return i;
	}
	
	/**
	 * Installs a bluetooth plug-in if there has not been one installed 
	 * already.
	 */
	public void installBluetooth() {
		if (! bluetoothInstalled) {
			//  register bluetooth plug-in and manager (to detect on/off events)
			invocationBroker.getPluginManager().addPlugin(new MxBluetoothTransceiver());
			registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
			bluetoothInstalled = true;
		}
	}

	/**
	 * Removes a previously installed bluetooth plug-in. If there is none,
	 * this method does nothing.
	 */
	public void uninstallBluetooth() {
		if (bluetoothInstalled) {
			MxBluetoothTransceiver transceiver = (MxBluetoothTransceiver)getPlugin(MxBluetoothTransceiver.class);
			if (transceiver != null) {
				transceiver.setEnabled(false);
				invocationBroker.getPluginManager().removePlugin(transceiver);
			}
			unregisterReceiver(bluetoothReceiver);
			bluetoothInstalled = false;
		}
		
	}
	
	/**
	 * Determines whether a bluetooth plug-in has been installed.
	 * 
	 * @return True if a plug-in has been installed, false otherwise.
	 */
	public boolean isBluetoothInstalled() {
		return bluetoothInstalled;
	}
	
	/**
	 * Installs an ip plugin on top of the wifi adapter, if this has not been done
	 * so far.
	 */
	public void installWifi() {
		if (! wifiInstalled) {
			WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifi.getConnectionInfo();
			wifiLock = wifi.createMulticastLock(LOG_TAG);
			ConnectivityManager connectivity = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			MxIPMulticastTransceiver transceiver = new MxIPMulticastTransceiver();
			if (networkInfo == null || ! networkInfo.isConnected()) {
				Logging.debug(getClass(), "Disabling wifi upon install.");
				invocationBroker.getPluginManager().addPlugin(transceiver);
				transceiver.setEnabled(false);
			} else {
				Logging.debug(getClass(), "Enabling wifi upon install.");
				int ip = wifiInfo.getIpAddress();
				wifiLock.acquire();
				transceiver.setAddress(new byte[] { (byte)(ip & 0xff), (byte)(ip >> 8 & 0xff), (byte)(ip >> 16 & 0xff), (byte)(ip >> 24 & 0xff) });
				invocationBroker.getPluginManager().addPlugin(transceiver);
			}
			registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
			registerReceiver(wifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
			wifiInstalled = true;
		}
	}
	
	/**
	 * Removes a previously installed wifi ip plug-in. If there is none,
	 * the method simply does nothing.
	 */
	public void uninstallWifi() {
		if (wifiInstalled) {
			MxIPMulticastTransceiver transceiver = (MxIPMulticastTransceiver)getPlugin(MxIPMulticastTransceiver.class);
			if (transceiver != null) {
				transceiver.setEnabled(false);
				invocationBroker.getPluginManager().removePlugin(transceiver);
			}
			unregisterReceiver(wifiReceiver);
			wifiInstalled = false;
		}
		
	}

	/**
	 * Enables the user to determine whether a wifi plug-in for ip is installed.
	 * 
	 * @return True if wifi is installed, false otherwise.
	 */
	public boolean isWifiInstalled() {
		return wifiInstalled;
	}
	
	/**
	 * Returns a installed plug-in with the specified class
	 * or null if it does not exist.
	 * 
	 * @param clazz The class to find.
	 * @return The plug-in or null.
	 */
	private IPlugin getPlugin(@SuppressWarnings("rawtypes") Class clazz) {
		if (invocationBroker != null) { 
			IPlugin[] plugins = invocationBroker.getPluginManager().getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				if (plugins[i].getClass() == clazz) {
					return plugins[i];
				}
			}
		}
		return null; 
	}
	
	/**
	 * Called when the service is destroyed. This method
	 * shuts down the broker, if it is running.
	 */
	@Override
	public synchronized void onDestroy() {
		if (invocationBroker != null) {
			uninstallWifi();
			uninstallBluetooth();
			invocationBroker.shutdown();
		}
		invocationBroker = null;
		super.onDestroy();
	}

}
