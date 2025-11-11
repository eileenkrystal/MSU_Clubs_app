package com.example.cse476;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseApi {
    // GET /rest/v1/clubs?select=*
    @GET("rest/v1/clubs")
    Call<List<Club>> listClubs(@Query("select") String select);

    // Example search: /rest/v1/clubs?name=ilike.*{q}*&select=*
    @GET("rest/v1/clubs")
    Call<List<Club>> searchClubs(@Query("name") String nameFilter, @Query("select") String select);

    // GET /rest/v1/clubs?id=eq.<uuid>&select=*
    @GET("rest/v1/clubs")
    Call<List<Club>> getClubById(@Query("id") String idEq, @Query("select") String select);
}
