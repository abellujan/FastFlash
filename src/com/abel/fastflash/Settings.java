/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.io.DataOutputStream;
import java.io.IOException;
import com.abel.fastflash.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.XAServiceManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;



public class Settings extends Activity {

	static String[] args = {"setprop ctl.restart surfaceflinger","setprop ctl.restart zygote"};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(android.R.style.Theme_Holo);
		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
			getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment {
		@SuppressWarnings("deprecation")
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			
			Preference customResolution = (Preference) findPreference("custom_boolean");
			customResolution.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference p) {
					XAServiceManager manager = XAServiceManager.getService();
					manager.supported();
					Toast.makeText(getActivity(), "Check the Xposed Log for supported sizes!", Toast.LENGTH_LONG).show();
			        return true;
				}
			});
			
			/** RESET **/
	        Preference reset = (Preference) findPreference("reset");
			reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference p) {
					try {
						RunAsRoot(args);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return true;
				}
			});
		}
	}
	
	public static void RunAsRoot(String[] cmds) throws IOException{
		Process p = Runtime.getRuntime().exec("su");
		DataOutputStream dos = new DataOutputStream(p.getOutputStream());            
		for (String tmp : cmds) {
			dos.writeBytes(tmp + "\n");
		}
		dos.writeBytes("exit\n");
		dos.flush();
	}
}
