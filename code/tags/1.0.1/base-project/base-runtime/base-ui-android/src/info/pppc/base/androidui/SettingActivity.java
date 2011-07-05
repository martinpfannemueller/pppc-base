package info.pppc.base.androidui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * The settings activity enables the user to configure the
 * plug-ins that shall be installed.
 * 
 * @author Mac
 */
public class SettingActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting);
	}
	
}
