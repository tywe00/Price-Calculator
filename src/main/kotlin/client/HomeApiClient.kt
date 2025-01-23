package com.example.client

import com.example.models.VenueDynamic
import com.example.models.VenueStatic
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class HomeApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://consumer-api.development.dev.woltapi.com/home-assignment-api/v1/venues"

    suspend fun getVenueStatic(venueSlug: String): VenueStatic {
        val url = "$baseUrl/$venueSlug/static"
        return httpClient.get(url).body()
    }

    suspend fun getVenueDynamic(venueSlug: String): VenueDynamic {
        val url = "$baseUrl/$venueSlug/dynamic"
        return httpClient.get(url).body()
    }
}
