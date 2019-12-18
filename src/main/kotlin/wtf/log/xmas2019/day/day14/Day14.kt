package wtf.log.xmas2019.day.day14

import wtf.log.xmas2019.Day
import java.io.BufferedReader

object Day14 : Day<Set<Day14.Reaction>, Int, Int> {

    override fun parseInput(reader: BufferedReader): Set<Reaction> = reader
        .lineSequence()
        .map((Reaction)::parse)
        .toSet()

    override fun part1(input: Set<Reaction>): Int? {
        val reactions = input.associateBy { it.product.symbol }
        val consumed = mutableMapOf<String, Int>()
        val pool = mutableMapOf(Reagent.ORE to Int.MAX_VALUE)
        fun obtain(reagent: Reagent) {
            println("Request $reagent")
            val requested = reagent.quantity
            check(requested > 0)
            val available = pool[reagent.symbol] ?: 0
            val taken = requested.coerceAtMost(available)
            println("Take $taken ${reagent.symbol}")
            pool[reagent.symbol] = (pool[reagent.symbol] ?: 0) - taken
            consumed[reagent.symbol] = (consumed[reagent.symbol] ?: 0) + taken
            val remaining = requested - taken
            if (remaining == 0) return

            val reaction = reactions.getValue(reagent.symbol)
            val iterations = (remaining / reaction.product.quantity).coerceAtLeast(1)
            repeat(iterations) {
                reaction.reactants.forEach(::obtain)
                pool[reagent.symbol] = (pool[reagent.symbol] ?: 0) + reaction.product.quantity
            }
            obtain(reagent.copy(quantity = remaining))
        }
        obtain(Reagent(Reagent.FUEL, quantity = 1))
        return consumed.getValue(Reagent.ORE)
    }

    data class Reagent(val symbol: String, val quantity: Int) {

        companion object {

            const val ORE = "ORE"
            const val FUEL = "FUEL"

            fun parse(input: String): Reagent = input.split(' ').let { (quantity, symbol) ->
                Reagent(symbol = symbol, quantity = quantity.toInt())
            }
        }
    }

    data class Reaction(val reactants: List<Reagent>, val product: Reagent) {

        companion object {

            fun parse(input: String): Reaction = input.split("=>").let { (left, right) ->
                Reaction(
                    reactants = left.splitToSequence(',').map { Reagent.parse(it.trim()) }.toList(),
                    product = Reagent.parse(right.trim())
                )
            }
        }
    }
}
