package com.parse.starter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;
    Button callUberButton;
    Boolean requestActive = false;
    Handler handler = new Handler();
    TextView infoTextView;
    Boolean driverActive = false;

    public void checkForUpdates(){



        ParseQuery<ParseObject> query =ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e== null && objects.size() > 0){

                    ParseQuery<ParseUser>  query = ParseUser.getQuery();

                    // querying where the username of the driver is equal to the username of the driver
                    //who just picked up te rider's request for cab.

                    query.whereEqualTo("username" , objects.get(0).getString("driverUsername"));

                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {

                            if (e == null && objects.size()>0) {

                                driverActive = true;

                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if(lastKnownLocation != null) {

                                        // converting rider's loction into parsegeopoint
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        Double distanceInMiles = driverLocation.distanceInMilesTo(userLocation);

                                        if (distanceInMiles < 0.01){

                                            infoTextView.setText("Your ride is here!");
                                            ParseQuery<ParseObject> query =ParseQuery.getQuery("Request");
                                            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null){

                                                        for (ParseObject object : objects){

                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    infoTextView.setText("");
                                                    callUberButton.setVisibility(View.VISIBLE);
                                                    callUberButton.setText("Call An Uber");
                                                    driverActive= false;
                                                    requestActive = false;

                                                    // deleting the completed request after the driver arrives at the user's location



                                                }
                                            } , 5000);
                                        }


                                        //converting the distance to one decimal place

                                        Double distanceOneDp = Double.valueOf(Math.round(distanceInMiles * 10) / 10);


                                        infoTextView.setText("Your ride is" + distanceOneDp.toString() + " miles away.");


                                        // showing the driver's location to user on map.

                                        LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());

                                        LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(),userLocation.getLongitude() );

                                        ArrayList<Marker> markers = new ArrayList<Marker>();

                                        mMap.clear();

                                        markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver's Position")));

                                        markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Position")
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                                        //calculating the bounds of all the markers

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        for (Marker marker : markers) {
                                            builder.include(marker.getPosition());
                                        }
                                        LatLngBounds bounds = builder.build();

                                        //obtaining a movement description object by using the factory: CameraUpdateFactory

                                        int padding = 30; // offset from edges of the map in pixels
                                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                        //Finally move the map

                                        mMap.animateCamera(cu);

                                        callUberButton.setVisibility(View.INVISIBLE);

                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {

                                                checkForUpdates();

                                            }
                                        } , 2000);
                                    }
                                }
                            }



                        }
                    });

                    infoTextView.setText("Your ride is on the way.");


                    }


            }
        });

    }

    public void Logout(View view){

        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext() , MainActivity.class);
        startActivity(intent);

        Toast.makeText(this, "User Logged out successfully.", Toast.LENGTH_SHORT).show();

    }

    public void callAnUber(View view){

        // checking after pressing the button if already a request has been made for uber



        if(requestActive){

            // if the reuest is already active

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

            query.whereEqualTo("username" , ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null){

                        if (objects.size()> 0 ){

                            // deleting the previous called uber request

                            for (ParseObject object : objects){

                                object.deleteInBackground();
                            }

                            requestActive = false;
                            callUberButton.setText("Call An Uber.");
                        }

                    }

                }
            });

        }
        else {

            // the request for uber is made and the location and username are fetched and stored in the parse server

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {

                    ParseObject request = new ParseObject("Request");

                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                    request.put("Location", parseGeoPoint);

                    Log.i("Info", "Call uber.");

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {

                            callUberButton.setText("Cancel Uber.");
                            requestActive = true;

                           checkForUpdates();
                        }
                    });
                }


            }
        }




    }

    public void updateMap(Location location){

        if (driverActive != false) {

            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                if(ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 0 , 0 , locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                updateMap(lastKnownLocation);
            }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoTextView = (TextView) findViewById(R.id.infotextview);

        callUberButton= (Button) findViewById(R.id.callUber);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

        query.whereEqualTo("username" , ParseUser.getCurrentUser().getUsername());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null){

                    if (objects.size()> 0 ){

                        requestActive = true;
                        callUberButton.setText("Cancel Uber.");

                        checkForUpdates();
                    }

                }

            }
        });
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

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

              updateMap(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        mMap = googleMap;

        if (Build.VERSION.SDK_INT < 23){

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 0 , 0 , locationListener);
        }

        else{

            // we dont have per,ission so reqesting for permission

           if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

           }

           else{
               // we already have the permission.
               locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0 ,0 ,locationListener);

               Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

               if(lastKnownLocation != null) {

                   updateMap(lastKnownLocation);
               }

           }

        }
    }
}
