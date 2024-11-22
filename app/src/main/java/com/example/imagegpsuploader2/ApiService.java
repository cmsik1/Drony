package com.example.imagegpsuploader2;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/upload")
    Call<Void> uploadData(@Body RequestBody body);
}

