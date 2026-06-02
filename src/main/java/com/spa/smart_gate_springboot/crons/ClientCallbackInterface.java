package com.spa.smart_gate_springboot.crons;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Retrofit client for POSTing delivery reports to a client's callback URL.
 * The URL is per-message, so it is passed dynamically via {@link Url} (an absolute
 * URL overrides the Retrofit baseUrl). The body is a pre-serialised JSON
 * {@link RequestBody} (built with the app ObjectMapper). The full request/response is
 * logged by the interceptor in {@link ClientCallbackRetrofitConfig}.
 */
public interface ClientCallbackInterface {

    @Headers("Content-Type: application/json")
    @POST
    Call<ResponseBody> sendCallback(@Url String url, @Body RequestBody body);
}
