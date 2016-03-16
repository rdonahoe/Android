package com.example.ryan.comptest;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView compView;

    private float currentDegree = 0f;
    private Location currentLocation;

    private SensorManager senManager;
    private LocationManager locManager;

    private LocationListener locListener;

    private Sensor MFsensor;        // magnetic field
    private Sensor Asensor;         // accelerometer
    private GeomagneticField GMF;
    private float[] gravity;
    private float[] geomagnetic;
    private float[] azimuthValues;
    private int azimuthCount;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compView = (ImageView) findViewById(R.id.compView);
        compView.setImageResource(R.drawable.windrose);

        senManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Asensor = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        MFsensor = senManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        gravity = new float[3];
        geomagnetic = new float[3];
        azimuthValues = new float[10];
        azimuthCount = 0;

        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            // default to last known cached location
            currentLocation = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch(SecurityException e) { e.printStackTrace(); }

        // if last location was available, instantiate a geomagnetic field object
        if(currentLocation != null) {
            GMF = new GeomagneticField(
                    (float) currentLocation.getLatitude(),
                    (float) currentLocation.getLongitude(),
                    (float) currentLocation.getAltitude(),
                    System.currentTimeMillis());
        }

        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;

                GMF = new GeomagneticField(
                        (float) location.getLatitude(),
                        (float) location.getLongitude(),
                        (float) location.getAltitude(),
                        System.currentTimeMillis());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop location and sensors listeners
        try {
            locManager.removeUpdates(locListener);
        } catch(SecurityException e) { e.printStackTrace(); }

        senManager.unregisterListener(this, Asensor);
        senManager.unregisterListener(this, MFsensor);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start listeners
        try {
            locManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    60000, 200,
                    locListener);
        } catch(SecurityException e) { e.printStackTrace(); }

        senManager.registerListener(this, Asensor, SensorManager.SENSOR_DELAY_NORMAL);
        senManager.registerListener(this, MFsensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not terribly important
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3);
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3);
        }

        float[] R = new float[9];
        if(SensorManager.getRotationMatrix(R, null, gravity, geomagnetic) && GMF != null) {

            float[] val = new float[3];
            SensorManager.getOrientation(R, val);

            // newDegree points to magnetic north. If gmf object was instantiated, it can be
            // used to find the declination offset that is used to calculate true north
            float newDegree = (float) Math.toDegrees(val[0]);

            // using mod 10 makes it so we don't have to put the variable back to zero
            azimuthValues[azimuthCount % 10] = newDegree;
            azimuthCount++;

            // only perform once every tenth azimuth value
            if(azimuthCount % 10 == 0) {
                // obtain average from our azimuth values to make gui look smooth
                float avg = 0;
                for(float n : azimuthValues) { avg += n; }
                avg /= 10;
                newDegree = avg + GMF.getDeclination();

                // convert degree to 360 scale instead of -180 to 180
                newDegree = (newDegree + 360) % 360;

                // create rotation object, declaring beginning and end point, and where to pivot.
                // The pivot point in this case is the center
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -newDegree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);

                ra.setDuration(200);
                ra.setFillAfter(true);

                compView.startAnimation(ra);
                currentDegree = -newDegree;
            }
        }
    }
}