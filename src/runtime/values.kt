@file:Suppress("DataClassPrivateConstructor")

package runtime

import parsing.Block
import parsing.Statement
import java.lang.StringBuilder
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinClassFinder

sealed class Value
object Nil : Value()
data class IntValue private constructor(val value: Int) : Value() {
    companion object {
        private val intCache = Array(256) {
            IntValue(it - 128)
        }

        fun of(value: Int): IntValue {
            return if (-128 <= value && value <= 127) {
                intCache[value + 128]
            } else {
                IntValue(value)
            }
        }
    }
}

data class FloatValue(val value: Double) : Value() {
    override fun hashCode(): Int {
        return value.hashCode()
    }
    override fun equals(other: Any?): Boolean {
        return other is FloatValue && value == other.value
    }
}
data class BoolValue private constructor(val value: Boolean) : Value() {
    companion object {
        private val boolFalse = BoolValue(false)
        private val boolTrue = BoolValue(true)

        fun of(value: Boolean): BoolValue {
            return if (value) {
                boolTrue
            } else {
                boolFalse
            }
        }
    }
}

data class StringValue(val value: String) : Value() {
    companion object {
        private val cache = mutableMapOf<String, StringValue>()
        fun fromCache(value: String): StringValue {
            return cache.getOrPut(value) { StringValue(value )}
        }
    }
}
data class ListValue(val value: MutableList<Value>) : Value()
data class DictValue(val value: MutableMap<Value, Value>) : Value()
data class RangeValue(val start: Int, val end: Int, val step: Int) : Value()
data class FunctionValue(val parameters: List<String>, val body: Block, val outerScope: Scope) : Value()
data class NativeFunctionValue(val nativeFunction: NativeFunction) : Value()
data class SpecialFunctionValue(val specialFunction: SpecialFunction) : Value()

val intZero = IntValue.of(0)
val floatZero = FloatValue(0.0)
val boolZero = BoolValue.of(false)
val stringZero = StringValue("")
val listZero = ListValue(mutableListOf())
val dictZero = DictValue(mutableMapOf())
val rangeZero = RangeValue(0, 0, 1)
val functionZero = FunctionValue(listOf(), Block(listOf()), Scope("MockScope"))

val boolFalse = BoolValue.of(false)
val boolTrue = BoolValue.of(true)

private fun valueToString(value: Value, builder: StringBuilder) {
    when (value) {
        Nil -> builder.append("nil")
        is IntValue -> builder.append(value.value)
        is FloatValue -> builder.append(value.value)
        is BoolValue -> builder.append(value.value)
        is StringValue -> builder.append(value.value)
        is ListValue -> {
            val list = value.value
            builder.append("[")
            for (i in (0..list.size - 2)) {
                valueToString(list[i], builder)
                builder.append(", ")
            }
            if (list.size > 0) {
                valueToString(list.last(), builder)
            }
            builder.append("]")
        }
        is DictValue -> {
            val dict = value.value
            builder.append("{")
            var prefix = ""
            for ((key, item) in dict) {
                builder.append(prefix)
                prefix = ", "
                valueToString(key, builder)
                builder.append(": ")
                valueToString(item, builder)
            }
            builder.append("}")
        }
        is RangeValue -> builder.append("<range ${value.start} ${value.end} ${value.step}>")
        is FunctionValue, is NativeFunctionValue, is SpecialFunctionValue ->
            builder.append("<func>")
    }
}

fun isTruthy(value: Value): Boolean {
    return value != Nil && value != boolFalse
}

fun valueToString(value: Value): String {
    val builder = StringBuilder()
    valueToString(value, builder)
    return builder.toString()
}

fun valueTypeName(value: Value): String {
    return when (value) {
        Nil -> "Nil"
        is IntValue -> "Int"
        is FloatValue -> "Float"
        is BoolValue -> "Bool"
        is StringValue -> "String"
        is ListValue -> "List"
        is DictValue -> "Dict"
        is RangeValue -> "Range"
        is FunctionValue, is NativeFunctionValue, is SpecialFunctionValue -> "Func"
    }
}