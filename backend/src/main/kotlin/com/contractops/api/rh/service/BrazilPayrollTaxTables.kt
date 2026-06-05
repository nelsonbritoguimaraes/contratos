package com.contractops.api.rh.service

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tabelas progressivas INSS e IRRF vigentes para 2026 (Lei 14.663/2023 + reajustes anuais).
 */
object BrazilPayrollTaxTables {

    private val HUNDRED = BigDecimal("100")
    private val SCALE = 2
    private val ROUND = RoundingMode.HALF_UP

    data class InssBracket(val upperLimit: BigDecimal, val rate: BigDecimal)

    /** Faixas INSS empregado — contribuição progressiva (Portaria MPS 2026). */
    val INSS_2026: List<InssBracket> = listOf(
        InssBracket(BigDecimal("1518.00"), BigDecimal("0.075")),
        InssBracket(BigDecimal("2793.45"), BigDecimal("0.09")),
        InssBracket(BigDecimal("4190.83"), BigDecimal("0.12")),
        InssBracket(BigDecimal("8157.41"), BigDecimal("0.14"))
    )

    data class IrrfBracket(val upperLimit: BigDecimal, val rate: BigDecimal, val deduction: BigDecimal)

    /** Tabela IRRF mensal 2026 (Receita Federal). */
    val IRRF_2026: List<IrrfBracket> = listOf(
        IrrfBracket(BigDecimal("2428.80"), BigDecimal.ZERO, BigDecimal.ZERO),
        IrrfBracket(BigDecimal("2823.65"), BigDecimal("0.075"), BigDecimal("182.16")),
        IrrfBracket(BigDecimal("3751.05"), BigDecimal("0.15"), BigDecimal("394.16")),
        IrrfBracket(BigDecimal("4664.68"), BigDecimal("0.225"), BigDecimal("675.49")),
        IrrfBracket(BigDecimal("999999999.99"), BigDecimal("0.275"), BigDecimal("908.73"))
    )

    /** Dedução por dependente (2026). */
    val DEPENDENT_DEDUCTION_2026 = BigDecimal("189.59")

    /** Desconto simplificado mensal (2026). */
    val SIMPLIFIED_DEDUCTION_2026 = BigDecimal("607.20")

    fun calculateInss2026(grossEarnings: BigDecimal): BigDecimal {
        if (grossEarnings <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(SCALE, ROUND)
        var remaining = grossEarnings.min(INSS_2026.last().upperLimit)
        var previousLimit = BigDecimal.ZERO
        var total = BigDecimal.ZERO

        for (bracket in INSS_2026) {
            if (remaining <= previousLimit) break
            val taxable = remaining.min(bracket.upperLimit).subtract(previousLimit).max(BigDecimal.ZERO)
            total = total.add(taxable.multiply(bracket.rate))
            previousLimit = bracket.upperLimit
        }
        return total.setScale(SCALE, ROUND)
    }

    fun calculateIrrf2026(
        grossEarnings: BigDecimal,
        inss: BigDecimal,
        dependents: Int = 0,
        otherDeductions: BigDecimal = BigDecimal.ZERO,
        useSimplifiedDeduction: Boolean = true
    ): BigDecimal {
        val dependentDeduction = DEPENDENT_DEDUCTION_2026.multiply(BigDecimal(dependents.coerceAtLeast(0)))
        val simplified = if (useSimplifiedDeduction) SIMPLIFIED_DEDUCTION_2026 else BigDecimal.ZERO
        val base = grossEarnings
            .subtract(inss)
            .subtract(dependentDeduction)
            .subtract(otherDeductions)
            .subtract(simplified)
            .max(BigDecimal.ZERO)

        if (base <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(SCALE, ROUND)

        for (bracket in IRRF_2026) {
            if (base <= bracket.upperLimit) {
                if (bracket.rate == BigDecimal.ZERO) return BigDecimal.ZERO.setScale(SCALE, ROUND)
                val tax = base.multiply(bracket.rate).subtract(bracket.deduction)
                return tax.max(BigDecimal.ZERO).setScale(SCALE, ROUND)
            }
        }
        return BigDecimal.ZERO.setScale(SCALE, ROUND)
    }
}
