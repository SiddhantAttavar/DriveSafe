package com.example.android.drivesafe;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PastRidesActivity extends AppCompatActivity {

    LinearLayout linearLayout;

    DatePicker datePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_rides);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent= getIntent();
        Bundle b = intent.getExtras();

        if(b!=null)
        {
            String j =(String) b.get("name");
        }

        linearLayout = findViewById(R.id.linear_layout);
        datePicker = findViewById(R.id.date_picker);
    }

    public void getStatistics(View view) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear() - 1900;

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        Date d = new Date(year, month, day);
        String date = df.format(d);

        Intent intent=new Intent(PastRidesActivity.this, StatisticsActivity.class);
        intent.putExtra("name", date);
        startActivity(intent);
    }
}
