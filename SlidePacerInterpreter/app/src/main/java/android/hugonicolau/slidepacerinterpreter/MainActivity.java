package android.hugonicolau.slidepacerinterpreter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.hugonicolau.androidbluetoothlibrary.client.MessageSender;
import android.hugonicolau.androidbluetoothlibrary.server.BluetoothServer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "SlidePacerInterpreter";

    // BT listener
    private static BluetoothServer mBluetoothHost = null;
    private String UUID = "05f2934c-1e81-4554-bb08-44aa761afbfb";
    private boolean mIsListening = false;
    private boolean mIsConnected = false;
    private int REQUEST_ENABLE_BT = 1;
    public MessageSender mSender = null;
    public String mMessage = "";

    // UI
    private TextView mTvStatus = null;
    private TextView mTvAddress = null;
    private Button mBtnReady = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvStatus = (TextView) findViewById(R.id.tvStatus);

        mTvAddress = (TextView) findViewById(R.id.tvAddress);

        mBtnReady = (Button) findViewById(R.id.btnReady);
        mBtnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSender != null){

                    // send message
                    mSender.sendMessage(mMessage);

                    // reset message
                    mSender = null;
                    mMessage = "";

                    // update UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtnReady.setText("");
                            Drawable d = mBtnReady.getBackground();
                            d.clearColorFilter();
                        }
                    });
                }
            }
        });

        mBluetoothHost = new BluetoothServer(UUID) {

            @Override
            protected void onConnected(final BluetoothDevice remoteDevice) {
                Log.d(TAG, "connected: " + remoteDevice.getName());
                mIsConnected = true;

                // update UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatus.setText("Status: connected to " + remoteDevice.getName());
                        mTvStatus.setTextColor(Color.GREEN);
                    }
                });
            }

            @Override
            protected void onDisconnected(final BluetoothDevice remoteDevice) {
                Log.d(TAG, "disconnected: " + remoteDevice.getName());
                mIsConnected = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatus.setText("Status: disconnected from " + remoteDevice.getName());
                        mTvStatus.setTextColor(Color.RED);
                    }
                });
            }

            @Override
            protected void onReceiveMessage(final String message, final MessageSender sender) {
                Log.d(TAG, "message: " + message);

                mSender = sender;
                mMessage = message;

                // update UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnReady.setText("Touch when finish interpreting");
                        Drawable d = mBtnReady.getBackground();
                        d.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.OVERLAY));
                    }
                });
            }

            @Override
            protected void onStart() {
                // start listening
                Log.d(TAG, "start listening");
                mIsListening = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatus.setText("Status: waiting for connection from instructor");
                        mTvStatus.setTextColor(Color.DKGRAY);
                    }
                });
            }

            @Override
            protected void onStop() {
                // stop listening
                Log.d(TAG, "stop listening");
                mIsListening = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatus.setText("Status: stopped. Please restart the application");
                        mTvStatus.setTextColor(Color.DKGRAY);
                    }
                });
            }
        };
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!mIsConnected && !mIsListening) {
            // set button size
            WindowManager wm = (WindowManager) this.getSystemService(MainActivity.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int w = display.getWidth();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBtnReady.getLayoutParams();
            params.width = w - 50;
            params.height = w - 50;
            mBtnReady.setLayoutParams(params);

            // reset state
            mBtnReady.setText("");
            Drawable d = mBtnReady.getBackground();
            d.clearColorFilter();

            // starts listening
            if (mBluetoothHost.getAdapter() == null) {
                Toast.makeText(this, "Device Does not Support Bluetooth", Toast.LENGTH_LONG).show();
            } else if (!mBluetoothHost.getAdapter().isEnabled()) {
                // we need to wait until bt is enabled before set up, so that's done either in the following else, or
                // in the onActivityResult for our code ...
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                mTvAddress.setText("Address: " + mBluetoothHost.getAdapter().getAddress());
                mTvAddress.setTextColor(Color.GRAY);
                mBluetoothHost.startListening();
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        // stop listening
        mBluetoothHost.stopListening();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Failed to enable Bluetooth", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_LONG).show();
                //if(!mIsListening) mBluetoothHost.startListening();
            }
        }
    }
}
