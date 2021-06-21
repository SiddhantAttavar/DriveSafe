package com.example.android.drivesafe;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static boolean start = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void startRide(View view) {
        Intent intent = new Intent(this, TrackRideActivity.class);
        startActivity(intent);
    }

    public void goToStatistics(View view) {
        Intent intent = new Intent(this, PastRidesActivity.class);
        startActivity(intent);
    }
}
