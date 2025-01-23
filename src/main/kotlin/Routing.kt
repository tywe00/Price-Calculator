package com.example

import com.example.service.PriceCalculatorService
import com.example.models.DeliveryOrderPriceRequest
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

fun Application.configureRouting(priceCalculatorService: PriceCalculatorService) {
    routing {
        route("/api/v1") {
            get("/delivery-order-price") {
                // Extract and validate query parameters
                val params = extractAndValidateParams(call) ?: return@get

                try {
                    // Calculate price using the service
                    val response = priceCalculatorService.calculatePrice(params)

                    // Respond with JSON
                    call.respond(response)
                } catch (e: Exception) {
                    // Handle exceptions
                    call.application.environment.log.error("Error calculating delivery price", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(error = "Failed to calculate delivery price")
                    )
                }
            }
        }
    }
}

/**
 * Helper function to extract a query parameter and convert it to the target type.
 * If the parameter is missing or conversion fails, the function responds with a 400 error and returns null.
 */
suspend inline fun <T> ApplicationCall.extractQueryParam(
    name: String,
    crossinline converter: (String) -> T?
): T? {
    val value = request.queryParameters[name]
    if (value.isNullOrBlank()) {
        respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Missing or empty query parameter: $name"))
        return null
    }
    return converter(value) ?: run {
        respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Invalid query parameter: $name"))
        null
    }
}

private suspend fun extractAndValidateParams(call: ApplicationCall): DeliveryOrderPriceRequest? {
    val venueSlug = call.extractQueryParam("venue_slug") { it } ?: return null
    val cartValue = call.extractQueryParam("cart_value") { it.toIntOrNull() } ?: return null
    val userLat = call.extractQueryParam("user_lat") { it.toDoubleOrNull() } ?: return null
    val userLon = call.extractQueryParam("user_lon") { it.toDoubleOrNull() } ?: return null

    return DeliveryOrderPriceRequest(
        venueSlug = venueSlug,
        cartValue = cartValue,
        userLat = userLat,
        userLon = userLon
    )
}

/**
 * Error response data class for consistent error messaging.
 */
@Serializable
data class ErrorResponse(val error: String)
