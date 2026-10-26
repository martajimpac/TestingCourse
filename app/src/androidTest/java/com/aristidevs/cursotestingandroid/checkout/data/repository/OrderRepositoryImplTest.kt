package com.aristidevs.cursotestingandroid.checkout.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aristidevs.cursotestingandroid.checkout.domain.repository.OrderRepository
import com.aristidevs.cursotestingandroid.core.domain.model.AppError
import com.aristidevs.cursotestingandroid.core.mockwebserver.MiniMarketApiDispatcher
import com.aristidevs.cursotestingandroid.core.mockwebserver.MockWebServerUrlHolder
import com.aristidevs.cursotestingandroid.core.mockwebserver.OrderErrorDispatcher
import com.aristidevs.cursotestingandroid.core.mockwebserver.rules.MockWebServerRule
import com.aristidevs.cursotestingandroid.core.utils.asAsset
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * EXAMEN — Tests de INTEGRACIÓN del repositorio de pedidos.
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: [OrderRepositoryImpl] sobre RemoteDataSource real + MockWebServer
 * (200 -> OrderConfirmation mapeada; 404 -> AppError.NotFoundError; red caída -> AppError.NetworkError).
 * Pistas: encola respuestas en `mockWebServer.server`, inyecta con Hilt (`hilt.inject()`).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OrderRepositoryImplTest {

    @get:Rule(order = 0)
    val mockWebServer = MockWebServerRule()

    @get:Rule(order = 1)
    val hilt = HiltAndroidRule(this)


    @Inject
    lateinit var orderRepository: OrderRepository



    @Before
    fun setUp() =
        runTest {
            hilt.inject()
        }

    @After
    fun tearDown() {
        MockWebServerUrlHolder.baseUrl = "http://localhost:8080/"
    }

    @Test
    fun givenSuccessfulResponse_whenPlaceOrder_thenReturnsOrderConfirmation() = runTest {

        // GIVEN
        serveProductsSuccess()


        // WHEN
        val order = orderRepository.placeOrder()

        // THEN
        assert(order.orderId.isNotEmpty())
    }

    @Test(expected = AppError.NotFoundError::class)
    fun given404Response_whenPlaceOrder_thenThrowsNotFoundError() = runTest {
        // GIVEN
        serveProductsError(404)

        // WHEN
        orderRepository.placeOrder()
    }

    @Test(expected = AppError.NetworkError::class)
    fun givenNetworkFailure_whenPlaceOrder_thenThrowsNetworkError() = runTest {
        // GIVEN
        serveProductsError(500)

        // WHEN
        orderRepository.placeOrder()
    }

    private fun serveProductsError(errorCode: Int) {
        mockWebServer.server.dispatcher = OrderErrorDispatcher(errorCode)
    }

    private fun serveProductsSuccess() {
        mockWebServer.server.dispatcher = MiniMarketApiDispatcher(
            productJson = "product_list_default.json".asAsset()
        )
    }


}
