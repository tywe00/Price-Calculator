package com.example.service

import com.example.client.HomeApiClient
import com.example.models.DeliveryOrderPriceRequest
import com.example.models.DeliveryOrderPriceResponse
import com.example.models.DeliveryDetails

class PriceCalculatorService(private val homeApiClient: HomeApiClient) {

    suspend fun calculatePrice(request: DeliveryOrderPriceRequest): DeliveryOrderPriceResponse {
        // Fetch static and dynamic data from Home API
        val staticData = homeApiClient.getVenueStatic(request.venueSlug)
        val dynamicData = homeApiClient.getVenueDynamic(request.venueSlug)

        // Calculate small order surcharge
        val smallOrderSurcharge = if (request.cartValue < staticData.minimumOrderValue) {
            staticData.minimumOrderValue - request.cartValue
        } else {
            0
        }

        // Delivery fee from static data
        val deliveryFee = staticData.deliveryFee

        // Total price calculation
        val totalPrice = request.cartValue + deliveryFee + smallOrderSurcharge

        // Build response
        val deliveryDetails = DeliveryDetails(
            fee = deliveryFee,
            distance = dynamicData.distance
        )

        return DeliveryOrderPriceResponse(
            totalPrice = totalPrice,
            smallOrderSurcharge = smallOrderSurcharge,
            cartValue = request.cartValue,
            delivery = deliveryDetails
        )
    }
}
