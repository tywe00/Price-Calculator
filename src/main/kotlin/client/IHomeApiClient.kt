package com.example.client

import com.example.models.*

/**
 * Defines the contract for fetching venue data from an external source.
 */
interface IHomeApiClient {
    /**
     * Fetches the venue's **static** data from the Home Assignment API.
     *
     * @param venueSlug The unique slug identifier for the venue.
     * @return A [VenueStaticResponse] containing location and other static data.
     * @throws Exception If the underlying GET request fails or response parsing fails.
     */
    suspend fun getVenueStatic(venueSlug: String): VenueStaticResponse

    /**
     * Fetches the venue's **dynamic** data from the Home Assignment API.
     *
     * @param venueSlug The unique slug identifier for the venue.
     * @return A [VenueDynamicResponse] containing dynamic data such as delivery specs.
     * @throws Exception If the underlying GET request fails or response parsing fails.
     */
    suspend fun getVenueDynamic(venueSlug: String): VenueDynamicResponse

    /**
     * Retrieves the coordinates ([longitude, latitude]) of a venue
     * by fetching and parsing the static data.
     *
     * @param venueSlug The unique slug identifier for the venue.
     * @return A [Coordinates] object if found and parsed successfully, or `null` if an error occurs or data is invalid.
     */
    suspend fun fetchVenueCoordinates(venueSlug: String): Coordinates?

    /**
     * Fetches the [DeliverySpecs] for a given venue by calling the **dynamic** endpoint.
     *
     * @param venueSlug The unique slug identifier for the venue.
     * @return The [DeliverySpecs], or `null` if any error occurs (e.g., networking or parsing).
     */
    suspend fun fetchDeliverySpecs(venueSlug: String): DeliverySpecs?
}