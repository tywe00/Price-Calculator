package com.example

interface ExternalService {
    suspend fun calculateDistance(lat: Double, lon: Double): Int
}