package com.aristidevs.cursotestingandroid.checkout.presentation

import org.junit.Test

/**
 * EXAMEN — Tests UNITARIOS de la validación del formulario de checkout.
 *
 * Completa cada test siguiendo Given-When-Then. No modifiques producción.
 * SUT: [CheckoutForm.validate], [CheckoutFormErrors.isValid], [FieldError].
 */
class CheckoutFormTest {

    @Test
    fun `given blank name when validate then nameError is REQUIRED`() {
        // GIVEN
        val name = ""

        // WHEN
        val error = CheckoutForm(name = name).validate()

        // THEN
        assert(error.nameError == FieldError.REQUIRED)
    }

    @Test
    fun `given blank address when validate then addressError is REQUIRED`() {
        // GIVEN
        val address = ""

        // WHEN
        val error = CheckoutForm(address = address).validate()

        // THEN
        assert(error.addressError == FieldError.REQUIRED)
    }

    @Test
    fun `given blank email when validate then emailError is REQUIRED`() {
        // GIVEN
        val email = ""

        // WHEN
        val error = CheckoutForm(email = email).validate()

        // THEN
        assert(error.emailError == FieldError.REQUIRED)
    }

    @Test
    fun `given malformed email when validate then emailError is INVALID_EMAIL`() {
        // GIVEN
        val email = "invalid-email"

        // WHEN
        val error = CheckoutForm(email = email).validate()

        // THEN
        assert(error.emailError == FieldError.INVALID_EMAIL)
    }

    @Test
    fun `given all fields valid when validate then errors isValid is true`() {
        // GIVEN
        val name = "John Doe"
        val address = "123 Main St"
        val email = "william.henry.harrison@example-pet-store.com"

        // WHEN
        val error = CheckoutForm(name = name, address = address, email = email).validate()

        // THEN
        assert(error.isValid)
    }
}
