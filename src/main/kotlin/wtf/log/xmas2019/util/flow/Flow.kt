package wtf.log.xmas2019.util.flow

import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Analogous to [Sequence.chunked] or [Flowable.buffer].
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    require(size > 0)
    val buffer = ArrayList<T>(size)
    collect { element ->
        buffer += element
        if (buffer.size == size) {
            emit(buffer.toList())
            buffer.clear()
        }
    }
    if (buffer.isNotEmpty()) {
        emit(buffer) // no copy needed for last one
    }
}
