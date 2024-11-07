package com.example.imagegpsuploader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int INTERVAL_MS = 3000; // 3초 간격

    private GPSManager gpsManager;
    private DataSender dataSender;
    private ImageCapture imageCapture;
    private Handler handler;
    private Runnable captureRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpsManager = new GPSManager(this);
        dataSender = new DataSender();
        handler = new Handler(Looper.getMainLooper());

        // 권한 확인 및 요청 후 자동으로 카메라 설정
        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 허용된 경우 카메라 시작
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "카메라와 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "필요한 권한이 없습니다.");
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageCapture(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "카메라 바인딩 실패", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageCapture(@NonNull ProcessCameraProvider cameraProvider) {
        imageCapture = new ImageCapture.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture);

        // 5초마다 사진을 촬영하고 전송하는 작업 시작
        startRepeatingCapture();
    }

    // 5초마다 자동 촬영 및 데이터 전송을 위한 Runnable 설정
    private void startRepeatingCapture() {
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                captureAndSendData();  // 촬영 및 데이터 전송
                handler.postDelayed(this, INTERVAL_MS);  // 5초 후 다시 실행
            }
        };
        handler.post(captureRunnable);  // 처음 실행
    }

    // 자동 촬영 및 데이터 전송 메서드
    private void captureAndSendData() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                String encodedImage = encodeImage(image);
                image.close();

                gpsManager.getCurrentLocation(gpsData -> {
                    if (gpsData != null && encodedImage != null) {
                        dataSender.sendData(
                                gpsData.getLatitude(),
                                gpsData.getLongitude(),
                                gpsData.getAltitude(),
                                gpsData.getHeading(),
                                encodedImage
                        );
                        Log.d(TAG, "데이터 전송 성공");
                    } else {
                        Log.e(TAG, "GPS 또는 이미지 인코딩 오류");
                        Toast.makeText(MainActivity.this, "GPS 또는 이미지 처리 오류", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "이미지 캡처 실패", exception);
            }
        });
    }

    // 이미지 인코딩 메서드
    private String encodeImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytes, 0, bytes.length);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    // 앱이 종료되거나 중지될 때 반복 작업 제거
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && captureRunnable != null) {
            handler.removeCallbacks(captureRunnable);  // 반복 작업 중지
        }
    }
}
