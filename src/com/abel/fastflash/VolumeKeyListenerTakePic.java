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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.os.WorkSource;
import android.os.XAServiceManager;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XCallback;

@SuppressLint("Wakelock")
public class VolumeKeyListenerTakePic extends WorkSource  {
	private static boolean volumeDown, vibration, shutter, customBoolean, frontCam;
	static String customValues = "";
	static int height, width;
	private static boolean mIsLongPress = false;
	static List<byte[]> images = new ArrayList<byte[]>();
	static Camera cam = null;
	static Context mContext;
	static int focusMode, flashMode;
	static int restoreRinger = AudioManager.RINGER_MODE_NORMAL;
	public static final String PACKAGE_NAME = Settings.class.getPackage().getName();
	public static XSharedPreferences prefs;
	
	private static void log(String log) {
		XposedBridge.log("FastFlash: " + log);
	}
	private static void logE(Throwable e) {
		XposedBridge.log("FastFlash:" + e);
	}
	
	static void init() {
		updateVars();
//		log("Using settings:\nVolumeDown: " + volumeDown + "\nShutter :" + shutter + "\nFlashMode: " + flashMode + "\nFocusMode: " + focusMode + "\nVibration: " + vibration);
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
		VolumeKeyListenerTakePic.volumeDown = prefs.getBoolean("volume_down", false);
		VolumeKeyListenerTakePic.shutter = prefs.getBoolean("shutter", false);
		VolumeKeyListenerTakePic.flashMode = Integer.valueOf(prefs.getString("flash_options", "1"));
		VolumeKeyListenerTakePic.focusMode = Integer.valueOf(prefs.getString("focus_options", "1"));
		VolumeKeyListenerTakePic.frontCam = prefs.getBoolean("front_camera", false);
		VolumeKeyListenerTakePic.vibration = prefs.getBoolean("vibrate", false);
		VolumeKeyListenerTakePic.customBoolean = prefs.getBoolean("custom_boolean", false);
		if (customBoolean){
			VolumeKeyListenerTakePic.customValues = prefs.getString("custom_resolution", "123 1234");
			if (customValues.length() > 3){
				String[] parts = customValues.split(" ");
				if (parts[0] != null && parts[1] != null){
					height = Integer.valueOf(parts[0]);
					width = Integer.valueOf(parts[1]);
				} else {
					customBoolean = false;
					log("Invalid custom resolution values!!");
				}
			} else {
				log("Invalid custom resolution values!!");
			}
		}
	}

	private static XC_MethodHook handleInterceptKeyBeforeQueueing = new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
			Runnable mSendPictures = (Runnable) getAdditionalInstanceField(param.thisObject, "mSendPictures");
			PowerManager.WakeLock mBroadcastWakeLock = (WakeLock) getObjectField(param.thisObject, "mBroadcastWakeLock");
			mBroadcastWakeLock.acquire();
			final boolean isScreenOn = (Boolean) param.args[2];
			if (!isScreenOn) {
				mHandler.removeCallbacks(mSendPictures);
				final KeyEvent event = (KeyEvent) param.args[0];
				final int keyCode = event.getKeyCode();
				//Get context
				mContext = (Context) getObjectField(param.thisObject, "mContext");
				// update settings variables
				updateVars();
				if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN  && volumeDown) 
						|| (keyCode == KeyEvent.KEYCODE_VOLUME_UP && !volumeDown) || keyCode == KeyEvent.KEYCODE_CAMERA) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
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
			} else {
				//Send pictures thread
				mHandler.post(mSendPictures);
			}
			if (mBroadcastWakeLock != null && mBroadcastWakeLock.isHeld()){
				mBroadcastWakeLock.release();
			}
		}
	};

	private static XC_MethodHook handleConstructPhoneWindowManager = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
			Runnable mSendPictures = new Runnable() {
				public void run() {
					/** THE SYSTEM SERVICE CAN ONLY HANDLE SO MANY BYTES AT A TIME OR IT WILL CRASH **/
					/** by keeping image size the lowest it can go without sacrificing quality, around 1500x900 (WxH)
					 * 	we are able to send 2 images at a time, possibly 4 but at that point it becomes unstable
					 * **/
					while(true){
						XAServiceManager manager = XAServiceManager.getService();
						if (images.isEmpty()){
							break;
						}
						if(manager.isQueued()) {
							// do nothing, waiting to send images
						} else { // handle pictures
							manager.setQueued(true);
							List<byte[]> temp = new ArrayList<byte[]>();
							int i = 0;
							for(byte[] b : images){
								if (i > 1){ //we've handled enough images already, send rest into temp
									temp.add(b);
								} else {
									log("Sending picture to service");
									manager.putImage(b);
									i++;
								}
							}
							images.clear();
							images = temp;
						}
					}
				};
			};
			
			/** code to run on long press or other preferred button preference to take picture **/
			Runnable longPress = new Runnable() {
				@Override
				public void run() {
					mContext = (Context) getObjectField(param.thisObject, "mContext");
					
					mIsLongPress = true;
					
					//mute if necessary
					if (!shutter){
						AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
						restoreRinger = mgr.getRingerMode();
						mgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
					}
					/** Camera setup and takepicture method**/
					takePicture();
					
					//unmute if necessary
					if (!shutter){
						Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
						Runnable mUnmute = (Runnable) getAdditionalInstanceField(param.thisObject, "mUnmute");
						mHandler.postDelayed(mUnmute, 1000);
					}
				};
			};
			
			Runnable mUnmute = new Runnable() {
				@Override
				public void run() {
					mContext = (Context) getObjectField(param.thisObject, "mContext");
					
					if (mContext != null) {
						AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
						mgr.setRingerMode(restoreRinger);
					}
				};
			};
			
			setAdditionalInstanceField(param.thisObject, "longPress", longPress);
			setAdditionalInstanceField(param.thisObject, "mUnmute", mUnmute);
			setAdditionalInstanceField(param.thisObject, "mSendPictures", mSendPictures);
		}
	};
	
	private static void takePicture(){
		if (cam == null){
			cam = getCameraInstance();
			if (cam != null){	
				/** Set camera parameters **/
				Camera.Parameters parameters = cam.getParameters();
				//parameters don't usually exist for front cam
				if (!frontCam){
			        if (focusMode == 0) {
			        	parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					} else if (focusMode == 1) {
						parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
					}
			        
			        if (flashMode == 0){
			        	parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			        } else if (flashMode == 1){
			        	parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			        } else if (flashMode == 2){
			        	parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			        }
		        
			        parameters.setJpegQuality(100); //always use full quality
				}
		        
		        List<Size> supported = parameters.getSupportedPictureSizes();
		        Double h;
		        Double w;
		        Size use = null;
		        /** find the ratio closest to h/w = 0.6 **//** The perfect ratio for my phone (HTC One S) h:832, w:1456 **/
		        for(Size z : supported) {
		        	h = (double) z.height;
		        	w = (double) z.width;
		        	if (use == null){
			        	if (customBoolean){
			        		if (z.height == height && z.width == width){
			        			use = z;
			        		}
			        	} else {
				        	if (0.60 >= (h/w)  && (h/w) >= 0.55 && h < 870){
				        		use = z;
				        	}
			        	}
		        	}
		        }
		        if (use != null){
		        	log("Using picture size h:" + use.height + " w:" + use.width);
		 	        parameters.setPictureSize(use.width, use.height);
		        } else {
		        	log("No suitable size found, using default. Set a custom size or fix your custom setting");
		        }
		        cam.setParameters(parameters);
		        
		        /** Use dummy surface texture to allow the camera 
		        to take pictures without having a real view	**/
		        SurfaceTexture st = new SurfaceTexture(1);
		        try {
					cam.setPreviewTexture(st);
				} catch (IOException e) {
					logE(e);
				}
		        
		        /** Start dummy preview **/
		        cam.startPreview();
		        
		        /** wait until the dummy view is ready so we don't get a shitty picture **/
		        try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
		        /** Take picture! **/
		        log("taking pic.");
		        cam.takePicture(getShutterCallback(), null, getJpegCallback());
			} else {
				log("Failed to open camera");
			}
		}
	}
	
	public static PictureCallback getJpegCallback() {
		PictureCallback callback = new PictureCallback() {
			public void onPictureTaken(byte[] data, Camera camera) {
				//add picture to list
				images.add(data);
				
				cam.release();
				cam = null;
			};
		};
		return callback;
	}
	
	private  static ShutterCallback getShutterCallback(){
		ShutterCallback shutterCallback = new ShutterCallback() {
			public void onShutter() {
				if (mContext != null){
					if(vibration){
						Vibrator vbService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
						vbService.vibrate(150);
					}
				}
	        };
		};
		return shutterCallback;
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
	
	public static Camera getCameraInstance() {
		if (frontCam && Camera.getNumberOfCameras() > 1){
			return Camera.open(1);
		} else {
			return Camera.open();
		}
	}
}
