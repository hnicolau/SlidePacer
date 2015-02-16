package android.hugonicolau.androidbluetoothlibrary.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.hugonicolau.androidbluetoothlibrary.client.MessageSender;
import android.os.Handler;
import android.util.Log;

public abstract class BluetoothServer {
	private static final String TAG = "BluetoothServer";
	
	// BT adapter
	private BluetoothAdapter mAdapter = null;
	
	// thread used to accept connection requests
	private AcceptThread mAcceptThread = null;
	
	// thread used on accepted (connected) requests
	private ConnectedThread mConnectedThread = null;
	
	// handler used to send messages and prevent blocking
	private Handler mHandler = new Handler();

    private String mUUID = "";
    private boolean mIsStopped = false;
	
	protected abstract void onStart();
	protected abstract void onStop();
	protected abstract void onConnected(BluetoothDevice remoteDevice);
	protected abstract void onDisconnected(BluetoothDevice remoteDevice);
	protected abstract void onReceiveMessage(String message, MessageSender sender);

    public BluetoothAdapter getAdapter()
    {
        return mAdapter;
    }
	
	public BluetoothServer(String uuid)
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();
        mUUID = uuid;
	}

	// starts listening for client requests
	public void startListening()
	{
		Log.d(TAG, "Starting listening");
		mIsStopped = false;

		if(mAcceptThread != null) 
		{
			// already accepting a request, thus cancel
			mAcceptThread.cancel();
        }
		else if (mConnectedThread!= null) 
		{
			// a connection is on, thus cancel
            mConnectedThread.cancel();
        }
		else 
		{
        	mAcceptThread = new AcceptThread();
        	mAcceptThread.start();
        }
	}

    // TODO: Does not work properly when there's a client connected
    // TODO: seems like socket stays opened preventing client from reconnecting
    public void stopListening()
    {
        Log.d(TAG, "Stop listening");

        mIsStopped = true;
        if(mAcceptThread != null)
        {
            mAcceptThread.mAccepting = false;
            try {
                mAcceptThread.mServerSocket.close();
            } catch (IOException e) {
                Log.v(TAG, e.getMessage());
            }

            // accepting a request, thus cancel
            // this calls onStop
           // mAcceptThread.cancel();
            mAcceptThread = null;

            onStop();
        }
        else if (mConnectedThread != null)
        {
            try {
                mConnectedThread.mInStream.close();
                mConnectedThread.mOutStream.close();
                mConnectedThread.mClientSocket.close();
            } catch (IOException e) {
                Log.v(TAG, e.getMessage());
            }

            mConnectedThread.connectionLost(mConnectedThread.mClientSocket);
            // a connection is on, thus cancel
            // this call onDisconnected, but does not start listening
            //mConnectedThread.cancel();
            mConnectedThread = null;

            onStop();
        }

    }
	
	private void manageConnectedSocket(final BluetoothSocket socket) 
	{
		// start our connection thread
	    mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mHandler.post(new Runnable() {

			@Override
			public void run() {
				onConnected(socket.getRemoteDevice());
			}
		});
     }
	
	/**
	 * 
	 * @author hugonicolau
	 * Thread used to accept requests
	 */
	private class AcceptThread extends Thread 
	{
		// BT socket
        public BluetoothServerSocket mServerSocket = null;
        
        // name for server
        private final String NAME="BTServer";

        public boolean mAccepting = true;

        public AcceptThread() {
            try 
            {
            	// get server socket
            	mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, UUID.fromString(mUUID));

                mAccepting = true;
            	mHandler.post(new Runnable() {

        			@Override
        			public void run() {
        				onStart();
        			}
        		});
            } 
            catch (IOException e) 
            { 
            	Log.d(TAG, "Error listen for devices: " + e.getMessage());
            }
        }

        public void run() 
        {
            Log.d(TAG, "Server running");
            
            BluetoothSocket clientSocket = null;
            
            // keep listening until exception occurs or a socket is returned
            while (mAccepting)
            {
            	try 
            	{
            		// waits for request
            		clientSocket = mServerSocket.accept();
                } 
            	catch (IOException e) 
            	{
                    Log.d(TAG, "Error waiting for requests: " + e.getMessage());
                    break;
                }
            	
                // if a connection was accepted or an error occurred
                if (clientSocket != null)
                {
                	// if a connection was accepted
                    try 
                    {
                    	// close server socket to prevent further requests
                        serverStop();
                    }
                    catch (IOException e)
                    {
                    	Log.d(TAG, "Error closing server socket: " + e.getMessage());
                    }
                    
                    // do work to manage the connection (in a separate thread)
                    manageConnectedSocket(clientSocket);

                    break;
                }
            }
        }

        // will cancel the listening socket, and cause the thread to finish
        public void cancel() {
            try
            {
                serverStop();
            } 
            catch (IOException e) 
            { 
            	Log.d(TAG, "Error canceling accept thread: " + e.getMessage());
            }
        }
        
        public void serverStop() throws IOException
        {
            mAccepting = false;

        	mServerSocket.close();
        	
        	mHandler.post(new Runnable() {

    			@Override
    			public void run() {
    				onStop();
    			}
    		});
        }
    }
	
	/**
	 * 
	 * @author hugonicolau
	 * Thread used when a connection is accepted
	 */
	private class ConnectedThread extends Thread {
        public BluetoothSocket mClientSocket = null;
        public InputStream mInStream = null;
        public OutputStream mOutStream = null;
        private MessageReader mReader = null;
        private MessageSender mSender = null;

        public ConnectedThread(BluetoothSocket clientSocket) 
        {
            Log.d(TAG, "Create ConnectedThread");
            
            mClientSocket = clientSocket;
            
            // get the BT input and output streams
            try 
            {
            	mInStream = clientSocket.getInputStream();
            	mOutStream = clientSocket.getOutputStream();

                mReader = new MessageReader();
                mReader.setInputStream(mInStream);
                mSender = new MessageSender();
                mSender.setOutputStream(mOutStream);
            } 
            catch (IOException e) 
            {
                Log.d(TAG, "Error when getting In and Out streams: " + e.getMessage());
            }
        }

        public void run() 
        {
            Log.d(TAG, "Start ConnectedThread");

            // reads from inputstream and calls onReceiveMessage
            String message = null;
			while ((message = mReader.read()) != null)
			{
				//if(message.equalsIgnoreCase("\r\n")) break; // close
				sendMessage(message, mSender);
			}
			
			try {
				mClientSocket.close();
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
			}
			connectionLost(mClientSocket);
        }
        
        private void sendMessage(final String message, final MessageSender sender)
        {
        	mHandler.post(new Runnable() {

    			@Override
    			public void run() {
    				onReceiveMessage(message, sender);
    			}
    		});
        }
        
        private void connectionLost(final BluetoothSocket socket) {
        	Log.d(TAG, "client disconnected");
    		mHandler.post(new Runnable() {

    			@Override
    			public void run() {
    				onDisconnected(socket.getRemoteDevice());
    			}
    		});

            if(!mIsStopped) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
    	}

        public void cancel() 
        {
            try 
            {
                mClientSocket.close();
            } 
            catch (IOException e) {
                Log.d(TAG, "Error when closing ConnectedThread : " + e.getMessage());
            }

        }
    }

}
