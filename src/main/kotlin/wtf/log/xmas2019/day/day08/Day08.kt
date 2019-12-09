package wtf.log.xmas2019.day.day08

import wtf.log.xmas2019.Day
import java.io.BufferedReader

object Day08 : Day<Day08.Image, Int, String> {

    override fun parseInput(reader: BufferedReader): Image = Image.parse(reader.readText().trim())

    override fun part1(input: Image): Int? {
        val minLayer = input.layers.minBy { it.count(Color.BLACK) }!!
        val oneDigits = minLayer.count(Color.WHITE)
        val twoDigits = minLayer.count(Color.TRANSPARENT)
        return oneDigits * twoDigits
    }

    override fun part2(input: Image): String = input.flatten().toString()

    enum class Color(val character: Char) {
        BLACK('█'),
        WHITE('░'),
        TRANSPARENT(' ');

        operator fun plus(other: Color): Color = when (other) {
            BLACK, WHITE -> other
            TRANSPARENT -> this
        }

        companion object {

            private val values = values()

            fun from(ordinal: Int) = values[ordinal]
        }
    }

    class Image(val layers: List<Layer>) {

        fun flatten(): Layer = layers.asReversed().reduce(Layer::plus)

        class Layer(private val pixels: List<Color>) {

            operator fun get(x: Int, y: Int): Color = pixels[y * WIDTH + x]

            operator fun plus(other: Layer) = Layer(pixels.zip(other.pixels, Color::plus))

            fun count(color: Color) = pixels.count { it == color }

            override fun toString(): String = buildString {
                append('┌')
                repeat(WIDTH) {
                    append('─')
                }
                append("┐\n")
                repeat(HEIGHT) { y ->
                    append('│')
                    repeat(WIDTH) { x ->
                        append(get(x, y).character)
                    }
                    append("│\n")
                }
                append('└')
                repeat(WIDTH) {
                    append('─')
                }
                append('┘')
            }
        }

        companion object {

            private const val WIDTH = 25
            private const val HEIGHT = 6
            private const val PIXEL_COUNT = WIDTH * HEIGHT
            private const val START_DIGITS = '0'.toInt()

            fun parse(input: String): Image {
                require(input.length % PIXEL_COUNT == 0)
                val layerCount = input.length / PIXEL_COUNT
                return Image((0 until layerCount).map { layerIndex ->
                    val start = layerIndex * PIXEL_COUNT
                    Layer((0 until PIXEL_COUNT).map { Color.from(input[start + it].toInt() - START_DIGITS) })
                })
            }
        }
    }
}
