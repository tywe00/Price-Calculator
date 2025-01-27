package com.example.service

import com.example.client.HomeApiClient
import com.example.models.*
import io.mockk.coEvery
import io.mockk.mockk
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
    fun `calculatePrice should fail if venue not found`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("invalid-venue") } returns null

        val request = DeliveryOrderPriceRequest(
            venueSlug = "invalid-venue",
            cartValue = 1000,
            userLat = 60.17094,
            userLon = 24.93087
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                fail("Expected a failure, but got a success with: ${result.response}")
            }
            is PriceCalculationResult.Failure -> {
                assertTrue(
                    result.errorMessage.contains("Venue not found"),
                    "Expected error message to contain 'Venue not found'"
                )
            }
        }
    }

    @Test
    fun `calculatePrice should fail if delivery specs not found`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("test-venue") } returns Coordinates(
            longitude = 24.93087,
            latitude = 60.17094
        )
        coEvery { homeApiClient.fetchDeliverySpecs("test-venue") } returns null

        val request = DeliveryOrderPriceRequest(
            venueSlug = "test-venue",
            cartValue = 1000,
            userLat = 60.17094,
            userLon = 24.93087
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                fail("Expected a failure, but got success with: ${result.response}")
            }
            is PriceCalculationResult.Failure -> {
                assertTrue(
                    result.errorMessage.contains("Delivery details not found"),
                    "Expected error message to contain 'Delivery details not found'"
                )
            }
        }
    }

    @Test
    fun `calculatePrice should return correct total price`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("test-venue") } returns Coordinates(
            longitude = 24.9300,
            latitude = 60.1709
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

        val request = DeliveryOrderPriceRequest(
            venueSlug = "test-venue",
            cartValue = 1000,
            userLat = 60.1700,
            userLon = 24.9300
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                val response = result.response
                // distance ~100 => fee=100+50+(1*100/10)=160 => total=1160
                assertEquals(1160, response.totalPrice)
                assertEquals(160, response.delivery.fee)
                assertEquals(1000, response.cartValue)
                assertEquals(0, response.smallOrderSurcharge)
            }
            is PriceCalculationResult.Failure -> {
                fail("Expected a successful price calculation, but got error: ${result.errorMessage}")
            }
        }
    }

    @Test
    fun `calculatePrice should fail if distance is out of any valid range`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("far-venue") } returns Coordinates(
            longitude = 24.93087,
            latitude = 60.17094
        )
        coEvery { homeApiClient.fetchDeliverySpecs("far-venue") } returns DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 200,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 500, a = 50, b = 1.0, flag = null),
                    // "max=0" means not available beyond 500m
                    DistanceRange(min = 500, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )

        val request = DeliveryOrderPriceRequest(
            venueSlug = "far-venue",
            cartValue = 1200,
            userLat = 61.0,
            userLon = 25.0
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                fail("Expected a failure because distance is out of range, got success: ${result.response}")
            }
            is PriceCalculationResult.Failure -> {
                assertTrue(
                    result.errorMessage.contains("Delivery not possible"),
                    "Expected 'Delivery not possible' message but got '${result.errorMessage}'"
                )
            }
        }
    }

    @Test
    fun `calculatePrice should add small order surcharge if cart value is too low`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("surcharge-venue") } returns Coordinates(
            longitude = 24.9300,
            latitude = 60.1705
        )
        coEvery { homeApiClient.fetchDeliverySpecs("surcharge-venue") } returns DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 2000, a = 50, b = 0.0, flag = null),
                    DistanceRange(min = 2000, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )

        val request = DeliveryOrderPriceRequest(
            venueSlug = "surcharge-venue",
            cartValue = 800,
            userLat = 60.1700,
            userLon = 24.9300
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                val response = result.response
                assertEquals(1150, response.totalPrice)
                assertEquals(150, response.delivery.fee)
                assertEquals(200, response.smallOrderSurcharge)
                assertEquals(800, response.cartValue)
            }
            is PriceCalculationResult.Failure -> {
                fail("Expected success with small order surcharge, but got error: ${result.errorMessage}")
            }
        }
    }

    @Test
    fun `calculatePrice should handle exact boundary distance`() = runBlocking {
        coEvery { homeApiClient.fetchVenueCoordinates("boundary-venue") } returns Coordinates(
            longitude = 24.9310,
            latitude = 60.1710
        )
        coEvery { homeApiClient.fetchDeliverySpecs("boundary-venue") } returns DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 1000, a = 50, b = 1.0, flag = null),
                    DistanceRange(min = 1000, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )

        val request = DeliveryOrderPriceRequest(
            venueSlug = "boundary-venue",
            cartValue = 1000,
            userLat = 60.1610,
            userLon = 24.9310
        )

        val result = service.calculatePrice(request)

        when (result) {
            is PriceCalculationResult.Success -> {
                fail("Expected failure for distance=1000 boundary, but got success: ${result.response}")
            }
            is PriceCalculationResult.Failure -> {
                assertTrue(
                    result.errorMessage.contains("Delivery not possible"),
                    "Expected 'Delivery not possible' at boundary distance=1000, got: ${result.errorMessage}"
                )
            }
        }
    }
}