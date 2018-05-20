package com.example.gabriel.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback {

    private static final int GEOFENCE_RADIUS = 50;
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000;
    private GoogleApiClient mClient;
    private Context mContext;
    private PendingIntent mGeofencePendingIntent;
    private List<Geofence> mGeofenceList;

    @Override
    public void onResult(@NonNull Result result) {
    }
    
    public void registerAllGeofences() {
        if (mClient == null || !mClient.isConnected() || mGeofenceList == null || mGeofenceList.size() == 0) {
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                mClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void unregisterAllGeofences() {
        if (mClient == null || !mClient.isConnected() || mGeofenceList == null || mGeofenceList.size() == 0) {
            return;
        }

        try {
            LocationServices.GeofencingApi.removeGeofences(
                mClient,
                getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public Geofencing(GoogleApiClient mClient, Context mContext) {
        this.mClient = mClient;
        this.mContext = mContext;
        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<>();
    }

    public void updateGeofencesList(PlaceBuffer places) {
        mGeofenceList = new ArrayList<>();

        if (places == null || places.getCount() == 0) {
            return;
        }

        for (Place place : places) {
            String id = place.getId();
            double lat = place.getLatLng().latitude;
            double lon = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(id)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(lat, lon, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            mGeofenceList.add(geofence);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }
}
