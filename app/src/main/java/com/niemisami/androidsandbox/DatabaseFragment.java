package com.niemisami.androidsandbox;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.niemisami.androidsandbox.Database.SQLiteDatabaseManager;
import com.niemisami.androidsandbox.Reading.Reading;
import com.niemisami.androidsandbox.Reading.ReadingManager;
import com.niemisami.androidsandbox.services.SensorService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class DatabaseFragment extends Fragment implements Stopwatch.StopwatchListener {
    final private String TAG = "DatabaseFragment";

    private Toolbar mToolBar;
    private ServiceMessageHandler mHandler;

    private Intent mServiceIntent;


    //    Sensor data values
    private List<Float> mAccZ;
    private List<Float> mAccX;
    private List<Float> mAccY;
    private List<Long> mAccTimestamp;
    private List<Float> mGyroZ;
    private List<Float> mGyroX;
    private List<Float> mGyroY;
    private List<Long> mGyroTimestamp;


    private final int arraySize = 100;
    private float[] mAccZarray = new float[arraySize];
    private float[] mAccXarray = new float[arraySize];
    private float[] mAccYarray = new float[arraySize];
    private long[] mAccTimestampArray = new long[arraySize];
    private float[] mGyroZarray = new float[arraySize];
    private float[] mGyroXarray = new float[arraySize];
    private float[] mGyroYarray = new float[arraySize];
    private long[] mGyroTimestampArray = new long[arraySize];

    private boolean mSensorsRunning = false;

    private Button mSaveReadingInfoButton, mStartStopSensorsButton;
    private TextView mVerboseTextView;
    private EditText mNameEditText, mInfoEditText;
    private ScrollView mVerboseScrollView;
    private Stopwatch mStopwatch;
    private FrameLayout mStopwatchLayout;

    //    Stopwatch animation
    private Animation mSlideDown;
    private Animation mSlideUp;


    private Reading mReading;

    private SQLiteDatabaseManager mDatabaseManager;


    //    Reading manager to take care of sqlite and reading information
    private ReadingManager mReadingManager;


    public DatabaseFragment() {
    }


    /////////FRAGMENT LIFECYCLE METHODS////////
//  region
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_database, container, false);

//        Find toolbar from the view and set it to support actionbar
//        This can be set into the activity if menu items doesn't change even if fragments does
        setHasOptionsMenu(true);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        mToolBar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolBar.setTitle(TAG);
        appCompatActivity.setSupportActionBar(mToolBar);

        initView(view);

        return view;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        mReadingManager = ReadingManager.get(getActivity());

        mAccX = new ArrayList<>();
        mAccY = new ArrayList<>();
        mAccZ = new ArrayList<>();
        mAccTimestamp = new ArrayList<>();
        mGyroX = new ArrayList<>();
        mGyroY = new ArrayList<>();
        mGyroZ = new ArrayList<>();
        mGyroTimestamp = new ArrayList<>();

        if (mHandler == null) {
            mHandler = new ServiceMessageHandler(this);
        } else {
            mHandler.setTarget(this);
        }


        //Get manager that creates new database connection if not initialized
        mDatabaseManager = SQLiteDatabaseManager.get(getActivity());

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabaseManager.closeDataManager();
        Log.d(TAG, "Db frag destroyed");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_verbose:
                boolean verboseState = mDatabaseManager.switchVerbose();
                item.setTitle(verboseState ? "Turn verbose off" : "Turn verbose on");
                return true;
        }
        return false;

    }


    //    Create material design Toolbar as menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.db_menu, menu);
    }

//    endregion


    /////VIEW INITIALIZATION AND ANIMATIONS////


//    region

    long start;

    private void initView(View view) {

        mSaveReadingInfoButton = (Button) view.findViewById(R.id.save_information_button);
        mSaveReadingInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start = System.currentTimeMillis();
                final boolean result = mDatabaseManager.deleteData(mReading);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mVerboseTextView.append("Removed data " + result + "\n");

                    }
                });
                mDatabaseManager.exportDb();
//                saveReadingInformation();

            }
        });
        mStartStopSensorsButton = (Button) view.findViewById(R.id.start_sensor_button);
        mStartStopSensorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSensorsRunning) {
                    mStopwatch.stop();
                    mSensorsRunning = !mSensorsRunning;
                } else {
//                    Start stopwatch and listen onTimeZero listener which starts service
                    mStopwatch.start();
                    mSensorsRunning = !mSensorsRunning;
                }
                mStartStopSensorsButton.setText((mSensorsRunning ? "Stop" : "Start"));
                printDBData();
            }
        });

        mVerboseTextView = (TextView) view.findViewById(R.id.verbose_box);
        mVerboseTextView.setText("Events:\n");

        mStopwatch = new Stopwatch((TextView) view.findViewById(R.id.stopwatch_view));
        mStopwatch.setStopwatchListener(this);

        mInfoEditText = (EditText) view.findViewById(R.id.info_text_box);
        mNameEditText = (EditText) view.findViewById(R.id.name_text_box);

        mVerboseScrollView = (ScrollView) view.findViewById(R.id.verbose_scroll);

        mStopwatchLayout = (FrameLayout) view.findViewById(R.id.stopwatch_frame);
//        initStopwatchAnimation();
    }


    /**
     * Animation making clock watch appear from the top of the view. NOT IN USE
     */
    private void initStopwatchAnimation() {
        mSlideDown = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
        mSlideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mStopwatchLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mSlideUp = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
        mSlideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mStopwatchLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void showStopwatch() {
        mStopwatchLayout.startAnimation(mSlideDown);
    }

    private void hideStopwatch() {
        mStopwatchLayout.startAnimation(mSlideUp);
    }


//    endregion

    private void stopSensorService() {
        if (mServiceIntent != null) {
            getActivity().stopService(mServiceIntent);
        }
    }


    /**
     * Parse information about the user and save it to the database
     */
    private void saveReadingInformation() {
        String name = mNameEditText.getText().toString().trim();
        String notes = mInfoEditText.getText().toString().trim();
        mVerboseTextView.append("information about: " + name + " saved\n");
        if (name.length() > 0 && notes.length() > 0) {
            mReading = new Reading(name, notes);
        } else if (name.length() > 0 && notes.length() == 0) {
            mReading = new Reading(name, "Not provided");
        } else if (name.length() == 0 && notes.length() > 0) {
            mReading = new Reading("Not provided", notes);
        } else {
            mReading = new Reading();
        }

        new DatabaseAsyncTask().execute(mReading);


    }

    private void printDBData() {
        List<Reading> readingList = mDatabaseManager.getData();

        for (Reading r : readingList) {
            mVerboseTextView.append("data: " + r.getId() + " name: " + r.getPatientName() + " time: " + r.getStartTime() + "\n");
        }

        printSensorStatus();
        mVerboseScrollView.post(new Runnable() {
            @Override
            public void run() {
                mVerboseScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void printSensorStatus() {
        mVerboseTextView.append("is sensors running " + mSensorsRunning + "\n");
    }

//    endregion


    /////SQLITE TASK AND METHODS//////


    private class DatabaseAsyncTask extends AsyncTask<Reading, Integer, Integer> {
        @Override
        protected Integer doInBackground(Reading... params) {
            mReading.setId(mDatabaseManager.insertReadingToDB(params[0]));
//            mDatabaseManager.exportDb();

//            Log.d(TAG, "Saved " + (System.currentTimeMillis() - start));
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    /**
     * Async task that puts array values to tmp arrays which are sent to the database
     */
    private class SensorAddingAsyncTask extends AsyncTask<Long, Integer, Integer> {

        private float[] tmpAccZarray;
        private float[] tmpAccXarray;
        private float[] tmpAccYarray;
        private long[] tmpAccTimestampArray;
        private float[] tmpGyroZarray;
        private float[] tmpGyroXarray;
        private float[] tmpGyroYarray;
        private long[] tmpGyroTimestampArray;
        private long readingId;

        @Override
        protected Integer doInBackground(Long... params) {
            mDatabaseManager.addSensors(readingId, tmpAccXarray, tmpAccYarray, tmpAccZarray, tmpAccTimestampArray, tmpGyroXarray, tmpGyroYarray, tmpGyroZarray, tmpGyroTimestampArray);
            return null;
        }

        /**
         * Check if reading has id, unles don't perform async task
         */
        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

        }

        @Override
        protected void onPreExecute() {
            tmpAccZarray = mAccXarray;
            tmpAccXarray = mAccXarray;
            tmpAccYarray = mAccXarray;
            tmpAccTimestampArray = mAccTimestampArray;
            tmpGyroZarray = mGyroXarray;
            tmpGyroXarray = mGyroXarray;
            tmpGyroYarray = mGyroXarray;
            tmpGyroTimestampArray = mGyroTimestampArray;
            readingId = mReading.getId();
            if (readingId != -1) {
                super.onPreExecute();
            }
        }
    }


    /////SENSOR SERVICE METHODS///////

    private void startSensorService() {
        Messenger mMessenger = new Messenger(mHandler);
        mServiceIntent = new Intent(getActivity(), SensorService.class);
        mServiceIntent.putExtra("MESSENGER", mMessenger);
        saveReadingInformation();
        getActivity().startService(mServiceIntent);
    }

    public static class ServiceMessageHandler extends Handler {

        private static final String TAG = "ServiceMessageHandler";
        private WeakReference<DatabaseFragment> mFragmentReference;

        public ServiceMessageHandler(DatabaseFragment fragment) {
            mFragmentReference = new WeakReference<>(fragment);
        }

        public void setTarget(DatabaseFragment fragment) {
            mFragmentReference.clear();
            mFragmentReference = new WeakReference<>(fragment);
        }

        public void handleMessage(Message msg) {

            Bundle sensorBundle;
//            if (getActivity() != null) {

            DatabaseFragment fragment = mFragmentReference.get();
            switch (msg.arg1) {
                case SensorService.START_SENSORS:
                    Toast.makeText(fragment.getActivity().getApplicationContext(), "Sensors are running", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case SensorService.STOP_SENSORS:
                    Bundle data = msg.getData();
//                        int readingAmount = data.getInt(SensorService.SENSOR_VALUES);
                    Log.d(TAG, "Sensors stopped with " + mFragmentReference.get().getReadingCount() + " reading");
//                    Toast.makeText(fragment.getActivity().getApplicationContext(), "Sensors stopped with " + mFragmentReference.get().getReadingCount() + " reading", Toast.LENGTH_SHORT)
//                            .show();
                    break;
                case SensorService.SENSOR_ERROR:
                    Toast.makeText(fragment.getActivity().getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case SensorService.SENSOR_ACC:
                case SensorService.SENSOR_GYRO:
                    fragment.appendSensorDataArray(msg);
                    break;
            }

        }

//        }
    }

    private int accIndex;
    private int gyroIndex;

    public void appendSensorDataArray(Message message) {
        Bundle sensorBundle;

        sensorBundle = message.getData();
        float x = sensorBundle.getFloat(SensorService.SENSOR_X);
        float y = sensorBundle.getFloat(SensorService.SENSOR_Y);
        float z = sensorBundle.getFloat(SensorService.SENSOR_Z);
        long time = sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP);

        switch (message.arg1) {

            case SensorService.SENSOR_ACC:

                mAccXarray[accIndex] = x;
                mAccYarray[accIndex] = y;
                mAccZarray[accIndex] = z;
                mAccTimestampArray[accIndex] = time;
                accIndex++;
//                mAccX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
//                mAccY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
//                mAccZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
//                mAccTimestamp.add(sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP));

                break;
            case SensorService.SENSOR_GYRO:

                mGyroXarray[gyroIndex] = x;
                mGyroYarray[gyroIndex] = y;
                mGyroZarray[gyroIndex] = z;
                mGyroTimestampArray[gyroIndex] = time;
                gyroIndex++;
//                mGyroX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
//                mGyroY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
//                mGyroZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
//                mGyroTimestamp.add(sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP));
                break;
        }
        if (accIndex == arraySize || gyroIndex == arraySize) {
            new SensorAddingAsyncTask().execute();
            accIndex = 0;
            gyroIndex = 0;
        }
    }

    public int getReadingCount() {
        return mAccTimestamp.size();
    }

    @Override
    public void onStartStopwatch() {
        Toast.makeText(getActivity().getApplicationContext(), "Watch started", Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Stop sensor service
     */
    @Override
    public void onStopStopwatch() {
        stopSensorService();
//        Toast.makeText(getActivity().getApplicationContext(), "Stopped at " + mStopwatch.getTimeInSeconds(), Toast.LENGTH_SHORT)
//                .show();
    }

    @Override
    public void onTimeZero() {
        startSensorService();
    }
}

