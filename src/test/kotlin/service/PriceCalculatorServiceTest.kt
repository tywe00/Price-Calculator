package com.example.service

import com.example.client.HomeApiClient
import com.example.models.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PriceCalculatorServiceTest {

    private lateinit var homeApiClient: HomeApiClient
    private lateinit var service: PriceCalculatorService

    @BeforeEach
    fun setUp() {
        homeApiClient = mockk()
        service = PriceCalculatorService(homeApiClient)
    }

    @Test
    fun `calculatePrice should throw exception if venue not found`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("invalid-venue") } returns null

        val request = DeliveryOrderPriceRequest(
            venueSlug = "invalid-venue",
            cartValue = 1000,
            userLat = 60.17094,
            userLon = 24.93087
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.calculatePrice(request)
            }
        }

        assertTrue(exception.message!!.contains("Venue not found"))
    }

    @Test
    fun `calculatePrice should throw exception if delivery specs not found`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("test-venue") } returns Coordinates(24.93087, 60.17094)
        coEvery { homeApiClient.fetchDeliverySpecs("test-venue") } returns null

        val request = DeliveryOrderPriceRequest(
            venueSlug = "test-venue",
            cartValue = 1000,
            userLat = 60.17094,
            userLon = 24.93087
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.calculatePrice(request)
            }
        }

        assertTrue(exception.message!!.contains("Delivery details not found"))
    }

    @Test
    fun `calculatePrice should return correct total price`() = runBlocking {
        // 1. Mocking client responses
        // Here we assume your Coordinates class is defined as:
        // data class Coordinates(val longitude: Double, val latitude: Double)
        //
        // In PriceCalculatorService, you call:
        //   calculateDistance(userLat, userLon, coordinates.longitude, coordinates.latitude)
        // which means "coordinates.longitude" is used as the venue LAT
        // and "coordinates.latitude" is used as the venue LON.
        //
        // By setting longitude = 60.17184 and latitude = 24.93087,
        // we ensure a ~100m difference from the user at (60.17094, 24.93087).
        //
        // This yields about 100m distance by Haversine formula.
        coEvery { homeApiClient.fetchVenueCoordinates("test-venue") } returns Coordinates(
            longitude = 60.17184, // Interpreted as venueLat in your code
            latitude = 24.93087   // Interpreted as venueLon in your code
        )

        coEvery { homeApiClient.fetchDeliverySpecs("test-venue") } returns DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 1000, a = 50, b = 1.0, flag = null),
                    DistanceRange(min = 1000, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )

        // 2. Request data
        val request = DeliveryOrderPriceRequest(
            venueSlug = "test-venue",
            cartValue = 1000,
            userLat = 60.17094,  // user lat
            userLon = 24.93087   // user lon
        )

        // 3. Call the service
        val response = service.calculatePrice(request)

        // 4. Verify
        // If distance ~ 100m, fee = 100 + 50 + (1.0 * 100 / 10) = 160
        // total_price = cartValue (1000) + surcharge (0) + fee (160) = 1160
        assertEquals(1160, response.totalPrice)
        assertEquals(160, response.delivery.fee)
        assertEquals(1000, response.cartValue)
        assertEquals(0, response.smallOrderSurcharge)
    }

}
