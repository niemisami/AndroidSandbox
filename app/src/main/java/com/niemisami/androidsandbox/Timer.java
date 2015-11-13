package com.niemisami.androidsandbox;

import android.widget.TextView;

import android.os.Handler;


/**
 * Created by sakrnie on 13.11.2015.
 */
public class Stopwatch interface Runnable {

    private TextView mTimeView;
    private long mStart, mCurrentTime;

    private Handler mHandler;


    public Stopwatch(TextView view) {

        mTimeView = view;
    }

    public void startTimer() {
        mStart = System.currentTimeMillis();

    }
    public void stopTimer() {

    }
    @Override
    public void run() {
        updateTime();
    }

    private void updateTime() {
        mCurrentTime = System.currentTimeMillis() - mStart;

        int sec = (int) (mCurrentTime / 1000);
        int mins = sec / 60;
        sec = sec % 60;

        mTimeView.setText(mins + ":" + String.format("%02d", sec));
        mHandler
    }


    public interface StopwatchListener {
        void onStop();
        void onStart();
        void onTimerValueChange();
    }

}
