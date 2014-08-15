/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.XAServiceManager;
import android.widget.TextView;
import com.android.server.XAService;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		XAService.inject();
		VolumeKeyListenerTakePic.init();
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;
        
        /** Since the updateClock method runs right when the screen turns on and also has rw permission 
         * and we have no rw permission in the PhoneWindowManager or even in the custom system service
         * we save images right when the screen turns on, although it may cause some delay it's rather safe
         * **/
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
			@Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				/**
				 * Need to find a new way to implement this, the thread that I'm creating is falling into the same queue as the keyguard
				 * and entirely new threads are hanging. As a result this implementation hangs at the lockscreen until the pictures are done saving.
				 * It's noticeably slow after about 5 images.
				 */
				XAServiceManager manager = XAServiceManager.getService();
				List<byte[]> images = manager.getImages();
				Context c = ((TextView) param.thisObject).getContext();
				SaveImages s = new SaveImages(c, manager);
				if (images != null && !images.isEmpty() && !manager.isRunning() && !s.isAlive()){
	            	Handler h = new Handler(c.getMainLooper());
	            	h.post(s);
				}
				images = null;
            }
        });
	}
}