package com.niemisami.androidsandbox;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class DatabaseFragment extends Fragment implements Stopwatch.StopwatchListener {
    /////VARIABLES/////
//    region
    final private String TAG = "DatabaseFragment";

    private Toolbar mToolBar;
    private ServiceMessageHandler mHandler;

    private Intent mServiceIntent;

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

    //    Thread making short database queries and injections
    private Looper mDbLooper;
    private Handler mDbHandler;


    private Reading mReading;

    private SQLiteDatabaseManager mDatabaseManager;

//    endregion


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

        if (mHandler == null) {
            mHandler = new ServiceMessageHandler(this);
        } else {
            mHandler.setTarget(this);
        }

        initDbUpdateThread();

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
        stopDatabaseThread();
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
            case R.id.action_export_db:
                mDatabaseManager.exportDb();
                appendVerboseView("Database exported");
        }
        return false;

    }

    private void appendVerboseView(String message) {
        message = message + "\n";
        mVerboseTextView.append(message);
        mVerboseScrollView.post(new Runnable() {
            @Override
            public void run() {
                mVerboseScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    //    Create material design Toolbar as menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.db_menu, menu);
    }

//    endregion


    /////VIEW INITIALIZATION, ANIMATIONS AND UI METHOD////
//    region

    private void initView(View view) {

        mSaveReadingInfoButton = (Button) view.findViewById(R.id.save_information_button);
        mSaveReadingInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean result = mDatabaseManager.deleteData(mReading);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendVerboseView("Removed data " + result);
                    }
                });
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

    private void printDBData() {
        List<Reading> readingList = mDatabaseManager.getData();

        for (Reading r : readingList) {
            appendVerboseView("data: " + r.getId() + " name: " + r.getPatientName() + " time: " + (r.getEndTime() - r.getStartTime()) + "ms");
        }
        printSensorStatus();
    }

    private void printSensorStatus() {
        appendVerboseView("is sensors running " + mSensorsRunning);
    }


//    endregion


    /**
     * Parse information about the user and save it to the database
     */
    private void saveReadingInformation() {


        String name = mNameEditText.getText().toString().trim();
        String notes = mInfoEditText.getText().toString().trim();
        appendVerboseView("information about: " + name + " saved");
        if (name.length() > 0 && notes.length() > 0) {
            mReading = new Reading(name, notes);
        } else if (name.length() > 0 && notes.length() == 0) {
            mReading = new Reading(name, "Not provided");
        } else if (name.length() == 0 && notes.length() > 0) {
            mReading = new Reading("Not provided", notes);
        } else {
            mReading = new Reading();
        }

        mDbHandler.post(new Runnable() {
            @Override
            public void run() {
                mReading.setStartTime();
                Log.d(TAG, "start " + mReading.getStartTime());
//                Sets reading to database manager
//                mReadingId = mDatabaseManager.insertReadingToDB(mReading);
                mDatabaseManager.startNewReading(mReading);
//                TODO this id might not be necessary
//                mReading.setId(mReadingId);

            }
        });
//        new DatabaseAsyncTask().execute(mReading);


    }



//    endregion


    /////SQLITE METHODS//////
//    region

    private void initDbUpdateThread() {
        HandlerThread dbThread = new HandlerThread("ShortDatabaseThread");
        dbThread.start();
        mDbLooper = dbThread.getLooper();
        mDbHandler = new Handler(mDbLooper);
    }

    private void stopDatabaseThread() {
        mDbHandler.removeCallbacksAndMessages(null);
        mDbLooper.quit();
    }
//    endregion
    /////SENSOR SERVICE METHODS///////
//    region
    /**
     * Start sensor service which reads data and sends it back to the database handler
     */
    private void startSensorService() {
        Messenger mMessenger = new Messenger(mHandler);
        mServiceIntent = new Intent(getActivity(), SensorService.class);
        mServiceIntent.putExtra("MESSENGER", mMessenger);
        //Save reading information to the database
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

//            Bundle sensorBundle;
//            if (getActivity() != null) {

            DatabaseFragment fragment = mFragmentReference.get();
            switch (msg.arg1) {
                case SensorService.START_SENSORS:
                    Toast.makeText(fragment.getActivity().getApplicationContext(), "Sensors are running", Toast.LENGTH_SHORT)
                            .show();

//                    start = System.nanoTime();
                    break;
                case SensorService.STOP_SENSORS:

                    Bundle data = msg.getData();
                    mFragmentReference.get().finishReading();
                    mFragmentReference.get().appendVerboseView("Reading stopped with " +
                            data.getInt(SensorService.SENSOR_VALUES) + " acc values");
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

    static long start;


    /**
     * Save data to temporary arrays
     */
    public void appendSensorDataArray(Message message) {
        mDatabaseManager.addData(message);
    }

    private void stopSensorService() {
        if (mServiceIntent != null) {
            getActivity().stopService(mServiceIntent);
        }
    }

//    endregion
    ////////STOPWATCH INTERFACE CALLS///////
//    region
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

        // If reading is shorter than 5 seconds remove it from database
//        Toast.makeText(getActivity().getApplicationContext(), "Stopped at " + mStopwatch.getTimeInSeconds(), Toast.LENGTH_SHORT)
//                .show();
    }

    @Override
    public void onTimeZero() {
        startSensorService();
    }
//    endregion


    /**
     * <p>Indicate that reading is finished and set ending time for the reading by getting latest timestamp
     * value saved into the Acc timestamp array.</p>
     * <p>If reading duration is less than 5 seconds, remove data</p>
     */

    public void finishReading() {
        mDbHandler.post(new Runnable() {
            @Override
            public void run() {

                if (mStopwatch.getTimeInSeconds() <= 5) {
                    Log.d(TAG, "start removing");
                    mHandler.removeCallbacksAndMessages(null);
                    mDatabaseManager.deleteData(mReading);
                    mReading = null;
                } else {
                    Log.d(TAG, "set end time");

                    long endTime = System.currentTimeMillis();
//                      sets end time and resets acc and gyro arrays in case of new reading
                    mDatabaseManager.setEndTime(mReading.getId(), endTime);
                    mReading.setEndTime(endTime);
                }
            }
        });
    }
}

