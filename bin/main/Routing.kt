package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*

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
        respond(HttpStatusCode.BadRequest, "Missing or empty query parameter: $name")
        return null
    }
    return converter(value) ?: run {
        respond(HttpStatusCode.BadRequest, "Invalid query parameter: $name")
        null
    }
}

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            get("/delivery-order-price") {
                // Validate and convert parameters
                val venueSlug = call.extractQueryParam("venue_slug") { it } ?: return@get
                val cartValue = call.extractQueryParam("cart_value") { it.toIntOrNull() } ?: return@get
                val userLat = call.extractQueryParam("user_lat") { it.toDoubleOrNull() } ?: return@get
                val userLon = call.extractQueryParam("user_lon") { it.toDoubleOrNull() } ?: return@get

                // Example calculation (adjust these as needed)
                val smallOrderSurcharge = 0
                val deliveryFee = 190
                val totalPrice = cartValue + deliveryFee + smallOrderSurcharge
                val deliveryDistance = 177

                // Build JSON manually as a string
                // (No need for data classes or serialization if you don’t want them.)
                val jsonResponse = """
                {
                  "total_price": $totalPrice,
                  "small_order_surcharge": $smallOrderSurcharge,
                  "cart_value": $cartValue,
                  "delivery": {
                    "fee": $deliveryFee,
                    "distance": $deliveryDistance
                  }
                }
                """.trimIndent()

                // Return the raw JSON string
                call.respondText(jsonResponse, ContentType.Application.Json)
            }
        }
    }
}
