package com.niemisami.androidsandbox.Database;

import android.content.Context;
import android.hardware.SensorEvent;
import android.util.Log;

import com.niemisami.androidsandbox.Reading.Reading;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sakrnie on 9.9.2015.
 * SQLiteDatabaseManger takes care of saving and fetching data from SQLite db
 */
public class SQLiteDatabaseManager implements DataManager {

    private final String TAG = "SQLiteDatabaseManager";


    private Context mContext;
    private DatabaseHelper mHelper;
    private static SQLiteDatabaseManager mSqliteHelper;



    /**SQLiteDatabaseManager handles SQLite database and offers methods for
     * fetching, saving, deleting data etc*/
    public SQLiteDatabaseManager(Context context) {
        mContext = context;
        initDatabase();
    }

    public static SQLiteDatabaseManager get(Context context) {
        if(mSqliteHelper == null) {
            mSqliteHelper = new SQLiteDatabaseManager(context);
        }
        return mSqliteHelper;
    }


    @Override
    public void initDatabase() {
        mHelper = new DatabaseHelper(mContext);
    }

    @Override
    public void startNewReading(Reading reading) {
//       Adds new row to the database
        insertReadingToDB(reading);
    }

    @Override
    public void stopReading() {

    }


    @Override
    public void saveData(List<Reading> readings) {

    }

    @Override
    public boolean deleteData(Reading reading) {
        if(reading != null) {
            return mHelper.deleteReading(reading.getId());
        }
        return false;
    }

    @Override
    public void deleteAll() {

    }

    @Override
    public void addData(int sensorId, SensorEvent event) {
//        mHelper.insertSensorData(sensorId, event.values[0], event.values[1], event.values[2], event.timestamp);
    }

    /**Save sensor values in arrays with id to database*/
    public void addSensors(long id, float[] ax, float[] ay, float[] az, long[] at, float[] gx, float[] gy, float[] gz, long[] gt) {
        mHelper.bulkInsertSensorData(id, ax, ay, az, at, gx, gy, gz, gt);
    }

    /**Tell database to set end time*/
    public boolean setEndTime(long id, long endTime){
        return mHelper.setEndTime(id, endTime);
    }

    @Override
    public int getCount() {
        return mHelper.getDataCount();
    }

    /**Returns list of Readings*/
    @Override
    public List<Reading> getData() {
        DatabaseHelper.ReadingCursor wrappedData = mHelper.queryReadings();
        List<Reading> readings = new ArrayList<>();
        Log.d(TAG, "db size " + wrappedData.getCount() + " cursor position ");
        if(wrappedData.getCount() > 0) {
            for (int i = 0; i < wrappedData.getCount(); i++) {
                wrappedData.moveToNext();
                readings.add(wrappedData.getReading());
            }
        }
        return readings;
    }

    /**Returns id of the reading stored in database*/
    public long insertReadingToDB(Reading reading) {
         return mHelper.insertReading(reading);
    }


    /**Close data manager*/
    @Override
    public void closeDataManager() {
        mHelper.close();
        Log.d(TAG, "Db closed");

    }

    /**Export database for debugging*/
    public void exportDb() {
        mHelper.exportDatabase(mContext);

    }

    /**Debuggin, turn verbose on*/
    public boolean switchVerbose() {
        return mHelper.switchVerbose();
    }
}
