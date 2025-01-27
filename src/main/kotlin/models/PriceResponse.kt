package com.example.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Represents the response for a delivery order price calculation
@Serializable
data class DeliveryOrderPriceResponse(
    @SerialName("total_price") val totalPrice: Int, // Total price of the order including all fees
    @SerialName("small_order_surcharge") val smallOrderSurcharge: Int, // Additional charge for orders below a certain value
    @SerialName("cart_value") val cartValue: Int, // Value of items in the cart
    val delivery: DeliveryDetails // Details about the delivery
)

// Contains specific details about the delivery
@Serializable
data class DeliveryDetails(
    val fee: Int, // The delivery fee
    val distance: Int // The delivery distance (likely in meters)
)

// Error response data class for consistent error messaging
@Serializable
data class ErrorResponse(val error: String) // Contains the error message