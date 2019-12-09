package wtf.log.xmas2019.day.day06

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import wtf.log.xmas2019.Day
import java.io.BufferedReader
import java.util.ArrayDeque
import java.util.PriorityQueue
import java.util.stream.Stream

object Day06 : Day<SetMultimap<String, String>, Int, Int> {

    @Suppress("UnstableApiUsage")
    override fun parseInput(reader: BufferedReader): SetMultimap<String, String> = reader
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .collect(Multimaps.flatteningToMultimap(
            { it.substringBefore(')') },
            { Stream.of(it.substringAfter(')')) },
            { HashMultimap.create<String, String>() }
        ))
        .let { Multimaps.unmodifiableSetMultimap(it) }

    override fun part1(input: SetMultimap<String, String>): Int {
        val stack = ArrayDeque<Pair<String, Int>>()
        var sum = 0
        stack.push("COM" to 0)
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.pop()
            sum += depth
            val childDepth = depth + 1
            for (child in input[node]) {
                stack.push(child to childDepth)
            }
        }
        return sum
    }

    override fun part2(input: SetMultimap<String, String>): Int? {
        val parents = input
            .entries()
            .associate { (parent, child) -> child to parent }
        val targetNode = parents.getValue("SAN")
        var currentNode = parents.getValue("YOU")
        val previousNodes = mutableMapOf<String, String>()
        val queue = PriorityQueue(compareBy(Pair<String, Int>::second))
            .apply { add(currentNode to 0) }
        val distances = mutableMapOf(currentNode to 0)
        while (currentNode != targetNode && queue.isNotEmpty()) {
            currentNode = queue.remove().first
            val alternateDistance = distances[currentNode]?.let { it + 1 } ?: Int.MAX_VALUE
            val parent = parents[currentNode]?.let { sequenceOf(it) } ?: emptySequence()
            val children = input[currentNode].asSequence()
            for (neighbor in (parent + children)) {
                if (alternateDistance < distances[neighbor] ?: Int.MAX_VALUE) {
                    distances[neighbor] = alternateDistance
                    previousNodes[neighbor] = currentNode
                    queue.add(neighbor to alternateDistance)
                }
            }
        }
        check(currentNode == targetNode) { "Santa keeps to himself, I guess" }
        return generateSequence(currentNode, previousNodes::get).count() - 1
    }
}
