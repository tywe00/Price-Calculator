package com.example

import com.example.service.PriceCalculatorService
import com.example.models.DeliveryOrderPriceRequest
import com.example.models.PriceCalculationResult
import com.example.models.ErrorResponse
import com.example.service.IPriceCalculatorService
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*

/**
 * Configures the routing for the Ktor application.
 *
 * This function sets up the API endpoints and their corresponding handlers.
 * Specifically, it defines the `/api/v1/delivery-order-price` GET endpoint,
 * which calculates the delivery order price based on provided query parameters.
 *
 * @param priceCalculatorService An instance of [PriceCalculatorService] used to calculate delivery order prices.
 */
fun Application.configureRouting(priceCalculatorService: IPriceCalculatorService) {
    routing {
        route("/api/v1") {
            /**
             * Handles GET requests to the `/api/v1/delivery-order-price` endpoint.
             *
             * This endpoint expects the following query parameters:
             * - `venue_slug`: The unique identifier for the venue.
             * - `cart_value`: The total value of the cart in the smallest currency unit (e.g., cents).
             * - `user_lat`: The latitude of the user's location.
             * - `user_lon`: The longitude of the user's location.
             *
             * The function performs the following steps:
             * 1. Extracts and validates the query parameters.
             * 2. Calls [PriceCalculatorService.calculatePrice] with the validated parameters.
             * 3. Responds with the calculated price or an error message based on the result.
             */
            get("/delivery-order-price") {
                // Extract and validate query parameters
                val params = extractAndValidateParams(call) ?: return@get

                // Calculate the delivery price using the service
                when (val result = priceCalculatorService.calculatePrice(params)) {
                    is PriceCalculationResult.Success -> {
                        // Respond with the successful price calculation
                        call.respond(HttpStatusCode.OK, result.response)
                    }
                    is PriceCalculationResult.Failure -> {
                        // Respond with an error message indicating why the calculation failed
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.errorMessage))
                    }
                }
            }
        }
    }
}

/**
 * Extracts a query parameter from the [ApplicationCall], converts it to the desired type,
 * and handles validation and error responses.
 *
 * If the specified query parameter is missing, empty, or fails to convert to the target type,
 * the function responds with a `400 Bad Request` and an [ErrorResponse], then returns `null`.
 *
 * @param name The name of the query parameter to extract.
 * @param converter A lambda function that converts the [String] value to the desired type [T].
 *                  Should return `null` if conversion fails.
 * @return The converted value of type [T] if successful; otherwise, `null`.
 *
 * @throws IllegalArgumentException if the parameter is present but invalid (handled internally).
 */
suspend inline fun <T> ApplicationCall.extractQueryParam(
    name: String,
    crossinline converter: (String) -> T?
): T? {
    val value = request.queryParameters[name]
    if (value.isNullOrBlank()) {
        respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(error = "Missing or empty query parameter: $name")
        )
        return null
    }
    return converter(value) ?: run {
        respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(error = "Invalid query parameter: $name")
        )
        null
    }
}

/**
 * Extracts and validates all required query parameters from the [ApplicationCall],
 * then constructs a [DeliveryOrderPriceRequest] object.
 *
 * This function ensures that all necessary parameters are present and correctly formatted.
 * If any parameter is missing or invalid, it responds with a `400 Bad Request` and an [ErrorResponse],
 * then returns `null`.
 *
 * @param call The [ApplicationCall] from which to extract query parameters.
 * @return A [DeliveryOrderPriceRequest] with validated parameters if successful; otherwise, `null`.
 */
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