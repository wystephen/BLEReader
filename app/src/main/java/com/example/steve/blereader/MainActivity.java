package com.example.steve.blereader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Set;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    Boolean is_searching = false; //Flag for collecting data(IMU and BLE)
    Boolean keep_true = true; // aux value.

    /**
     * For Maintain the collecting state.
     */
    private class BLERunnable implements Runnable {
        private final static String TAG = "KeepSearchThread";
//        public TextView mTextView; // show data
//        public Button mControlButton;// start or stop collect data
//        public BluetoothAdapter mBluetoothAdapter;

        @Override
        public void run() {
            // TODO Auto-generated method stub
            //// Never use mText in this function!!!
            Log.d(TAG, "begin to run");

            try {
                while (keep_true) {
                        if (is_searching) {
                            if (mBluetoothAdapter.isDiscovering()) {
                                Log.d(TAG,"wait for current discovering");
                                sleep(5);
                            } else {
                                mBluetoothAdapter.startDiscovery();
                            }
                        } else {
                            sleep(100);
                        }
                }

            } catch (Exception e) {
//                mTextView.append("Error at "+e.toString() );
                e.printStackTrace();

            }

        }
    }

    private TextView mTextView; // show data
    private Button mControlButton;// start or stop collect data
    private BluetoothAdapter mBluetoothAdapter; // BLE adapter

    private SensorManager mSensorManager;

    private Thread ble_discovering_thread; // thread don't change to local variable

    /**
     * BLE RSSI pre-processing and saving.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub

            String action = intent.getAction();
            // 获得已经搜索到的蓝牙设备
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                // 显示在TextView上
                mTextView.append(String.valueOf(System.currentTimeMillis())+","+
                        device.getAddress() +","+
                                Integer.toString(rssi)+ "\n");
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

        // 注册用以接收到已搜索到的蓝牙设备的receiver
        IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, mFilter);
        // 注册搜索完时的receiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, mFilter);


        // Start runnable
        BLERunnable bleRunnable = new BLERunnable();
        ble_discovering_thread = new Thread(bleRunnable);
        ble_discovering_thread.start();

        // Sensor manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


    }





    public void onClick_Search(View v) {
//        setProgressBarIndeterminateVisibility(true);
        if (is_searching) {
            is_searching = false;
//            setTitle("Searching stopped");
            mControlButton.setText("Start");

            mSensorManager.unregisterListener(this);

        } else {
            is_searching = true;
//            setTitle("正在扫描....");
            Log.d("test", "clicked on search");
//            mBluetoothAdapter.startDiscovery();
            mControlButton.setText("Stop");

            // register sensors listener
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_FASTEST);

            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_FASTEST);

            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_FASTEST);

        }

    }

    // Sensor Event listener

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float[] values = sensorEvent.values;

        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType)
        {
            case Sensor.TYPE_ACCELEROMETER:

                break;
            case Sensor.TYPE_GYROSCOPE:

                break;
            case Sensor.TYPE_MAGNETIC_FIELD:

                break;

        }


    }
}
