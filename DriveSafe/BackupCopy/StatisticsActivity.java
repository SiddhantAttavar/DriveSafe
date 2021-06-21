package com.example.android.drivesafe;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class StatisticsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    BarChart barChart;

    String FILE_NAME = "rides.csv";

    String date;

    int zoom = 13;

    int accelerationCount, brakeCount, turnCount, speedCount;

    TextView dateTextView;

    private ArrayList<DrivingData> dataCollection = new ArrayList<>();
    private ArrayList<DrivingData> finDataCollection = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        dateTextView = findViewById(R.id.date_text_view);

        if (b!=null) {
            String j =(String) b.get("name");
            date = j;
        }

        StatisticsActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                dateTextView.setText(date);
            }
        });

        barChart = findViewById(R.id.bar_graph);
    }

    public void readData() {
        try {
            FileInputStream fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(new DataInputStream(fis));
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null ) {
                String[] tokens = line.split(",");
                DrivingData drivingData = new DrivingData(
                        tokens[0],
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3]),
                        Float.parseFloat(tokens[4]),
                        Long.parseLong(tokens[5]),
                        Double.parseDouble(tokens[6]),
                        Double.parseDouble(tokens[7]),
                        tokens[8]);
                dataCollection.add(drivingData);
            }
            fis.close();

            processData();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void processData() {
        for (int i = 0; i < dataCollection.size(); i++) {
            DrivingData x = dataCollection.get(i);
            if (x.getDate().equals(date)) {
                finDataCollection.add(x);
            }
        }

        for (int j = 0; j < finDataCollection.size(); j++) {
            DrivingData y = finDataCollection.get(j);
            switch (y.getType()) {
                case "Acceleration":
                    accelerationCount++;
                    goToLocation((float) y.getLastLatitude(), (float) y.getLastLongitude(), zoom, BitmapDescriptorFactory.HUE_BLUE, "Acceleration");
                    break;
                case "Brake":
                    brakeCount++;
                    goToLocation((float) y.getLastLatitude(), (float) y.getLastLongitude(), zoom, BitmapDescriptorFactory.HUE_RED, "Brake");
                    break;
                case "Turn":
                    turnCount++;
                    goToLocation((float) y.getLastLatitude(), (float) y.getLastLongitude(), zoom, BitmapDescriptorFactory.HUE_GREEN, "Turn");
                    break;
                case "Speeding":
                    speedCount++;
                    goToLocation((float) y.getLastLatitude(), (float) y.getLastLongitude(), zoom, BitmapDescriptorFactory.HUE_ORANGE, "Speed");
                    break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        accelerationCount = 0;
        brakeCount = 0;
        turnCount = 0;
        speedCount = 0;

        readData();

        Integer[] num = { accelerationCount, brakeCount, turnCount, speedCount };
        int max = Collections.max(Arrays.asList(num));

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(accelerationCount, 0));
        barEntries.add(new BarEntry(brakeCount, 1));
        barEntries.add(new BarEntry(turnCount, 2));
        barEntries.add(new BarEntry(speedCount, 3));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Reckless Driving Event");

        ArrayList<String> events = new ArrayList<>();
        events.add("Acc.");
        events.add("Brake");
        events.add("Turn");
        events.add("Speed");

        barDataSet.setColors(new int[] {Color.RED, Color.RED, Color.RED, Color.RED, Color.RED});

        BarData data = new BarData(events, barDataSet);
        barChart.setData(data);
        barChart.setDescription(" ");
    }

    public void goToLocation(float latitude, float longitude, int zoom, float color, String title) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
        LatLng latLng = new LatLng(latitude, longitude);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.moveCamera(update);
        marker.setPosition(latLng);
        marker.setTitle(title);
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(color));
    }
}
