package com.example.cse476;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
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


    // ---------- FAVORITES ----------

    // GET /rest/v1/favorites?user_id=eq.<uid>&club_id=eq.<cid>&select=*
    @GET("rest/v1/favorites")
    Call<List<Favorite>> getFavorite(
            @Query("user_id") String userIdFilter,   // e.g. "eq.<uid>"
            @Query("club_id") String clubIdFilter,   // e.g. "eq.<cid>"
            @Query("select") String select           // e.g. "*"
    );

    // POST /rest/v1/favorites
    @POST("rest/v1/favorites")
    Call<Void> addFavorite(@Body Favorite favorite);

    // DELETE /rest/v1/favorites?user_id=eq.<uid>&club_id=eq.<cid>
    @DELETE("rest/v1/favorites")
    Call<Void> deleteFavorite(
            @Query("user_id") String userIdFilter,   // "eq.<uid>"
            @Query("club_id") String clubIdFilter    // "eq.<cid>"
    );

    // POST /rest/v1/users
    @POST("rest/v1/users")
    Call<Void> createUser(@Body UserRow user);



}
