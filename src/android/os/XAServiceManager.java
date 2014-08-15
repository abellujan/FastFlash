package android.os;

import java.util.List;

//import android.os.ServiceManager;
/*
 * This is a wrapper for XAService that is accessible to all. 
 * Based on how all Android services work.
 */
public final class XAServiceManager {
	private static XAServiceManager oInstance;
    private IXAService mService;

    public static XAServiceManager getService() {
        if (oInstance == null) {
            oInstance = new XAServiceManager();
        }
        return oInstance;
    }

    private XAServiceManager() {
        mService = IXAService.Stub.asInterface(ServiceManager.getService("savepictures.service"));
    }

    @SuppressWarnings("unchecked")
	public List<byte[]> getImages() {
        try {
            return (List<byte[]>) mService.getImages();
        } catch (RemoteException e) { 
        	e.printStackTrace();
        }
        return null;
    }
	
	public void putImage(byte[] data) {
		try {
			mService.putImage(data);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void clearImages(){
		try {
			mService.clearImages();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void setQueued(boolean b) {
		try {
			mService.setQueued(b);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isQueued() {
		// TODO Auto-generated method stub
		try {
			return mService.isQueued();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void supported() {
		try {
			mService.supported();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isRunning() {
		// TODO Auto-generated method stub
		try {
			return mService.isRunning();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void setRunning(boolean b) {
		try {
			mService.setRunning(b);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
