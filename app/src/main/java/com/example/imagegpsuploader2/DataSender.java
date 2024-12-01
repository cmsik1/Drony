package com.example.imagegpsuploader2;

import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

public class DataSender {
    private static final String TAG = "DataSender";
    private static final String SERVER_URL = "http://34.64.75.204:5000/gps-data";
    private OkHttpClient client;

    public DataSender() {
        this.client = new OkHttpClient();
    }

    public void sendData(double latitude, double longitude, double altitude, float heading, String encodedImage, OnDataSendListener listener) {
        try {
            JSONObject json = new JSONObject();
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("altitude", altitude);
            json.put("heading", heading);
            json.put("image", encodedImage);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(SERVER_URL).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "데이터 전송 실패", e);
                    listener.onDataSendFailed(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        listener.onDataSendSuccess(response.body().string());
                    } else {
                        listener.onDataSendFailed(new IOException("서버 오류: " + response.code()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "데이터 처리 중 오류 발생", e);
            listener.onDataSendFailed(e);
        }
    }

    public interface OnDataSendListener {
        void onDataSendSuccess(String response);
        void onDataSendFailed(Exception e);
    }
}
