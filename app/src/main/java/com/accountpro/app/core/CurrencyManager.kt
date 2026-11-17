package com.accountpro.app.core

import java.text.NumberFormat
import java.util.Currency as JavaCurrency
import java.util.Locale

data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val rateFromUsd: Double,
)

object CurrencyManager {
    val supportedCurrencies: List<SupportedCurrency> = listOf(
        SupportedCurrency("USD", "$", "US Dollar", 1.0),
        SupportedCurrency("EUR", "€", "Euro", 0.85),
        SupportedCurrency("GBP", "£", "British Pound", 0.73),
        SupportedCurrency("JPY", "¥", "Japanese Yen", 110.0),
        SupportedCurrency("CAD", "C$", "Canadian Dollar", 1.25),
        SupportedCurrency("AUD", "A$", "Australian Dollar", 1.35),
        SupportedCurrency("CHF", "CHF", "Swiss Franc", 0.92),
        SupportedCurrency("CNY", "¥", "Chinese Yuan", 6.45),
        SupportedCurrency("INR", "₹", "Indian Rupee", 74.5),
    )

    fun byCode(code: String): SupportedCurrency =
        supportedCurrencies.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies.first()

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        if (fromCode.equals(toCode, ignoreCase = true)) return amount
        val fromRate = byCode(fromCode).rateFromUsd
        val toRate = byCode(toCode).rateFromUsd
        val usdAmount = amount / fromRate
        return usdAmount * toRate
    }

    fun format(
        amount: Double,
        currencyCode: String,
        languageCode: String = "en",
        convertFromUsd: Boolean = false,
        targetBaseCode: String = currencyCode,
    ): String {
        val displayCode = if (convertFromUsd) targetBaseCode else currencyCode
        val displayAmount = if (convertFromUsd) {
            convert(amount, currencyCode, targetBaseCode)
        } else {
            amount
        }
        val locale = when (languageCode) {
            "es" -> Locale.forLanguageTag("es-ES")
            "fr" -> Locale.forLanguageTag("fr-FR")
            else -> Locale.US
        }
        return try {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = JavaCurrency.getInstance(displayCode)
            formatter.minimumFractionDigits = if (displayCode == "JPY") 0 else 2
            formatter.maximumFractionDigits = if (displayCode == "JPY") 0 else 2
            formatter.format(displayAmount)
        } catch (_: Exception) {
            val symbol = byCode(displayCode).symbol
            "$symbol${String.format(Locale.US, "%,.2f", displayAmount)}"
        }
    }
}
