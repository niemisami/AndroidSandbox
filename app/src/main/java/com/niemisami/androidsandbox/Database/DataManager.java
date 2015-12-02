package com.niemisami.androidsandbox.Database;

import android.os.Message;

import com.niemisami.androidsandbox.Reading.Reading;

import java.util.List;

/**
 * Created by sakrnie on 9.9.2015.
 */
public interface DataManager {

    /**Used for managing data.Saving and fetching data from the storage.
     * Removing or adding data*/

    /**Make all necessary initializations for the storage*/
    void initDatabase();

    /**Start new reading that will be saved to the storage*/
    void startNewReading(Reading reading);

    /**Stop running reading*/
    void stopReading();

    /**Save all the data to the storage*/
    void saveData(List<Reading> readings);

    /**Fetch all data from the storage*/
    List<Reading> getData();

    /**Delete data from certain index*/
    boolean deleteData(Reading reading);

    /**Delete all data from the storage*/
    void deleteAll();

    /**Add sensor data right away to the storage*/
    void addData(int sensorType, float x, float y ,float z, long timestamp);

    void addData(Message message);

    /**Return count of all readings*/
    int getCount();

    /**Close storage*/
    void closeDataManager();


}
