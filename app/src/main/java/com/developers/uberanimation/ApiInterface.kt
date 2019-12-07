package com.developers.uberanimation

import com.developers.uberanimation.models.Result

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by Amanjeet Singh on 8/9/18.
 */
interface ApiInterface {
  @GET("maps/api/directions/json")
  fun getDirections(
      @Query("mode") mode: String,
      @Query("transit_routing_preference") routingPreference: String,
      @Query("origin") origin: String,
      @Query("destination") destination: String,
      @Query("key") apiKey: String
  ): Single<Result>
}
