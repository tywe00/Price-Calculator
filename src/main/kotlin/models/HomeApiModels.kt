package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Represents the response for static venue information
@Serializable
data class VenueStaticResponse(
    @SerialName("venue_raw") val venueRaw: VenueRaw
)

// Represents the response for dynamic venue information
@Serializable
data class VenueDynamicResponse(
    @SerialName("venue_raw") val venueRaw: VenueRawDynamic
)

// Contains static venue information, including location
@Serializable
data class VenueRaw(
    val location: Location
)

// Contains dynamic venue information, including delivery specifications
@Serializable
data class VenueRawDynamic(
    @SerialName("delivery_specs") val deliverySpecs: DeliverySpecs
)

// Represents the geographical location of a venue
@Serializable
data class Location(
    val coordinates: List<Double> // Typically [longitude, latitude]
)

// Contains delivery-related specifications for a venue
@Serializable
data class DeliverySpecs(
    @SerialName("order_minimum_no_surcharge") val orderMinimumNoSurcharge: Int, // Minimum order amount without surcharge
    @SerialName("delivery_pricing") val deliveryPricing: DeliveryPricing
)

// Defines the pricing structure for deliveries
@Serializable
data class DeliveryPricing(
    @SerialName("base_price") val basePrice: Int, // Base delivery price
    @SerialName("distance_ranges") val distanceRanges: List<DistanceRange> // Price adjustments based on distance
)

// Represents a distance range with associated pricing information
@Serializable
data class DistanceRange(
    val min: Int, // Minimum distance for this range
    val max: Int, // Maximum distance for this range
    val a: Int, // Pricing parameter A (possibly a fixed cost)
    val b: Double, // Pricing parameter B (possibly a variable cost factor)
    val flag: String? // Optional flag for special conditions
)

// Utility class to represent geographical coordinates
data class Coordinates(
    val longitude: Double,
    val latitude: Double
)