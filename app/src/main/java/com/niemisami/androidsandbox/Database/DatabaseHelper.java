package com.niemisami.androidsandbox.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.niemisami.androidsandbox.Reading.Reading;

/**
 * Created by sakrnie on 9.11.2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper{

    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "readings_sqlite";
    private static final int DB_VERSION = 1;


    //    Table containing information about the reading, patient name, info etc
    private static final String TABLE_READING_INFO = "reading_information";
    private static final String READING_ID = "_id";
    private static final String PATIENT_NAME = "patient_name";
    private static final String READING_NOTES = "reading_notes";
    private static final String READING_START_TIME = "start_time";
    private static final String READING_END_TIME = "end_time";
    private static final String READING_FILE_NAME = "filename";

    //    Table containing sensor data
    private static final String TABLE_SENSOR_DATA = "sensor_data";
    //TODO is it better to have two tables, one for acc other for gyro?
    private static final String SENSOR_READING_ID = "reading_id";
    private static final String READING_SENSOR_TYPE = "sensor_type"; // sensor type can be Acc (1) or Gyro (2)
    private static final String READING_X = "x_val";
    private static final String READING_Y = "y_val";
    private static final String READING_Z = "z_val";
    private static final String READING_TIMESTAMP = "timestamp";


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    /*Called when upgrading the version number of the database*/
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Updating database from " + oldVersion + " to " + newVersion);
//        Drop all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READING_INFO);
//        Create database again
        onCreate(db);
    }

    /*Database structure:               TABLE: reading_information
    *
    *      ----------------------------------------------------------------------------------------
    *      | _id | patient_name | reading_notes |  start_time |  end_time | sensor_count | filename|
    *      ----------------------------------------------------------------------------------------
    *
    *
    *                                     TABLE: sensor_data
    *            ------------------------------------------------------------------------
    *            | reading_id (_id) | sensor_type | x_val | y_val |  z_val |  timestamp |
    *            ------------------------------------------------------------------------
    */

    @Override
    public void onCreate(SQLiteDatabase db) {

        long start = System.nanoTime();

//        Create "Reading information" table
        db.execSQL("CREATE TABLE " + TABLE_READING_INFO + " (" +
                READING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PATIENT_NAME + " VARCHAR(50) DEFAULT 'Not provided'," +
                READING_NOTES + " VARCHAR(100) DEFAULT 'Not provided'," +
                READING_START_TIME + " INTEGER," +
                READING_END_TIME + " INTEGER," +
                READING_FILE_NAME + " VARCHAR(30))");
//        Create "sensor data" table
        db.execSQL("CREATE TABLE " + TABLE_SENSOR_DATA + "(" +
//                Next line sets info tables id to foreign key
                SENSOR_READING_ID + " INTEGER REFERENCES " + TABLE_READING_INFO + "(" + READING_ID + ")," +
                READING_SENSOR_TYPE + " INTEGER," +
                READING_X + " REAL," +
                READING_Y + " REAL," +
                READING_Z + " REAL," +
                READING_TIMESTAMP + " INTEGER)");
        Log.i(TAG, "Databases created in " + (System.nanoTime() - start) + "ns");

        db.execSQL("PRAGMA synchronous=FULL");
    }

    //////////Inserting data/////////////

    /**
     * When new reading is started insert new row of readings information to the database.
     * Returns id of the reading added
     */
    public long insertReading(Reading reading) {

        ContentValues cv = new ContentValues();
        cv.put(PATIENT_NAME, reading.getPatientName());
        cv.put(READING_NOTES, reading.getReadingNotes());
        cv.put(READING_START_TIME, reading.getStartTime());
        cv.put(READING_FILE_NAME, reading.getFileName());
        return getWritableDatabase().insert(TABLE_READING_INFO, null, cv);
    }



    public long insertSensorData(int id, float x, float y, float z, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(READING_SENSOR_TYPE, id);
        cv.put(READING_X, x);
        cv.put(READING_Y, y);
        cv.put(READING_Z, z);
        cv.put(READING_TIMESTAMP, timestamp);
//        Log.d(TAG, "" + x + " " + y + " " + z + " " + timestamp);

        return getWritableDatabase().insert(TABLE_SENSOR_DATA, null, cv);
    }

//        TODO jatka nopeustusta/////////////////////////
    public long fastInsertSensor(int count) {

        String sql ="INSERT " + TABLE_READING_INFO + " (id, x, y, ,z time) VALUES ( ?,?,?,?)"
    }

    /////////Querying data////////////

    /**
     * Returns all readings from the table reading_information
     */
    public ReadingCursor queryReadings() {
        // Same to query SELECT * FROM reading_information. Organize readings by start time
        Cursor wrapper = getReadableDatabase().query(TABLE_READING_INFO,
                null, null, null, null, null, READING_START_TIME + " asc");
        return new ReadingCursor(wrapper);
    }

    public int getCount() {
        Cursor cursor = getReadableDatabase().query(TABLE_READING_INFO,
                null, null, null, null, null, null);
        return cursor.getCount();
    }

    /**
     * Returns count of gyro and acc rows
     */
    public int getDataCount() {
        Cursor cursor = getReadableDatabase().query(TABLE_SENSOR_DATA,
                null, null, null, null, null, null);
        return cursor.getCount();
    }

    /**
     * A convenient class to cursors that returns Readings info.
     * The [@Link getReading()} method returns data from the row where cursor is
     */
    public static class ReadingCursor extends CursorWrapper {

        public ReadingCursor(Cursor c) {
            super(c);
        }

        /**
         * Retuns a Reading object for the current row, or null if invalid
         */
        public Reading getReading() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }
            Reading reading = new Reading();
            long readingId = getLong(getColumnIndex(READING_ID));
            String patientName = getString(getColumnIndex(PATIENT_NAME));
            String readingNotes = getString(getColumnIndex(READING_NOTES));
            Long readingStartTime = getLong(getColumnIndex(READING_START_TIME));
            Long readingEndTime = getLong(getColumnIndex(READING_END_TIME));
            String readingFile = "";

            reading.setId(readingId);
            reading.setPatientName(patientName);
            reading.setReadingNotes(readingNotes);
            reading.setStartTime(readingStartTime);
            reading.setEndTime(readingEndTime);
            reading.setFileName(readingFile);

            return reading;
        }

    }
}