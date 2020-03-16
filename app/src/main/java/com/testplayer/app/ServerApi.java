package com.testplayer.app;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ServerApi {
    @GET("tech.php")
    Call<String> getInfo();
}
