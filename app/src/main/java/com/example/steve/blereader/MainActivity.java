package com.example.steve.blereader;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    Boolean is_searching = false; //Flag for collecting data(IMU and BLE)
    Boolean keep_true = true; // aux value.
    float[] sensorvalues = new float[10];// time acc_x acc_y acc_z gyr_x gyr_y gyr_z mag_x mag_y mag_z
    long save_acc_time = 0;
    boolean is_acc_updated = false;
    boolean is_gyr_updated = false;
    boolean is_mag_updated = false;

    //    File ble_file = null;
//    File sensor_file = null;
    FileOutputStream ble_file ;//new FileOutputStream();
    FileOutputStream sensor_file ;//= new FileOutputStream();


    private class SensorDataSaver implements Runnable {
        private final static String TAG = "SensorDataSaver";

        int writed_counter = 0;
        StringBuilder local_sb = new StringBuilder();

        @Override
        public void run() {
            try {
                while (keep_true) {
                    if (is_searching) {
                        if (is_acc_updated && is_gyr_updated && is_mag_updated) {
                            // TODO: save to file
                            try {
                                Log.i(TAG,local_sb.toString());
                                if (writed_counter > 10) {
                                    sensor_file.write(local_sb.toString().getBytes(),0,local_sb.toString().getBytes().length);
                                    sensor_file.flush();
                                    local_sb.delete(0,local_sb.length());
                                    writed_counter = 0;
                                } else {
                                    writed_counter +=1;
                                    local_sb.append(String.valueOf(save_acc_time)+",");
                                    for(int i=1;i<9;++i)
                                    {

                                       local_sb.append(String.format("%.20f,",sensorvalues[i]));
                                    }
                                    local_sb.append(String.valueOf(sensorvalues[9])+"\n");

                                }
                                is_acc_updated = false;
                                is_gyr_updated = false;
                                is_mag_updated = false;

                            }catch (Exception e){
                                Log.i(TAG,"error in write to file (sensor file)");
                                System.out.print("error while save file");
                            }




                        } else {
                            // 100 ns...
                            sleep(0, 100);
                        }

                    } else {
                        // sleep for 100 ms
                        sleep(100);
                    }
                }

            } catch (Exception e) {

            }
        }
    }


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
                            Log.d(TAG, "wait for current discovering");
//                            mBluetoothAdapter.cancelDiscovery();
//                            sleep(5);
                        } else {
                            if (ble_file != null) {
                                ble_file.flush();
                            }
                            mBluetoothAdapter.startDiscovery();
                            sleep(0,200);
                        }
                    } else {
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        sleep(0,10);
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
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                mTextView.append(String.valueOf(System.currentTimeMillis()) + "," +
                        device.getAddress() + "," +
                        Integer.toString(rssi) + "\n");

                // write to file at each moments but flush when discovering ended(see : BLERunnable).
                StringBuilder tmp_sb = new StringBuilder();
                tmp_sb.append(String.valueOf(System.currentTimeMillis()) + "," +
                        device.getAddress() + "," +
                        Integer.toString(rssi) + "\n");
                try {

                    ble_file.write(tmp_sb.toString().getBytes(),0,tmp_sb.toString().getBytes().length);
//                    ble_file.write("aaa".getBytes());
                } catch (Exception e) {
                    mTextView.append("broadcastReceiver" + e.getLocalizedMessage()+e.toString() + "\n");

                }
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.showText);
        mTextView.setGravity(Gravity.BOTTOM);// keep the point of view at last line.


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

        SensorDataSaver saver = new SensorDataSaver();
        new Thread(saver).start();


        // Sensor manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //// Getting permission...
        String[] needed_permission = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS
        };

        boolean permission_ok = true;

        for(String permission: needed_permission)
        {
            if(ContextCompat.checkSelfPermission(this,
                    permission)!= PackageManager.PERMISSION_GRANTED)
            {
                permission_ok = false;
                mTextView.append(String.valueOf(permission_ok)+"\n");
            }
        }

        if(!permission_ok)
        {
            ActivityCompat.requestPermissions(
                    this,
                    needed_permission,
                    1
            );
        }



    }


    public void onClick_Search(View v) {
//        setProgressBarIndeterminateVisibility(true);
        if (is_searching) {

            /**
             * STOP
             */
            is_searching = false;
//            setTitle("Searching stopped");
            mControlButton.setText("Start");

            mSensorManager.unregisterListener(this);

            // TODO: close file
            try {

                if (ble_file != null) {
                    ble_file.close();
                }
                if (sensor_file != null) {
                    sensor_file.close();
                }

                ble_file = null;
                sensor_file = null;
            } catch (IOException e) {
                Log.d("test", e.toString());
                mTextView.append(e.getLocalizedMessage()+e.toString() + "\n");
            }




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


            // TODO: openfile
            long time_now = System.currentTimeMillis();
            try {



                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File sdCardDir = Environment.getExternalStorageDirectory();

                    File ble_file_creator = new File(sdCardDir.getCanonicalPath() + "/a/"+ String.valueOf(time_now) + "ble.txt");
                    boolean is_ble_file_ok =  ble_file_creator.createNewFile();
                    mTextView.append("is ble file ok :" + String.valueOf(is_ble_file_ok) + "\n");


                    File sensor_file_creator = new File( sdCardDir.getCanonicalPath() + "/a/"+ String.valueOf(time_now) + "sensor.txt");

                    boolean is_seasor_file_ok = sensor_file_creator.createNewFile();
                    mTextView.append("is sensor file ok : " + String.valueOf(is_seasor_file_ok)+"\n");

                    ble_file = new FileOutputStream(ble_file_creator.getCanonicalPath());
                    sensor_file = new FileOutputStream(sensor_file_creator.getCanonicalPath());

                    assert(ble_file!=null && sensor_file!=null);
                } else {
                    mTextView.append("Cannot access SDCARD!!!\n");
                }

            } catch (IOException e) {;//
                Log.d("test", e.toString());
                e.printStackTrace();
                mTextView.append(e.getLocalizedMessage()+e.toString() + "\n");
            }


        }

    }

    // Sensor Event listener

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * backcall for sensor information
     *
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float[] values = sensorEvent.values;
        StringBuilder sb = null;//= new StringBuilder();


        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
//                sb = new StringBuilder();
//                sb.append(String.valueOf(System.currentTimeMillis()));
//                sb.append(":acc:");
//                sb.append(String.valueOf(values[0]) + ',');
//                sb.append(String.valueOf(values[1]) + ',');
//                sb.append(String.valueOf(values[2]) + '\n');
//                mTextView.append(sb.toString());
//                sensorvalues[0] = System.currentTimeMillis();
                save_acc_time = System.currentTimeMillis();
                sensorvalues[1] = values[0];
                sensorvalues[2] = values[1];
                sensorvalues[3] = values[2];
                is_acc_updated = true;


                break;
            case Sensor.TYPE_GYROSCOPE:
//                sb = new StringBuilder();
//                sb.append(String.valueOf(System.currentTimeMillis()));
//                sb.append(":gyr:");
//                sb.append(String.valueOf(values[0]) + ',');
//                sb.append(String.valueOf(values[1]) + ',');
//                sb.append(String.valueOf(values[2]) + '\n');
//                mTextView.append(sb.toString());
                sensorvalues[4] = values[0];
                sensorvalues[5] = values[1];
                sensorvalues[6] = values[2];
                is_gyr_updated = true;

                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
//                sb = new StringBuilder();
//                sb.append(String.valueOf(System.currentTimeMillis()));
//                sb.append(":mag:");
//                sb.append(String.valueOf(values[0]) + ',');
//                sb.append(String.valueOf(values[1]) + ',');
//                sb.append(String.valueOf(values[2]) + '\n');
//                mTextView.append(sb.toString());

                sensorvalues[7] = values[0];
                sensorvalues[8] = values[1];
                sensorvalues[9] = values[2];

                is_mag_updated = true;
                break;

        }


    }
}
