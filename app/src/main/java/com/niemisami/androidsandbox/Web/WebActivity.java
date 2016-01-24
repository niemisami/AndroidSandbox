package com.niemisami.androidsandbox.Web;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.niemisami.androidsandbox.MainFragment;
import com.niemisami.androidsandbox.R;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        inflateFragments();
    }


    /**
     * Find fragment layout and add it to the activity
     */
    private void inflateFragments() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentById(R.id.fragmentContainer) == null) {
            WebFragment fragment = new WebFragment();
            manager.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
