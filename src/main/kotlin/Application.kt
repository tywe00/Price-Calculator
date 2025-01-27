package com.example

import com.example.client.HomeApiClient
import com.example.client.IHomeApiClient
import com.example.service.IPriceCalculatorService
import com.example.service.PriceCalculatorService
import io.ktor.server.application.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.json.Json

/**
 *
 * This function initializes and starts the Ktor server using the Netty engine.
 *
 * @param args Command-line arguments passed to the application.
 */
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

/**
 * Configures the Ktor application module.
 *
 * This function sets up essential components such as content negotiation for both
 * the server and the HTTP client, initializes necessary clients and services, and
 * configures routing with the injected services.
 *
 * @param environment The [Application] environment provided by Ktor.
 */
fun Application.module(homeApiClient: IHomeApiClient = HomeApiClient(HttpClient(CIO) {
    install(ClientContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignores unknown JSON keys to prevent failures
            // Add additional JSON configuration options here if needed
        })
    }
    // Configure additional client settings if necessary (e.g., timeouts, logging)
})) {
    // Install Content Negotiation for the server to handle JSON serialization/deserialization
    install(ServerContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true // Ignores unknown JSON keys to prevent failures
                // Add additional JSON configuration options here if needed
            }
        )
    }

    // Initialize the Price Calculator Service with the Home API client
    val priceCalculatorService: IPriceCalculatorService = PriceCalculatorService(homeApiClient)

    // Configure routing by injecting the Price Calculator Service
    configureRouting(priceCalculatorService)
}