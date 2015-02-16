package hugonicolau.android.bluetoothlibrary.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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
	
	protected abstract void onStart();
	protected abstract void onStop();
	protected abstract void onConnected(BluetoothDevice remoteDevice);
	protected abstract void onDisconnected(BluetoothDevice remoteDevice);
	protected abstract void onReceiveMessage(String message);
	
	public BluetoothServer()
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	// starts listening for client requests
	public void startListening()
	{
		Log.d(TAG, "Starting listening");
		
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
        private BluetoothServerSocket mServerSocket = null;
        
        // name for server
        private final String NAME="BTServer";
        
        // UUIDs
        private final String uuid1 = "05f2934c-1e81-4554-bb08-44aa761afbfb";

        public AcceptThread() {
            try 
            {
            	// get server socket
            	mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, UUID.fromString(uuid1));
            	
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
            while (true) 
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
        private BluetoothSocket mClientSocket = null;
        private InputStream mInStream = null;
        //private OutputStream mOutStream = null;
        private MessageReader mReader = null;

        public ConnectedThread(BluetoothSocket clientSocket) 
        {
            Log.d(TAG, "Create ConnectedThread");
            
            mClientSocket = clientSocket;
            
            // get the BT input and output streams
            try 
            {
            	mInStream = clientSocket.getInputStream();
            	//mOutStream = clientSocket.getOutputStream();
            } 
            catch (IOException e) 
            {
                Log.d(TAG, "Error when getting In and Out streams: " + e.getMessage());
            }
        }

        public void run() 
        {
            Log.d(TAG, "Start ConnectedThread");

            mReader = new MessageReader();
            mReader.setInputStream(mInStream);

            // reads from inputstream and calls onReceiveMessage
            String message = null;
			while ((message = mReader.read()) != null)
			{
				//if(message.equalsIgnoreCase("\r\n")) break; // close
				sendMessage(message);
			}
			
			try {
				mClientSocket.close();
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
			}
			connectionLost(mClientSocket);
        }
        
        private void sendMessage(final String message)
        {
        	mHandler.post(new Runnable() {

    			@Override
    			public void run() {
    				onReceiveMessage(message);
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
    		mAcceptThread = new AcceptThread();
    		mAcceptThread.start();
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
