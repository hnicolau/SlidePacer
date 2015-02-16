package android.hugonicolau.androidbluetoothlibrary.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public abstract class BluetoothClient {
	private final String TAG = "BluetoothClient";
	
	private BluetoothAdapter mAdapter;
	private ConnectThread mConnThread = null;
	private ConnectedThread mConnectedThread = null;
	private MessageSender mSender = null;
	private BluetoothSocket mHostSocket = null;
	
	// handler used to send messages and prevent blocking
	private Handler mHandler = new Handler();
	
	protected abstract void onConnected(BluetoothDevice remoteDevice);
	protected abstract void onDisconnected(BluetoothDevice remoteDevice);
	
	public BluetoothClient()
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();		
	}
	
	public BluetoothAdapter getAdapter()
	{
		return mAdapter;
	}
	
	public boolean isConnected()
	{
		return mConnectedThread != null;
	}
	
	public void disconnect()
	{
		connectionLost();
	}
	
	public void connect(String address, String uuid)
	{
		BluetoothDevice dev = mAdapter.getRemoteDevice(address);
        mConnThread = new ConnectThread(dev, uuid);
        mConnThread.run();
	}
	
	public void sendMessage(String message) {
		mSender.sendMessage(message);
	}
	
	private void connectionLost() {
		//if(mSender != null) mSender.sendMessage("\r");
    	mSender = null;
    	if(mConnectedThread != null) mConnectedThread.cancel();
    	else if(mConnThread != null) mConnThread.cancel();
    	mConnThread = null; 
    	mConnectedThread = null;
    	
    	mHandler.post(new Runnable() {

			@Override
			public void run() {
				onDisconnected(mHostSocket.getRemoteDevice());
			}
		});
    }
	
	private class ConnectThread extends Thread {
        private BluetoothSocket mHostSocket = null;
        //private BluetoothDevice mHostDevice = null;

        public ConnectThread(BluetoothDevice device, String uuid) {
            Log.e(TAG,"ConnectThread start....");
            // Cancel discovery because it will slow down the connection
            mAdapter.cancelDiscovery();
            
            //mHostDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // this seems to work ...
            	mHostSocket = device.createInsecureRfcommSocketToServiceRecord(
            			UUID.fromString(uuid));
            } catch (Exception e) {
                Log.d(TAG,"Error in connecting: " + e.getMessage());
            }
        }

        public void run() {
           
            Log.d(TAG,"started connection, thus stopping discovery");

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(TAG,"connecting ...");

                mHostSocket.connect();
            } catch (IOException connectException) {

                Log.d(TAG,"Error failed to connect: " + connectException.getMessage());

                // Unable to connect; close the socket and get out
                try {
                    Log.d(TAG,"close the socket and exit");

                    mHostSocket.close();
                } catch (IOException closeException) {
                    Log.d(TAG,"Error failed to close the socket: " + closeException.getMessage());

                }
                Log.d(TAG,"returning ...");

                return;
            }

            Log.d(TAG,"we can now manage our connection!");

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mHostSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mHostSocket.close();
                //Message msg = handle.obtainMessage(READY_TO_CONN);
                //handle.sendMessage(msg);
            } catch (IOException e) {
            	Log.d(TAG, "Error canceling ConnectThread: " + e.getMessage());
            }
        }
    }
	
	public void manageConnectedSocket(final BluetoothSocket socket) {
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        mHandler.post(new Runnable() {

			@Override
			public void run() {
				onConnected(socket.getRemoteDevice());
			}
		});
    }
    private class ConnectedThread extends Thread {
        //private InputStream mInStream = null;
        private OutputStream mOutStream = null;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mHostSocket = socket;

            // Get the BluetoothSocket input and output streams
            try {
            	//mInStream = socket.getInputStream();
            	mOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Error in creating In and Out Streams" + e.getMessage());
                connectionLost();
            }
            
            mSender = new MessageSender();
            mSender.setOutputStream(mOutStream);
        }

        public void run() {
            Log.i(TAG, "Start ConnectedThread");
        }

        public void cancel() {
            try {
            	mSender = null;
                mHostSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Error close() of connected socket: " + e.getMessage());
            }
        }
    }
}
