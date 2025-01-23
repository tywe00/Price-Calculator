package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VenueStatic(
    @SerialName("delivery_fee") val deliveryFee: Int,
    @SerialName("minimum_order_value") val minimumOrderValue: Int,
    // Add other relevant fields if needed
)

@Serializable
data class VenueDynamic(
    @SerialName("distance") val distance: Int,
    // Add other relevant fields if needed
)
