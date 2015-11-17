package com.niemisami.androidsandbox.Reading;

import android.content.Context;
import android.hardware.SensorEvent;
import android.os.Handler;
import android.util.Log;

import com.niemisami.androidsandbox.Database.SQLiteDatabaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton ReadingManger deals between Views and storage
 */
public class ReadingManager {

    private static final String TAG = "ReadingManager";
    private static ReadingManager mReadingManger;

    private Context mContext;
    private Reading mReading;
    private List<Reading> mReadings;
    private SQLiteDatabaseManager mDataManager;
//    private SensorReadingHandler mSensorReadingHandler;
    private Handler mHandler;


    private ReadingManager(Context context) {
        mContext = context;
        mReadings = new ArrayList<>();
        mDataManager = SQLiteDatabaseManager.get(mContext);
        mReadings = getDataFromStorage();
        mHandler = new Handler();
    }

    /**
     * Return new instance of ReadingManager only if there isn't earlier in the app
     */
    public static ReadingManager get(Context context) {
        if (mReadingManger == null) {
            mReadingManger = new ReadingManager(context);
        }
        return mReadingManger;
    }

    /**
     * Called when user press start button on UI. Starts reading in new thread
     */
    public void addNewReading(Reading reading) {
        mReading = reading;
//        mSensorReadingHandler = new SensorReadingHandler(mContext, mDataManager);
        mHandler.removeCallbacks(mSensorReaderRunnable);
        mHandler.post(mSensorReaderRunnable);
    }


    /**
     * SensorReaderRunnable saves data of the new reading to the db and starts sensors in background thread
     */
    private Runnable mSensorReaderRunnable = new Runnable() {
        @Override
        public void run() {
            mReading.setId(mDataManager.insertReadingToDB(mReading));  // set data to the db and returns id of the reading
            Log.d(TAG, "Reading: " + mReading.getPatientName() + " id: " + mReading.getId() + " added to db");
            startSensors();
        }
    };

    public void startSensors() {
//        mSensorReadingHandler.startReadingSensorData();
//        SensorReaderService.actionStartSensors(mContext);
    }


    /**
     * Called in UI. Unregisters SensorReaderRunnable
     */
    public void stopReading() {
        mReading.setEndTime(System.currentTimeMillis());
        stopSensors();
        Log.d(TAG, "Reading stopped. Duration " + mReading.getReadingDuration());
    }

    /**
     * Stops event listeners
     */
    public void stopSensors() {
        Log.d(TAG, "Reading stopped");
//        Log.d(TAG, "After reading " + mDataManager.getCount());
//        mSensorReadingHandler.stopReadingSensorData();
        mHandler.removeCallbacks(mSensorReaderRunnable);
//        SensorReaderService.actionStopSensors(mContext);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "After stopping service reading " + mDataManager.getCount());
            }
        }).start();


    }

    /**
     * Closes database
     */
    public void close() {
        mDataManager.closeDataManager();
    }

    /**
     * Basic getter for List of readings
     */
    public List<Reading> getReadings() {
        mReadings = mDataManager.getData();
        return mReadings;
    }

    /**
     * Save all data again to the storage. Used if readings info has changes
     */
    public void saveReadings() {
        mDataManager.saveData(mReadings);
    }

    /**
     * Return all readings from the storage as a list
     */
    private List<Reading> getDataFromStorage() {
        return mDataManager.getData();
    }

    /**
     * Deletes selected reading from the storage
     */
    public void deleteReading(Reading reading) {
        mDataManager.deleteData(reading);
    }

    /**
     * Be careful with this one!
     */
    public void deleteAll() {
        mDataManager.deleteAll();
    }

    /**
     * Method stores sensor data to the database
     */
    public void addSensorDataToStorage(int sensorId, SensorEvent event) {
        mDataManager.addData(sensorId, event);
    }
}
