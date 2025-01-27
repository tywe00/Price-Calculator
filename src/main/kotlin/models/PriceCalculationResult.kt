package com.example.models

// Sealed class representing the result of a price calculation operation
// This class can only have two subclasses: Success and Failure
sealed class PriceCalculationResult {
    // Represents a successful price calculation
    data class Success(val response: DeliveryOrderPriceResponse) : PriceCalculationResult()

    // Represents a failed price calculation
    data class Failure(val errorMessage: String) : PriceCalculationResult()
}