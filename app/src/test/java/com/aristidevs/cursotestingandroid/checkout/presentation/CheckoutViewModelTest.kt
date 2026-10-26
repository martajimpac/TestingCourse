package com.aristidevs.cursotestingandroid.checkout.presentation

import app.cash.turbine.test
import com.aristidevs.cursotestingandroid.cart.domain.usecase.GetCartSummaryUseCase
import com.aristidevs.cursotestingandroid.checkout.domain.usecase.PlaceOrderUseCase
import com.aristidevs.cursotestingandroid.core.MainDispatcherRule
import com.aristidevs.cursotestingandroid.core.builders.cartItem
import com.aristidevs.cursotestingandroid.core.builders.product
import com.aristidevs.cursotestingandroid.core.fakes.FakeCartItemRepository
import com.aristidevs.cursotestingandroid.core.fakes.FakeOrderRepository
import com.aristidevs.cursotestingandroid.core.fakes.FakeProductRepository
import com.aristidevs.cursotestingandroid.core.fakes.FakePromotionRepository
import com.aristidevs.cursotestingandroid.core.fakes.FakeSystemClock
import com.aristidevs.cursotestingandroid.core.mothers.ProductMother
import com.aristidevs.cursotestingandroid.productlist.domain.usecase.GetPromotionForProduct
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * EXAMEN — Tests UNITARIOS del ViewModel de checkout.
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: [CheckoutViewModel] — estados [CheckoutUiState], `canSubmit`, `onConfirm`, eventos.
 * Pistas: usa Turbine sobre `uiState`/`event`, `runTest(mainDispatcherRule.scheduler)`,
 * fakes (FakeCartItemRepository, FakeProductRepository, FakePromotionRepository, FakeSystemClock)
 * y un fake de OrderRepository que tendrás que crear.
 */
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        fakeProductRepository: FakeProductRepository = FakeProductRepository(),
        fakeCartItemRepository: FakeCartItemRepository = FakeCartItemRepository(),
        fakeOrderRepository: FakeOrderRepository = FakeOrderRepository(),
        fakePromotionRepository: FakePromotionRepository = FakePromotionRepository(),
        fakeSystemClock: FakeSystemClock = FakeSystemClock(),
    ): CheckoutViewModel {

        val placeOrderUseCase = PlaceOrderUseCase(
            cartItemRepository = fakeCartItemRepository,
            orderRepository = fakeOrderRepository,
        )
        val getCartSummaryUseCase = GetCartSummaryUseCase(
            cartItemRepository = fakeCartItemRepository,
            productRepository = fakeProductRepository,
            promotionRepository = fakePromotionRepository,
            getPromotionForProduct = GetPromotionForProduct(),
            clock = fakeSystemClock,
        )

        return CheckoutViewModel(
            placeOrderUseCase = placeOrderUseCase,
            getCartSummaryUseCase = getCartSummaryUseCase,
        )
    }

    @Test
    fun `given empty cart when initialized then canSubmit is false`() =
        runTest(mainDispatcherRule.scheduler) {
            // WHEN
            val viewModel = createViewModel()

            // THEN
            viewModel.uiState.test {
                val state = awaitItem() as CheckoutUiState.Idle
                assertFalse(state.canSubmit)
                assertTrue(state.isCartEmpty)
            }
        }

    @Test
    fun `given valid form and non empty cart when form completed then canSubmit is true`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val productId = "1"
            val product = product {
                withId(productId)
            }
            val item =
                cartItem {
                    withProductId(productId)
                    withQuantity(3)
                }
            val fakeProductRepository = FakeProductRepository().apply { setProducts(listOf(product)) }
            val fakeCartRepository = FakeCartItemRepository().apply { setCartItems(listOf(item)) }

            // WHEN
            val viewModel = createViewModel(
                fakeCartItemRepository = fakeCartRepository,
                fakeProductRepository = fakeProductRepository
            )
            viewModel.onNameChange("John")
            viewModel.onAddressChange("Main St 123")
            viewModel.onEmailChange("john@example.com")

            // THEN
            viewModel.uiState.test {
                val state = awaitItem() as CheckoutUiState.Idle
                assertTrue(state.canSubmit)
            }
        }

    @Test
    fun `given malformed email when email changed then emailError is INVALID_EMAIL and canSubmit is false`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val email = "invalid-email"

            // WHEN
            val viewModel = createViewModel()
            viewModel.onEmailChange(email)

            // THEN
            viewModel.uiState.test {
                val state = awaitItem() as CheckoutUiState.Idle
                assertEquals(FieldError.INVALID_EMAIL, state.errors.emailError)
                assertFalse(state.canSubmit)
            }
        }

    @Test
    fun `given valid form when onConfirm succeeds then emits Success state`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val product = ProductMother.bread()
            val item = cartItem {
                withProductId(product.id)
                withQuantity(3)
            }
            val fakeCartItemRepository = FakeCartItemRepository().apply { setCartItems(listOf(item)) }
            val fakeProductRepository = FakeProductRepository().apply { setProducts(listOf(product)) }

            val viewModel = createViewModel(
                fakeCartItemRepository = fakeCartItemRepository,
                fakeProductRepository = fakeProductRepository
            )
            viewModel.onNameChange("John")
            viewModel.onAddressChange("Main St 123")
            viewModel.onEmailChange("john@example.com")

            // WHEN
            viewModel.onConfirm()

            // THEN
            viewModel.uiState.test {
                val state = awaitItem() as CheckoutUiState.Success
                assertEquals("123", state.confirmation.orderId)
            }
        }

    @Test
    fun `given place order fails when onConfirm then emits Error state and ShowMessage event`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val product = ProductMother.bread()
            val item = cartItem {
                withProductId(product.id)
                withQuantity(3)
            }
            val fakeCartItemRepository = FakeCartItemRepository().apply { setCartItems(listOf(item)) }
            val fakeProductRepository = FakeProductRepository().apply { setProducts(listOf(product)) }
            val fakeOrderRepository = FakeOrderRepository().apply {
                setError(RuntimeException("Error"))
            }

            val viewModel = createViewModel(
                fakeCartItemRepository = fakeCartItemRepository,
                fakeProductRepository = fakeProductRepository,
                fakeOrderRepository = fakeOrderRepository
            )
            viewModel.onNameChange("John")
            viewModel.onAddressChange("Main St 123")
            viewModel.onEmailChange("john@example.com")
            
            viewModel.event.test {
                viewModel.uiState.test {
                    awaitItem() //Wait for Idle
                    viewModel.onConfirm()

                    val state = awaitItem() as CheckoutUiState.Error
                    assertEquals("Error", state.message)
                }

                val event = awaitItem() as CheckoutEvent.ShowMessage
                assertEquals("Error", event.message)
            }

        }

    @Test
    fun `given invalid form when onConfirm then does not place order`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val product = ProductMother.bread()
            val item = cartItem {
                withProductId(product.id)
                withQuantity(3)
            }
            val fakeCartItemRepository = FakeCartItemRepository().apply { setCartItems(listOf(item)) }
            val fakeProductRepository = FakeProductRepository().apply { setProducts(listOf(product)) }

            val viewModel = createViewModel(
                fakeCartItemRepository = fakeCartItemRepository,
                fakeProductRepository = fakeProductRepository
            )

            // WHEN
            viewModel.onConfirm()

            // THEN
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is CheckoutUiState.Idle)
            }
        }

    @Test //AÑADIDO POR MI. Comprobar que el onRetry funciona
    fun `given failed order when onRetry then returns to idle and can submit`() =
        runTest(mainDispatcherRule.scheduler) {
            // GIVEN
            val product = ProductMother.bread()
            val item = cartItem {
                withProductId(product.id)
                withQuantity(3)
            }
            val fakeCartItemRepository = FakeCartItemRepository().apply { setCartItems(listOf(item)) }
            val fakeProductRepository = FakeProductRepository().apply { setProducts(listOf(product)) }
            val fakeOrderRepository = FakeOrderRepository().apply {
                setError(RuntimeException("Error"))
            }

            val viewModel = createViewModel(
                fakeCartItemRepository = fakeCartItemRepository,
                fakeProductRepository = fakeProductRepository,
                fakeOrderRepository = fakeOrderRepository
            )
            viewModel.onNameChange("John")
            viewModel.onAddressChange("Main St 123")
            viewModel.onEmailChange("john@example.com")

            viewModel.onConfirm()
            viewModel.uiState.test {
                awaitItem() as CheckoutUiState.Error

                // WHEN
                viewModel.onRetry()

                // THEN
                val state = awaitItem() as CheckoutUiState.Idle
                assertTrue(state.canSubmit)
                assertFalse(state.isCartEmpty)
                assertEquals("John", state.form.name)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
