package com.niemisami.androidsandbox.BluetoothView;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.niemisami.androidsandbox.DatabaseFragment;
import com.niemisami.androidsandbox.R;

public class BluetoothActivity extends AppCompatActivity {

    private final static String TAG = "BluetoothActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        inflateFragments();

    }


    /**
     * Find fragment layout and add it to the activity
     */
    private void inflateFragments() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentById(R.id.databaseContainer) == null) {
            DatabaseFragment fragment = new DatabaseFragment();
            manager.beginTransaction()
                    .add(R.id.databaseContainer, fragment)
                    .commit();
        }
    }
}
