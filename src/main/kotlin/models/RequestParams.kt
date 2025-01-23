package com.example.models

data class DeliveryOrderPriceRequest(
    val venueSlug: String,
    val cartValue: Int,
    val userLat: Double,
    val userLon: Double
)
