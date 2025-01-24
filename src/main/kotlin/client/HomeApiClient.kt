package com.example.client

import com.example.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json

class HomeApiClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://consumer-api.development.dev.woltapi.com/home-assignment-api/v1/venues"

    // Fetch venue static data
    suspend fun getVenueStatic(venueSlug: String): VenueStaticResponse {
        return httpClient.get("$baseUrl/$venueSlug/static").body()
    }

    // Fetch venue dynamic data
    suspend fun getVenueDynamic(venueSlug: String): VenueDynamicResponse {
        return httpClient.get("$baseUrl/$venueSlug/dynamic").body()
    }

    // Get coordinates from the venue static data
    suspend fun fetchVenueCoordinates(venueSlug: String): Coordinates? {
        return try {
            val venueStatic = getVenueStatic(venueSlug)
            val coordinates = venueStatic.venueRaw.location.coordinates

            if (coordinates.size >= 2) {
                Coordinates(longitude = coordinates[0], latitude = coordinates[1])
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching venue coordinates: ${e.localizedMessage}")
            null
        }
    }

    // Fetch delivery specifications such as min order, base price, distance ranges
    suspend fun fetchDeliverySpecs(venueSlug: String): DeliverySpecs? {
        return try {
            val venueDynamic = getVenueDynamic(venueSlug)
            venueDynamic.venueRaw.deliverySpecs
        } catch (e: Exception) {
            println("Error fetching delivery specs: ${e.localizedMessage}")
            null
        }
    }
}
