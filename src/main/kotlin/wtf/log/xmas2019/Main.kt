package wtf.log.xmas2019

import com.beust.jcommander.IValueValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wtf.log.xmas2019.day.day01.Day01
import wtf.log.xmas2019.day.day02.Day02
import wtf.log.xmas2019.day.day03.Day03
import wtf.log.xmas2019.day.day04.Day04
import wtf.log.xmas2019.day.day05.Day05
import wtf.log.xmas2019.day.day06.Day06
import wtf.log.xmas2019.day.day07.Day07
import wtf.log.xmas2019.day.day08.Day08
import wtf.log.xmas2019.day.day09.Day09
import wtf.log.xmas2019.day.day10.Day10
import wtf.log.xmas2019.util.time.toPrettyFormat
import java.io.File
import java.time.Duration
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val DAYS = listOf<Day<*, *, *>?>(
    Day01,
    Day02,
    Day03,
    Day04,
    Day05,
    Day06,
    Day07,
    Day08,
    Day09,
    Day10
)

private object ProgramArguments {

    @Parameter(
        names = ["--help", "-h"],
        description = "Prints usage information",
        help = true
    )
    var help: Boolean = false

    class DayValidator : IValueValidator<List<Int>> {
        override fun validate(name: String, value: List<Int>) {
            val errors = value.filter { DAYS.getOrNull(it - 1) == null }.distinct()
            // the lesson here is: never build sentences in code, kids
            when (errors.size) {
                0 -> return
                1 -> throw ParameterException("I haven't implemented day ${errors.single()}!")
                2 -> throw ParameterException("I haven't implemented days ${errors[0]} or ${errors[1]}!")
                else -> {
                    val joined = errors.joinToString(limit = errors.lastIndex, truncated = "or ${errors.last()}")
                    throw ParameterException("I haven't implemented days $joined!")
                }
            }
        }
    }

    @Parameter(
        names = ["--days", "-d"],
        description = "Days of the advent calendar to solve",
        validateValueWith = [DayValidator::class]
    )
    var days: List<Int>? = null
}

private object Spinner {

    private val spinBars = charArrayOf('-', '\\', '|', '/', '-', '\\', '|', '/')

    fun <R> call(title: String, printReturnValue: Boolean = true, block: () -> R): R = runBlocking {
        val job = launch(Dispatchers.IO) {
            var index = 0
            while (true) {
                print("\r-> $title (${spinBars[index]})")
                System.out.flush()
                delay(100L)
                index = (index + 1) % spinBars.size
            }
        }
        val (result, duration) = try {
            measureDuration(block)
        } finally {
            job.cancel()
        }
        val prefix = "\r-> $title"
        print(prefix)
        if (printReturnValue) {
            val indentSize = prefix.length + 1
            val indent = buildString(indentSize) {
                append('\n')
                repeat(indentSize) { append(' ') }
            }
            val lineSequence = result.toString().lineSequence()
            val resultString = lineSequence.joinToString(indent)
            print(": $resultString")
        }
        println(" (${duration.toPrettyFormat()})")
        result
    }
}

private inline fun <R> measureDuration(block: () -> R): Pair<R, Duration> {
    var result: R? = null
    val nanos = measureNanoTime {
        result = block()
    }
    val duration = Duration.ofNanos(nanos)
    @Suppress("UNCHECKED_CAST")
    return (result as R) to duration
}

private fun String.indent(amount: Int): String = buildString {
    for (i in 0 until amount) {
        append(' ')
    }
    append(this@indent)
}

private fun printIndented(header: String, input: String) {
    print(header)
    val lines = input.lines()
    println(lines.first())
    lines.drop(1).map { it.indent(header.length) }.forEach(::println)
}

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val commander = JCommander(ProgramArguments).apply {
        programName = "AdventOfKotlin2019"
    }

    fun exitWithUsage(message: String? = null): Nothing {
        message?.let { System.err.println("[Error] $it") }
        commander.usage()
        exitProcess(1)
    }

    try {
        commander.parse(*args)
    } catch (e: Exception) {
        exitWithUsage(e.message)
    }

    if (ProgramArguments.help) {
        exitWithUsage()
    }

    val dayNumbers = ProgramArguments.days ?: 1..DAYS.size

    for (dayNumber in dayNumbers) {
        @Suppress("UNCHECKED_CAST")
        val day = (DAYS[dayNumber - 1] as Day<Any, Any, Any>?) ?: continue
        println()
        println("========")
        println("Day $dayNumber")
        println("========")
        val input = Spinner.call("Parse input", printReturnValue = false) {
            val dayClass = day::class.java
            val directory = dayClass.`package`.name.replace('.', File.separatorChar)
            dayClass
                .classLoader
                .getResourceAsStream("$directory${File.separator}input.txt")!!
                .bufferedReader()
                .use(day::parseInput)
        }
        Spinner.call("Part 1") { day.part1(input)?.toString() ?: "<unsolved>" }
        Spinner.call("Part 2") { day.part2(input)?.toString() ?: "<unsolved>" }
        println()
    }
}
