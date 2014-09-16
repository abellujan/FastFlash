package com.abel.fastflash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;

public class PictureVideoBroadcastReceiver extends BroadcastReceiver{
	public final String PACKAGE_NAME = Settings.class.getPackage().getName();
	int restoreRinger = AudioManager.RINGER_MODE_NORMAL;
	public String path;
	boolean vibration, customBoolean, frontCam, shutter, isVideo;
	boolean systemReady = false;
	boolean videoInProgress = false;
	boolean stop = false;
	String customValues = "";
	int height, width;
	Handler mHandler = null;
	Camera cam = null;
	Context mContext;
	int focusMode, flashMode;
	MediaRecorder recorder;
	public XSharedPreferences prefs;

	private void log(String log) {
		XposedBridge.log("FastFlash: " + log);
	}
	private void logE(Throwable e) {
		XposedBridge.log("FastFlash:" + e);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(Context context, Intent intent) {
		log("broadcast received");
		if (!systemReady){
			log("System not ready yet..");
			systemReady = intent.getBooleanExtra("systemReady", false);
			if (systemReady){
				log("System is now ready to receive picture requests!");
			}
			return;
		}
		mContext = context;
		log("1");
		updateVars();
		isVideo = intent.getBooleanExtra("video", false);
		stop = intent.getBooleanExtra("stop", false);
		log("2");
		if (cam == null && !videoInProgress && !stop){
			cam = getCameraInstance();
			log("0");
			if (cam != null){
				log("3");
				/** Set camera parameters **/
				Camera.Parameters parameters = cam.getParameters();
				//parameters don't usually exist for front cam
				if (!frontCam || isVideo){
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
				log("4");
		        /** Use dummy surface texture to allow the camera 
		        to take pictures without having a real view	**/
		        try {
					cam.setPreviewTexture(new SurfaceTexture(1));
				} catch (IOException e) {
					logE(e);
				}
		        log("5");
		        //mute if necessary
				if (!shutter){
					AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
					restoreRinger = mgr.getRingerMode();
					mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				}
				log("6");
				if (isVideo) {
					CamcorderProfile cp = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
					cam.setParameters(parameters);
					log("Video Recording started: " + System.currentTimeMillis());
					videoInProgress = true;
					Date date = new Date();
					String fname = date.getYear() + date.getMonth() + date.getDay() +date.getHours() + date.getMinutes() + System.currentTimeMillis() + ".mp4";
					/** Initialize mediarecorder and start recording **/
					recorder = new MediaRecorder();
					log("7");
					cam.unlock();
					recorder.setCamera(cam);
					
					recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
					
					recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		        	
					log("Using video size h:" + cp.videoFrameHeight + " w:" + cp.videoFrameWidth);
		 	        recorder.setVideoSize(cp.videoFrameWidth, cp.videoFrameHeight);
			        
		 	        recorder.setVideoFrameRate(cp.videoFrameRate);
			        recorder.setVideoEncodingBitRate(cp.videoBitRate);
			        recorder.setAudioEncodingBitRate(cp.audioBitRate);
			        recorder.setAudioSamplingRate(cp.audioSampleRate);
					
			        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
					recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
					
					recorder.setOutputFile(path + fname);
					recorder.setOrientationHint(90);
					log("8");
					try {
						recorder.prepare();
					} catch (IllegalStateException e) {
						XposedBridge.log(e);
					} catch (IOException e) {
						XposedBridge.log(e);
					}
					
					recorder.start();
					log("9");
				} else {
					List<Size> supported = parameters.getSupportedPictureSizes();
			        Double h, w;
			        Size usableSize = null;
			        /** find the ratio closest to h/w = 0.6 **/
			        for(Size z : supported) {
			        	h = (double) z.height;
			        	w = (double) z.width;
			        	if (usableSize == null){
				        	if (customBoolean){
				        		if (z.height == height && z.width == width){
				        			usableSize = z;
				        		}
				        	} else {
					        	if (0.60 >= (h/w)  && (h/w) >= 0.55/* && h < 870*/){
					        		usableSize = z;
					        	}
				        	}
			        	} else {
			        		break;
			        	}
			        }
			        if (usableSize != null){
			        	log("Using picture size h:" + usableSize.height + " w:" + usableSize.width);
			 	        parameters.setPictureSize(usableSize.width, usableSize.height);
			        } else {
			        	log("No suitable size found, using default.");
			        }
			        log("10");
					//force orientation for picture mode
			        parameters.setRotation(90);
			        
			        cam.setParameters(parameters);
					/** Start dummy preview **/
			        cam.startPreview();
			        
			        /** wait until the dummy view is "ready" so we don't get a bad/dark picture on some phones **/
			        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
			        
			        /** Take picture! **/
					cam.takePicture(getShutterCallback(), null, getJpegCallback());
					log("11");
				}
			} else {
				log("Failed to open camera");
			}
		} else {
			if (videoInProgress) {
				log("Video Recording stopped: " + System.currentTimeMillis());
				videoInProgress = false;
				mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + path)));
			}
			if (cam != null && recorder != null) {
				recorder.stop();
				recorder.release();
				recorder = null;
				cam.stopPreview();
				cam.release();
				cam = null;
			}
			log("12");
		}
		//unmute if necessary
		if (!shutter){
			if (mHandler == null) {
				mHandler = new Handler(Looper.getMainLooper());
			}
			Runnable mUnmute = new Runnable() {
				@Override
				public void run() {
					if (mContext != null) {
						AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
						mgr.setRingerMode(restoreRinger);
					}
				}
			};
			mHandler.postDelayed(mUnmute, 300);
		}
	}
	
	public PictureCallback getJpegCallback() {
		PictureCallback callback = new PictureCallback() {
			@SuppressWarnings("deprecation")
			public void onPictureTaken(byte[] data, Camera camera) {
				log("Saving...");
				cam.stopPreview();
				cam.release();
				cam = null;
				if (data == null){
					log("Something went wrong no image data.. :/");
					return;
				}

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
				
				Date date = new Date();
				String fname = date.getYear() + date.getMonth() + date.getDay() +date.getHours() + date.getMinutes() + System.currentTimeMillis() + ".jpeg";
				File file = new File(myDir, fname);
				/** save **/
				FileOutputStream out;
				try {
					out = new FileOutputStream(file);
					out.write(data);
					out.close();
				} catch (FileNotFoundException e){
					XposedBridge.log("File not found");
				} catch (IOException e) {
					XposedBridge.log("IOException");
				}
				log("Picture saved to: " + myDir  + "/" + fname);
				// Send broadcast to search for new pictures in gallery
				mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + path)));
			};
		};
		return callback;
	}
	
	public ShutterCallback getShutterCallback(){
		ShutterCallback shutterCallback = new ShutterCallback() {
			public void onShutter() {
				if (mContext != null && vibration){
					Vibrator vbService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
					vbService.vibrate(150);
				}
	        };
		};
		return shutterCallback;
	}
	
	public void updateVars() {
		prefs = new XSharedPreferences(PACKAGE_NAME);
    	if (prefs.getBoolean("custom_path", false)){
    		path = prefs.getString("custom_paths", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()) + "/FastFlash";
    	} else {
    		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/FastFlash";
    	}
		flashMode = Integer.valueOf(prefs.getString("flash_options", "1"));
		focusMode = Integer.valueOf(prefs.getString("focus_options", "1"));
		frontCam = prefs.getBoolean("front_camera", false);
		vibration = prefs.getBoolean("vibrate", false);
		shutter = prefs.getBoolean("shutter", false);
		customBoolean = prefs.getBoolean("custom_boolean", false);
		if (customBoolean){
			customValues = prefs.getString("custom_resolution", "123 1234");
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
	
	public  Camera getCameraInstance() {
		if (frontCam && Camera.getNumberOfCameras() > 1 && !isVideo) {
			return Camera.open(1);
		} else {
			return Camera.open();
		}
	}
}
