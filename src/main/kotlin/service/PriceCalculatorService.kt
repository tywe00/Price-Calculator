package com.example.service

import com.example.client.HomeApiClient
import com.example.models.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PriceCalculatorService(private val homeApiClient: HomeApiClient) {

    suspend fun calculatePrice(request: DeliveryOrderPriceRequest): DeliveryOrderPriceResponse {
        // 1. Fetch venue coordinates
        val coordinates = homeApiClient.fetchVenueCoordinates(request.venueSlug)
            ?: throw IllegalArgumentException("Venue not found")

        // 2. Fetch delivery specs
        val deliverySpecs = homeApiClient.fetchDeliverySpecs(request.venueSlug)
            ?: throw IllegalArgumentException("Delivery details not found")

        // 3. Calculate delivery distance
        val distance = calculateDistance(
            request.userLat,
            request.userLon,
            coordinates.longitude,  // because your data class's first field is 'longitude'
            coordinates.latitude
        )

        // 4. Determine delivery fee
        val deliveryFee = calculateDeliveryFee(distance, deliverySpecs)

        // 5. Calculate small order surcharge
        val surcharge = maxOf(0, deliverySpecs.orderMinimumNoSurcharge - request.cartValue)

        // 6. Calculate total price
        val totalPrice = request.cartValue + surcharge + deliveryFee

        // 7. Build and return a strongly typed response
        return DeliveryOrderPriceResponse(
            totalPrice = totalPrice,
            smallOrderSurcharge = surcharge,
            cartValue = request.cartValue,
            delivery = DeliveryDetails(
                fee = deliveryFee,
                distance = distance
            )
        )
    }

    private fun calculateDeliveryFee(distance: Int, specs: DeliverySpecs): Int {
        val basePrice = specs.deliveryPricing.basePrice

        // Find distance range that applies
        val range = specs.deliveryPricing.distanceRanges.find { r ->
            distance >= r.min && distance < r.max  // Strictly < r.max
        }

        // If we didn't find any range, that means either distance was too large
        // or the only 'range' that matches uses "max=0" => not possible.
        if (range == null) {
            // Throw an exception => let your route respond with 400
            throw IllegalArgumentException("Delivery not possible (distance $distance m)")
        }

        // Convert b to a usable Double if needed; else use 0.0
        val bValue = range.b
        return (basePrice + range.a + (bValue * distance / 10)).toInt()
    }

    private fun calculateDistance(
        userLat: Double,
        userLon: Double,
        venueLat: Double,
        venueLon: Double
    ): Int {
        // Your distance calculation (Haversine or otherwise)
        return computeHaversineDistance(userLat, userLon, venueLat, venueLon)
    }

    private fun computeHaversineDistance(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Int {
        // Example Haversine
        val R = 6371000 // Earth radius in meters
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