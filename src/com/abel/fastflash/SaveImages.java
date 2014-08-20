package com.abel.fastflash;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.XAServiceManager;

class SaveImages implements Runnable {
	public final String PACKAGE_NAME = Settings.class.getPackage().getName();
	public XSharedPreferences prefs;
	public String path;
	private Context mContext;
	private XAServiceManager manager;

	public SaveImages(Context context, XAServiceManager xaServiceManager) {
		this.mContext = context;
		this.manager = xaServiceManager;
	}

	private void log(String log) {
		XposedBridge.log("FastFlash: " + log);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		prefs = new XSharedPreferences(PACKAGE_NAME);
    	if (prefs.getBoolean("custom_path", false)){
    		path = prefs.getString("custom_paths", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()) + "/FastFlash";
    	} else {
    		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FastFlash";
    	}
    	while (true) {
			//mContext.getSystemService("savepictures.service");
			List<byte[]> images = manager.getImages();
			if (images == null){
				log("service returned null");
				manager.setRunning(false);
				break;
			} else if (images.isEmpty()){
				manager.setRunning(false);
				break;
			}
			
			// set unqueued so the server can send more pics to service while we save these images
			manager.clearImages();
			manager.setQueued(false);
			
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
	        		// Attempt to send broadcast to search for new pictures in gallery
					mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + path)));
				}
			}
    	}
	}
}
