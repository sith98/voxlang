package runtime

abstract class VoxRuntimeException(line: Int, cause: String) : Exception("Runtime Exception in line $line: $cause")

class VariableException(line: Int, cause: String) : VoxRuntimeException(line, cause)
class FunctionException(line: Int, cause: String) : VoxRuntimeException(line, cause)
class IllegalStatementException(line: Int, cause: String) : VoxRuntimeException(line, cause)
class InvalidTypeException(line: Int, expectedType: String, actualType: String) :
    VoxRuntimeException(line, "Expected type $expectedType, got type $actualType")
class WrongNumberOfArgumentsException(line: Int, expectedNumber: Int, actualNumber: Int) :
    VoxRuntimeException(line, "Expected $expectedNumber arguments, got $actualNumber")
class WrongArgumentException(line: Int, cause: String) : VoxRuntimeException(line, cause)
class ListOutOfBoundException(line: Int, listSize: Int, index: Int) :
    VoxRuntimeException(line, "Index out of bounds (index: $index, list size: $listSize)")
class ForLoopException(line: Int, cause: String) : VoxRuntimeException(line, cause)


fun expectedType(line: Int, arg: Value, type: Value): Nothing {
    throw InvalidTypeException(
        line,
        expectedType = valueTypeName(type),
        actualType = valueTypeName(arg)
    )
}
fun expectedOneOfTwoTypes(line: Int, arg: Value, typeOne: Value, typeTwo: Value): Nothing {
    throw InvalidTypeException(
        line,
        expectedType = "${valueTypeName(typeOne)} or ${valueTypeName(typeTwo)}",
        actualType = valueTypeName(arg)
    )
}

fun argumentsCheck(line: Int, args: List<Value>, expectedNumber: Int) {
    if (args.size != expectedNumber) {
        throw  WrongNumberOfArgumentsException(line, expectedNumber, args.size)
    }
}