package com.niemisami.androidsandbox.Database;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.niemisami.androidsandbox.Reading.Reading;
import com.niemisami.androidsandbox.services.SensorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sakrnie on 9.9.2015.
 * SQLiteDatabaseManger takes care of saving and fetching data from SQLite db
 */
public class SQLiteDatabaseManager implements DataManager {

    private final String TAG = "SQLiteDatabaseManager";


    private Context mContext;
    private DatabaseHelper mHelper;
    private static SQLiteDatabaseManager mSqliteHelper;

    private AtomicInteger mRunningProcessCount;

    private Reading mReading;

    private static final int arraySize = 200;
    private int accIndex;

    private int gyroIndex;
    //    Sensor data values
    private List<Float> mAccZ;
    private List<Float> mAccX;
    private List<Float> mAccY;
    private List<Long> mAccTimestamp;
    private List<Float> mGyroZ;
    private List<Float> mGyroX;
    private List<Float> mGyroY;


    private List<Long> mGyroTimestamp;

    /**
     * SQLiteDatabaseManager handles SQLite database and offers methods for
     * fetching, saving, deleting data etc
     */
    public SQLiteDatabaseManager(Context context) {
        mContext = context;
        initDatabase();
        mRunningProcessCount = new AtomicInteger(0);

        mAccX = new ArrayList<>();
        mAccY = new ArrayList<>();
        mAccZ = new ArrayList<>();
        mAccTimestamp = new ArrayList<>();
        mGyroX = new ArrayList<>();
        mGyroY = new ArrayList<>();
        mGyroZ = new ArrayList<>();
        mGyroTimestamp = new ArrayList<>();
    }

    public static SQLiteDatabaseManager get(Context context) {
        if (mSqliteHelper == null) {
            mSqliteHelper = new SQLiteDatabaseManager(context);
        }
        return mSqliteHelper;
    }


    @Override
    public void initDatabase() {
        mHelper = new DatabaseHelper(mContext);
    }

    /**Adds new row to the database and sets id*/
    @Override
    public void startNewReading(Reading reading) {
        insertReadingToDB(reading);
    }

    /**Reset indexes and arrays for new reading*/
    @Override
    public void stopReading() {
        accIndex = 0;
        gyroIndex = 0;
        resetArrays();
        setEndTime(mReading.getId(), System.currentTimeMillis());

    }


    @Override
    public void saveData(List<Reading> readings) {

    }

    @Override
    public boolean deleteData(Reading reading) {
        if (reading != null) {
            return mHelper.deleteReading(reading.getId());
        }
        return false;
    }

    @Override
    public void deleteAll() {

    }

    @Override
    public void addData(int sensorType, float x, float y, float z, long timestamp) {

    }

    @Override
    public void addData(Message message) {
        Bundle sensorBundle;

        sensorBundle = message.getData();

        switch (message.arg1) {
            case SensorService.SENSOR_ACC:
//                Gyro is often 55 milliseconds behind acc on sony z3

                long time = sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP);

                mAccX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
                mAccY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
                mAccZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
                mAccTimestamp.add(time);
                accIndex++;

                if (accIndex == arraySize) {
                    new SensorAddingAsyncTask().execute(1, mAccTimestamp.size());
                    accIndex = 0;
                }
                break;

            case SensorService.SENSOR_GYRO:
//                Uncomment if gyro faster than acc
//                if (mAccTimestamp.size() == 0) {
//                    Log.d(TAG, "empty");
//                    break;
//                }
                long timeG = sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP);
                mGyroX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
                mGyroY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
                mGyroZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
                mGyroTimestamp.add(timeG - mAccTimestamp.get(0)); //current time - start time
                gyroIndex++;

                if (gyroIndex == arraySize) {
                    new SensorAddingAsyncTask().execute(2, mGyroTimestamp.size());
                    gyroIndex = 0;
                }
                break;
        }
    }

    /**
     * Save sensor values in arrays with id to database
     */
    public void addSensors(long id, float[] ax, float[] ay, float[] az, long[] at, float[] gx, float[] gy, float[] gz, long[] gt) {
        mHelper.bulkInsertSensorData(id, ax, ay, az, at, gx, gy, gz, gt);
    }


    /*TODO ENSURE THREAD SAFETY!!!!*/

    public void addSensors(int sensorId, long id, float[] x, float[] y, float[] z, long[] t) {
        mRunningProcessCount.incrementAndGet();
//        Log.d(TAG, sensorId + " " + System.currentTimeMillis());
        while (mRunningProcessCount.get() > 1) {
            try {
                Thread.sleep(10);
                Log.d(TAG, "Running threads " + mRunningProcessCount.get());

            } catch (InterruptedException e) {
                Log.e(TAG, "Thread sleeping error", e);
            }
        }
        mHelper.insertSensorData(sensorId, id, x, y, z, t);
        mRunningProcessCount.decrementAndGet();
    }

    public synchronized void addSensors(int sensorId, long id, List<Float> x, List<Float> y, List<Float> z, List<Long> t) {
        mRunningProcessCount.incrementAndGet();
//        Log.d(TAG, sensorId + " " + System.currentTimeMillis());
        while (mRunningProcessCount.get() > 1) {
            try {
                Thread.sleep(30);
                Log.d(TAG, "Running threads " + mRunningProcessCount.get());

            } catch (InterruptedException e) {
                Log.e(TAG, "Thread sleeping error", e);
            }
        }
        mHelper.insertSensorData(sensorId, id, x, y, z, t);
        mRunningProcessCount.decrementAndGet();

    }




    /**
     * Tell database to set end time
     */
    public boolean setEndTime(long id, long endTime) {
        return mHelper.setEndTime(id, endTime);
    }

    @Override
    public int getCount() {
        return mHelper.getDataCount();
    }

    /**
     * Returns list of Readings
     */
    @Override
    public List<Reading> getData() {
        DatabaseHelper.ReadingCursor wrappedData = mHelper.queryReadings();
        List<Reading> readings = new ArrayList<>();

        Log.d(TAG, "db size " + wrappedData.getCount() + " cursor position ");
        if (wrappedData.getCount() > 0) {
            for (int i = 0; i < wrappedData.getCount(); i++) {
                wrappedData.moveToNext();
                readings.add(wrappedData.getReading());
            }
        }
        return readings;
    }

    /**
     * Returns id of the reading stored in database
     */
    public long insertReadingToDB(Reading reading) {
        long id = mHelper.insertReading(reading);
        mReading = reading;
        mReading.setId(id);
        return id;
    }


    /**
     * Close data manager
     */
    @Override
    public void closeDataManager() {
        mHelper.close();
        Log.d(TAG, "Db closed");

    }

    /**
     * Export database for debugging
     */
    public void exportDb() {
        mHelper.exportDatabase(mContext);

    }

    /**
     * Debuggin, turn verbose on
     */
    public boolean switchVerbose() {
        return mHelper.switchVerbose();
    }

    /**
     * Initialize arrays
     */
    private void resetArrays() {
        mAccZ.clear();
        mAccX.clear();
        mAccY.clear();
        mAccTimestamp.clear();
        mGyroZ.clear();
        mGyroX.clear();
        mGyroY.clear();
        mGyroTimestamp.clear();
    }

    ////////ASYNC TASKS///////////

//    region
    /**
     * Async task that puts array values to tmp arrays which are sent to the database
     */
    private class SensorAddingAsyncTask extends AsyncTask<Integer, Integer, Integer> {

        private float[] tmpZarray;
        private float[] tmpXarray;
        private float[] tmpYarray;
        private long[] tmpTimestampArray;
        private float[] tmpGyroZarray;
        private float[] tmpGyroXarray;
        private float[] tmpGyroYarray;
        private long[] tmpGyroTimestampArray;
        private long readingId;

        public SensorAddingAsyncTask() {
            readingId = mReading.getId();
            tmpXarray = new float[arraySize];
            tmpYarray = new float[arraySize];
            tmpZarray = new float[arraySize];
            tmpTimestampArray = new long[arraySize];
        }


        @Override
        protected Integer doInBackground(Integer... params) {
            int sensorId = params[0];
            int lastEntryPoint = params[1];
//            if (sensorId == 1) {
//                System.arraycopy(mAccXarray, 0, tmpXarray, 0, arraySize);
//                System.arraycopy(mAccYarray, 0, tmpYarray, 0, arraySize);
//                System.arraycopy(mAccZarray, 0, tmpZarray, 0, arraySize);
//                System.arraycopy(mAccTimestampArray, 0, tmpTimestampArray, 0, arraySize);
//            } else {
//                System.arraycopy(mGyroXarray, 0, tmpXarray, 0, arraySize);
//                System.arraycopy(mGyroYarray, 0, tmpYarray, 0, arraySize);
//                System.arraycopy(mGyroZarray, 0, tmpZarray, 0, arraySize);
//                System.arraycopy(mGyroTimestampArray, 0, tmpTimestampArray, 0, arraySize);
//            }
            if (sensorId == 1) {
                int arrayIndex = 0;
                for (int i = lastEntryPoint - arraySize; i < lastEntryPoint; i++) {
                    tmpXarray[arrayIndex] = mAccX.get(i);
                    tmpYarray[arrayIndex] = mAccY.get(i);
                    tmpZarray[arrayIndex] = mAccZ.get(i);
                    tmpTimestampArray[arrayIndex] = mAccTimestamp.get(i) - mAccTimestamp.get(0);
                    arrayIndex++;
                }
                addSensors(sensorId, readingId, tmpXarray, tmpYarray, tmpZarray, tmpTimestampArray);

//                mDatabaseManager.addSensors(sensorId, readingId,
//                        mAccX.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mAccY.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mAccZ.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mAccTimestamp.subList(lastEntryPoint - arraySize, lastEntryPoint));
            } else {
                int arrayIndex = 0;
                for (int i = lastEntryPoint - arraySize; i < lastEntryPoint; i++) {
                    tmpXarray[arrayIndex] = mGyroX.get(i);
                    tmpYarray[arrayIndex] = mGyroY.get(i);
                    tmpZarray[arrayIndex] = mGyroZ.get(i);
                    tmpTimestampArray[arrayIndex] = mGyroTimestamp.get(i);
                    arrayIndex++;
                }
                addSensors(sensorId, readingId, tmpXarray, tmpYarray, tmpZarray, tmpTimestampArray);
            }

//                mDatabaseManager.addSensors(sensorId, readingId,
//                        mGyroX.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mGyroY.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mGyroZ.subList(lastEntryPoint - arraySize, lastEntryPoint),
//                        mGyroTimestamp.subList(lastEntryPoint - arraySize, lastEntryPoint));
//            }

//            mDatabaseManager.addSensors(sensorId, readingId, tmpXarray, tmpYarray, tmpZarray, tmpTimestampArray);
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }



//    endregion
}


