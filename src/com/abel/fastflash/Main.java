/**
 * Copyright 2014 Abel Lujan
 */

package com.abel.fastflash;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.XAServiceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.server.XAService;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	public static final String PACKAGE_NAME = Settings.class.getPackage().getName();
	public static XSharedPreferences prefs;
	public static String path;
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
        
        /** Since the updateClock method runs right when the screen turns on and also has rw permission 
         * and we have no rw permission in the PhoneWindowManager or even in the custom system service
         * we save images right when the screen turns on, although it may cause some delay it's rather safe
         * **/
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
            @SuppressWarnings("deprecation")
			@Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            	prefs = new XSharedPreferences(PACKAGE_NAME);
            	if (prefs.getBoolean("custom_path", false)){
            		path = prefs.getString("custom_paths", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()) + "/FastFlash";
            	} else {
            		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FastFlash";
            	}
            	TextView tv = (TextView) param.thisObject;
            	XAServiceManager manager = XAServiceManager.getService();
            	while(true){
					List<byte[]> images = manager.getImages();
					if (images == null){
						log("service returned null");
						break;
					} else if (images.isEmpty()){
						break;
					}
					// set unqueued so the server can send more pics to service while we save these images
					manager.clearImages();
					manager.setQueued(false);
					
					Toast.makeText(tv.getContext(), "Saving Images", Toast.LENGTH_SHORT).show();
					log("# of pics retrieved: " + images.size());
					File myDir = null;
					if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
						myDir = new File(path);
		        		if (!myDir.exists())
		        			myDir.mkdirs();
					} else { //use alternate storage?
						log("Something is likely wrong!!");
						myDir = new File(path);
		        		if (!myDir.exists())
		        			myDir.mkdirs();
					}
					for (byte[] b : images){
						if (b != null && b.length > 0){
							//Flip image manually because camera settings weren't making a difference
							Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
							Matrix matrix = new Matrix();
							matrix.postRotate(90);
							Bitmap newB = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							newB.compress(Bitmap.CompressFormat.JPEG, 100, stream);
							b = stream.toByteArray();
			        		
							Date date = new Date();
			        		String fname = date.getYear() + date.getMonth() + date.getDay() +date.getHours() + date.getMinutes() + System.currentTimeMillis() + ".jpeg";
			        		File file = new File(myDir, fname);
			        		/** save **/
			        		FileOutputStream out;
			        		try {
			        			out = new FileOutputStream(file);
			        			out.write(b);
			        			out.close();
			        		} catch (FileNotFoundException e){
			        			XposedBridge.log(e);
			        		} catch (IOException e) {
			        			XposedBridge.log(e);
			        		}
			        		XposedBridge.log("Picture saved to: " + myDir  + "/" + fname);
						}
					}
        		}
				// Attempt to send broadcast to search for new pictures in gallery
				Context c = tv.getContext();
				c.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	        }
        });
	}
}