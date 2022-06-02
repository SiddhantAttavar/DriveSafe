package com.example.android.accelerometertest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class GPSActivity extends Activity {

    private LocationManager locationMangaer=null;
    private LocationListener locationListener=null;

    private EditText editLocation = null;

    double lastLatitude;
    double lastLongitude;

    private static final String TAG = "Debug";
    private Boolean flag = false;

    int minTimeGPS = 5 * 1000;
    int minDistanceGPS = 0;
    double distance = 0.0;
    long time = System.currentTimeMillis();
    double speed = 0.0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        editLocation = findViewById(R.id.editTextLocation);

        locationMangaer = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    public void onClick(View v) {
        flag = displayGpsStatus();
        if (flag) {

            Log.v(TAG, "onClick");

            editLocation.setText("Latitude: " + lastLatitude + "\n" + "Longitude: " + lastLongitude + "\n" + "Distance: " + distance);

            locationListener = new MyLocationListener();

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

            locationMangaer.requestLocationUpdates(LocationManager
                    .GPS_PROVIDER, minTimeGPS, minDistanceGPS,locationListener);

        } else {
            alertbox();
        }

    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
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
            speed =  calculateSpeed(distance, timeSeconds);

            lastLatitude = loc.getLatitude();
            lastLongitude = loc.getLongitude();

            editLocation.setText("");
            Toast.makeText(getBaseContext(),"Location changed : Lat: " +
                            loc.getLatitude()+ " Lng: " + lastLongitude + "Dist: " + distance,
                    Toast.LENGTH_SHORT).show();
            String longitude = "Longitude: " + lastLatitude;
            Log.v(TAG, longitude);
            String latitude = "Latitude: " + lastLongitude;
            Log.v(TAG, latitude);
            String distanceText = "Distance: " + String.valueOf(distance);
            String timeText = "Time: " + String.valueOf(time);
            String speedText = "Speed: " + String.valueOf(speed);

    /*----------to get City-Name from coordinates ------------- */
            String cityName=null;
            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.getDefault());
            List<Address>  addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                if (addresses.size() > 0)
                    System.out.println(addresses.get(0).getLocality());
                cityName=addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String string = longitude + "\n" + latitude + "\n" + distanceText + "\n" + timeText + "\n" + speedText + "\n\nMy Currrent City is: "+cityName;
            editLocation.setText(string);

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
    }

    public void previousScreen(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public static Double getDistanceBetween(double lat1, double long1, double lat2, double long2) {
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
