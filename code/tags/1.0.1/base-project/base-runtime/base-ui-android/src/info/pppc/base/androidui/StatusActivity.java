package info.pppc.base.androidui;

import java.util.LinkedList;

import info.pppc.base.androidui.CoreService.CoreBinder;
import info.pppc.base.system.DeviceDescription;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.SystemID;
import info.pppc.base.system.event.Event;
import info.pppc.base.system.event.IListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The status activity enables a user to start and stop the middleware.
 * A preference screen can be used to configure which managed plug-ins 
 * shall be installed.
 * 
 * @author Mac
 */
public class StatusActivity extends Activity {
    
	/**
	 * The device list adapter is used to display the devices that are currently 
	 * connected.
	 * 
	 * @author Mac
	 */
	private class DeviceListAdapter extends ArrayAdapter<DeviceDescription> {
		
		/**
		 * The resource that points to individual views in the list.
		 */
		private int resource;
		
		/**
		 * Creates a new adapter for the context, resource and description set.
		 * 
		 * @param c The context of the adapter.
		 * @param resid The resource of the view elements.
		 * @param descs The descriptions.
		 */
		public DeviceListAdapter(Context c, int resid, DeviceDescription[] descs) {
			super(c, resid, descs);
			this.resource = resid;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        view = vi.inflate(resource, null);
			} else {
				view = convertView;
			}
			DeviceDescription d = getItem(position);
			TextView name = (TextView)view.findViewById(R.id.deviceName);
			if (name != null)
				name.setText(d.getName());
			TextView id = (TextView)view.findViewById(R.id.deviceIdentifier);
			if (id != null) {
				String s = d.getSystemID().toString();
				int l = s.length();
				id.setText(s.substring(0, l/2) + "\n" + s.substring(l/2 + 1, l));
			}
				
			return view;
		}
		
	}
	
	/**
	 * The service connection that is used to bind to the core service.
	 */
	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName name) {
			setCore(null);
		}
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			setCore(((CoreBinder)service).getCore());
		}
	};
	
	/**
	 * The device listener that updates the list upon changes.
	 */
	protected IListener deviceListener = new IListener() {
		public void handleEvent(Event event) {
			updateList();
		}
	};
	
	/**
	 * The core service reference which should always be set.
	 */
	protected CoreService core = null;
	
	/**
	 * The label that displays the core status.
	 */
	protected TextView brokerStatus;
	
	/**
	 * The label that displays the bluetooth status.
	 */
	protected TextView bluetoothStatus;
	
	/**
	 * The label that displays the wifi status.
	 */
	protected TextView wifiStatus;
	
	/**
	 * The list of connected devices.
	 */
	protected ListView deviceList;
	
	/**
	 * The handler used to run deferred ui actions.
	 */
	protected Handler handler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status);
        wifiStatus = (TextView)findViewById(R.id.wifiStatus);
        brokerStatus = (TextView)findViewById(R.id.brokerStatus);
        bluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        deviceList = (ListView)findViewById(R.id.deviceList);
    }
    
    @Override
    protected void onPause() {
    	unbindService(connection);    	
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	bindService(new Intent(this, CoreService.class), connection, BIND_AUTO_CREATE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inf = new MenuInflater(this);
    	inf.inflate(R.menu.status, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toggle:
				toggleService();
				break;
			case R.id.log:
				startActivity(new Intent(getApplicationContext(), LogActivity.class));
				break;
			case R.id.setting: 
				startActivity(new Intent(getApplicationContext(), SettingActivity.class));
				break;
			default:
		}
		return super.onOptionsItemSelected(item);
	}    
    /**
     * Called when the link to the core service has been established or removed.
     * 
     * @param core The core service.
     */
    private void setCore(CoreService core) {
    	if (this.core != null) {
    		InvocationBroker broker = core.getInvocationBroker();
    		if (broker != null) 
    			broker.getDeviceRegistry().removeDeviceListener(Event.EVENT_EVERYTHING, deviceListener);
    	}
    	this.core = core;
    	if (core != null) {
    		InvocationBroker broker = core.getInvocationBroker();
        	if (broker != null) 
        		broker.getDeviceRegistry().addDeviceListener(Event.EVENT_EVERYTHING, deviceListener);
    	}
    	updateList();
    	updateStatus();
    }
    
    /**
     * Updates the device list.
     */
    private void updateList() {
    	handler.post(new Runnable() {
			public void run() {
				if (deviceList != null) {
					if (core != null) {
						InvocationBroker broker = core.getInvocationBroker();
						if (broker != null) {
							SystemID[] ids = broker.getDeviceRegistry().getDevices();
							LinkedList<DeviceDescription> descriptions = new LinkedList<DeviceDescription>();
							for (int i = 0; i < ids.length; i++) {
								DeviceDescription d = broker.getDeviceRegistry().getDeviceDescription(ids[i]);
								if (d != null) descriptions.add(d);
							}
							deviceList.setAdapter(new DeviceListAdapter
									(getApplicationContext(), R.layout.status_device, descriptions.toArray
											(new DeviceDescription[descriptions.size()])));	
							return;
						}
					}
					deviceList.setAdapter(new DeviceListAdapter
							(getApplicationContext(), R.layout.status_device, new DeviceDescription[0]));					
				}
			}
		});
    }
    
    /**
     * Updates the status labels.
     */
    private void updateStatus() {
    	handler.post(new Runnable() {
			public void run() {
				if (core != null) {
		    		InvocationBroker broker = core.getInvocationBroker();
		        	if (brokerStatus != null)
		        		brokerStatus.setText((broker != null)?R.string.broker_started:R.string.broker_stopped);
		        	if (bluetoothStatus != null)
		        		bluetoothStatus.setText((core.isBluetoothInstalled())?R.string.plugin_installed:R.string.plugin_uninstalled);
		        	if (wifiStatus != null)
		        		wifiStatus.setText((core.isWifiInstalled())?R.string.plugin_installed:R.string.plugin_uninstalled);					
				} else {
					if (brokerStatus != null)
		        		brokerStatus.setText("");
		        	if (bluetoothStatus != null)
		        		bluetoothStatus.setText("");
		        	if (wifiStatus != null)
		        		wifiStatus.setText("");			
				}
			}
		});
    }
    
    /**
     * Enables or disables the service.
     */
    private void toggleService() {
    	if (core != null) {
    		Intent intent = new Intent(getApplicationContext(), CoreService.class);
    		InvocationBroker broker = core.getInvocationBroker();
    		if (broker != null) {
    			unbindService(connection);
    			stopService(intent);
    			bindService(new Intent(getApplicationContext(), CoreService.class), connection, BIND_AUTO_CREATE);
    		} else {
    			SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        		intent.putExtra(CoreService.EXTRA_INSTALL_BLUETOOTH, p.getBoolean("bluetooth", false));
        		intent.putExtra(CoreService.EXTRA_INSTALL_WIFI, p.getBoolean("wifi", false));
    			unbindService(connection);
        		startService(intent);
        		bindService(new Intent(getApplicationContext(), CoreService.class), connection, BIND_AUTO_CREATE);
    		}
    	}
    }
    
}