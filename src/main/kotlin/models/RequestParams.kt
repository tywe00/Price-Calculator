package com.example.models

// Represents a request for calculating the price of a delivery order
data class DeliveryOrderPriceRequest(
    val venueSlug: String,  // Unique identifier for the venue/restaurant
    val cartValue: Int,     // Total value of items in the cart (likely in cents)
    val userLat: Double,    // Latitude of the user's delivery location
    val userLon: Double     // Longitude of the user's delivery location
)