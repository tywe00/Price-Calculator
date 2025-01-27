package com.example.service

import com.example.models.DeliveryOrderPriceRequest
import com.example.models.DeliveryOrderPriceResponse
import com.example.models.PriceCalculationResult

/**
 * Defines the contract for calculating delivery order prices.
 */
interface IPriceCalculatorService {
    /**
     * Calculates the final price for a delivery order, including:
     *  - Delivery fee (based on distance and pricing rules)
     *  - Small order surcharge (if cart value is below the threshold)
     *  - Total price (sum of cart value, surcharge, and fee)
     *
     * @param request Data for the delivery order, including venue slug, cart value, and user location.
     * @return A [PriceCalculationResult] indicating either success (with [DeliveryOrderPriceResponse]) or failure.
     */
    suspend fun calculatePrice(request: DeliveryOrderPriceRequest): PriceCalculationResult
}