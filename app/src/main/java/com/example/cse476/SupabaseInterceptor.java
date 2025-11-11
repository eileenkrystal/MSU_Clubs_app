package com.example.cse476;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SupabaseInterceptor implements Interceptor {

    private final String anonKey;

    public SupabaseInterceptor(String anonKey) {
        this.anonKey = anonKey;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request request = original.newBuilder()
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + anonKey)
                .header("Accept", "application/json")
                .build();
        return chain.proceed(request);
    }
}
