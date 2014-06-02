/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /root/Android_Application_source/origin training suite/Training Suites/src/com/cnu/eslab/suite/ITrainingService.aidl
 */
package com.cnu.eslab.suite;
public interface ITrainingService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.cnu.eslab.suite.ITrainingService
{
private static final java.lang.String DESCRIPTOR = "com.cnu.eslab.suite.ITrainingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.cnu.eslab.suite.ITrainingService interface,
 * generating a proxy if needed.
 */
public static com.cnu.eslab.suite.ITrainingService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.cnu.eslab.suite.ITrainingService))) {
return ((com.cnu.eslab.suite.ITrainingService)iin);
}
return new com.cnu.eslab.suite.ITrainingService.Stub.Proxy(obj);
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
case TRANSACTION_setServiceMode:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setServiceMode(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.cnu.eslab.suite.ITrainingService
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
@Override public void setServiceMode(int mode) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(mode);
mRemote.transact(Stub.TRANSACTION_setServiceMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_setServiceMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void setServiceMode(int mode) throws android.os.RemoteException;
}
