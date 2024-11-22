package com.example.imagegpsuploader2;

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
    private OnImageCapturedListener listener;

    public CameraHandler(Activity activity, OnImageCapturedListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    // 카메라 열기
    public void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        activity.startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    // onActivityResult에서 호출하여 이미지 캡처 후 인코딩 및 전송
    public void handleCameraResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            if (capturedImage != null) {
                String encodedImage = encodeImage(capturedImage);
                listener.onImageCaptured(encodedImage);
            } else {
                Log.e("CameraHandler", "Captured image is null");
            }
        }
    }

    // 이미지 인코딩
    private String encodeImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // 이미지 캡처 결과를 전달하는 인터페이스
    public interface OnImageCapturedListener {
        void onImageCaptured(String encodedImage);
    }
}
