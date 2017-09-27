package com.example.steve.blereader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Boolean is_searching = false;

    private class BLERunnable implements Runnable {
        private final static String TAG = "My Runnable ===> ";
//        public TextView mTextView; // show data
//        public Button mControlButton;// start or stop collect data
//        public BluetoothAdapter mBluetoothAdapter;

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG, "run");
            mTextView.append("started run");

            try {
                while (true) {
                    if (is_searching) {
                        mTextView.append("Searching...");
                        if (mBluetoothAdapter.isDiscovering()) {
//                    mBluetoothAdapter.cancelDiscovery();
                            wait(10);
                        } else {
                            mTextView.append("start Discovery()");

                            mBluetoothAdapter.startDiscovery();
                        }
                        Log.i("Test", "try to out");
                    } else {
                        wait(100);
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private TextView mTextView; // show data
    private Button mControlButton;// start or stop collect data
    private BluetoothAdapter mBluetoothAdapter;

    private Thread ble_discovering_thread;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub

            String action = intent.getAction();
            // 获得已经搜索到的蓝牙设备
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 搜索到的不是已经绑定的蓝牙设备
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                // 显示在TextView上
                mTextView.append(device.getName() + ":"
                        + device.getAddress() + "\n");
//                }
                // 搜索完成
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
//                setTitle("搜索蓝牙设备");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.showText);
        mControlButton = (Button) findViewById(R.id.ControlButton);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取所有已经绑定的蓝牙设备
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                mTextView.append(bluetoothDevice.getName() + ":"
                        + bluetoothDevice.getAddress() + "\n\n");
            }
        }
        // 注册用以接收到已搜索到的蓝牙设备的receiver
        IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, mFilter);
        // 注册搜索完时的receiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, mFilter);


        // Start runnable
        BLERunnable bleRunnable = new BLERunnable();
//        bleRunnable.mBluetoothAdapter = mBluetoothAdapter;
//        bleRunnable.mControlButton = mControlButton;
//        bleRunnable.mTextView = mTextView;
//        new Thread(new BLERunnable()).start();
        ble_discovering_thread = new Thread(bleRunnable);
        ble_discovering_thread.start();
    }

    public void onClick_Search(View v) {
//        setProgressBarIndeterminateVisibility(true);
        if (is_searching) {
            is_searching = false;
//            setTitle("Searching stopped");
            mControlButton.setText("Start");
        } else {
            is_searching = true;
//            setTitle("正在扫描....");
            Log.d("test", "clicked on search");
//            mBluetoothAdapter.startDiscovery();
            mControlButton.setText("Stop");
        }


    }
}
