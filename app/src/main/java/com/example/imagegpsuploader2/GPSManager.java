package com.example.imagegpsuploader2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class GPSManager implements SensorEventListener {
    private static final String TAG = "GPSManager";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magneticFieldSensor;
    private Activity activity;

    private float[] gravity;
    private float[] geomagnetic;
    private float heading;

    public GPSManager(Activity activity) {
        this.activity = activity;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);

        // SensorManager 초기화
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 센서 등록
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magneticFieldSensor != null) {
            sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void getCurrentLocation(OnLocationReceivedListener listener) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(activity, location -> {
                        if (location != null) {
                            // 센서에서 계산된 헤딩 데이터 사용
                            GPSData gpsData = new GPSData(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    location.getAltitude(),
                                    heading // 헤딩 데이터를 센서 기반으로 업데이트
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                heading = (float) Math.toDegrees(orientation[0]); // 라디안에서 각도로 변환
                if (heading < 0) {
                    heading += 360; // 0~360도로 변환
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 센서 정확도 변경 시 처리 (필요 시 구현)
    }

    public void stop() {
        // 센서 등록 해제
        sensorManager.unregisterListener(this);
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(GPSData gpsData);
    }
}
