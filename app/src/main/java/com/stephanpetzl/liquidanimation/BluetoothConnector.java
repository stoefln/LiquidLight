package com.stephanpetzl.liquidanimation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.stephanpetzl.liquidanimation.R.id.textView;

/**
 * Created by steph on 02/03/17.
 */

public class BluetoothConnector {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service
    private Activity mActivity;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    final Handler mHandler = new Handler();
    private boolean mStopThread = false;
    private byte[] mBuffer;
    private boolean mIsConnecting = false;

    public BluetoothConnector(Activity activity) {
        mActivity = activity;
    }

    public void init() {
        connect();
        setupReadThread();
    }

    private void connect() {
        if (mIsConnecting) {
            return;
        }
        mIsConnecting = true;

        new Thread(new Runnable() {
            public void run() {


                Log.v("Bluetooth", "Initializing bluetooth connector...");
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (bluetoothAdapter == null) {
                    Toast.makeText(mActivity, "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
                }
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    mActivity.startActivityForResult(enableAdapter, 0);
                }
                BluetoothDevice device = null;
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                String deviceName = "LiquidLight";
                if (bondedDevices.isEmpty()) {
                    Toast.makeText(mActivity, "Please Pair the Device first", Toast.LENGTH_LONG).show();
                } else {
                    for (BluetoothDevice iterator : bondedDevices) {

                        if (iterator.getName().equals(deviceName)) //Replace with iterator.getName() if comparing Device names.
                        {
                            device = iterator; //device is an object of type BluetoothDevice
                            Log.v("Bluetooth", "Device " + deviceName + " found");
                            break;
                        }
                    }
                }
                if (device != null) {
                    try {
                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
                        socket.connect();
                        mOutputStream = socket.getOutputStream();
                        mInputStream = socket.getInputStream();

                        Log.v("Bluetooth", "Bluetooth paired successfully!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(mActivity, "No device with name " + deviceName + " found!", Toast.LENGTH_LONG).show();
                }
                mIsConnecting = false;
            }
        }).run();
    }


    private void setupReadThread() {
        mStopThread = false;
        mBuffer = new byte[1024];
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !mStopThread) {
                    try {
                        if (mInputStream == null) {
                            Log.v("Bluetooth", "Disconnected... trying to reconnect in 3 seconds...");
                            Thread.currentThread().sleep(3000);
                            //connect();
                            continue;
                        }
                        int byteCount = mInputStream.available();
                        if (byteCount > 0) {
                            Log.v("Bluetooth", "received " + byteCount + " bytes");
                            byte[] rawBytes = new byte[byteCount];
                            mInputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            mHandler.post(new Runnable() {
                                public void run() {
                                    Log.v("Bluetooth", "received:" + string);
                                }
                            });

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        mStopThread = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void send(final String str) {
        Log.v("Bluetooth", "sending: " + str);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                str.concat("\n");
                try {
                    if (mOutputStream != null) {
                        mOutputStream.write(str.getBytes());
                        Log.v("Bluetooth", "sent");
                    }else{
                        Log.v("Bluetooth", "Disconnected... trying to reconnect in 3 seconds...");
                        //Thread.sleep(3000);
                        //connect();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void close() {

        mStopThread = true;
        try {
            mInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
