package com.niemisami.androidsandbox.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;


public class SensorService extends Service {

    private final String TAG = "SensorService";
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;


    /////KEY VALUES/////
    public static final int STOP_SENSORS = 0;
    public static final int START_SENSORS = 1;
    public static final int SENSOR_ERROR = -1;
    public static final int SENSOR_ACC = 2;
    public static final int SENSOR_GYRO = 3;

    private final String SENSOR_COMMAND = "SENSOR_COMMAND";
    public static final String STATUS = "STATUS";
    public static final String SENSOR_VALUES = "SENSOR_VALUES";
    public static final String SENSOR_TYPE = "SENSOR_TYPE";
    public static final String SENSOR_X = "SENSOR_X";
    public static final String SENSOR_Y = "SENSOR_Y";
    public static final String SENSOR_Z = "SENSOR_Z";
    public static final String SENSOR_TIMESTAMP = "SENSOR_TIMESTAMP";


    public SensorService() {
    }


    //    Start service from Activity


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Started service");

//        Start up new background thread where sensor can be read without
//        blocking the main thread
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();

//        Use background looper with ServiceHandler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service on destroy");
        Message msg = mServiceHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt(SENSOR_COMMAND, STOP_SENSORS);
        msg.setData(bundle);
        mServiceHandler.stopSensors();
        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        Start command is sent from the fragment
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId; // id to know which request is being processed
        msg.obj = intent;   // intent that binds messenger to client

//        Bundle contains message to start sensor reading
        Bundle bundle = new Bundle();
        bundle.putInt(SENSOR_COMMAND, START_SENSORS);
        msg.setData(bundle);
        mServiceHandler.sendMessage(msg);
//        Do not try to start service again if this crashes

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    ////SERVICE BACKGROUND THREAD/////
    //    region
    private final class ServiceHandler extends Handler implements SensorEventListener {


        private int serviceId;
        private Messenger mMessenger;

        private Sensor mAccSensor, mGyroSensor;
        private SensorManager mSensorManager;
        private final int mSensorDelay = 0;

        private int command;

        private List<Float> accArray;
        private int accAmount;
        private int gyroAmount;

        public ServiceHandler(Looper looper) {
            super(looper);
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            command = bundle.getInt(SENSOR_COMMAND);
            if (command == START_SENSORS) {

//                Save information about service where thread is started and intent containing
//                messenger
                serviceId = msg.arg1;
                Intent intent = (Intent) msg.obj;
                mMessenger = (Messenger) intent.getExtras().get("MESSENGER");

                post(SensorRunnable);
            }
        }

        private void startSensors() {
            Log.d(TAG, "Sensors Started");
            mSensorManager.registerListener(this, mAccSensor, mSensorDelay);
            mSensorManager.registerListener(this, mGyroSensor, mSensorDelay);
            sendResultToClient(START_SENSORS);
        }

        /**Runnable to read sensor data and send it back to the target*/
        private Runnable SensorRunnable = new Runnable() {
            @Override
            public void run() {
                startSensors();
            }
        };

        private void stopSensors() {

            Log.d(TAG, "Sensors Stopped");
            mSensorManager.unregisterListener(this);
            sendResultToClient(STOP_SENSORS);
            removeCallbacks(SensorRunnable);
            stopSelf(serviceId);
        }

        private void sendResultToClient(int result) {

            Log.d(TAG, "Sent result " + result);
            Message statusMessage = Message.obtain();

            statusMessage.arg1 = result;

            if (result == 0) {
                Bundle b = new Bundle();
                b.putInt(SENSOR_VALUES, accAmount);
                statusMessage.setData(b);
            }
            try {
                mMessenger.send(statusMessage);

            } catch (RemoteException e) {
                Log.e(TAG, "Error in service's background thread", e);
            }
        }

        /**Sends x, y, z and timestamp back to the handler in target.
         * Message arg1 contains sensor type identifier, 2: acc, 3: gyro*/
        private void sendSensorData(int sensorType, float x, float y, float z, long timestamp) {

            Message msg = Message.obtain();
            if(sensorType == Sensor.TYPE_ACCELEROMETER) msg.arg1 = SENSOR_ACC;
            else if(sensorType == Sensor.TYPE_GYROSCOPE) msg.arg1 = SENSOR_GYRO;

            Bundle bundle = new Bundle();
            bundle.putFloat(SENSOR_X, x);
            bundle.putFloat(SENSOR_Y, y);
            bundle.putFloat(SENSOR_Z, z);
            bundle.putLong(SENSOR_TIMESTAMP, timestamp);
            msg.setData(bundle);

            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending sensor data", e);
            }
        }


        ////SENSOR READING////
        @Override
        public void onSensorChanged(SensorEvent event) {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    sendSensorData(Sensor.TYPE_ACCELEROMETER, event.values[0], event.values[1], event.values[2], event.timestamp);
                    accAmount++;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sendSensorData(Sensor.TYPE_GYROSCOPE, event.values[0], event.values[1], event.values[2], event.timestamp);
                    gyroAmount--;
                    break;
            }
            if (accAmount > 5000) {
                stopSensors();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }


    }

//    endregion


}
