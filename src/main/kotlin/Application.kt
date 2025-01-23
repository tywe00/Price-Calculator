package com.example

import com.example.client.HomeApiClient
import com.example.service.PriceCalculatorService
import io.ktor.server.application.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Install Content Negotiation for the server
    install(ServerContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            // Add any other JSON configuration options here
        })
    }

    // Initialize HttpClient with client Content Negotiation
    val httpClient = HttpClient(CIO) {
        // Install Content Negotiation for the server
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                // Add any other JSON configuration options here
            })
        }
        // Configure additional client settings if necessary
    }

    // Initialize clients and services
    val homeApiClient = HomeApiClient(httpClient)
    val priceCalculatorService = PriceCalculatorService(homeApiClient)

    // Configure error handling
    //configureErrorHandling()

    // Configure routing with injected services
    configureRouting(priceCalculatorService)
}
