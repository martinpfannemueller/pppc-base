/**
 * 
 */
package info.pppc.base.androidui;

import java.util.LinkedList;
import java.util.List;

import info.pppc.base.androidui.CoreService.CoreBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * The log activity displays the log entries.
 * 
 * @author Mac
 */
public class LogActivity extends Activity {

	/**
	 * The text view that contains the log entries.
	 */
	protected TextView logStatus;
	
	/**
	 * The handler for asynchronous updates.
	 */
	protected Handler handler = new Handler();
	
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
	 * The core that holds the actual log.
	 */
	protected CoreService core = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);
		logStatus = (TextView)findViewById(R.id.logStatus);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inf = new MenuInflater(this);
		inf.inflate(R.menu.log, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				updateStatus();
				break;
			default:
		}
		return super.onOptionsItemSelected(item);
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
	
    /**
     * Called whenever the core reference changes.
     * 
     * @param core The reference to the core.
     */
	protected void setCore(CoreService core) {
		this.core = core;
		updateStatus();
	}
	
	/**
	 * Updates the log.
	 */
	protected void updateStatus() {
		handler.post(new Runnable() {
			public void run() {
				List<String> log = new LinkedList<String>();
				if (core != null) {
					log = core.getMessageLog();
				}
				if (logStatus != null) {
					StringBuffer b = new StringBuffer();
					for (String s : log) {
						b.append(s);
					}
					logStatus.setText(b.toString());
				}
			}
		});
	}
	
	
}
