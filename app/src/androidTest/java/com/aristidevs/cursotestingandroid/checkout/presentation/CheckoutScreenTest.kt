package com.aristidevs.cursotestingandroid.checkout.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aristidevs.cursotestingandroid.cart.domain.model.CartSummary
import com.aristidevs.cursotestingandroid.checkout.domain.model.OrderConfirmation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * EXAMEN — Tests de UI (Compose) de la pantalla de checkout.
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: composables de [CheckoutScreen] / CheckoutContent renderizando cada [CheckoutUiState].
 * Pistas: usa `composeRule.setContent { ... }` pasando el estado deseado y callbacks de prueba;
 * localiza nodos por texto (la pantalla aún no expone testTags) y verifica habilitación del botón.
 */
class CheckoutScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenLoadingState_whenRendered_thenShowsProgress() {
        // GIVEN & WHEN
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Loading,
                onBack = {},
                onRetry = {},
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // THEN
        // Progress indicator is rendered
    }

    @Test
    fun givenIdleStateWithEmptyCart_whenRendered_thenConfirmButtonDisabled() {
        // GIVEN & WHEN
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Idle(
                    summary = CartSummary(0.0, 0.0, 0.0),
                    form = CheckoutForm(),
                    errors = CheckoutFormErrors(),
                    isCartEmpty = true,
                    isSubmitting = false,
                    canSubmit = false
                ),
                onBack = {},
                onRetry = {},
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // THEN
        composeRule.onNodeWithText("Confirmar pedido").assertIsNotEnabled()
        composeRule.onNodeWithText("Tu carrito está vacío").assertExists()
    }

    @Test
    fun givenIdleStateWithValidForm_whenRendered_thenConfirmButtonEnabled() {
        // GIVEN & WHEN
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Idle(
                    summary = CartSummary(10.0, 0.0, 10.0),
                    form = CheckoutForm("John", "123 Main St", "john@example.com"),
                    errors = CheckoutFormErrors(),
                    isCartEmpty = false,
                    isSubmitting = false,
                    canSubmit = true
                ),
                onBack = {},
                onRetry = {},
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // THEN
        composeRule.onNodeWithText("Confirmar pedido").assertIsEnabled()
    }

    @Test
    fun givenIdleState_whenTypingInvalidEmail_thenConfirmButtonDisabled() {
        // GIVEN
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Idle(
                    summary = CartSummary(10.0, 0.0, 10.0),
                    form = CheckoutForm("John", "123 Main St", "invalid-email"),
                    errors = CheckoutFormErrors(emailError = FieldError.INVALID_EMAIL),
                    isCartEmpty = false,
                    isSubmitting = false,
                    canSubmit = false
                ),
                onBack = {},
                onRetry = {},
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // WHEN & THEN
        composeRule.onNodeWithText("Confirmar pedido").assertIsNotEnabled()
    }

    @Test
    fun givenSuccessState_whenRendered_thenShowsOrderConfirmation() {
        // GIVEN & WHEN
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Success(OrderConfirmation("ORD-123", 25, 49.99)),
                onBack = {},
                onRetry = {},
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // THEN
        composeRule.onNodeWithText("Pedido confirmado: ORD-123").assertExists()
        composeRule.onNodeWithText("Tiempo estimado: 25").assertExists()
        composeRule.onNodeWithText("Precio: 49.99").assertExists()
    }

    @Test
    fun givenErrorState_whenRetryClicked_thenInvokesRetryCallback() {
        // GIVEN
        var retryClicked = false
        composeRule.setContent {
            CheckoutContent(
                uiState = CheckoutUiState.Error("Error al realizar el pedido"),
                onBack = {},
                onRetry = { retryClicked = true },
                onNameChange = {},
                onEmailChange = {},
                onAddressChange = {},
                onConfirm = {}
            )
        }

        // WHEN
        composeRule.onNodeWithText("Reintentar").performClick()

        // THEN
        assertTrue(retryClicked)
    }
}
