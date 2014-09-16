/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		VolumeKeyListener.init();
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
            return;
		Class<?> powerUI = XposedHelpers.findClass(/*"com.android.systemui.statusbar.policy.Clock"*/"com.android.systemui.power.PowerUI", lpparam.classLoader);
		Class<?> bootReceiver = XposedHelpers.findClass("com.android.systemui.BootReceiver", lpparam.classLoader);
		Method onReceive = XposedHelpers.findMethodBestMatch(bootReceiver, "onReceive", Context.class, Intent.class);
        /** We can really hook just about any constructor here, so long as the app has read write permission
         * all I'm doing in injecting a broadcast receiver into the constructor in order to receive my intent from the 
         * PhoneWindowManager which has no access to the sd card or internal storage in the camera callback.
         * I'm still not positive how much affect this broadcast receiver has on battery life since it's never unregistered, 
         * I need to find a more dynamic way of getting the broadcast receiver code to run while phone is asleep.
         */
        XposedBridge.hookAllMethods(powerUI, "start", new XC_MethodHook() {
        	@Override
        	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        		Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        		Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
    			IntentFilter filter = new IntentFilter("com.abel.fastflash.PictureVideoBroadcastReceiver");
    			PictureVideoBroadcastReceiver receiver = new PictureVideoBroadcastReceiver();
    			filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
    			try {
    				mContext.registerReceiver(receiver, filter, null, mHandler);
    				XposedBridge.log("receiver registered");
    			} catch (IllegalArgumentException e){}
        	}
		});
        
        XposedBridge.hookMethod(onReceive, new XC_MethodHook(){
        	@Override
        	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        		Context context = (Context) param.args[0];
        		XposedBridge.log("boot completed");
        		Intent intent = new Intent("com.abel.fastflash.PictureVideoBroadcastReceiver", null);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.putExtra("systemReady", true);
                context.sendOrderedBroadcast(intent, null);
        	}
        });
	}
}