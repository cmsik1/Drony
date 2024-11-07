package com.example.imagegpsuploader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;

public class CameraHandler {
    private static final int CAMERA_REQUEST_CODE = 1001;
    private Activity activity;
    private Bitmap capturedImage;
    private OnImageCapturedListener listener;  // 자동 전송을 위한 리스너 추가

    public CameraHandler(Activity activity) {
        this.activity = activity;
        if (activity instanceof OnImageCapturedListener) {
            this.listener = (OnImageCapturedListener) activity;
        }
    }

    // 카메라 열기 및 자동 촬영 설정
    public void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        activity.startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    // onActivityResult에서 호출하여 이미지 설정 후 자동 전송
    public void handleCameraResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            capturedImage = (Bitmap) data.getExtras().get("data");
            if (capturedImage == null) {
                Log.e("CameraHandler", "Captured image is null");
                return;
            }

            // 이미지 인코딩 후 자동 전송
            if (listener != null) {
                listener.onImageCaptured(getEncodedImage());
            }
        }
    }

    // 이미지 Base64로 인코딩
    private String getEncodedImage() {
        if (capturedImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            capturedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] byteArray = baos.toByteArray();
            capturedImage = null; // 메모리 해제
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        }
        return null;
    }

    // 이미지가 캡처되었을 때 호출되는 리스너 인터페이스
    public interface OnImageCapturedListener {
        void onImageCaptured(String encodedImage);
    }
}
