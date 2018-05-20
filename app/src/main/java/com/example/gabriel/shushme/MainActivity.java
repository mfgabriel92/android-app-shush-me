package com.example.gabriel.shushme;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.gabriel.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity
    extends AppCompatActivity
    implements GoogleApiClient.ConnectionCallbacks,
               GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int PLACE_PICKER_REQUEST = 222;
    private boolean mIsChecked;
    private RecyclerView mRvMainActivity;
    private Switch mSwitch;
    private PlaceListAdapter mAdapter;
    private GoogleApiClient mClient;
    private Geofencing mGeofencing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapter = new PlaceListAdapter(this, null);

        mSwitch = findViewById(R.id.switchEnable);
        mIsChecked = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled), false);
        mSwitch.setChecked(mIsChecked);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                mIsChecked = isChecked;
                editor.commit();

                if (isChecked) {
                    mGeofencing.registerAllGeofences();
                } else {
                    mGeofencing.unregisterAllGeofences();
                }
            }
        });

        mRvMainActivity = findViewById(R.id.rvMainActivity);
        mRvMainActivity.setLayoutManager(new LinearLayoutManager(this));
        mRvMainActivity.setAdapter(mAdapter);

        mClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .addApi(Places.GEO_DATA_API)
            .enableAutoManage(this, this)
            .build();

        mGeofencing = new Geofencing(mClient, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CheckBox locationPermission = findViewById(R.id.cbLocationPermission);

        if (checkFineLocationPermission() != PackageManager.PERMISSION_GRANTED) {
            locationPermission.setChecked(false);
        } else {
            locationPermission.setChecked(true);
            locationPermission.setEnabled(false);
        }

        CheckBox ringerPermission = findViewById(R.id.cbRingerModePermission);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 24 && nm.isNotificationPolicyAccessGranted()) {
            ringerPermission.setChecked(false);
        } else {
            ringerPermission.setChecked(true);
            ringerPermission.setEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);

            if (place == null) {
                Toast.makeText(this, "No place selected", Toast.LENGTH_SHORT).show();
                return;
            }

            String placeID = place.getId();

            ContentValues values = new ContentValues();
            values.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);

            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, values);
            refreshPlacesData();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        refreshPlacesData();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "API Client connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "API Client connection failed");
    }

    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(
            MainActivity.this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            PERMISSIONS_REQUEST_FINE_LOCATION
        );
    }

    public void onAddNewLocationButtonClicked(View view) {
        if (checkFineLocationPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();

        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent intent = builder.build(this);

            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onRingerPermissionClicked(View view) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private int checkFineLocationPermission() {
        return ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void refreshPlacesData() {
        Cursor data = getContentResolver().query(
            PlaceContract.PlaceEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (data == null || data.getCount() == 0) {
            return;
        }

        List<String> ids = new ArrayList<>();

        while (data.moveToNext()) {
            ids.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        PendingResult<PlaceBuffer> pendingResult = Places.GeoDataApi.getPlaceById(mClient, ids.toArray(new String[ids.size()]));
        pendingResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapData(places);
                mGeofencing.updateGeofencesList(places);
                if (mIsChecked) {
                    mGeofencing.registerAllGeofences();
                }
            }
        });
    }
}
