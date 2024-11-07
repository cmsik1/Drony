package com.example.imagegpsuploader;

import android.content.Context;
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
    private static final String SERVER_URL = "http://34.47.87.250:5000/gps-data";  // PC IP 주소를 입력
    private OkHttpClient client;

    // OkHttpClient 초기화
    public DataSender() {
        this.client = new OkHttpClient();
    }

    // GPS 데이터를 서버로 전송하는 메서드
    public void sendData(double latitude, double longitude, double altitude, float heading, String encodedImage) {
        try {
            // JSON 형식으로 데이터를 구성
            JSONObject json = new JSONObject();
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("altitude", altitude);
            json.put("heading", heading);
            json.put("image", encodedImage);

            // JSON을 RequestBody로 변환
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            // 요청 설정 (POST 방식으로 서버 URL에 전송)
            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();

            // 비동기로 요청 전송
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // 전송 실패 시 로그 출력
                    Log.e(TAG, "데이터 전송 실패", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // 전송 성공 시 서버 응답 출력
                        Log.d(TAG, "데이터 전송 성공: " + response.body().string());
                    } else {
                        // 서버 오류 응답 시 로그 출력
                        Log.e(TAG, "서버 오류 응답: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "데이터 처리 중 오류 발생", e);
        }
    }
}
