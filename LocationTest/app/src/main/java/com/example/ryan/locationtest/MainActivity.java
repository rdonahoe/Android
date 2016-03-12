package com.example.ryan.locationtest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Location currentLocation;
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private LocationListener networkListener;

    private TextView latitudeTV;
    private TextView longitudeTV;
    private TextView distanceTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check and make sure this app has location permissions (permissions allowed in manifest)
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        // instantiate textviews objects
        latitudeTV = (TextView) findViewById(R.id.LatTV);
        longitudeTV = (TextView) findViewById(R.id.LongTV);
        distanceTV = (TextView) findViewById(R.id.DistTV);

        // instantiate managers and locations listeners. Listeners fire based on location change
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;
                latitudeTV.setText(String.format("Lat = %f", currentLocation.getLatitude()));
                longitudeTV.setText(String.format("Long = %f", currentLocation.getLongitude()));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // after checking network location, if it is at least 20 meters from the current
                // gps location, activate gps listener to try and find a single more accurate reading
                if(currentLocation.distanceTo(location) > 20) {
                    try {
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsListener, null);
                    }
                    catch(SecurityException e) {
                        e.printStackTrace();
                    }
                }

                distanceTV.setText(String.format("Distance between net and gps results = %f", currentLocation.distanceTo(location)));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // grab cached locational data from network provider for default values
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (currentLocation != null) {
            latitudeTV.setText(String.format("Lat = %f", currentLocation.getLatitude()));
            longitudeTV.setText(String.format("Long = %f", currentLocation.getLongitude()));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // for this app we only need location when we are using the app so only
        // turn on listener when app is in use
        try {
            // set listener for network provider with 0 min time and 0 min distance
            // meaning the listener will provide location data whenever available
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            locationManager.removeUpdates(networkListener);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }
}
