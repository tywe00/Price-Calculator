package com.example.client

import com.example.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Concrete implementation of [IHomeApiClient], fetching venue data
 * from Wolt's Home Assignment API via Ktor.
 */
class HomeApiClient(
    private val httpClient: HttpClient
) : IHomeApiClient {

    private val baseUrl = "https://consumer-api.development.dev.woltapi.com/home-assignment-api/v1/venues"

    override suspend fun getVenueStatic(venueSlug: String): VenueStaticResponse {
        return httpClient.get("$baseUrl/$venueSlug/static").body()
    }

    override suspend fun getVenueDynamic(venueSlug: String): VenueDynamicResponse {
        return httpClient.get("$baseUrl/$venueSlug/dynamic").body()
    }

    override suspend fun fetchVenueCoordinates(venueSlug: String): Coordinates? {
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

    override suspend fun fetchDeliverySpecs(venueSlug: String): DeliverySpecs? {
        return try {
            val venueDynamic = getVenueDynamic(venueSlug)
            venueDynamic.venueRaw.deliverySpecs
        } catch (e: Exception) {
            println("Error fetching delivery specs: ${e.localizedMessage}")
            null
        }
    }
}
