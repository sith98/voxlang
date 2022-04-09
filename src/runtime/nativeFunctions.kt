package runtime

import kotlin.math.pow
import kotlin.random.Random

typealias NativeFunction = (line: Int, List<Value>) -> Value

val nativeFunctions = mapOf<String, NativeFunction>(
    // arithmetic
    "add" to { line, args ->
        variadicFunction(line, args) { innerLine, innerArgs ->
            var intSum = 0
            var floatSum = 0.0
            var floatAddition = false
            for (arg in innerArgs) {
                when (arg) {
                    is IntValue -> {
                        if (floatAddition) {
                            floatSum += arg.value
                        } else {
                            intSum += arg.value
                        }
                    }
                    is FloatValue -> {
                        if (!floatAddition) {
                            floatSum += intSum
                        }
                        floatAddition = true
                        floatSum += arg.value
                    }
                    else -> {
                        expectedOneOfTwoTypes(innerLine, arg, intZero, floatZero)
                    }
                }
            }
            if (floatAddition) FloatValue(floatSum) else IntValue.of(intSum)
        }
    },
    "mul" to { line, args ->
        variadicFunction(line, args) { innerLine, innerArgs ->
            var intProduct = 1
            var floatProduct = 1.0
            var floatMultiplication = false
            for (arg in innerArgs) {
                when (arg) {
                    is IntValue -> {
                        if (floatMultiplication) {
                            floatProduct *= arg.value
                        } else {
                            intProduct *= arg.value
                        }
                    }
                    is FloatValue -> {
                        if (!floatMultiplication) {
                            floatProduct *= intProduct
                        }
                        floatMultiplication = true
                        floatProduct *= arg.value
                    }
                    else -> {
                        expectedOneOfTwoTypes(innerLine, arg, intZero, floatZero)
                    }
                }
            }
            if (floatMultiplication) FloatValue(floatProduct) else IntValue.of(intProduct)
        }
    },
    "sub" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        when (first) {
            is IntValue -> when (second) {
                is IntValue -> IntValue.of(first.value - second.value)
                is FloatValue -> FloatValue(first.value - second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            is FloatValue -> when (second) {
                is IntValue -> FloatValue(first.value - second.value)
                is FloatValue -> FloatValue(first.value - second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            else -> expectedOneOfTwoTypes(line, first, intZero, floatZero)
        }
    },
    "div" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        when (first) {
            is IntValue -> when (second) {
                is IntValue -> FloatValue(first.value.toDouble() / second.value)
                is FloatValue -> FloatValue(first.value / second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            is FloatValue -> when (second) {
                is IntValue -> FloatValue(first.value / second.value)
                is FloatValue -> FloatValue(first.value / second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            else -> expectedOneOfTwoTypes(line, first, intZero, floatZero)
        }
    },
    "intdiv" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        if (first !is IntValue) {
            expectedType(line, first, intZero)
        }
        if (second !is IntValue) {
            expectedType(line, second, intZero)
        }

        IntValue.of(first.value / second.value)
    },
    "mod" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        when (first) {
            is IntValue -> when (second) {
                is IntValue -> IntValue.of(first.value % second.value)
                is FloatValue -> FloatValue(first.value % second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            is FloatValue -> when (second) {
                is IntValue -> FloatValue(first.value % second.value)
                is FloatValue -> FloatValue(first.value % second.value)
                else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
            }
            else -> expectedOneOfTwoTypes(line, first, intZero, floatZero)
        }
    },
    "pow" to { line, args ->
        argumentsCheck(line, args, 2)
        val (base, exponent) = args
        if (base !is FloatValue) {
            expectedType(line, base, floatZero)
        }
        if (exponent !is FloatValue) {
            expectedType(line, exponent, floatZero)
        }
        FloatValue(base.value.pow(exponent.value))
    },

    // comparisons
    "eq" to { line, args ->
        argumentsCheck(line, args, 2)
        BoolValue.of(args[0] == args[1])
    },
    "id" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        BoolValue.of(if (first == second) {
            if (first is ListValue && second is ListValue) {
                first.value === second.value
            } else if (first is DictValue && second is DictValue) {
                first.value === second.value
            } else {
                false
            }
        } else {
            false
        })
    },
    "lt" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        BoolValue.of(
            when (first) {
                is IntValue -> when (second) {
                    is IntValue -> first.value < second.value
                    is FloatValue -> first.value < second.value
                    else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
                }
                is FloatValue -> when (second) {
                    is IntValue -> first.value < second.value
                    is FloatValue -> first.value < second.value
                    else -> expectedOneOfTwoTypes(line, second, intZero, floatZero)
                }
                is StringValue -> when (second) {
                    is StringValue -> first.value < second.value
                    else -> expectedType(line, second, stringZero)
                }
                else -> expectedOneOfTwoTypes(line, first, intZero, floatZero)
            }
        )
    },
    "min" to { l, a ->
        variadicFunction(l, a) { line, args ->
            var sawFloat = false
            var smallestNumber = Double.POSITIVE_INFINITY
            for (arg in args) {
                when (arg) {
                    is IntValue -> {
                        if (arg.value < smallestNumber) {
                            smallestNumber = arg.value.toDouble()
                        }
                    }
                    is FloatValue -> {
                        sawFloat = true
                        if (arg.value < smallestNumber) {
                            smallestNumber = arg.value
                        }
                    }
                    else -> {
                        expectedOneOfTwoTypes(line, arg, intZero, floatZero)
                    }
                }
            }
            if (sawFloat) {
                FloatValue(smallestNumber)
            } else {
                IntValue.of(smallestNumber.toInt())
            }
        }
    },
    "max" to { l, a ->
        variadicFunction(l, a) { line, args ->
            var sawFloat = false
            var greatestNumber = Double.NEGATIVE_INFINITY
            for (arg in args) {
                when (arg) {
                    is IntValue -> {
                        if (arg.value > greatestNumber) {
                            greatestNumber = arg.value.toDouble()
                        }
                    }
                    is FloatValue -> {
                        sawFloat = true
                        if (arg.value > greatestNumber) {
                            greatestNumber = arg.value
                        }
                    }
                    else -> {
                        expectedOneOfTwoTypes(line, arg, intZero, floatZero)
                    }
                }
            }
            if (sawFloat) {
                FloatValue(greatestNumber)
            } else {
                IntValue.of(greatestNumber.toInt())
            }
        }
    },

    // Effects
    "print" to { _, args ->
        for (arg in args) {
            print(valueToString(arg))
        }
        println()
        Nil
    },
    "read" to { line, args ->
        argumentsCheck(line, args, 0)
        readLine()?.let { StringValue(it) } ?: Nil
    },
    "panic" to { line, args ->
        argumentsCheck(line, args, 1)
        val (msg) = args
        if (msg !is StringValue) {
            expectedType(line, msg, stringZero)
        }
        throw UserPanicExcpetion(line, "[panic] ${msg.value}")
    },
    "random" to { line, args ->
        argumentsCheck(line, args, 0)
        FloatValue(Random.nextDouble())
    },

    // string functions
    "charList" to { line, args ->
        argumentsCheck(line, args, 1)
        val (string) = args
        if (string !is StringValue) {
            expectedType(line, string, stringZero)
        }
        val chars: MutableList<Value> = string.value
            .toCharArray()
            .map { StringValue(it.toString()) }
            .toMutableList()
        ListValue(chars)
    },
    "concat" to { line, args ->
        variadicFunction(line, args) { _, list ->
            val builder = StringBuilder()
            for (item in list) {
                builder.append(valueToString(item))
            }
            StringValue(builder.toString())
        }
    },

    // number conversions
    "int" to { line, args ->
        argumentsCheck(line, args, 1)
        val (arg) = args
        when (arg) {
            is IntValue -> arg
            is FloatValue -> IntValue.of(arg.value.toInt())
            is StringValue -> {
                val int = arg.value.toIntOrNull()
                if (int == null) {
                    Nil
                } else {
                    IntValue.of(int)
                }
            }
            else -> expectedOneOfThreeTypes(line, arg, intZero, floatZero, stringZero)
        }
    },
    "float" to { line, args ->
        argumentsCheck(line, args, 1)
        val (arg) = args
        when (arg) {
            is IntValue -> FloatValue(arg.value.toDouble())
            is FloatValue -> arg
            is StringValue -> {
                val double = arg.value.toDoubleOrNull()
                if (double == null) {
                    Nil
                } else {
                    FloatValue(double)
                }
            }
            else -> expectedOneOfThreeTypes(line, arg, intZero, floatZero, stringZero)
        }
    },

    // collections (general)
    "get" to { line, args ->
        if (args.size < 2) {
            throw WrongNumberOfArgumentsException(line, 2, args.size)
        }
        getNestedCollectionElement(line, args.first(), args.drop(1))
    },
    "set" to { line, args ->
        if (args.size < 3) {
            throw WrongNumberOfArgumentsException(line, 3, args.size)
        }
        val collection = getNestedCollectionElement(line, args.first(), args.drop(1).dropLast(2))
        val (key, newValue) = args.takeLast(2)
        when (collection) {
            is ListValue -> {
                if (key !is IntValue) {
                    expectedType(line, key, intZero)
                }
                val index = key.value
                val list = collection.value
                if (0 <= index && index < list.size) {
                    list[index] = newValue
                } else {
                    throw ListOutOfBoundException(line, listSize = list.size, index = index)
                }
            }
            is DictValue -> {
                val dict = collection.value
                dict[key] = newValue
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
        Nil
    },
    "size" to { line, args ->
        argumentsCheck(line, args, 1)
        val (collection) = args
        when (collection) {
            is ListValue -> {
                IntValue.of(collection.value.size)
            }
            is DictValue -> {
                IntValue.of(collection.value.size)
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
    },
    "in" to { line, args ->
        argumentsCheck(line, args, 2)
        val (collection, key) = args
        BoolValue.of(
            when (collection) {
                is ListValue -> {
                    val list = collection.value
                    key in list
                }
                is DictValue -> {
                    val dict = collection.value
                    key in dict
                }
                is RangeValue -> {
                    when (key) {
                        is IntValue -> collection.start <= key.value && key.value <= collection.end
                        is FloatValue -> collection.start <= key.value && key.value <= collection.end
                        else -> expectedOneOfTwoTypes(line, key, intZero, floatZero)
                    }
                }
                else -> expectedOneOfThreeTypes(line, collection, listZero, dictZero, rangeZero)
            }
        )
    },
    "remove" to { line, args ->
        argumentsCheck(line, args, 2)
        val (collection, value) = args
        when (collection) {
            is ListValue -> {
                val list = collection.value
                BoolValue.of(list.remove(value))
            }
            is DictValue -> {
                val dict = collection.value
                BoolValue.of(dict.remove(value) == null)
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
    },

    // lists
    "list" to { _, args ->
        ListValue(args.toMutableList())
    },
    "push" to { line, args ->
        argumentsCheck(line, args, 2)
        val (list, newValue) = args
        if (list !is ListValue) {
            expectedType(line, list, listZero)
        }
        list.value.add(newValue)
        Nil
    },
    "pop" to { line, args ->
        argumentsCheck(line, args, 2)
        val (list) = args
        if (list !is ListValue) {
            expectedType(line, list, listZero)
        }
        val l = list.value
        if (l.isEmpty()) {
            Nil
        } else {
            l.removeAt(l.lastIndex)
        }
    },

    // dicts
    "dict" to { line, args ->
        if (args.size % 2 != 0) {
            throw WrongArgumentException(line, "function \"dict\" requires an even number of arguments")
        }
        val dict = mutableMapOf<Value, Value>()
        for (i in 0 until args.size step 2) {
            dict[args[i]] = args[i + 1]
        }
        DictValue(dict)
    },

    // ranges
    "range" to { line, args ->
        if (args.size > 3) {
            throw WrongNumberOfArgumentsException(line, 3, args.size)
        }
        if (args.size < 2) {
            throw WrongNumberOfArgumentsException(line, 2, args.size)
        }
        val (start, end) = args
        val step = if (args.size == 3) args[2] else IntValue.of(1)
        if (start !is IntValue) {
            expectedType(line, start, intZero)
        }
        if (end !is IntValue) {
            expectedType(line, end, intZero)
        }
        if (step !is IntValue) {
            expectedType(line, step, intZero)
        }
        RangeValue(start.value, end.value, step.value)
    },
    "rangeProps" to { line, args ->
        argumentsCheck(line, args, 1)
        val (range) = args
        if (range !is RangeValue) {
            expectedType(line, range, rangeZero)
        }
        ListValue(
            mutableListOf(
                IntValue.of(range.start),
                IntValue.of(range.end),
                IntValue.of(range.step)
            )
        )
    },

    // meta programming
    "type" to { line, args ->
        argumentsCheck(line, args, 1)
        val (value) = args
        StringValue(valueTypeName(value))
    }
)

tailrec fun getNestedCollectionElement(line: Int, collection: Value, keys: List<Value>): Value {
    if (keys.isEmpty()) {
        return collection
    }
    val key = keys.first()
    return when (collection) {
        is ListValue -> {
            if (key !is IntValue) {
                expectedType(line, key, intZero)
            }
            val index = key.value
            val list = collection.value
            if (0 <= index && index < list.size) {
                getNestedCollectionElement(line, list[index], keys.drop(1))
            } else {
                throw ListOutOfBoundException(line, listSize = list.size, index = index)
            }
        }
        is DictValue -> {
            val result = collection.value[key]
            if (result == null) {
                Nil
            } else {
                getNestedCollectionElement(line, result, keys.drop(1))
            }
        }
        else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
    }
}

fun variadicFunction(line: Int, args: List<Value>, fn: NativeFunction): Value {
    if (args.isEmpty()) {
        throw WrongNumberOfArgumentsException(line, 1, 0)
    }
    return if (args.size == 1) {
        val (list) = args
        if (list !is ListValue) {
            expectedType(line, list, listZero)
        }
        fn(line, list.value)
    } else {
        fn(line, args)
    }
}

enum class SpecialFunction(val identifier: String) {
    AND("and"), OR("or"), CHOICE("choice")
}