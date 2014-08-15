package android.os;
interface IXAService {
/**
* {@hide}
*/
	void putImage(in byte[] images);
	List getImages();
	void clearImages();
	boolean isQueued();
	void setQueued(boolean tf);
	List getCamera();
	void supported();
	boolean isRunning();
	void setRunning(boolean tf);
}