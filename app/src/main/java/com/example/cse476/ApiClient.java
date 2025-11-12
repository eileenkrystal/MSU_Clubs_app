package com.example.cse476;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static SupabaseApi api;

    public static SupabaseApi get(Context ctx) {
        if (api == null) {
            String baseUrl = ctx.getString(R.string.supabase_base_url);
            String anonKey = ctx.getString(R.string.supabase_anon_key);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new SupabaseInterceptor(anonKey))
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            api = retrofit.create(SupabaseApi.class);
        }
        return api;
    }
}
