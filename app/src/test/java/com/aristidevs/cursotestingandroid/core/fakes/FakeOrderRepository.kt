package com.aristidevs.cursotestingandroid.core.fakes

import com.aristidevs.cursotestingandroid.checkout.domain.model.OrderConfirmation
import com.aristidevs.cursotestingandroid.checkout.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOrderRepository: OrderRepository {
    private val _order = MutableStateFlow<OrderConfirmation?>(null)
    private var error: Exception? = null

    fun setError(exception: Exception) {
        error = exception
    }

    override suspend fun placeOrder(): OrderConfirmation {
        error?.let { throw it }
        val confirmation = OrderConfirmation("123", 30, 100.0)
        _order.value = confirmation
        return confirmation
    }
}
