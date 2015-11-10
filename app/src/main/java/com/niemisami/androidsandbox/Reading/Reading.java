package com.niemisami.androidsandbox.Reading;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by sakrnie on 8.9.2015.
 * Reading class includes basic information about the reading
 */
public class Reading {

    private long mId;
    private String mFileName, mPatientName, mReadingNotes;
    private long mStartTime, mEndTime, mReadingDuration;
    private String mReadingDate;
    private List<Float> mAccXValues,mAccYValues,mAccZValues, mAccTimestampValues;
    private List<Float> mGyroXValues,mGyroYValues,mGyroZValues, mGyroTimestampValues;


    public Reading(String name, String notes){
        mId = -1;
        mPatientName = name;
        mReadingNotes = notes;
        mStartTime = System.currentTimeMillis();
        mReadingDuration = -1l;
        createReadingDate();
    }

    public Reading() {
        this("Not provided" , "Not provided");
    }

//    region    Getters and Setters (region)
    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public String getPatientName() {
        return mPatientName;
    }

    public void setPatientName(String mPatientName) {
        this.mPatientName = mPatientName;
    }

    public String getReadingNotes() {
        return mReadingNotes;
    }

    public void setReadingNotes(String mReadingNotes) {
        this.mReadingNotes = mReadingNotes;
    }


    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long mStartTime) {
        this.mStartTime = mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(long mEndTime) {
        this.mEndTime = mEndTime;
    }

    public long getReadingDuration() {
        if(mReadingDuration <= 0) {
            mReadingDuration = mEndTime - mStartTime;
        }
        return mReadingDuration;
    }

    public String getReadingDurationPretty() {
        return "00:00:00";
    }

    public void setReadingDuration(long mReadingDuration) {
        this.mReadingDuration = mReadingDuration;
    }

    public String getReadingDate() {
        return mReadingDate;
    }

    public void setReadingDate(String mReadingDate) {
        this.mReadingDate = mReadingDate;
    }

//    endregion


    private void startReading() {
        mStartTime = System.currentTimeMillis();
        createReadingDate();
    }

    private void stopReading() {
        mEndTime = System.currentTimeMillis();
        mReadingDuration = mEndTime - mStartTime;
    }

    /**Convert start time to human readable format and sets it to filename*/
    private void createReadingDate() {
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss", Locale.ENGLISH);
        Date date = new Date(mStartTime);
        mReadingDate = date.toString();
    }

    public static String formatDuration(int durationSeconds) {
        int seconds = durationSeconds % 60;
        int minutes = ((durationSeconds - seconds) / 60) % 60;
        int hours = (durationSeconds - (minutes * 60) - seconds) / 3600;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
