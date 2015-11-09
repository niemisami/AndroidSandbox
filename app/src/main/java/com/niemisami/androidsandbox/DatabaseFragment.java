package com.niemisami.androidsandbox;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.niemisami.androidsandbox.services.SensorService;


/**
 * A simple {@link Fragment} subclass.
 */
public class DatabaseFragment extends Fragment {
    final private String TAG = "DatabaseFragment";

    private Toolbar mToolBar;
    private ServiceMessageHandler mHandler;

    private Intent mServiceIntent;

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

        return view;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        mHandler = new ServiceMessageHandler();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        Messenger mMessenger = new Messenger(mHandler);
        mServiceIntent = new Intent(getActivity(), SensorService.class);
        mServiceIntent.putExtra("MESSENGER", mMessenger);
        getActivity().startService(mServiceIntent);
    }

    @Override
    public void onPause() {
        getActivity().stopService(mServiceIntent);
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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


    private class ServiceMessageHandler extends Handler {
        public void handleMessage(Message msg) {
            if (getActivity() != null) {
                if (msg.arg1 == 1) {
                    Toast.makeText(getActivity().getApplicationContext(), "Sensors are running", Toast.LENGTH_SHORT)
                            .show();
                } else if (msg.arg1 == 0) {
                    Bundle data = msg.getData();
                    int readingAmount = data.getInt(SensorService.SENSOR_VALUES);
                    Toast.makeText(getActivity().getApplicationContext(), "Sensors stopped with " + readingAmount + " reading", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT)
                            .show();
                }
            }

        }
    }

    ;

}
