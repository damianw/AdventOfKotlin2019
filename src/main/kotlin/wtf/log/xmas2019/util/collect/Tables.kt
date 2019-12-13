package wtf.log.xmas2019.util.collect

import com.google.common.collect.Table

operator fun <R : Any, C : Any, V : Any> Table<R, C, V>.set(rowKey: R, columnKey: C, value: V): V? {
    return put(rowKey, columnKey, value)
}
