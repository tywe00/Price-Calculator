package com.example.service

import com.example.models.*
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Tests for the helper functions in [PriceCalculatorService] that are otherwise private:
 *  - calculateDeliveryFee
 *  - calculateDistance
 *  - computeHaversineDistance
 *
 * Note: In a real-world scenario, we'd typically test these through public APIs,
 * or make them internal/public if we genuinely need direct tests. For demonstration,
 * we're using reflection to access private methods.
 */
class PriceCalculatorServiceHelpersTest {

    private lateinit var service: PriceCalculatorService

    @BeforeEach
    fun setUp() {
        val homeApiClientMock = mockk<com.example.client.HomeApiClient>()
        service = PriceCalculatorService(homeApiClientMock)
    }

    @Test
    fun `calculateDeliveryFee returns expected fee for valid range`() {
        val specs = DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 1000, a = 50, b = 1.0, flag = null),
                    DistanceRange(min = 1000, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )
        val distance = 100

        val fee = invokePrivateMethod<Int>(
            instance = service,
            methodName = "calculateDeliveryFee",
            paramTypes = arrayOf(Int::class.java, DeliverySpecs::class.java),
            args = arrayOf(distance, specs)
        )

        assertEquals(160, fee)
    }

    @Test
    fun `calculateDeliveryFee throws IllegalArgumentException if distance out of range`() {
        val specs = DeliverySpecs(
            orderMinimumNoSurcharge = 1000,
            deliveryPricing = DeliveryPricing(
                basePrice = 100,
                distanceRanges = listOf(
                    DistanceRange(min = 0, max = 500, a = 50, b = 1.0, flag = null),
                    DistanceRange(min = 500, max = 0, a = 0, b = 0.0, flag = null)
                )
            )
        )
        val distance = 600

        val exception = assertThrows(IllegalArgumentException::class.java) {
            invokePrivateMethod<Int>(
                instance = service,
                methodName = "calculateDeliveryFee",
                paramTypes = arrayOf(Int::class.java, DeliverySpecs::class.java),
                args = arrayOf(distance, specs)
            )
        }
        assertTrue(exception.message!!.contains("Delivery not possible"))
    }

    @Test
    fun `calculateDistance returns correct integer distance`() {
        val userLat = 60.1700
        val userLon = 24.93
        val venueLat = 60.1709
        val venueLon = 24.93

        val distance = invokePrivateMethod<Int>(
            instance = service,
            methodName = "calculateDistance",
            paramTypes = arrayOf(Double::class.java, Double::class.java, Double::class.java, Double::class.java),
            args = arrayOf(userLat, userLon, venueLat, venueLon)
        )

        assertTrue(distance in 90..120, "Expected distance around 100m, got $distance")
    }

    @Test
    fun `computeHaversineDistance returns correct integer distance`() {
        val lat1 = 60.1700
        val lon1 = 24.93
        val lat2 = 60.1709
        val lon2 = 24.93

        val distance = invokePrivateMethod<Int>(
            instance = service,
            methodName = "computeHaversineDistance",
            paramTypes = arrayOf(Double::class.java, Double::class.java, Double::class.java, Double::class.java),
            args = arrayOf(lat1, lon1, lat2, lon2)
        )

        assertTrue(distance in 90..120, "Expected distance around 100m, but got $distance")
    }

    @Test
    fun `computeHaversineDistance returns zero for identical points`() {
        // Same exact latitude and longitude => distance should be 0
        val lat = 60.1700
        val lon = 24.93

        val distance = invokePrivateMethod<Int>(
            instance = service,
            methodName = "computeHaversineDistance",
            paramTypes = arrayOf(Double::class.java, Double::class.java, Double::class.java, Double::class.java),
            args = arrayOf(lat, lon, lat, lon)
        )

        assertEquals(0, distance, "Distance should be 0 for identical coordinates")
    }

    @Test
    fun `computeHaversineDistance handles negative lat and lon`() {
        val lat1 = -33.865143
        val lon1 = 151.209900
        val lat2 = 40.712776
        val lon2 = -74.005974

        val distance = invokePrivateMethod<Int>(
            instance = service,
            methodName = "computeHaversineDistance",
            paramTypes = arrayOf(Double::class.java, Double::class.java, Double::class.java, Double::class.java),
            args = arrayOf(lat1, lon1, lat2, lon2)
        )

        assertTrue(distance in 15_000_000..17_000_000,
            "Expected ~15,900km between Sydney and NYC, got $distance")
    }

    @Test
    fun `computeHaversineDistance calculates near maximum for opposite points on Earth`() {
        // Opposite / Antipodal points: lat1=0, lon1=0 vs lat2=0, lon2=180 => ~20,037 km
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 0.0
        val lon2 = 180.0  // Directly opposite on Earth

        val distance = invokePrivateMethod<Int>(
            instance = service,
            methodName = "computeHaversineDistance",
            paramTypes = arrayOf(Double::class.java, Double::class.java, Double::class.java, Double::class.java),
            args = arrayOf(lat1, lon1, lat2, lon2)
        )

        assertTrue(distance in 19_000_000..21_000_000,
            "Expected ~20,000km for antipodal points, got $distance")
    }

    /**
     * Utility function to call a private method of [instance] using reflection.
     * - [methodName] is the private function name to invoke
     * - [paramTypes] must match the function's parameter types
     * - [args] is the array of arguments to pass
     *
     * @return The result cast to [T].
     */
    private inline fun <reified T> invokePrivateMethod(
        instance: Any,
        methodName: String,
        paramTypes: Array<Class<*>>,
        args: Array<Any?>
    ): T {
        val clazz = instance::class
        val method = clazz.declaredFunctions.firstOrNull {
            it.name == methodName && it.parameters.size == paramTypes.size + 1
        } ?: throw NoSuchMethodException("No method found with name '$methodName'.")

        method.isAccessible = true

        return try {
            method.call(instance, *args) as T
        } catch (ex: InvocationTargetException) {
            // Rethrow the original cause if it's a runtime exception
            if (ex.cause is RuntimeException) {
                throw ex.cause as RuntimeException
            } else {
                throw ex
            }
        }
    }
}