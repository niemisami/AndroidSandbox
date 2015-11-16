package com.niemisami.androidsandbox;

import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

/**
 * Created by smjoke on 9.3.2015.
 */
public class Stopwatch {

    private long startTime;
    private long elapsedTime;
    private Handler mHandler = new Handler();
    private final int REFRESH_RATE = 100;
    private String minutes, seconds, milliseconds;
    private long secs, mins, msecs;
    private boolean stopped;
    TextView tw;
    private StopwatchListener mStopwatchListener;

    /**
     * Consturcts new stopwatch.
     *
     * @param tw TextView element to bind the stopwatch
     */
    public Stopwatch(TextView tw) {

        Spannable spannableTime = new SpannableString("-00:00");
        spannableTime.setSpan(new ForegroundColorSpan(Color.parseColor("#dddddd")), 0,1,Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        tw.setText(spannableTime);
        this.tw = tw;
    }

    /**
     * Starts the clock
     */
    public void start() {
        mStopwatchListener.onStartNegative();
        startTime = System.currentTimeMillis() + 5000;

//        startTime = System.currentTimeMillis();
        mHandler.removeCallbacks(startTimer);
        mHandler.postDelayed(startTimer, 0);
    }

    /**
     * Stops the clock
     */
    public void stop() {
        mStopwatchListener.onStop();
        mHandler.removeCallbacks(startTimer);
        stopped = true;
    }

    /**
     * Resets the clock
     */
    public void reset() {
        stopped = false;
    }

    /**
     * Returns the time in string
     *
     * @return elapsed time
     */
    public String getTime() {

        if (mins < 0 || secs < 0) {
            return "-" + minutes + ":" + seconds;
        } else {
            return "-" + minutes + ":" + seconds;
        }
    }

    /**
     * Returns the time in seconds (long)
     *
     * @return elapsed time
     */
    public long getTimeInSeconds() {
        return (secs + (60 * mins));
    }

    /**
     * Updates the values
     *
     * @param time
     */
    private void updateTimer(long time) {
        secs = (long) (time / 1000);
        mins = (long) ((time / 1000) / 60);

        secs = secs % 60;
        seconds = String.valueOf(Math.abs(secs));
        if (secs == 0) {
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
        }

        if (secs > -10 && secs < 0)
            seconds = "0" + seconds;

        mins = mins % 60;
        minutes = String.valueOf(Math.abs(mins));
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        }

        Spannable spannableTime = new SpannableString(this.getTime());
        if (mins >= 0 && secs >= 0) {
            spannableTime.setSpan(new ForegroundColorSpan(Color.parseColor("#dddddd")), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        tw.setText(spannableTime);
    }

    /**
     * Runs the time
     */
    private Runnable startTimer = new Runnable() {
        @Override
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
//            This ensures that timer doesn't calculate two zeros
            if(elapsedTime < 0) {
                updateTimer(elapsedTime - 1000);
                mHandler.postDelayed(this, REFRESH_RATE);

            } else {
                mStopwatchListener.onStart();
                updateTimer(elapsedTime);
                mHandler.postDelayed(this, REFRESH_RATE);
            }
        }
    };

    public void setStopwatchListener(StopwatchListener stopwatchListener) {
        this.mStopwatchListener = stopwatchListener;
    }

    public interface StopwatchListener {
        void onStop();
        void onStartNegative();
        void onStart();
        void onTimerValueChange();
    }

}
