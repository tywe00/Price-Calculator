package com.example

import com.example.client.IHomeApiClient
import com.example.models.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json

/**
 * Integration tests for the Ktor application defined in [module].
 *
 * These tests use Ktor's [testApplication] to spin up an in-memory server
 * and send HTTP requests to verify routing and parameter handling in
 * "/api/v1/delivery-order-price" endpoint.
 */
class ApplicationTest {

    /**
     * Helper function to create a mocked [IHomeApiClient].
     *
     * @param venueSlug The venue slug to be mocked.
     * @param staticResponse The mocked static response.
     * @param dynamicResponse The mocked dynamic response.
     * @return A mocked [IHomeApiClient] with predefined responses.
     */
    private fun createMockHomeApiClient(
        venueSlug: String,
        staticResponse: VenueStaticResponse?,
        dynamicResponse: DeliverySpecs?
    ): IHomeApiClient {
        val mock = mockk<IHomeApiClient>()

        coEvery { mock.fetchVenueCoordinates(venueSlug) } returns staticResponse?.let {
            Coordinates(
                longitude = it.venueRaw.location.coordinates[0],
                latitude = it.venueRaw.location.coordinates[1]
            )
        }

        coEvery { mock.fetchDeliverySpecs(venueSlug) } returns dynamicResponse

        return mock
    }

    /**
     * Verifies that a request with **all valid** query parameters returns a 200 OK status
     * with the expected response body when the external API returns valid data.
     */
    @Test
    fun `test valid request parameters`() = testApplication {
        // Create mocked HomeApiClient with expected responses
        val venueSlug = "home-assignment-venue-helsinki"
        val staticResponse = VenueStaticResponse(
            venueRaw = VenueRaw(
                location = Location(
                    coordinates = listOf(24.93087, 60.17094)
                )
            )
        )
        val deliverySpecs = DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 1000, a = 50, b = 1.0, flag = null),
                    DistanceRange(min = 1000, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )
        val mockHomeApiClient = createMockHomeApiClient(venueSlug, staticResponse, deliverySpecs)

        // Inject the mocked HomeApiClient into the application module
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // Perform the GET request with valid parameters
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", venueSlug)
            parameter("cart_value", "1000")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        // Assert the response status is OK
        assertEquals(HttpStatusCode.OK, response.status)

        // Parse and assert response body
        val responseBody = response.bodyAsText()
        val deliveryResponse = Json.decodeFromString<DeliveryOrderPriceResponse>(responseBody)

        assertEquals(1150, deliveryResponse.totalPrice)
        assertEquals(150, deliveryResponse.delivery.fee)
        assertEquals(1000, deliveryResponse.cartValue)
        assertEquals(0, deliveryResponse.smallOrderSurcharge)
    }

    /**
     * Tests that the endpoint returns 400 if the "venue_slug" parameter is missing.
     */
    @Test
    fun `test missing venue_slug parameter`() = testApplication {
        // Inject a mocked HomeApiClient (behavior irrelevant for this test)
        val mockHomeApiClient = mockk<IHomeApiClient>()
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // "venue_slug" is missing
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("cart_value", "1000")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Missing or empty query parameter: venue_slug"),
            "Expected error about missing 'venue_slug', got: $body"
        )
    }

    /**
     * Tests that the endpoint returns 400 if the "cart_value" parameter is missing.
     */
    @Test
    fun `test missing cart_value parameter`() = testApplication {
        // Inject a mocked HomeApiClient (behavior irrelevant for this test)
        val mockHomeApiClient = mockk<IHomeApiClient>()
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // Missing "cart_value"
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", "home-assignment-venue-helsinki")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Missing or empty query parameter: cart_value"),
            "Expected error about missing 'cart_value', got: $body"
        )
    }

    /**
     * Tests that the endpoint returns 400 if the "user_lat" parameter is invalid (not a number).
     */
    @Test
    fun `test invalid user_lat parameter`() = testApplication {
        // Inject a mocked HomeApiClient (behavior irrelevant for this test)
        val mockHomeApiClient = mockk<IHomeApiClient>()
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // user_lat is set to an invalid string
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", "home-assignment-venue-helsinki")
            parameter("cart_value", "1000")
            parameter("user_lat", "not-a-number")
            parameter("user_lon", "24.93087")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Invalid query parameter: user_lat"),
            "Expected error about invalid 'user_lat', got: $body"
        )
    }

    /**
     * Tests that the endpoint returns 400 if the "cart_value" is not an integer.
     */
    @Test
    fun `test invalid cart_value parameter`() = testApplication {
        // Inject a mocked HomeApiClient (behavior irrelevant for this test)
        val mockHomeApiClient = mockk<IHomeApiClient>()
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // cart_value is set to an invalid string
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", "home-assignment-venue-helsinki")
            parameter("cart_value", "invalid-int")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Invalid query parameter: cart_value"),
            "Expected error about invalid 'cart_value', got: $body"
        )
    }

    /**
     * Tests that a request with an empty parameter also yields a 400 error.
     */
    @Test
    fun `test empty venue_slug parameter`() = testApplication {
        // Inject a mocked HomeApiClient (behavior irrelevant for this test)
        val mockHomeApiClient = mockk<IHomeApiClient>()
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // "venue_slug" is empty string
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", "")
            parameter("cart_value", "1000")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Missing or empty query parameter: venue_slug"),
            "Expected error about missing 'venue_slug', got: $body"
        )
    }

    /**
     * Demonstrates a scenario where the user passes all parameters but the external API
     * might respond with data that leads to a failure, e.g., an unknown venue.
     * We expect a 400 from the service if it can't find the venue.
     */
    @Test
    fun `test unknown venue_slug leads to 400`() = testApplication {
        // Create mocked HomeApiClient with null responses to simulate unknown venue
        val venueSlug = "unknown-venue-xyz"
        val mockHomeApiClient = createMockHomeApiClient(
            venueSlug,
            staticResponse = null,      // Simulate venue not found
            dynamicResponse = null      // Not needed as fetchVenueCoordinates returns null
        )

        // Inject the mocked HomeApiClient into the application module
        application {
            module(homeApiClient = mockHomeApiClient)
        }

        // Perform the GET request with an unknown venue_slug
        val response = client.get("/api/v1/delivery-order-price") {
            parameter("venue_slug", venueSlug)
            parameter("cart_value", "1000")
            parameter("user_lat", "60.17094")
            parameter("user_lon", "24.93087")
        }

        // The service should respond with BAD_REQUEST because the external client returns null
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Venue not found") || body.contains("Delivery not possible"),
            "Expected 'Venue not found' or similar error message, got: $body"
        )
    }
}