/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {

  public void redirectAcitvity(){

      // Redirect the user to the rder or driver's activity

      // Redirecting to the rider activity

    if(ParseUser.getCurrentUser().get("riderOrDriver").equals( "rider")){

      Intent intent = new Intent(MainActivity.this , RiderActivity.class );

      startActivity(intent);

    }
    else{

        // Redirecting to teh driver's sctivity "ViewRequestsActivity".

          Intent intent = new Intent(this , ViewRequestsActivity.class);
          startActivity(intent);

      }

  }



  public void getStarted(View view){

    Switch userTypeSwitch = (Switch) findViewById(R.id.switch1);

    String userType = "rider";

    if(userTypeSwitch.isChecked()){

      userType = "Driver";

    }
    ParseUser.getCurrentUser().put("riderOrDriver" , userType);

    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
        @Override
        public void done(ParseException e) {

            if (e == null) {
                redirectAcitvity();
            }

            else{

                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
    });

    Log.i("info"  , "Rediricting as " + userType);


  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Anonymous login as we dont need yo store username or password

    if(ParseUser.getCurrentUser() == null ){

      ParseAnonymousUtils.logIn(new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {

          if(e == null){

            Log.i("Info" , "Anonynous Successful");
          }

          else{
            Log.i("Info" , "Anonymous Login Failed");
          }

        }
      });
    }

    else {

      if (ParseUser.getCurrentUser().get("riderOrDriver") != null){

          redirectAcitvity();

          Log.i("Info" , "Redirecting as " + ParseUser.getCurrentUser().get("riderOrDriver"));
      }

      Log.i("Info" , String.valueOf(ParseUser.getCurrentUser()));
    }
    getSupportActionBar().hide();
    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}