package com.example.android.accelerometertest;

//importing external classes

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.Toast;

import static com.example.android.accelerometertest.GoogleMapsActivity.speed;
import static com.example.android.accelerometertest.GoogleMapsActivity.lastLatitude;
import static com.example.android.accelerometertest.GoogleMapsActivity.lastLongitude;
import static java.lang.Math.abs;

//main class
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity"; //For logging

    private MediaRecorder mRecorder = null;

    String FILE_NAME;

    private SensorManager sensorManager; //To initialize sensor
    Sensor accelerometer;

    long samplingRate; //Declaring variable for time difference between two accelerometer events(packets of data)
    long startTime; //Declaring variable for beginning time
    long endTime; //Declaring variable for end time

    float xValue; //Declaring variable for accelerometer x axis value
    float yValue;
    float zValue;

    TextView xAxis; //Declaring variable for x axis text view
    TextView yAxis;
    TextView zAxis;

    TextView xAverageText; //Declaring variable for x axis moving average
    TextView yAverageText;
    TextView zAverageText;

    TextView calibration; //Declaring variable for calibration state text view

    TextView samplingRateText; //Declaring variable for sampling rate text view

    TextView overSpeedText;

    TextView suddenAccelerationText; //Declaring variable for sudden acceleration state text view
    TextView suddenBrakeText;
    TextView sharpTurnText;
    TextView volumeTextView;
    TextView honkState;

    float xCalibrationSum; //Declaring variable of x values for calibration
    float yCalibrationSum;
    float zCalibrationSum;

    float xOffset; //Declaring variable for the x axis offset
    float yOffset;
    float zOffset;

    float xAverage; //Declaring variable for x axis moving average
    float yAverage;
    float zAverage;

    float xSum;
    float ySum;
    float zSum;

    int xAverageCount;
    int yAverageCount;
    int zAverageCount;

    boolean running = true;

    static boolean accelerationState;
    static boolean brakeState;
    static boolean turnState;
    static boolean speedState;
    static boolean hornState;

    private static DecimalFormat df = new DecimalFormat("0.00"); //Declaring class instance for expressing floats up to 2 significant values

    int p = 0; //Initializing variable for checking if calibration is complete

    int calibrationCount = 50; //Initializing variable for number of times to take values for calibration

    int movingAveragePeriod = 1000; //Initializing time period for calculating moving average

    float suddenAccelerationThreshold = -1.25f; //Initializing variable for sudden acceleration threshold
    float suddenBrakeThreshold = 2.0f;
    float sharpTurnThreshold = 1.25f;

    double speedLimit = 30.0 * (5.0 / 18.0);

    double volume;

    double honkLimit = 112;

    //Called when app is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //Set phone orientation as potrait

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        Log.d(TAG, "onCreate: Registered accelerometer listener");

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                //Initializing text views and mapping them to the xml elements

                xAxis = findViewById(R.id.x_axis);
                yAxis = findViewById(R.id.y_axis);
                zAxis = findViewById(R.id.z_axis);

                xAverageText = findViewById(R.id.x_average);
                yAverageText = findViewById(R.id.y_average);
                zAverageText = findViewById(R.id.z_average);

                calibration = findViewById(R.id.calibration_text_view);

                samplingRateText = findViewById(R.id.sampling_rate_text_view);

                suddenAccelerationText = findViewById(R.id.sudden_acceleration);
                suddenBrakeText = findViewById(R.id.sudden_brake);
                sharpTurnText = findViewById(R.id.sharp_turn);
                overSpeedText = findViewById(R.id.over_speeding);
                volumeTextView = findViewById(R.id.volume_text_view);
                honkState = findViewById(R.id.volume_display);
            }
        });

        FILE_NAME = "logfile.csv";

        startTime = System.currentTimeMillis(); //Initializing samplingRate to current time in phone

        start();

        //Declaring new timer for calculating moving average
        //This will call the function for calculating moving average at regular set intervals
        Timer timer = new Timer();
        timer.schedule(new movingAverageTimer(), 0, movingAveragePeriod);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
        public void onSensorChanged(final SensorEvent sensorEvent) {
            if (!running) {return;}

            //Calculates time period between 2 accelerometer readings(sampling rate)
            endTime = System.currentTimeMillis();
            samplingRate = endTime - startTime;

            //Puts samplingRate value into a text field
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    samplingRateText.setText("Sampling Rate: " + String.valueOf(samplingRate));
                }
            });

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
            }
            else {
                //adds xValue to xCalibrationSum
                xCalibrationSum = xCalibrationSum + xValue;
                yCalibrationSum = yCalibrationSum + yValue;
                zCalibrationSum = zCalibrationSum + zValue;

                //Displays "Calibrating..."
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        calibration.setText("Calibrating...");
                    }
                });

                //Incrementing p by 1
                p = p + 1;

                if (p == calibrationCount) {
                    //Divides sum of xCalibrationSum by the size of xCalibrationSum to get average(xOffset)
                    xOffset = xCalibrationSum / calibrationCount;
                    yOffset = yCalibrationSum / calibrationCount;
                    zOffset = zCalibrationSum / calibrationCount;

                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            calibration.setText("");
                        }
                    });
                }
            }

        //Reducing xValue to 2 significant digits and displaying it
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                xAxis.setText(df.format(xValue));
                yAxis.setText(df.format(yValue));
                zAxis.setText(df.format(zValue));
            }
        });

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

            double amp = getAmplitude();
            volume = 20 * Math.log10(amp/ 2700.0);

            //Calling function to check for sudden acceleration and brake
            accelerationChecker();
            brakeChecker();
            turnChecker();
            speedChecker();
            hornChecker();

            if (accelerationState) {
                log("Acceleration" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + ","
                        + lastLatitude + ","
                        + lastLongitude + "\n");
            }
            else if (brakeState) {
                log("Brake" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + ","
                        + lastLatitude + ","
                        + lastLongitude + "\n");
            }
            else if (turnState) {
                log("Turn" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + ","
                        + lastLatitude + ","
                        + lastLongitude + "\n");
            }
            else if (speedState) {
                log("Speeding" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + ","
                        + lastLatitude + ","
                        + lastLongitude + "\n");
            }
            else if (hornState) {
                log("Honking" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + ","
                        + df.format(volume) + "\n");
            }
            else {
                log("No event" + ","
                        + df.format(xAverage) + ","
                        + df.format(yAverage) + ","
                        + df.format(zAverage) + ","
                        + df.format(speed) + ","
                        + System.currentTimeMillis() + "\n");
            }

            //Displays xAverage on screen
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(TAG, "run: " + zAverage);
                    xAverageText.setText(df.format(xAverage));
                    yAverageText.setText(df.format(yAverage));
                    zAverageText.setText(df.format(zAverage));
                    volumeTextView.setText(df.format(volume));
                }
            });

            xSum = 0.0f;
            ySum = 0.0f;
            zSum = 0.0f;
            xAverageCount = 0;
            yAverageCount = 0;
            zAverageCount = 0;

            accelerationState = false;
            brakeState = false;
            turnState = false;
            speedState = false;
        }
    }

    public void accelerationChecker() {
        //Checks if zAverage is above the threshold
        if (zAverage < suddenAccelerationThreshold) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    suddenAccelerationText.setText("Sudden Acceleration: " + "True");
                    suddenAccelerationText.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        }
        else {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    suddenAccelerationText.setText("Sudden Acceleration: " + "False");
                    suddenAccelerationText.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
    }

    public void brakeChecker() {
        if (zAverage > suddenBrakeThreshold) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    suddenBrakeText.setText("Sudden Brake: " + "True");
                    suddenBrakeText.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    suddenBrakeText.setText("Sudden Brake: " + "False");
                    suddenBrakeText.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
    }

    public void turnChecker() {
        if (abs(xAverage) > sharpTurnThreshold) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    sharpTurnText.setText("Sharp Turn: " + "True");
                    sharpTurnText.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    sharpTurnText.setText("Sharp Turn: " + "False");
                    sharpTurnText.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
    }

    public void speedChecker() {
        if (speed > speedLimit) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    overSpeedText.setText("OVER SPEEDING: " + "True");
                    overSpeedText.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    overSpeedText.setText("OVER SPEEDING: " + "False");
                    overSpeedText.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
    }

    public void hornChecker() {
        if (volume >= honkLimit) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    honkState.setText("HONKING: " + "True");
                    honkState.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    honkState.setText("HONKING: " + "False");
                    honkState.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
    }

    public void gpsActivityScreen(View view) {
        Intent intent = new Intent(this, GPSActivity.class);
        startActivity(intent);
    }

    public void googleMapsScreen(View view) {
        Intent intent = new Intent(this, GoogleMapsActivity.class);
        startActivity(intent);
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

    public void onAccelerationClick(View view) {accelerationState = true;}

    public void onBrakeClick(View view) {brakeState = true;}

    public void onTurnClick(View view) {turnState = true;}

    public void onSpeedingClick(View view) {speedState = true;}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1000:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
                else {}
        }
    }

    public boolean isExternalStorageWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i("State", "Yes, it is writable");
            return true;
        }
        else {
            return false;
        }
    }

    public void saveFile(View view) {
        File saveFile = new File(Environment.getExternalStorageDirectory(), FILE_NAME);
        try {
            saveFile.createNewFile();
            FileInputStream fis = openFileInput(FILE_NAME);
            FileOutputStream fos = new FileOutputStream(saveFile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            fos.write(sb.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + saveFile, e);
        }
    }

    public void start() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecorder.start();
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 0;

    }
}

