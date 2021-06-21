package com.example.android.drivesafe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static java.lang.Math.abs;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

public class TrackRideActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap mMap;

    private SensorManager sensorManager; //To initialize sensor
    Sensor accelerometer;

    long samplingRate, startTime, endTime;

    float xValue, yValue, zValue; //Declaring variable for accelerometer x axis value

    float xCalibrationSum, yCalibrationSum, zCalibrationSum; //Declaring variable of x values for calibration

    float xOffset, yOffset, zOffset; //Declaring variable for the x axis offset

    float xAverage, yAverage, zAverage;

    float xSum, ySum, zSum;

    int xAverageCount, yAverageCount, zAverageCount;

    boolean running = true;

    static boolean accelerationState = false;
    static boolean brakeState = false;
    static boolean turnState = false;
    static boolean speedState = false;

    private static DecimalFormat df = new DecimalFormat("0.00"); //Declaring class instance for expressing floats up to 2 significant values

    int p = 0; //Initializing variable for checking if calibration is complete

    int calibrationCount = 50; //Initializing variable for number of times to take values for calibration

    int movingAveragePeriod = 1000; //Initializing time period for calculating moving average

    float suddenAccelerationThreshold = -2.0f; //Initializing variable for sudden acceleration threshold
    float suddenBrakeThreshold = 3.0f;
    float sharpTurnThreshold = 1.75f;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    static double lastLatitude, lastLongitude;

    int minTimeGPS = 5 * 1000;
    int minDistanceGPS = 0;

    Boolean flag = false;

    int zoom = 15;

    Marker marker;

    double distance;
    long time;
    static double speed;

    double speedLimit = 50.0 * (5.0 / 18.0);

    String FILE_NAME;

    TextView display;

    boolean calibration = true;

    String formattedDate;

    MediaPlayer rashSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_ride);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Date date = new Date();  // to get the date
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        formattedDate = df.format(date.getTime());

        display = findViewById(R.id.display_text_view);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //Set phone orientation as potrait

        //Initializing Sensor Services
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        //onCreate: Registered accelerometer listener

        startTime = System.currentTimeMillis();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        FILE_NAME = "rides.csv";

        rashSound = MediaPlayer.create(this, R.raw.rash);

        //Declaring new timer for calculating moving average
        //This will call the function for calculating moving average at regular set intervals
        Timer timer = new Timer();
        timer.schedule(new movingAverageTimer(), 0, movingAveragePeriod);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        time = System.currentTimeMillis();

        mMap = googleMap;

        marker = mMap.addMarker(new MarkerOptions().position(new LatLng((float) lastLatitude, (float) lastLongitude)));
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker));

        goToLocation((float) 12.99655616, (float) 77.69054136, zoom, marker);

        flag = displayGpsStatus();
        if (flag) {

            locationListener = new TrackRideActivity.MyLocationListener();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeGPS, minDistanceGPS,locationListener);

        } else {alertbox();}
    }

    public void goToLocation(float latitude, float longitude, int zoom, Marker marker) {
        LatLng latLng = new LatLng(latitude, longitude);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.moveCamera(update);
        marker.setPosition(latLng);
        marker.setTitle("My location");
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
        Toast.makeText(this, "Location change" + speed + latitude + longitude, Toast.LENGTH_SHORT).show();
    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;
        }
        else {
            return false;
        }
    }

    /*----------Method to create an AlertBox ------------- */
    protected void alertbox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Device's GPS is Disable")
                .setCancelable(false)
                .setTitle("** Gps Status **")
                .setPositiveButton("Gps On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_SECURITY_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            time = System.currentTimeMillis() - time;
            long timeSeconds = time / (long) 1000;
            distance = getDistanceBetween(lastLatitude, lastLongitude, loc.getLatitude(), loc.getLongitude());
            speed = calculateSpeed(distance, timeSeconds);

            speedChecker();

            lastLatitude = loc.getLatitude();
            lastLongitude = loc.getLongitude();

            goToLocation((float) lastLatitude, (float) lastLongitude, zoom, marker);

            time = System.currentTimeMillis();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

        public Double getDistanceBetween(double lat1, double long1, double lat2, double long2) {
            if (lat1 == 0.0 || long1 == 0.0 || lat2 == 0.0 || long2 == 0.0) {
                return 0.0;
            }
            float[] result = new float[1];
            Location.distanceBetween(lat1, long1,
                    lat2, long2, result);
            return (double) result[0];
        }

        public double calculateSpeed(double distance, long time) {
            double speed = distance / (double) time;
            return speed;
        }
    }

    public void endRide(View view) {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(locationListener);

        Toast.makeText(this, "Ride ended", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onAccuracyChanged (Sensor sensor,int accuracy){}

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (!running) {
            return;
        }

        //Calculates time period between 2 accelerometer readings(sampling rate)
        endTime = System.currentTimeMillis();
        samplingRate = endTime - startTime;

        //Setting xValue as the value of x axis acceleration
        xValue = sensorEvent.values[0];
        yValue = sensorEvent.values[1];
        zValue = sensorEvent.values[2];

        //Checks if calibration is complete
        if (p >= calibrationCount) {
            //Subtracting xOffset from xValue
            xValue = xValue - xOffset;
            yValue = yValue - yOffset;
            zValue = zValue - zOffset;
        } else {
            //adds xValue to xCalibrationSum
            xCalibrationSum = xCalibrationSum + xValue;
            yCalibrationSum = yCalibrationSum + yValue;
            zCalibrationSum = zCalibrationSum + zValue;


            //Displays "Calibrating..."
            TrackRideActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    display.setText("Calibrating...");
                }
            });

            //Incrementing p by 1
            p = p + 1;

            if (p == calibrationCount) {
                //Divides sum of xCalibrationSum by the size of xCalibrationSum to get average(xOffset)
                xOffset = xCalibrationSum / calibrationCount;
                yOffset = yCalibrationSum / calibrationCount;
                zOffset = zCalibrationSum / calibrationCount;

                calibration = false;
            }
        }

        xAverageCount = xAverageCount + 1;
        yAverageCount = yAverageCount + 1;
        zAverageCount = zAverageCount + 1;

        xSum = xSum + xValue;
        ySum = ySum + yValue;
        zSum = zSum + zValue;

        //Setting startTime as current time(Used for calculating sampling rate)
        startTime = System.currentTimeMillis();
    }

    class movingAverageTimer extends TimerTask {
        public void run() {
            if (!running) {return;}

            //Dividing sum of xList by size of list to get average and put it in xAverage
            xAverage = xSum / xAverageCount;
            yAverage = ySum / yAverageCount;
            zAverage = zSum / zAverageCount;

            //Calling function to check for sudden acceleration and brake
            accelerationChecker();
            brakeChecker();
            turnChecker();
            speedChecker();

            if (!calibration) {
                if (!accelerationState && !brakeState && !turnState && !speedState) {
                    TrackRideActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            display.setText("You are driving safely");
                            display.setBackgroundColor(getResources().getColor(R.color.green));
                        }
                    });
                }
            }

            xSum = 0.0f;
            ySum = 0.0f;
            zSum = 0.0f;
            xAverageCount = 0;
            yAverageCount = 0;
            zAverageCount = 0;
        }
    }

    public void accelerationChecker() {
        //Checks if zAverage is above the threshold
        if (!calibration) {
            if (zAverage < suddenAccelerationThreshold) {
                if (!accelerationState) {
                    log("Acceleration" + ","
                            + df.format(xAverage) + ","
                            + df.format(yAverage) + ","
                            + df.format(zAverage) + ","
                            + df.format(speed) + ","
                            + System.currentTimeMillis() + ","
                            + lastLatitude + ","
                            + lastLongitude + ","
                            + formattedDate + "\n");
                }
                TrackRideActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        display.setText("You are driving rashly");
                        display.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                });
                rashSound.start();
            }
        }
        accelerationState = zAverage < suddenAccelerationThreshold;
    }

    public void brakeChecker() {
        if (!calibration) {
            if (zAverage > suddenBrakeThreshold) {
                if (!brakeState) {
                    log("Brake" + ","
                            + df.format(xAverage) + ","
                            + df.format(yAverage) + ","
                            + df.format(zAverage) + ","
                            + df.format(speed) + ","
                            + System.currentTimeMillis() + ","
                            + lastLatitude + ","
                            + lastLongitude + ","
                            + formattedDate + "\n");
                }
                TrackRideActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        display.setText("You are driving rashly");
                        display.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                });
                rashSound.start();
            }
        }
        brakeState = zAverage > suddenBrakeThreshold;
    }

    public void turnChecker() {
        if (!calibration) {
            if (abs(xAverage) > sharpTurnThreshold) {
                if (!turnState) {
                    log("Turn" + ","
                            + df.format(xAverage) + ","
                            + df.format(yAverage) + ","
                            + df.format(zAverage) + ","
                            + df.format(speed) + ","
                            + System.currentTimeMillis() + ","
                            + lastLatitude + ","
                            + lastLongitude + ","
                            + formattedDate + "\n");
                }
                TrackRideActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        display.setText("You are driving rashly");
                        display.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                });
                rashSound.start();
            }
        }
        turnState = abs(xAverage) > sharpTurnThreshold;
    }

    public void speedChecker() {
        if (!calibration) {
            if (speed > speedLimit) {
                if (!speedState) {
                    log("Speeding" + ","
                            + df.format(xAverage) + ","
                            + df.format(yAverage) + ","
                            + df.format(zAverage) + ","
                            + df.format(speed) + ","
                            + System.currentTimeMillis() + ","
                            + lastLatitude + ","
                            + lastLongitude + ","
                            + formattedDate + "\n");
                }
                TrackRideActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        display.setText("You are driving rashly");
                        display.setBackgroundColor(getResources().getColor(R.color.red));
                    }
                });
                rashSound.start();
            }
        }
        speedState = speed > speedLimit;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1000:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void log(String string) {
        try {
            FileOutputStream fos;
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            fos.write(string.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(locationListener);
        Toast.makeText(this, "Ride ended", Toast.LENGTH_SHORT).show();
    }
}
