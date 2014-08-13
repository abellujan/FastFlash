/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\Abel\\Desktop\\Dev-Projects\\FastFlash\\src\\android\\os\\IXAService.aidl
 */
package android.os;
public interface IXAService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements android.os.IXAService
{
private static final java.lang.String DESCRIPTOR = "android.os.IXAService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an android.os.IXAService interface,
 * generating a proxy if needed.
 */
public static android.os.IXAService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof android.os.IXAService))) {
return ((android.os.IXAService)iin);
}
return new android.os.IXAService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_putImage:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
this.putImage(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getImages:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getImages();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_clearImages:
{
data.enforceInterface(DESCRIPTOR);
this.clearImages();
reply.writeNoException();
return true;
}
case TRANSACTION_isQueued:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isQueued();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setQueued:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setQueued(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getCamera:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getCamera();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_supported:
{
data.enforceInterface(DESCRIPTOR);
this.supported();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements android.os.IXAService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
* {@hide}
*/
@Override public void putImage(byte[] images) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(images);
mRemote.transact(Stub.TRANSACTION_putImage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.List getImages() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getImages, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void clearImages() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_clearImages, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isQueued() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isQueued, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setQueued(boolean tf) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((tf)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setQueued, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.util.List getCamera() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCamera, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void supported() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_supported, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_putImage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getImages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_clearImages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_isQueued = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_setQueued = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getCamera = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_supported = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
/**
* {@hide}
*/
public void putImage(byte[] images) throws android.os.RemoteException;
public java.util.List getImages() throws android.os.RemoteException;
public void clearImages() throws android.os.RemoteException;
public boolean isQueued() throws android.os.RemoteException;
public void setQueued(boolean tf) throws android.os.RemoteException;
public java.util.List getCamera() throws android.os.RemoteException;
public void supported() throws android.os.RemoteException;
}
