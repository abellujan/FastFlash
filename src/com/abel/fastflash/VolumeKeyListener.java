/**
 * Copyright 2014 Abel Lujan
 * 
 * Heavily based off of rovo89's Xposed Tweakbox mod for skipping tracks with volume keys, thanks a lot for
 * the code and the Xposed Framework itself!
 */

package com.abel.fastflash;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XCallback;

@SuppressLint("Wakelock")
public class VolumeKeyListener {
	public static final String PACKAGE_NAME = Settings.class.getPackage().getName();
	public static XSharedPreferences prefs;
	private static boolean volumeDown, vibration;
	private static boolean mIsLongPress = false;
	private static boolean video = false;
	private static Context mContext;
	
	private static void log(String log) {
		XposedBridge.log("FastFlash: " + log);
	}
	private static void logE(Throwable e) {
		XposedBridge.log("FastFlash:" + e);
	}
	
	static void init() {
		updateVars();
		try {
			Class<?> classPhoneWindowManager = findClass("com.android.internal.policy.impl.PhoneWindowManager", null);
			XposedBridge.hookAllConstructors(classPhoneWindowManager, handleConstructPhoneWindowManager);
			findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, boolean.class, handleInterceptKeyBeforeQueueing);
		} catch (Exception e) {
			logE(e);
		}
	}
	
	private static void updateVars() {
		prefs = new XSharedPreferences(PACKAGE_NAME);
		VolumeKeyListener.volumeDown = prefs.getBoolean("volume_down", false);
		vibration = prefs.getBoolean("vibrate", false);
	}
	
	private static XC_MethodHook handleInterceptKeyBeforeQueueing = new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			PowerManager.WakeLock mBroadcastWakeLock = (WakeLock) getObjectField(param.thisObject, "mBroadcastWakeLock");
			mBroadcastWakeLock.acquire();
			final boolean isScreenOn = (Boolean) param.args[2];
			if (!isScreenOn) {
				final KeyEvent event = (KeyEvent) param.args[0];
				final int keyCode = event.getKeyCode();
				//Get context
				mContext = (Context) getObjectField(param.thisObject, "mContext");
				// update settings variables
				updateVars();
				//While testing on an LG optimus f6, I was getting keycode 225 instead of 27 for the camera button so I added that check as well
				if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
						|| keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == 225) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN  && !volumeDown || (keyCode == KeyEvent.KEYCODE_VOLUME_UP && volumeDown))){
							video = true;
						} else {
							video = false;
						}
						mIsLongPress = false;
						handleVolumeLongPress(param.thisObject);
						param.setResult(0);
						return;
					} else {	
						handleVolumeLongPressAbort(param.thisObject);
						if (mIsLongPress) {
							param.setResult(0);
							return;
						}
						// send an additional "key down" because the first one was eaten
						Object[] newArgs = new Object[3];
						newArgs[0] = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
						newArgs[1] = param.args[1];
						newArgs[2] = param.args[2];
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, newArgs);
					}
				}
			}
			Intent intent = new Intent("com.abel.fastflash.PictureVideoBroadcastReceiver", null);
	        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
	        /** stop video if video is still in progress when screen turned on **/
	        intent.putExtra("stop", true);
	        if (mContext != null) {
	        	mContext.sendOrderedBroadcast(intent, null);
	        }
			
			if (mBroadcastWakeLock != null && mBroadcastWakeLock.isHeld()){
				mBroadcastWakeLock.release();
			}
		}
	};

	private static XC_MethodHook handleConstructPhoneWindowManager = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			/** code to run on long press or other preferred button preference to take picture **/
			Runnable longPress = new Runnable() {
				@Override
				public void run() {
					mContext = (Context) getObjectField(param.thisObject, "mContext");
					mIsLongPress = true;
					if (vibration){ //so the user knows to let go of their volume button, trying to save hardware from getting destroyed
						Vibrator vbService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
						vbService.vibrate(150);
					}
					/** Send broadcast to handle camera event! **/
					sendBroadcast();
				};
			};
			setAdditionalInstanceField(param.thisObject, "longPress", longPress);
		}
	};
	
	private static void sendBroadcast(){
		log("sending broadcast");
		Intent intent = new Intent("com.abel.fastflash.PictureVideoBroadcastReceiver", null);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("video", video);
//        intent.putParcelableArrayListExtra("arrayList", value)
        mContext.sendOrderedBroadcast(intent, null);
	}
	
	private static void handleVolumeLongPress(Object phoneWindowManager) {
		Handler mHandler = (Handler) getObjectField(phoneWindowManager, "mHandler");
		Runnable longPress = (Runnable) getAdditionalInstanceField(phoneWindowManager, "longPress");
		// just in case we encounter a double longpress bug we kill the first one to keep from crashing on camera code
		mHandler.removeCallbacks(longPress);
		mHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
	}

	private static void handleVolumeLongPressAbort(Object phoneWindowManager) {
		Handler mHandler = (Handler) getObjectField(phoneWindowManager, "mHandler");
		Runnable longPress = (Runnable) getAdditionalInstanceField(phoneWindowManager, "longPress");
		mHandler.removeCallbacks(longPress);
	}
}
