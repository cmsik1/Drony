package com.example.imagegpsuploader2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.graphics.Color;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements GPSManager.OnLocationReceivedListener, DataSender.OnDataSendListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int INTERVAL_MS = 3000; // 3초 간격

    private GPSManager gpsManager;
    private DataSender dataSender;
    private ImageCapture imageCapture;
    private Handler handler;
    private Runnable captureRunnable;
    private String encodedImage;

    private LinearLayout logContainer;
    private ScrollView logScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpsManager = new GPSManager(this);
        dataSender = new DataSender();
        handler = new Handler(Looper.getMainLooper());

        // 로그 컨테이너와 스크롤 뷰 초기화
        logContainer = findViewById(R.id.log_container);
        logScrollView = findViewById(R.id.log_scroll_view);

        checkPermissions();
    }

    // 권한 확인 메서드
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

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "카메라와 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // CameraX로 카메라 시작 설정
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

        // 3초마다 사진을 촬영하고 전송하는 작업 시작
        startRepeatingCapture();
    }

    // 3초마다 자동 촬영 및 데이터 전송을 위한 Runnable 설정
    private void startRepeatingCapture() {
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                captureAndSendData();  // 촬영 및 데이터 전송
                handler.postDelayed(this, INTERVAL_MS);  // 3초 후 다시 실행
            }
        };
        handler.post(captureRunnable);  // 처음 실행
    }

    // 자동 촬영 및 데이터 전송 메서드
    private void captureAndSendData() {
        if (imageCapture != null) {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    encodedImage = encodeImage(image);
                    image.close();
                    gpsManager.getCurrentLocation(MainActivity.this);  // 위치 데이터 요청
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "이미지 캡처 실패", exception);
                }
            });
        }
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

    // GPSManager의 위치 데이터를 수신하여 서버로 전송
    @Override
    public void onLocationReceived(GPSData gpsData) {
        if (encodedImage != null && gpsData != null) {
            dataSender.sendData(
                    gpsData.getLatitude(),
                    gpsData.getLongitude(),
                    gpsData.getAltitude(),
                    gpsData.getHeading(),
                    encodedImage,
                    this
            );
        }
    }

    // 데이터 전송 성공 시 호출
    @Override
    public void onDataSendSuccess(String response) {
        runOnUiThread(() -> addLogEntry("전송 성공: " + response, true));
    }

    // 데이터 전송 실패 시 호출
    @Override
    public void onDataSendFailed(Exception e) {
        runOnUiThread(() -> addLogEntry("전송 실패 - " + e.getMessage(), false));
    }

    // 로그 추가 메서드
    private void addLogEntry(String message, boolean isSuccess) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logMessage = "[" + timeStamp + "] " + message;

        SpannableString spannable = new SpannableString(logMessage);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#B0BEC5")), 0, timeStamp.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int messageColor = isSuccess ? Color.parseColor("#4CAF50") : Color.parseColor("#FF5252");
        spannable.setSpan(new ForegroundColorSpan(messageColor), timeStamp.length() + 3, logMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView logText = new TextView(this);
        logText.setText(spannable);
        logText.setTextSize(16);
        logText.setPadding(8, 8, 8, 8);

        logContainer.addView(logText);
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    // 앱이 종료되거나 중지될 때 반복 작업 제거
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && captureRunnable != null) {
            handler.removeCallbacks(captureRunnable);
        }
    }
}
