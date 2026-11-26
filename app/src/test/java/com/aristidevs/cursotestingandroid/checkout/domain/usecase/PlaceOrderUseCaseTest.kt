package com.aristidevs.cursotestingandroid.checkout.domain.usecase

import app.cash.turbine.test
import com.aristidevs.cursotestingandroid.core.builders.cartItem
import com.aristidevs.cursotestingandroid.core.fakes.FakeCartItemRepository
import com.aristidevs.cursotestingandroid.core.fakes.FakeOrderRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EXAMEN — Tests UNITARIOS del caso de uso de realizar pedido.
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: [PlaceOrderUseCase] (éxito vacía el carrito; fallo NO lo vacía).
 * Pista: necesitarás un fake de OrderRepository y FakeCartItemRepository.
 */
class PlaceOrderUseCaseTest {

    @Test
    fun `given successful order when invoke then returns success and clears cart`() = runTest {
        // GIVEN
        val fakeCartItemRepository = FakeCartItemRepository().apply {
            setCartItems(listOf(cartItem { withProductId("1"); withQuantity(2) }))
        }
        val fakeOrderRepository = FakeOrderRepository()
        val useCase = PlaceOrderUseCase(fakeOrderRepository, fakeCartItemRepository)

        // WHEN
        val result = useCase()

        // THEN
        assertTrue(result.isSuccess)
        fakeCartItemRepository.getCartItems().test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
        }
    }

    @Test
    fun `given repository throws when invoke then returns failure`() = runTest {
        // GIVEN
        val fakeCartItemRepository = FakeCartItemRepository()
        val fakeOrderRepository = FakeOrderRepository().apply {
            setError(RuntimeException("Network error"))
        }
        val useCase = PlaceOrderUseCase(fakeOrderRepository, fakeCartItemRepository)

        // WHEN
        val result = useCase()

        // THEN
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given repository throws when invoke then does not clear cart`() = runTest {
        // GIVEN
        val initialItems = listOf(cartItem { withProductId("1"); withQuantity(2) })
        val fakeCartItemRepository = FakeCartItemRepository().apply {
            setCartItems(initialItems)
        }
        val fakeOrderRepository = FakeOrderRepository().apply {
            setError(RuntimeException("Network error"))
        }
        val useCase = PlaceOrderUseCase(fakeOrderRepository, fakeCartItemRepository)

        // WHEN
        useCase()

        // THEN
        fakeCartItemRepository.getCartItems().test {
            val items = awaitItem()
            assertEquals(initialItems, items)
        }
    }
}
