package info.pppc.base.androidui.util;

import info.pppc.base.system.util.Logging;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public class UUIDReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		BluetoothDevice deviceExtra = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
	    Parcelable[] uuidExtra = intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
	    Logging.debug(getClass(), intent.toString());
	    Bundle b = intent.getExtras();
	    for (String k : b.keySet()) {
	    	Logging.debug(getClass(), k);
	    }
	    //Parse the UUIDs and get the one you are interested i
	    
    	Logging.debug(getClass(), "Device is: " + deviceExtra);
	    for (int i = 0; i < uuidExtra.length; i++) {
	    	Logging.debug(getClass(), "Parcel is: " + uuidExtra[i]);
	    }
	}

}
