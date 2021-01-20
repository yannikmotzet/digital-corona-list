package com.example.testbeacon;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

// the application class needs to implement BootstrapNotifier
// this is responsible for an automatically background scan if the app is closed
// in the callback-methods of this interface, its possible to handle the scan-response
public class BackgroundApplication extends Application implements BootstrapNotifier {
    private static  final String TAG = "BackgroundApplication";
    private RegionBootstrap regionBootstrap;

    // this method is called when the app is started
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        // here you create an instance of the beacon manager, which is responsible for starting/stopping scanning
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
        // here you define that the beacon advertise with the eddystone protocoll
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
        // here you create a new region in which the beacons are advertising
        Region region = new Region("com.example.myapp.boostrapRegion", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        // create channel for notification
        createNotificationChannel();
    }

    // this method is executed when a beacon comes into the defined region
    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "Got a didEnterRegion callnnn");
        // with the following line it's necessary to open the app before you get new notifications for the same beacon
        regionBootstrap.disable();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        //this.startActivity(intent);

        // define the notification (content, appearance, action)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.ic_virus_notification)
                .setContentTitle("A room was detected!")
                .setContentText("Please tap on this notification to log your attendance.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // remove notification after clicking
                .setAutoCancel(true)
                // by pressing on notification, the app will be opened
                .setContentIntent(pendingIntent);

        // push the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());

    }

    @Override
    public void didExitRegion(Region region) {
        //Don't care
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        //Don't care
    }

    // create channel for notification (a channel is a definition about the notification's behaviour)
    private void createNotificationChannel(){
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("When a beacon is detected, the app will notify you.");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
