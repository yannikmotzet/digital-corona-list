package com.example.testbeacon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {
    protected static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private BeaconManager mBeaconManager;
    Region region;
    TreeSet<String> idList = new TreeSet<String>();
    Map<String, String> rooms = new HashMap<String, String>();

    // server urls
    // url for http request to get room list
    final static String SERVER_URL_GET_ROOM = "http://192.168.1.130:5000/rooms";
    // url for http post to store data on server
    final static String SERVER_URL_POST_DATA = "http://192.168.1.130:5000/store";


    final static String JSON_STRING = "";

    // this method is called when the app is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // popup: user has to consent that his data will be stored
        showConsentsPopup();

        // layout: bottom navigation view to navigate between fragments
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        NavController navController = Navigation.findNavController(this,  R.id.fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // http request for room list
        RoomListRequest();

        // for bebugging purpose: send http request for storing data on server
        //sendData("htwg-f123");

        // bluetooth communication
        rooms.put("0x00112233445566778898", "HTWG-F123");
        SharedPreferences sp = getSharedPreferences("RoomList", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("0x00112233445566778898", "htwg-f123");
        editor.putString("0x00112233445566778899", "htwg-f124");
        editor.apply();

        // TODO comment method
        verifyBluetooth();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_REQUEST_FINE_LOCATION);
        }
        else {
            //Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "json_string");
        Log.d(TAG, JSON_STRING);
    }


    // this method is called when the app is resumed from background
    public void onResume(){
        super.onResume();
        // TODO comment method
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        mBeaconManager.bind(this);

    }

    // this method is called when the app is closed but not exited
    @Override
    public void onPause() {
        super.onPause();
        // TODO comment method
        mBeaconManager.unbind(this);
    }


    // TODO comment method
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

    // TODO comment method
    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Bluetooth not enabled")
                        .setMessage("Please enable bluetooth in settings and restart this application.")
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //finish();
                        //System.exit(0);
                    }
                })
                        .show();
            }
        }
        catch (RuntimeException e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Bluetooth LE not available")
                    .setMessage("Sorry, this device does not support Bluetooth LE.")
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    //finish();
                    //System.exit(0);
                }
            })
                .show();
        }
    }

    // TODO comment method
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

    // TODO comment method
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

                    // get beacon id
                    String id = namespaceId.toString();
                    // get room name
                    SharedPreferences sp = getSharedPreferences("RoomList", MODE_PRIVATE);
                    String room = sp.getString(id, "");

                    // idText.setText("Raum: " + roomname);
                    idText.setText("room: " + room);
                    distanceText.setText("distance: " + distance + " m");
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    distanceText.setLayoutParams(params);
                    idText.setLayoutParams(params);
                    Button postButton = new Button(this);
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sendData(room);
                        }
                    };
                    postButton.setOnClickListener(listener);
                    postButton.setLayoutParams(params);
                    postButton.setText("SEND");
                    LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
                    linearLayout.addView(idText);
                    linearLayout.addView(distanceText);
                    linearLayout.addView(postButton);
                    idList.add(namespaceId.toString());

                    // turn off swipe to refresh animation
                    final SwipeRefreshLayout pullToRefresh = this.findViewById(R.id.swiperefresh);
                    pullToRefresh.setRefreshing(false);

                }
            }

        }
    }

    // TODO comment method
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

    // TODO comment method
    public void stopScan (View v){
        try{
            mBeaconManager.stopRangingBeaconsInRegion(region);
        }
        catch (RemoteException e){
            e.printStackTrace();
        }
    }

    // TODO comment method
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    // http post for storing user data on server
    public void sendData (String room_name){
        SharedPreferences sp = getSharedPreferences("UserData", MODE_PRIVATE);
        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat time_formatter = new SimpleDateFormat("HH:mm");
        Date now = new Date();
        Log.d(TAG, room_name);
        // data
        String room = room_name;
        String date = date_formatter.format(now);
        String time = time_formatter.format(now);
        String first_name = sp.getString("first_name", "");
        String sur_name = sp.getString("sur_name", "");
        String phone = sp.getString("phone", "");
        String e_mail = sp.getString("e_mail", "");
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
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest req = new JsonObjectRequest(SERVER_URL_POST_DATA, obj,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    String answer;
                    try {
                        Integer error = response.getInt("error");
                        if (error == 0) {
                            String room = response.getJSONObject("answer").getString("room");
                            String event = response.getJSONObject("answer").getString("event_name");
                            answer = "data stored:" + " " + event + " (" + room + ")";
                        }
                        else {
                            answer = response.getString("answer");
                        }
                        Toast.makeText(MainActivity.this, answer, Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(MainActivity.this, "data not stored", Toast.LENGTH_LONG).show();
                }
        });

        requestQueue.add(req);
    }

    // save user data in shared preferences
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

    // retrieve user data from shared preferences and put text in text fields of profile fragment
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

    // retrieve beacon-id room list from server and save to shared preferences
    private void RoomListRequest(){
        SharedPreferences sp = getSharedPreferences("RoomList", MODE_PRIVATE);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, SERVER_URL_GET_ROOM, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //return response;
                //.makeText(MainActivity.this, "connected to server", Toast.LENGTH_LONG).show();
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.getJSONArray("rooms");
                    SharedPreferences.Editor editor = sp.edit();
                    editor.clear();
                    for (int i = 0; i < arr.length(); i++){
                        String id = arr.getJSONObject(i).getString("id");
                        String room = arr.getJSONObject(i).getString("room");
                        editor.putString(id, room);
                        editor.apply();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "no connection to server", Toast.LENGTH_LONG).show();
            }
        });
        queue.add(stringRequest);
    }

    // save state of settings switch for background scanning to shared preferences
    public void saveSwitchState(boolean isChecked) {
        SharedPreferences sp_settings = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp_settings.edit();
        editor.putBoolean("switch_scanning", isChecked);
        editor.apply();
    }

    // retrieve state of settings switch for background scanning from shared preferences
    public boolean getSwitchState(){
        SharedPreferences sp_settings = getSharedPreferences("Settings", MODE_PRIVATE);
        return sp_settings.getBoolean("switch_scanning", true);
    }

    // force dark theme
    public void setDarkMode() {
        int isNightTheme = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (isNightTheme == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (isNightTheme == Configuration.UI_MODE_NIGHT_NO ){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    // show consents popup
    public void showConsentsPopup() {
        SharedPreferences sp_settings = getSharedPreferences("Settings", MODE_PRIVATE);

        if (sp_settings.getBoolean("consents", false) == false){
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Consent")
                    .setMessage("By tapping \"Accept\", you consent to the App to store your personal data on a App's server system. Your data will be deleted after 4 weeks.")
                    .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = sp_settings.edit();
                            editor.putBoolean("consents", true);
                            editor.apply();
                        }
                    })
                    .setNegativeButton("Do not accept", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            System.exit(0);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

}



