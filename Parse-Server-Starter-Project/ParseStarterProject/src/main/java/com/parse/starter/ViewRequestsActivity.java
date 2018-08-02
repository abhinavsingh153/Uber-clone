package com.parse.starter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    ListView requestsListView;
    ArrayList<String> requests = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    LocationManager locationManager;
    LocationListener locationListener;

    ArrayList<Double> requestLatitudes = new ArrayList<Double>();
    ArrayList<Double> requestLongitudes = new ArrayList<Double>();
    ArrayList<String> usernames = new ArrayList<String>();

    public void updateListView(Location location) {


        // creatina a parsequery on the request class in the parse server on the parseObject.

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

        //Getting the location of the driver

        final ParseGeoPoint driverLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

        // getting the nearby requests to the driver's location

        query.whereNear("Location", driverLocation);

        query.whereDoesNotExist("driverUsername");

        //limiting the number of requests for driver to be viewed by him on his screen

        query.setLimit(10);


        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                requests.clear();
                requestLatitudes.clear();
                requestLongitudes.clear();

                if (e == null) {

                    if (objects.size() > 0) {

                        for (ParseObject object : objects) {

                            // storing the location of the riders in requestLocation

                            ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("Location");

                            if (requestLocation != null) {

                                //Converting the distance between the driver and the rider into miles

                                Double distanceInMiles = driverLocation.distanceInMilesTo(requestLocation);

                                //converting the distance to one decimal place

                                Double distanceOneDp = Double.valueOf(Math.round(distanceInMiles * 10) / 10);

                                requests.add(distanceOneDp.toString() + "miles");

                                requestLatitudes.add(requestLocation.getLatitude());

                                requestLongitudes.add(requestLocation.getLongitude());

                                usernames.add(object.getString("username"));
                            }
                        }

                    } else {

                        requests.add("No active requests nearby");

                    }

                    arrayAdapter.notifyDataSetChanged();
                }

            }
        });


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    updateListView(lastKnownLocation);
                }
            }

        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        requestsListView = (ListView) findViewById(R.id.requestsListView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);

        requests.clear();

        requests.add("Getting nearby requests....");

        requestsListView.setAdapter(arrayAdapter);

        setTitle("Nearby requests");

        // Applying click event to every listView item i.e the nearby requests

        requestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // we need to check that we are getting the locations we want to send
                //The position of the listItem pressed can be 0 (if the 1st listitemistapped)
                // so we the size of requestLatitudes.size() & requestLongitudes.size() to be atleats 1


                if ( Build.VERSION.SDK_INT <23 || ContextCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {


                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (requestLatitudes.size() > position && requestLongitudes.size() > position && usernames.size()> position) {

                        Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);

                        intent.putExtra("requestLatitude", requestLatitudes.get(position));
                        intent.putExtra("requestLongitude", requestLongitudes.get(position));
                        intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
                        intent.putExtra("username" , usernames.get(position));

                        startActivity(intent);

                       // Log.i("Info" , "list item pressed" + position);



                    }
                }

                Toast.makeText(ViewRequestsActivity.this, "ListView Pressed", Toast.LENGTH_SHORT).show();



            }
        });


        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                updateListView(location);

                //saving the driver's loction on the parse server in User class

                ParseUser.getCurrentUser().put("location"  , new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();

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


        if (Build.VERSION.SDK_INT < 23) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {

            // we dont have per,ission so reqesting for permission

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            } else {
                // we already have the permission.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {

                    updateListView(lastKnownLocation);
                }

            }

        }


    }
}
