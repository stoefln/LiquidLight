package com.stephanpetzl.liquidanimation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.stephanpetzl.liquidanimation.R.id.content_main;
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

        log("mIsConnecting:" + mIsConnecting);
        if (mIsConnecting) {
            return;
        }
        mIsConnecting = true;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                log("Set mIsConnecting = true;");
                log("Initializing bluetooth connector... ");
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (bluetoothAdapter == null) {
                    log(mActivity.getResources().getString(R.string.does_not_support_bluetooth));
                    return null;
                }
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    mActivity.startActivityForResult(enableAdapter, 0);
                }
                BluetoothDevice device = null;
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                String deviceName = "LiquidLight";
                if (bondedDevices.isEmpty()) {
                    log("Please Pair the Device first");
                } else {
                    for (BluetoothDevice iterator : bondedDevices) {

                        if (iterator.getName().equals(deviceName)) //Replace with iterator.getName() if comparing Device names.
                        {
                            device = iterator; //device is an object of type BluetoothDevice
                            log("Device \"" + deviceName + "\" found");
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

                        log("Bluetooth paired successfully!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    log("No device with name " + deviceName + " found!");
                }
                mIsConnecting = false;
                log("Set mIsConnecting = false;");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


    }

    private void log(String message) {
        Log.v("BluetoothConnector", message);
    }


    private void setupReadThread() {
        mBuffer = new byte[1024];
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                while (!Thread.currentThread().isInterrupted() && !mStopThread) {
                    try {
                        if (mInputStream == null) {
                            log("Disconnected... trying to reconnect in 5 seconds...");
                            Thread.currentThread().sleep(5000);
                            connect();
                            continue;
                        }
                        int byteCount = mInputStream.available();
                        if (byteCount > 0) {
                            //log("received " + byteCount + " bytes");
                            byte[] rawBytes = new byte[byteCount];
                            mInputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            mHandler.post(new Runnable() {
                                public void run() {
                                    Log.v("BluetoothReceiver", string);
                                }
                            });

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }

    public void send(final String str) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                str.concat("\n");
                try {
                    if (mOutputStream != null) {
                        mOutputStream.write(str.getBytes());
                        //log("sending: " + str);
                    } else {
                        throw new IOException("Disconnected- can't send");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    connect();
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
