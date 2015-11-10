package com.niemisami.androidsandbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class DatabaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        inflateFragments();
        findViewById(R.id.databaseContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DatabaseActivity.this, MainActivity.class));
            }
        });
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
