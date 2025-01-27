package com.example.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HomeApiClientTest {

    /**
     * Verifies that getVenueStatic successfully parses a valid JSON response.
     */
    @Test
    fun `getVenueStatic should return VenueStaticResponse on success`() = runBlocking {
        // 1. MockEngine returning successful JSON
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "venue_raw": {
                        "location": {
                          "coordinates": [24.93087, 60.17094]
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val homeApiClient = HomeApiClient(httpClient)
        val result = homeApiClient.getVenueStatic("test-venue")

        assertNotNull(result)
        assertEquals(2, result.venueRaw.location.coordinates.size)
        assertEquals(24.93087, result.venueRaw.location.coordinates[0])
        assertEquals(60.17094, result.venueRaw.location.coordinates[1])
    }

    /**
     * Ensures getVenueStatic throws an exception on non-200 response (e.g., 404).
     */
    @Test
    fun `getVenueStatic should throw on http error`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respondError(HttpStatusCode.NotFound)
        }

        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        assertThrows(ClientRequestException::class.java) {
            runBlocking {
                homeApiClient.getVenueStatic("invalid-venue")
            }
        }
    }

    /**
     * Verifies that getVenueDynamic successfully parses a valid JSON response.
     */
    @Test
    fun `getVenueDynamic should return VenueDynamicResponse on success`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "venue_raw": {
                        "delivery_specs": {
                          "order_minimum_no_surcharge": 1000,
                          "delivery_pricing": {
                            "base_price": 199,
                            "distance_ranges": []
                          }
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val homeApiClient = HomeApiClient(httpClient)
        val result = homeApiClient.getVenueDynamic("test-venue-dynamic")

        assertNotNull(result)
        assertEquals(1000, result.venueRaw.deliverySpecs.orderMinimumNoSurcharge)
        assertEquals(199, result.venueRaw.deliverySpecs.deliveryPricing.basePrice)
    }

    /**
     * Ensures getVenueDynamic throws on non-200 (e.g., 500) response from server.
     */
    @Test
    fun `getVenueDynamic should throw on server error`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respondError(HttpStatusCode.InternalServerError)
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        assertThrows(ServerResponseException::class.java) {
            runBlocking {
                homeApiClient.getVenueDynamic("broken-venue")
            }
        }
    }

    /**
     * Checks fetchVenueCoordinates returns valid coordinates from getVenueStatic.
     */
    @Test
    fun `fetchVenueCoordinates should return Coordinates if static data is valid`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "venue_raw": {
                        "location": {
                          "coordinates": [24.93, 60.17]
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        val coords = homeApiClient.fetchVenueCoordinates("valid-venue")
        assertNotNull(coords)
        assertEquals(24.93, coords!!.longitude)
        assertEquals(60.17, coords.latitude)
    }

    /**
     * Checks fetchVenueCoordinates returns null if there's an exception (e.g., bad JSON).
     */
    @Test
    fun `fetchVenueCoordinates should return null if static data is invalid`() = runBlocking {
        val mockEngine = MockEngine { request ->
            // Return malformed JSON to cause an exception
            respond(content = """{ "invalid_json": "" """, status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        val coords = homeApiClient.fetchVenueCoordinates("bad-json-venue")
        assertNull(coords, "Expected null if JSON fails to parse or lacks valid coordinates.")
    }

    /**
     * Verifies fetchVenueCoordinates returns null if the coordinate array is too small.
     */
    @Test
    fun `fetchVenueCoordinates should return null if coordinate array is incomplete`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "venue_raw": {
                        "location": {
                          "coordinates": [24.93]
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)
        val coords = homeApiClient.fetchVenueCoordinates("incomplete-array")
        assertNull(coords, "Expected null if coordinates array has <2 elements.")
    }

    /**
     * Checks fetchDeliverySpecs returns valid data from getVenueDynamic.
     */
    @Test
    fun `fetchDeliverySpecs should return DeliverySpecs on success`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                      "venue_raw": {
                        "delivery_specs": {
                          "order_minimum_no_surcharge": 800,
                          "delivery_pricing": {
                            "base_price": 150,
                            "distance_ranges": [
                              { "min": 0, "max": 500, "a": 50, "b": 1.0, "flag": null }
                            ]
                          }
                        }
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        val specs = homeApiClient.fetchDeliverySpecs("some-dynamic-venue")
        assertNotNull(specs)
        assertEquals(800, specs!!.orderMinimumNoSurcharge)
        assertEquals(150, specs.deliveryPricing.basePrice)
        assertEquals(1, specs.deliveryPricing.distanceRanges.size)
    }

    /**
     * Checks fetchDeliverySpecs returns null if an exception occurs (e.g., 404).
     */
    @Test
    fun `fetchDeliverySpecs should return null on exception`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respondError(HttpStatusCode.NotFound)
        }
        val httpClient = HttpClient(mockEngine) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val homeApiClient = HomeApiClient(httpClient)

        val specs = homeApiClient.fetchDeliverySpecs("unknown-dynamic-venue")
        assertNull(specs, "Expected null when an exception is thrown (404).")
    }
}