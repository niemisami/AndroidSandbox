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


    private final int START_SENSORS = 1;
    private final int STOP_SENSORS = 0;
    private final String SENSOR_COMMAND = "SENSOR_COMMAND";
    public static final String STATUS = "STATUS";
    public static final String SENSOR_VALUES = "SENSOR_VALUES";


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
        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quit();
        super.onDestroy();
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
    private final class ServiceHandler extends Handler implements SensorEventListener  {


        private int serviceId;
        private Messenger mMessenger;

        private Sensor mAccSensor, mGyroSensor;
        private SensorManager mSensorManager;
        private final int mSensorDelay = 0;

//        Status 1 when started
//        Status 0 when stopping
//        Status -1 error
        private int status;
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
            if(command == START_SENSORS) {

//                Save information about service where thread is started and intent containing
//                messenger
                serviceId = msg.arg1;
                Intent intent = (Intent) msg.obj;
                mMessenger = (Messenger) intent.getExtras().get("MESSENGER");

                startSensors();
            } else if(command == STOP_SENSORS) {
                stopSensors();
            }


        }


        private void startSensors() {

            Log.d(TAG, "Sensors Started");
            mSensorManager.registerListener(this, mAccSensor, mSensorDelay, 0);
            mSensorManager.registerListener(this, mGyroSensor, mSensorDelay, 0);
            status = 1;
            sendResultToClient(status);
        }

        private void stopSensors() {

            Log.d(TAG, "Sensors Stopped");
            mSensorManager.unregisterListener(this);

            status = 0;
            sendResultToClient(status);
            stopSelf(serviceId);


        }

        private void sendResultToClient(int result) {

            Log.d(TAG, "Sent result " + result);
            Message statusMessage = Message.obtain();

            statusMessage.arg1 = result;

            if(result == 0) {
                Bundle b = new Bundle();
                b.putInt(SENSOR_VALUES, accAmount);
                statusMessage.setData(b);
            }

            try {
                mMessenger.send(statusMessage);
            } catch (RemoteException e) {
                Log.e(TAG, "Error in service's background thread",e);
            }
        }

        ////SENSOR READING////
        @Override
        public void onSensorChanged(SensorEvent event) {


            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accAmount++;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroAmount--;
                    break;
            }
            if(accAmount > 1000) {
                stopSensors();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }


    }

//    endregion



}
