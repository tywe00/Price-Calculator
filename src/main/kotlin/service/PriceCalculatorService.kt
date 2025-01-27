package com.example.service

import com.example.client.IHomeApiClient
import com.example.models.*
import kotlin.math.roundToInt

/**
 * Service responsible for calculating the total price of a delivery order
 * by fetching venue coordinates and delivery specifications from a remote API.
 *
 * @property homeApiClient A client that provides methods to fetch venue data.
 */
class PriceCalculatorService(
    private val homeApiClient: IHomeApiClient
) : IPriceCalculatorService {

    override suspend fun calculatePrice(request: DeliveryOrderPriceRequest): PriceCalculationResult {
        return try {
            val coordinates = homeApiClient.fetchVenueCoordinates(request.venueSlug)
                ?: throw IllegalArgumentException("Venue not found")

            val deliverySpecs = homeApiClient.fetchDeliverySpecs(request.venueSlug)
                ?: throw IllegalArgumentException("Delivery details not found")

            val distance = calculateDistance(
                userLat = request.userLat,
                userLon = request.userLon,
                venueLat = coordinates.latitude,
                venueLon = coordinates.longitude
            )

            val deliveryFee = calculateDeliveryFee(distance, deliverySpecs)
            val surcharge = maxOf(0, deliverySpecs.orderMinimumNoSurcharge - request.cartValue)
            val totalPrice = request.cartValue + surcharge + deliveryFee

            val response = DeliveryOrderPriceResponse(
                totalPrice = totalPrice,
                smallOrderSurcharge = surcharge,
                cartValue = request.cartValue,
                delivery = DeliveryDetails(fee = deliveryFee, distance = distance)
            )
            PriceCalculationResult.Success(response)

        } catch (e: IllegalArgumentException) {
            PriceCalculationResult.Failure(e.message ?: "Delivery not possible")
        } catch (e: Exception) {
            PriceCalculationResult.Failure("Unexpected error: ${e.message}")
        }
    }

    /**
     * Computes the delivery fee based on the provided [distance] and the [DeliverySpecs].
     * Determines the correct distance range and applies the formula:
     *   fee = basePrice + a + (b * distance / 10).
     *
     * @param distance The distance in meters between the user and the venue.
     * @param specs The [DeliverySpecs] containing base price, distance ranges, etc.
     * @throws IllegalArgumentException If the distance does not fall into any valid range (meaning delivery is not possible).
     * @return The calculated delivery fee in the lowest currency denomination (e.g., cents).
     */
    private fun calculateDeliveryFee(distance: Int, specs: DeliverySpecs): Int {
        val basePrice = specs.deliveryPricing.basePrice

        // Find distance range that applies
        val range = specs.deliveryPricing.distanceRanges.find { r ->
            distance >= r.min && distance < r.max
        }

        // If no range is found, that means either distance was too large
        // or the only 'range' that matches uses "max=0" => not deliverable.
        if (range == null) {
            throw IllegalArgumentException("Delivery not possible (distance $distance m)")
        }

        val bValue = range.b
        return (basePrice + range.a + (bValue * distance / 10)).toInt()
    }

    /**
     * Calculates the integer distance in meters between two points (lat1, lon1) and (lat2, lon2).
     * Internally calls [computeHaversineDistance].
     *
     * @param userLat Latitude of the user's location.
     * @param userLon Longitude of the user's location.
     * @param venueLat Latitude of the venue's location.
     * @param venueLon Longitude of the venue's location.
     * @return The distance in meters, rounded to the nearest integer.
     */
    private fun calculateDistance(
        userLat: Double,
        userLon: Double,
        venueLat: Double,
        venueLon: Double
    ): Int {
        return computeHaversineDistance(userLat, userLon, venueLat, venueLon)
    }

    /**
     * Uses the Haversine formula to calculate the great-circle distance between two points
     * on Earth given by their latitudes and longitudes.
     *
     * @param lat1 Latitude of the first point in degrees.
     * @param lon1 Longitude of the first point in degrees.
     * @param lat2 Latitude of the second point in degrees.
     * @param lon2 Longitude of the second point in degrees.
     * @return The calculated distance in meters, rounded to the nearest integer.
     */
    private fun computeHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val R = 6371000 // Earth's approximate radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) *
                kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (R * c).roundToInt()
    }
}