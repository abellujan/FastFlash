/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.XAServiceManager;
import android.widget.TextView;
import com.android.server.XAService;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	public final String PACKAGE_NAME = Settings.class.getPackage().getName();
	private XAServiceManager manager;
	protected Handler mHandler = null;
	@SuppressWarnings("unused")
	private static void log(String log) {
		XposedBridge.log("FastFlash: " + log);
	}
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		XAService.inject();
		VolumeKeyListenerTakePic.init();
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        Class<?> Clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
        /** Since the updateClock method runs right when the screen turns on and also has rw permission 
         * and we have no rw permission in the PhoneWindowManager or even in my custom system service
         * we save images right when the screen turns on, although it may cause some delay it's rather safe
         * **/
        XposedHelpers.findAndHookMethod(Clock, "updateClock", new XC_MethodHook() {
        	/**
			 * Need to find a new way to implement this, the thread that I'm creating is falling into the same queue as the keyguard
			 * and entirely new threads are hanging. As a result this implementation hangs at the lockscreen until the pictures are done saving.
			 * It's noticeably slow after about 5 images. :/
			 */
        	protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				manager = XAServiceManager.getService();
				List<byte[]> images = manager.getImages();
				TextView tv = (TextView) param.thisObject;
				if (images != null && !images.isEmpty() && !manager.isRunning()){
					SaveImages s = new SaveImages(tv.getContext(), manager);
					manager.setRunning(true);
					if(mHandler == null){
						mHandler = new Handler(Looper.getMainLooper());
					}
					mHandler.post(s);
				}
				images = null;
        	}
        });
	}
}