package com.example.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class DeliveryOrderPriceResponse(
    @SerialName("total_price") val totalPrice: Int,
    @SerialName("small_order_surcharge") val smallOrderSurcharge: Int,
    @SerialName("cart_value") val cartValue: Int,
    val delivery: DeliveryDetails
)

@Serializable
data class DeliveryDetails(
    val fee: Int,
    val distance: Int
)
