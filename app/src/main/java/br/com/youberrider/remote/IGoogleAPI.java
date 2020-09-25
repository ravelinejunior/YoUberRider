package br.com.youberrider.remote;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGoogleAPI {
    @GET("maps/api/directions/json")
    Observable<String> getDirections(
            @Query("mode") String mode,
            @Query("transit_routing_preference") String transitRouting,
            @Query("origin") String origin,
            @Query("destination") String to,
            @Query("key") String key
    );
}
