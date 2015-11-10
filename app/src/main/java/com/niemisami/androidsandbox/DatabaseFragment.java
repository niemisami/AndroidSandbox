package com.niemisami.androidsandbox;


import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
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
public class DatabaseFragment extends Fragment {
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


    private Button mSaveReadingInfoButton, mStartStopSensorsButton;
    private TextView mVerboseTextView;
    private EditText mNameEditText, mInfoEditText;
    private ScrollView mVerboseScrollView;

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
//        getActivity().stopService(mServiceIntent);
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabaseManager.closeDataManager();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    //    Create material design Toolbar as menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.db_menu, menu);
    }

//    endregion


    /////VIEW INITIALIZATION////


//    region

    long start;
    private void initView(View view) {

        mSaveReadingInfoButton = (Button) view.findViewById(R.id.save_information_button);
        mSaveReadingInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start = System.currentTimeMillis();
                saveReadingInformation();
            }
        });
        mStartStopSensorsButton = (Button) view.findViewById(R.id.start_sensor_button);
        mStartStopSensorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printDBData();
            }
        });

        mVerboseTextView = (TextView) view.findViewById(R.id.verbose_box);
        mVerboseTextView.setText("Events:\n");

        mInfoEditText = (EditText) view.findViewById(R.id.info_text_box);
        mNameEditText = (EditText) view.findViewById(R.id.name_text_box);

        mVerboseScrollView = (ScrollView) view.findViewById(R.id.verbose_scroll);
    }

    private void saveReadingInformation() {
        String name = mNameEditText.getText().toString().trim();
        String notes = mInfoEditText.getText().toString().trim();
        Reading reading;
        mVerboseTextView.append("information about: " + name + " saved\n");
        if (name.length() > 0 && notes.length() > 0) {
            reading = new Reading(name, notes);
        } else if (name.length() > 0 && notes.length() == 0) {
            reading = new Reading(name, "Not provided");
        } else if (name.length() == 0 && notes.length() > 0) {
            reading = new Reading("Not provided", notes);
        } else {
            reading = new Reading();
        }

        new DatabaseAsyncTask().execute(reading);
    }

    private void printDBData() {
        List<Reading> readingList = mDatabaseManager.getData();

        for(Reading r : readingList) {
            mVerboseTextView.append("data: " + r.getId() + " name: " + r.getPatientName() + " time: " + r.getStartTime() + "\n");
        }

        mVerboseScrollView.post(new Runnable() {
            @Override
            public void run() {
                mVerboseScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });



    }

//    endregion



    /////SQLITE TASK AND METHODS//////

    private void startSQLiteDB(Context context) {

    }
    private class DatabaseAsyncTask extends AsyncTask<Reading, Integer, Integer> {
        @Override
        protected Integer doInBackground(Reading... params) {
            Log.d(TAG, "starting save " + (System.currentTimeMillis() - start));
//            mDatabaseManager.insertReadingToDB(params[0]);
            mDatabaseManager.addsensor(1, 4.22f, 9.33f, 2.00f, 14000000 );
            Log.d(TAG, "Saved " + (System.currentTimeMillis() - start));

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


    /////SENSOR SERVICE METHODS///////

    private void startSensorService() {
        Messenger mMessenger = new Messenger(mHandler);
        mServiceIntent = new Intent(getActivity(), SensorService.class);
        mServiceIntent.putExtra("MESSENGER", mMessenger);
        getActivity().startService(mServiceIntent);
    }

    public static class ServiceMessageHandler extends Handler {

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
                    Toast.makeText(fragment.getActivity().getApplicationContext(), "Sensors stopped with " + mFragmentReference.get().getReadingCount() + " reading", Toast.LENGTH_SHORT)
                            .show();
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

    public void appendSensorDataArray(Message message) {
        Bundle sensorBundle;
        switch (message.arg1) {
            case SensorService.SENSOR_ACC:
                sensorBundle = message.getData();
                mAccX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
                mAccY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
                mAccZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
                mAccTimestamp.add(sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP));

                break;
            case SensorService.SENSOR_GYRO:
                sensorBundle = message.getData();
                mGyroX.add(sensorBundle.getFloat(SensorService.SENSOR_X));
                mGyroY.add(sensorBundle.getFloat(SensorService.SENSOR_Y));
                mGyroZ.add(sensorBundle.getFloat(SensorService.SENSOR_Z));
                mGyroTimestamp.add(sensorBundle.getLong(SensorService.SENSOR_TIMESTAMP));
                break;
        }
    }

    public int getReadingCount() {
        return mAccTimestamp.size();
    }
}

