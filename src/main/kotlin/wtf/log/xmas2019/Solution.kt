package wtf.log.xmas2019

import java.io.BufferedReader

interface Day<InputT : Any, out OutputT1 : Any, out OutputT2 : Any> {

    fun parseInput(reader: BufferedReader): InputT
    fun part1(input: InputT): OutputT1? = null
    fun part2(input: InputT): OutputT2? = null
}
