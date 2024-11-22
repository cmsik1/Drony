package com.example.imagegpsuploader2;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class GPSManager {
    private static final String TAG = "GPSManager";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Activity activity;

    public GPSManager(Activity activity) {
        this.activity = activity;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public void getCurrentLocation(OnLocationReceivedListener listener) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(activity, location -> {
                        if (location != null) {
                            GPSData gpsData = new GPSData(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    location.getAltitude(),
                                    location.getBearing()
                            );
                            listener.onLocationReceived(gpsData);
                        } else {
                            Log.e(TAG, "Failed to get GPS data");
                            Toast.makeText(activity, "GPS 데이터를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e(TAG, "위치 권한이 필요합니다.");
            Toast.makeText(activity, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(GPSData gpsData);
    }
}
