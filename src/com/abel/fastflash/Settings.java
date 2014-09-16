/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.abel.fastflash.R;


import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;



public class Settings extends PreferenceActivity {

	static String[] args = {"setprop ctl.restart surfaceflinger","setprop ctl.restart zygote"};
	
	@Override
	protected boolean isValidFragment(String fragmentName) {
	    return PrefsFragment.class.getName().equals(fragmentName);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(android.R.style.Theme_Holo);
		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
			getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        
        /*PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        root.addPreference(getListPreference());*/
	}
	
	public static class PrefsFragment extends PreferenceFragment {
		@SuppressWarnings("deprecation")
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			
			/** Change to display supported values **/
			Preference customResolution = (Preference) findPreference("custom_boolean");
			customResolution.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference p) {
					Camera cam = Camera.open();
					Camera.Parameters parameters = cam.getParameters();
					List<Size> supported = parameters.getSupportedPictureSizes();
			        Double h;
			        Double w;
			        for(Size z : supported) {
			        	h = (double) z.height;
			        	w = (double) z.width;
			        	Toast.makeText(getActivity(), "H: " + h + " W: " + w, Toast.LENGTH_SHORT).show();
			        }
			        cam.release();
			        cam = null;
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
	
	public ListPreference getListPreference(){
		ListPreference lp = new ListPreference(getApplication().getApplicationContext());
		return lp;
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
