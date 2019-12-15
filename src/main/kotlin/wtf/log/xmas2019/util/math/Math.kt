package wtf.log.xmas2019.util.math

/**
 * Returns digits of the receiver (using [radix]) in little-endian (ascending order).
 */
fun Long.digits(radix: Long = 10): Sequence<Long> = sequence {
    var value = this@digits
    do {
        yield(value % radix)
        value /= radix
    } while (value != 0L)
}
