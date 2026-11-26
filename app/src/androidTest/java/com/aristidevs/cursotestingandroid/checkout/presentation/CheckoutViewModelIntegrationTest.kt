package com.aristidevs.cursotestingandroid.checkout.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.aristidevs.cursotestingandroid.cart.domain.repository.CartItemRepository
import com.aristidevs.cursotestingandroid.cart.domain.usecase.GetCartSummaryUseCase
import com.aristidevs.cursotestingandroid.checkout.domain.usecase.PlaceOrderUseCase
import com.aristidevs.cursotestingandroid.core.MainDispatcherRule
import com.aristidevs.cursotestingandroid.core.mockwebserver.MiniMarketApiDispatcher
import com.aristidevs.cursotestingandroid.core.mockwebserver.MockWebServerUrlHolder
import com.aristidevs.cursotestingandroid.core.mockwebserver.OrderErrorDispatcher
import com.aristidevs.cursotestingandroid.core.mockwebserver.rules.MockWebServerRule
import com.aristidevs.cursotestingandroid.core.utils.asAsset
import com.aristidevs.cursotestingandroid.core.utils.awaitStateMatching
import com.aristidevs.cursotestingandroid.productlist.domain.repository.ProductRepository
import com.aristidevs.cursotestingandroid.productlist.domain.repository.PromotionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * EXAMEN — Tests de INTEGRACIÓN del ViewModel de checkout (extremo a extremo).
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: [CheckoutViewModel] con casos de uso reales + Room + MockWebServer.
 * Pistas: inyecta dependencias con Hilt, prepara el carrito real, observa `uiState` con Turbine,
 * y verifica que tras un pedido OK el estado es Success y el carrito queda vacío.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CheckoutViewModelIntegrationTest {

    private companion object {
        const val PRODUCT_ID = "p1"
    }

    @get:Rule(order = 0)
    val mockWebServer = MockWebServerRule()

    @get:Rule(order = 1)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var placeOrderUseCase: PlaceOrderUseCase

    @Inject
    lateinit var getCartSummaryUseCase: GetCartSummaryUseCase

    @Inject
    lateinit var cartItemRepository: CartItemRepository

    @Inject
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var promotionRepository: PromotionRepository

    @Before
    fun setUp() = runTest {
        mockWebServer.server.dispatcher = MiniMarketApiDispatcher(
            productJson = "product_list_default.json".asAsset()
        )
        hilt.inject()
        cartItemRepository.clearCart()
        productRepository.refreshProduct()
        promotionRepository.refreshPromotions()
    }

    @After
    fun tearDown() {
        MockWebServerUrlHolder.baseUrl = "http://localhost:8080/"
    }

    private fun createViewModel() = CheckoutViewModel(
        placeOrderUseCase = placeOrderUseCase,
        getCartSummaryUseCase = getCartSummaryUseCase
    )

    @Test
    fun givenItemsInCart_whenViewModelInitialized_thenIdleStateWithSummary() = runTest {
        // GIVEN
        cartItemRepository.addToCart(PRODUCT_ID, 2)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        viewModel.uiState.test {
            //Wait until Idle
            val state = awaitStateMatching { state ->
                state is CheckoutUiState.Idle &&
                state.summary.subtotal > 0
            } as CheckoutUiState.Idle

            assertFalse(state.isCartEmpty)
            assertTrue(state.summary.subtotal > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenValidFormAndSuccessfulOrder_whenOnConfirm_thenSuccessStateAndCartCleared() = runTest {
        // GIVEN
        cartItemRepository.addToCart(PRODUCT_ID, 2)


        val viewModel = createViewModel()
        viewModel.onNameChange("Jane")
        viewModel.onAddressChange("456 Elm St")
        viewModel.onEmailChange("jane@example.com")

        // WHEN
        viewModel.onConfirm()

        // THEN
        viewModel.uiState.test {
            val success = awaitStateMatching {
                it is CheckoutUiState.Success
            } as CheckoutUiState.Success
            assertEquals("ORD-1001", success.confirmation.orderId)
            cancelAndIgnoreRemainingEvents()
        }

        val cartItems = cartItemRepository.getCartItems().first()
        assertTrue(cartItems.isEmpty())
    }

    @Test
    fun givenOrderEndpointFails_whenOnConfirm_thenErrorState() = runTest {
        // GIVEN
        serveProductsError()

        cartItemRepository.addToCart(PRODUCT_ID, 2)

        val viewModel = createViewModel()
        viewModel.onNameChange("Jane")
        viewModel.onAddressChange("456 Elm St")
        viewModel.onEmailChange("jane@example.com")

        // WHEN
        viewModel.onConfirm()

        // THEN
        viewModel.uiState.test {
            val errorState = awaitStateMatching {
                it is CheckoutUiState.Error
            }
            assertTrue(errorState is CheckoutUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test //AÑADIDO POR MI. Comprobar que después del error el carrito no se vacía
    fun givenOrderEndpointFails_whenOnConfirm_thenCartIsNotCleared() = runTest {
        // GIVEN
        serveProductsError()

        cartItemRepository.addToCart(PRODUCT_ID, 2)

        val viewModel = createViewModel()
        viewModel.onNameChange("Jane")
        viewModel.onAddressChange("456 Elm St")
        viewModel.onEmailChange("jane@example.com")

        // WHEN
        viewModel.onConfirm()

        // THEN
        viewModel.uiState.test {
            awaitStateMatching { it is CheckoutUiState.Error }
            cancelAndIgnoreRemainingEvents()
        }

        val cartItems = cartItemRepository.getCartItems().first()
        assertEquals(1, cartItems.size)
        assertEquals(PRODUCT_ID, cartItems.first().productId)
        assertEquals(2, cartItems.first().quantity)
    }

    private fun serveProductsError() {
        mockWebServer.server.dispatcher = OrderErrorDispatcher()
    }


}
