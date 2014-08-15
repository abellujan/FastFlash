package com.android.server;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.RemoteException;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.os.IXAService;

public class XAService extends IXAService.Stub {
	@SuppressWarnings("unused")
	private Context mContext;
	private static XAService oInstance;
	final private static List<byte[]> images = new ArrayList<byte[]>();
	boolean queued = false;
	boolean running = false;
	
	public XAService(Context context) {
		mContext = context;
	}
	
	private void systemReady() {
		// Make your initialization here
		XposedBridge.log("Custom system service initialized");
	}
	
	public static void inject() {
		final Class<?> ActivityManagerServiceClazz = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", null);
		
		XposedBridge.hookAllMethods(ActivityManagerServiceClazz, "main", new XC_MethodHook() {
		@Override
		protected final void afterHookedMethod(final MethodHookParam param) {
				Context context = (Context) param.getResult();
				oInstance = new XAService(context);
				Class<?> ServiceManagerClazz = XposedHelpers.findClass("android.os.ServiceManager", null);
				XposedHelpers.callStaticMethod(ServiceManagerClazz, "addService", "savepictures.service", oInstance);
			}
		});
		
		XposedBridge.hookAllMethods(ActivityManagerServiceClazz, "systemReady", new XC_MethodHook() {
		@Override
		protected final void afterHookedMethod(
				final MethodHookParam param) {
				oInstance.systemReady();
			}
		});
	}

	@Override
	public List<byte[]> getImages() throws RemoteException {
		return images;
	}

	@Override
	public void clearImages() throws RemoteException {
		images.clear();
	}

	@Override
	public void putImage(byte[] data) throws RemoteException {
		images.add(data);
	}

	@Override
	public boolean isQueued() throws RemoteException {
		return queued;
		
	}

	@Override
	public void setQueued(boolean tf) throws RemoteException {
		queued = tf;
	}

	@Override
	public List<Camera> getCamera() throws RemoteException {
		List<Camera> rtn = new ArrayList<Camera>();
		rtn.add(Camera.open());
		return rtn;
	}

	@Override
	public void supported() throws RemoteException {
		Camera cam = Camera.open();
		Camera.Parameters parameters = cam.getParameters();
		List<Size> supported = parameters.getSupportedPictureSizes();
        Double h;
        Double w;
        XposedBridge.log("Supported picture sizes:"); 
        for(Size z : supported) {
        	h = (double) z.height;
        	w = (double) z.width;
			XposedBridge.log("Height: " + z.height + " Width: " + z.width + " Ratio: " + (h/w));
        }
        cam.release();
        cam = null;
	}

	@Override
	public boolean isRunning() throws RemoteException {
		return running;
	}

	@Override
	public void setRunning(boolean tf) throws RemoteException {
		running = tf;
	}
}