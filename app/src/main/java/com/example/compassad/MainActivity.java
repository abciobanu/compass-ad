package com.example.compassad;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {
    private final int COARSE_LOCATION_REQUEST_CODE = 0;
    private final int ROTATION_MATRIX_SIZE = 9;
    private final int GRAVITY_VECTOR_SIZE = 3;
    private final int GEOMAGNETIC_VECTOR_SIZE = 3;
    private final int NUM_OF_AXES = 3;

    private final float[] rotationMatrix = new float[ROTATION_MATRIX_SIZE];
    private final float[] orientationValues = new float[NUM_OF_AXES];
    private float[] gravityVector = new float[GRAVITY_VECTOR_SIZE];
    private float[] geomagneticVector = new float[GEOMAGNETIC_VECTOR_SIZE];

    private SensorManager sensorManager;
    private SensorEventListener accelerometerEventListener;
    private SensorEventListener magneticFieldEventListener;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private ImageView compassImageView;
    private EditText magneticDeclinationValue;
    private TextView degreeTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView altitudeTextView;

    private float previousCompassDegree = 0.0f;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        compassImageView = findViewById(R.id.compassImageView);
        degreeTextView = findViewById(R.id.degreeText);
        latitudeTextView = findViewById(R.id.latitude);
        longitudeTextView = findViewById(R.id.longitude);
        altitudeTextView = findViewById(R.id.altitude);

        magneticDeclinationValue = findViewById(R.id.magneticDeclinationInput);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    COARSE_LOCATION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        accelerometerEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                gravityVector = event.values;

                SensorManager.getRotationMatrix(rotationMatrix, null, gravityVector,
                        geomagneticVector);
                SensorManager.getOrientation(rotationMatrix, orientationValues);

                float currentCompassDegree = filterInput(getCompassDegree(), previousCompassDegree);

                compassImageView.setRotation(-currentCompassDegree);

                currentCompassDegree = (currentCompassDegree + 360) % 360;
                degreeTextView.setText(String.format(getResources().getConfiguration().locale,
                        "%d°", (int) currentCompassDegree));
                previousCompassDegree = currentCompassDegree;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        magneticFieldEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                geomagneticVector = event.values;

                SensorManager.getRotationMatrix(rotationMatrix, null, gravityVector,
                        geomagneticVector);
                SensorManager.getOrientation(rotationMatrix, orientationValues);

                float currentCompassDegree = filterInput(getCompassDegree(), previousCompassDegree);

                compassImageView.setRotation(-currentCompassDegree);

                currentCompassDegree = (currentCompassDegree + 360) % 360;
                degreeTextView.setText(String.format(getResources().getConfiguration().locale,
                        "%d°", (int) currentCompassDegree));
                previousCompassDegree = currentCompassDegree;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(accelerometerEventListener, accelerometer,
                SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME);

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(magneticFieldEventListener, magneticField,
                SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(accelerometerEventListener);
        sensorManager.unregisterListener(magneticFieldEventListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == COARSE_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permissions should be granted in order to get the" +
                        "latitude, longitude and altitude data!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitudeTextView.setText(String.format(getResources().getConfiguration().locale,
                        "%f", location.getLatitude()));
                longitudeTextView.setText(String.format(getResources().getConfiguration().locale,
                        "%f", location.getLongitude()));
                altitudeTextView.setText(String.format(getResources().getConfiguration().locale,
                        "%f", location.getAltitude()));
            }
        });
    }

    private float getCompassDegree() {
        double magneticDeclination = 0f;
        String magneticDeclinationText = magneticDeclinationValue.getText().toString();
        if (!"".equals(magneticDeclinationText) && !"-".equals(magneticDeclinationText)) {
            magneticDeclination = Double.parseDouble(magneticDeclinationText);
        }

        return (float) ((Math.toDegrees(orientationValues[0]) + magneticDeclination + 360) % 360);
    }

    private float filterInput(float currentValue, float previousValue) {
        if (currentValue - previousValue > 180) {
            currentValue -= 360;
        } else if (previousValue - currentValue > 180) {
            currentValue += 360;
        }

        return previousValue + 0.01f * (currentValue - previousValue);
    }
}