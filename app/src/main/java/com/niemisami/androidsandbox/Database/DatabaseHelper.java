package com.niemisami.androidsandbox.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.niemisami.androidsandbox.Reading.Reading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

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
    private static final String TABLE_SENSOR_ACC = "sensor_acc";
    private static final String TABLE_SENSOR_GYRO = "sensor_gyro";
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
//        db.execSQL("COMMIT; PRAGMA synchronous=OFF; BEGIN TRANSACTION");
        db.execSQL("CREATE TABLE " + TABLE_READING_INFO + " (" +
                READING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PATIENT_NAME + " VARCHAR(50) DEFAULT 'Not provided'," +
                READING_NOTES + " VARCHAR(100) DEFAULT 'Not provided'," +
                READING_START_TIME + " INTEGER," +
                READING_END_TIME + " INTEGER," +
                READING_FILE_NAME + " VARCHAR(30))");
//        Create "sensor data" table
//        db.execSQL("CREATE TABLE " + TABLE_SENSOR_DATA + "(" +
////                Next line sets info tables id to foreign key
//                SENSOR_READING_ID + " INTEGER REFERENCES " + TABLE_READING_INFO + "(" + READING_ID + ")," +
//                READING_SENSOR_TYPE + " INTEGER," +
//                READING_X + " REAL," +
//                READING_Y + " REAL," +
//                READING_Z + " REAL," +
//                READING_TIMESTAMP + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_SENSOR_ACC + "(" +
//                Next line sets info tables id to foreign key
                SENSOR_READING_ID + " INTEGER REFERENCES " + TABLE_READING_INFO + "(" + READING_ID + ") NOT NULL," +
                READING_X + " REAL," +
                READING_Y + " REAL," +
                READING_Z + " REAL," +
                READING_TIMESTAMP + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_SENSOR_GYRO + "(" +
//                Next line sets info tables id to foreign key
                SENSOR_READING_ID + " INTEGER REFERENCES " + TABLE_READING_INFO + "(" + READING_ID + ") NOT NULL," +
                READING_X + " REAL," +
                READING_Y + " REAL," +
                READING_Z + " REAL," +
                READING_TIMESTAMP + " INTEGER)");
        Log.i(TAG, "Databases created in " + (System.nanoTime() - start) + "ns");

    }

    @Override
    public synchronized void close() {
        super.close();
        Log.d(TAG, "DatabaseHelper closed");
    }

    //////////Inserting data/////////////

    /**
     * When new reading is started insert new row of readings information to the database.
     * Returns id of the new reading
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

    public long bulkInsertSensorData(long readingId, float[] ax, float[] ay, float[] az, long[] at, float[] gx, float[] gy, float[] gz, long[] gt) {

        SQLiteDatabase db = getWritableDatabase();
        try {
            long start = System.currentTimeMillis();
            String sqlQuery = "INSERT INTO " + TABLE_SENSOR_ACC + " (" + SENSOR_READING_ID + "," +  READING_X + ',' + READING_Y + ',' + READING_Z + ',' + READING_TIMESTAMP + ") VALUES (?,?,?,?,?);";
            SQLiteStatement stmt = db.compileStatement(sqlQuery);
            db.beginTransaction();
            for (int i = 0; i < at.length; i++) {
                stmt.bindLong(1, readingId);
                stmt.bindDouble(2, ((double) ax[i]));
                stmt.bindDouble(3, ((double) ay[i]));
                stmt.bindDouble(4, ((double) az[i]));
                stmt.bindLong(5, at[i]);
                stmt.execute();
                stmt.clearBindings();
            }
        db.setTransactionSuccessful();
        db.endTransaction();


            sqlQuery = "INSERT INTO " + TABLE_SENSOR_GYRO + " (" + SENSOR_READING_ID + "," + READING_X + ',' + READING_Y + ',' + READING_Z + ',' + READING_TIMESTAMP + ") VALUES (?,?,?,?,?);";
            stmt = db.compileStatement(sqlQuery);
            db.beginTransaction();
            for (int i = 0; i < at.length; i++) {
                stmt.bindLong(1, readingId);
                stmt.bindDouble(2, ((double) gx[i]));
                stmt.bindDouble(3, ((double) gy[i]));
                stmt.bindDouble(4, ((double) gz[i]));
                stmt.bindLong(5, gt[i]);
                stmt.execute();
                stmt.clearBindings();
            }
            db.setTransactionSuccessful();

            Log.d(TAG, (System.currentTimeMillis() - start) + "");
            db.endTransaction();

        } catch (Exception e) {
            Log.e(TAG, "Error inserting sensor data to database", e);
        }
        return 1;

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


    //////EXPORTING DATABASE////////
//    region


    public void exportDatabase(Context context) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/BackupFolder");
        directory.mkdirs();

        try {
            File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String  currentDBPath= "//data//" + "com.niemisami.androidsandbox"
                        + "//databases//" + DB_NAME;
                String backupDBPath  = "/BackupFolder/" + DB_NAME + ".sqlite";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Log.d(TAG, "Exported db successfully");

                String[] path = new String[]{backupDB.getAbsolutePath()};
                MediaScannerConnection.scanFile(context, path, null, null);
            }
        } catch (Exception e) {

            Log.e(TAG, "Error exporting database to file", e);

        }

    }

//    endregion
}