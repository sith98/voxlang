package runtime

import parsing.Block
import parsing.Statement
import java.lang.StringBuilder
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.load.kotlin.KotlinClassFinder

sealed class Value
object Nil : Value()
data class IntValue internal constructor(val value: Int) : Value() {
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

data class FloatValue(val value: Double) : Value()
data class BoolValue internal constructor(val value: Boolean) : Value() {
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

data class StringValue(val value: String) : Value()
data class ListValue(val value: MutableList<Value>) : Value()
data class DictValue(val value: MutableMap<Value, Value>) : Value()
data class FunctionValue(val parameters: List<String>, val body: Statement, val outerScope: Scope) : Value()

val intZero = IntValue.of(0)
val floatZero = FloatValue(0.0)
val boolZero = BoolValue.of(false)
val listZero = ListValue(mutableListOf())
val dictZero = DictValue(mutableMapOf())
val stringZero = StringValue("")
val functionZero = FunctionValue(listOf(), Block(listOf()), Scope())

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
                builder.append(valueToString(list[i], builder))
                builder.append(", ")
            }
            if (list.size > 0) {
                builder.append(valueToString(list.last(), builder))
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
                builder
                    .append(valueToString(key, builder))
                    .append(": ")
                    .append(valueToString(item, builder))
            }
            builder.append("}")
        }
        is FunctionValue -> builder.append("[Function]")
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
        is FunctionValue -> "Func"
    }
}