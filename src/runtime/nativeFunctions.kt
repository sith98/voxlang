package runtime

import parsing.Identifier

typealias NativeFunction = (line: Int, List<Value>) -> Value

val nativeFunctions = mapOf<String, NativeFunction>(
    "add" to { line, args ->
        var intSum = 0
        var floatSum = 0.0
        var floatAddition = false
        for (arg in args) {
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
                    expectedOneOfTwoTypes(line, arg, intZero, floatZero)
                }
            }
        }
        if (floatAddition) FloatValue(floatSum) else IntValue.of(intSum)
    },
    "mul" to { line, args ->
        var intProduct = 1
        var floatProduct = 1.0
        var floatMultiplication = false
        for (arg in args) {
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
                    expectedOneOfTwoTypes(line, arg, intZero, floatZero)
                }
            }
        }
        if (floatMultiplication) FloatValue(floatProduct) else IntValue.of(intProduct)
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
    "eq" to { line, args ->
        argumentsCheck(line, args, 2)
        BoolValue.of(args[0] == args[1])
    },
    "lt" to { line, args ->
        argumentsCheck(line, args, 2)
        val (first, second) = args
        BoolValue.of(when (first) {
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
        })
    },
    "print" to { line, args ->
        for (arg in args) {
            print(valueToString(arg))
        }
        println()
        Nil
    },
    "concat" to { line, args ->
        argumentsCheck(line, args, 1)
        val (list) = args
        if (list is ListValue) {
            val builder = StringBuilder()
            for (item in list.value) {
                builder.append(valueToString(item))
            }
            StringValue(builder.toString())
        } else {
            expectedType(line, list, listZero)
        }
    },
    "get" to { line, args ->
        argumentsCheck(line, args, 2)
        val (collection, key) = args
        when (collection) {
            is ListValue -> {
                if (key !is IntValue) {
                    expectedType(line, key, intZero)
                }
                val index = key.value
                val list = collection.value
                if (0 <= index && index < list.size) {
                    list[index]
                } else {
                    throw ListOutOfBoundException(line, listSize = list.size, index = index)
                }
            }
            is DictValue -> {
                val dict = collection.value
                dict[key] ?: Nil
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
    },
    "set" to { line, args ->
        argumentsCheck(line, args, 3)
        val (collection, key, newValue) = args
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
    "in" to { line, args ->
        argumentsCheck(line, args, 2)
        val (collection, key) = args
        when (collection) {
            is ListValue -> {
                val list = collection.value
                BoolValue.of(key in list)
            }
            is DictValue -> {
                val dict = collection.value
                BoolValue.of(key in dict)
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
    },
    "append" to { line, args ->
        argumentsCheck(line, args, 2)
        val (list, newValue) = args
        if (list !is ListValue) {
            expectedType(line, list, listZero)
        }
        list.value.add(newValue)
        Nil
    },
    "list" to { line, args ->
        ListValue(args.toMutableList())
    },
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
    "size" to { line, args ->
        argumentsCheck(line, args, 1)
        val (collection) = args
        when (collection) {
            is ListValue -> {
                IntValue(collection.value.size)
            }
            is DictValue -> {
                IntValue(collection.value.size)
            }
            else -> expectedOneOfTwoTypes(line, collection, listZero, dictZero)
        }
    },
    "type" to { line, args ->
        argumentsCheck(line, args, 1)
        val (value) = args
        StringValue(valueTypeName(value))
    }
)

enum class SpecialFunction(val identifier: String) {
    AND("and"), OR("or");
    companion object {
        fun byIdentifier(identifier: String): SpecialFunction? {
            return values().find { it.identifier == identifier }
        }
    }
}

val illegalVariableNames = nativeFunctions.keys.union(SpecialFunction.values().map{ it.identifier })