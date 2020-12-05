package com.example.testbeacon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {
    protected static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private BeaconManager mBeaconManager;
    Region region;
    TreeSet<String> idList = new TreeSet<String>();
    Map<String, String> rooms = new HashMap<String, String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rooms.put("0x00112233445566778898", "HTWG-F123");

        // layout
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        NavController navController = Navigation.findNavController(this,  R.id.fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);


        // bluetooth communication
        verifyBluetooth();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_REQUEST_FINE_LOCATION);
        }
        else {
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }


    public void onResume(){
        super.onResume();

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        mBeaconManager.bind(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "fine location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //finish();
                        //System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    //finish();
                    //System.exit(0);
                }

            });
            builder.show();

        }
    }

    @Override
    public void onBeaconServiceConnect() {
        /*
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);

         */
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Log.d(TAG, "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " approximately "+beacon.getDistance()+" meters away.");


                if(!idList.contains(namespaceId.toString())) {

                    TextView idText = new TextView(this);
                    TextView distanceText = new TextView(this);
                    String roomname = rooms.get(namespaceId.toString());
                    Double distance = round(beacon.getDistance(),2);
                    idText.setText("Raum: " + roomname);
                    distanceText.setText("Entfernung: " + distance + "m");
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    distanceText.setLayoutParams(params);
                    idText.setLayoutParams(params);
                    Button postButton = new Button(this);
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sendData(namespaceId.toString());
                        }
                    };
                    postButton.setOnClickListener(listener);
                    postButton.setLayoutParams(params);
                    postButton.setText("senden");
                    LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
                    linearLayout.addView(idText);
                    linearLayout.addView(distanceText);
                    linearLayout.addView(postButton);
                    idList.add(namespaceId.toString());

                }
            }

        }
    }

    public void scanClicked (View v){

        idList.clear();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.removeAllViews();
        region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
        //mBeaconManager.bind(this);
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try{
                    mBeaconManager.stopRangingBeaconsInRegion(region);
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }

            }
        }, 10000);



    }

    public void stopScan (View v){
        try{
            mBeaconManager.stopRangingBeaconsInRegion(region);
        }
        catch (RemoteException e){
            e.printStackTrace();
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void sendData (String id){
        SharedPreferences sp = getSharedPreferences("UserData", MODE_PRIVATE);
        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM.dd HH:mm:ss");
        SimpleDateFormat time_formatter = new SimpleDateFormat("HH:mm");
        Date now = new Date();
        Log.d(TAG, id);
        // data
        String room = id;
        String date = date_formatter.format(now);
        String time = time_formatter.format(now);
        String first_name = sp.getString("first_name", "");
        String sur_name = sp.getString("sur_name", "");
        String phone = sp.getString("phone", "");
        String e_mail = sp.getString("e-mail", "");
        //create json object
        JSONObject obj = new JSONObject();
        try {
            obj.put("room", room);
            obj.put("date", date);
            obj.put("time", time);
            obj.put("given_name", first_name);
            obj.put("sur_name", sur_name);
            obj.put("phone", phone);
            obj.put("e_mail", e_mail);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //send http post with json
        //check answer and make toast
    }

    // saves User Data in shared Preferences
    public void saveUserData(View v){
        SharedPreferences sp = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        com.google.android.material.textfield.TextInputEditText first_name = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.first_name_profile);
        com.google.android.material.textfield.TextInputEditText sur_name = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.sur_name_profile);
        com.google.android.material.textfield.TextInputEditText phone = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.phone_profile);
        com.google.android.material.textfield.TextInputEditText e_mail = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.email_profile);

        editor.putString("first_name", first_name.getText().toString());
        editor.putString("sur_name", sur_name.getText().toString());
        editor.putString("phone", phone.getText().toString());
        editor.putString("e_mail", e_mail.getText().toString());
        editor.apply();
        Toast.makeText(this, "Information saved.", Toast.LENGTH_LONG).show();
    }

    // retrieves User Data from shared Preferences
    public void loadUserData(){
        SharedPreferences sp = getSharedPreferences("UserData", MODE_PRIVATE);

        com.google.android.material.textfield.TextInputEditText first_name = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.first_name_profile);
        com.google.android.material.textfield.TextInputEditText sur_name = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.sur_name_profile);
        com.google.android.material.textfield.TextInputEditText phone = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.phone_profile);
        com.google.android.material.textfield.TextInputEditText e_mail = (com.google.android.material.textfield.TextInputEditText) findViewById(R.id.email_profile);

        first_name.setText(sp.getString("first_name", ""));
        sur_name.setText(sp.getString("sur_name", ""));
        phone.setText(sp.getString("phone", ""));
        e_mail.setText(sp.getString("e_mail", ""));
    }

}



