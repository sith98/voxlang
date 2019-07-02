package runtime

import parsing.*
import javax.swing.text.AbstractDocument

data class RunningContext(val isStandardLibrary: Boolean)

sealed class Continuation
object NormalContinuation : Continuation()
class Break(val line: Int) : Continuation()
class Continue(val line: Int) : Continuation()
class Return(val line: Int, val returnValue: Value) : Continuation()

fun loadSpecialAndNativeFunctionsIntoScope(scope: Scope) {
    for (fn in SpecialFunction.values()) {
        scope.defineConstant(fn.identifier, SpecialFunctionValue(fn))
    }
    for ((name, fn) in nativeFunctions) {
        scope.defineConstant(name, NativeFunctionValue(fn))
    }
}

fun loadStdLibIntoScope(scope: Scope) {
    val source = object {}.javaClass.getResource("/runtime/stdlib/stdlib.vox").readText()
    val stdLib = parse(TokenStream(tokenize(source)))
    val context = RunningContext(isStandardLibrary = true)
    for ((statement, line) in stdLib) {
        runStatement(line, statement, scope, context)
    }
}

fun runAst(statements: List<WithLine<Statement>>) {
    val scope = Scope("Global")
    loadSpecialAndNativeFunctionsIntoScope(scope)
    loadStdLibIntoScope(scope)
    val context = RunningContext(isStandardLibrary = false)
    for ((statement, line) in statements) {
        when (val continuation = runStatement(line, statement, scope, context)) {
            NormalContinuation -> { /* expected behavior */ }
            is Return -> throw IllegalStatementException(continuation.line, "return is only allowed inside functions")
            is Continue -> throw IllegalStatementException(continuation.line, "continue is only allowed in loops")
            is Break -> throw IllegalStatementException(continuation.line, "break is only allowed in loops")
        }
    }
}

// returns whether to continue execution in current function scope
fun runStatement(line: Int, statement: Statement, scope: Scope, context: RunningContext): Continuation {
    return when (statement) {
        is Definition -> {
            val (names) = statement
            for (name in names) {
                if (scope.isVariableDefinedInThisScope(name)) {
                    throw VariableException(line, "Cannot define variable $name because it already exists.")
                }
                scope.defineVariable(name)
            }
            NormalContinuation
        }
        is Assignment -> {
            val (name, expression) = statement
            if (!scope.isVariableDefined(name)) {
                throw VariableException(line, "Cannot assign to variable $name because it does not exist.")
            }
            if (scope.isConstant(name)) {
                throw VariableException(line, "Cannot assign to constant $name")
            }
            scope.setValue(name, evaluateExpression(line, expression, scope, context))
            NormalContinuation
        }
        is ConstantDefinition -> {
            val (name, expression) = statement

            if (scope.isVariableDefinedInThisScope(name)) {
                throw VariableException(line, "Cannot define variable $name because it already exists.")
            }
            scope.defineConstant(name, evaluateExpression(line, expression, scope, context))
            NormalContinuation
        }
        is FunctionCall -> {
            evaluateFunctionExpression(line, statement.function, scope, context)
            NormalContinuation
        }
        is GroupedStatement -> {
            for (subStatement in statement.statements) {
                val continueExecution = runStatement(line, subStatement, scope, context)
                if (continueExecution != NormalContinuation) {
                    return continueExecution
                }
            }
            NormalContinuation
        }
        is Block -> {
            runBlock(line, statement, scope, context)
        }
        is IfElse -> {
            val (condition, thenBody, elseBody) = statement
            val isTrue = isTruthy(evaluateExpression(line, condition, scope, context), line)
            if (isTrue) {
                return runBlock(line, thenBody, scope, context)
            } else if (elseBody != null) {
                return runBlock(line, elseBody, scope, context)
            }
            NormalContinuation
        }
        is While -> {
            val (condition, body) = statement
            loop@while (isTruthy(evaluateExpression(line, condition, scope, context), line)) {
                val continueExecution = runBlock(line, body, scope, context)
                when (continueExecution) {
                    is Return -> return continueExecution
                    is Continue, NormalContinuation -> { /* simply continue execution */ }
                    is Break -> break@loop
                }
            }
            NormalContinuation
        }
        is For -> {
            val (identifier, iterableExpr, body) = statement
            if (!scope.isVariableDefined(identifier)) {
                throw ForLoopException(line, "For loop variable $identifier has to be defined beforehand.")
            }
            when (val iterable = evaluateExpression(line, iterableExpr, scope, context)) {
                is ListValue -> {
                    val list = iterable.value
                    loop@for (element in list) {
                        scope.setValue(identifier, element)
                        val continueExecution = runBlock(line, body, scope, context)
                        when (continueExecution) {
                            is Return -> return continueExecution
                            is Continue, NormalContinuation -> { /* simply continue execution */ }
                            is Break -> break@loop
                        }
                    }
                    NormalContinuation
                }
                is DictValue -> {
                    val dict = iterable.value
                    loop@for (key in dict.keys) {
                        scope.setValue(identifier, key)
                        val continueExecution = runBlock(line, body, scope, context)
                        when (continueExecution) {
                            is Return -> return continueExecution
                            is Continue, NormalContinuation -> { /* simply continue execution */ }
                            is Break -> break@loop
                        }
                    }
                    NormalContinuation
                }
                is RangeValue -> {
                    val (start, end, step) = iterable
                    var i = start

                    loop@while (step > 0 && i <= end || step < 0 && i >= end) {
                        scope.setValue(identifier, IntValue.of(i))
                        val continueExecution = runBlock(line, body, scope, context)
                        when (continueExecution) {
                            is Return -> return continueExecution
                            is Continue, NormalContinuation -> { /* simply continue execution */ }
                            is Break -> break@loop
                        }
                        i += step
                    }
                    NormalContinuation
                }
                else -> {
                    throw ForLoopException(
                        line,
                        "Can only iterate over value of type ${valueTypeName(listZero)}, ${valueTypeName(dictZero)} or ${valueTypeName(rangeZero)}, found ${valueTypeName(iterable)}"
                    )
                }
            }
        }
        is ReturnStatement -> {
            val returnValue = evaluateExpression(line, statement.expression, scope, context)
            Return(line, returnValue)
        }
        BreakStatement -> {
            Break(line)
        }
        ContinueStatement -> {
            Continue(line)
        }
    }
}

private fun runBlock(line: Int, block: Block, scope: Scope, context: RunningContext, newScope: Boolean = true): Continuation {
    val blockScope = if (newScope) {
        Scope(label = "Block@$line", parentScope = scope)
    } else {
        scope
    }
    for ((subStatement, subLine) in block.statements) {
        val continueExecution = runStatement(subLine, subStatement, blockScope, context)
        if (continueExecution != NormalContinuation) {
            return continueExecution
        }
    }
    return NormalContinuation
}

fun evaluateExpression(line: Int, expression: Expression, scope: Scope, context: RunningContext): Value {
    return when (expression) {
        NilExpression -> Nil
        is IntConst -> IntValue.of(expression.value)
        is FloatConst -> FloatValue(expression.value)
        is BoolConst -> BoolValue.of(expression.value)
        is StringConst -> StringValue.fromCache(expression.value)
        is Variable ->
            scope.getValue(expression.name) ?: throw VariableException(
                line,
                "Cannot access variable ${expression.name} because it does not exist."
            )
        is FunctionExpression -> evaluateFunctionExpression(line, expression, scope, context)
        is FunctionDefinition -> FunctionValue(expression.args, expression.body, scope, context.isStandardLibrary)
    }
}

@Suppress("FoldInitializerAndIfToElvis")
fun evaluateFunctionExpression(line: Int, expression: FunctionExpression, scope: Scope, context: RunningContext): Value {
    val (function, args) = expression

    when (val value = evaluateExpression(line, function, scope, context)) {
        is SpecialFunctionValue -> {
            when (value.specialFunction) {
                SpecialFunction.AND -> {
                    for (arg in args) {
                        if (!isTruthy(evaluateExpression(line, arg, scope, context), line)) {
                            return BoolValue.of(false)
                        }
                    }
                    return BoolValue.of(true)
                }
                SpecialFunction.OR -> {
                    for (arg in args) {
                        if (isTruthy(evaluateExpression(line, arg, scope, context), line)) {
                            return BoolValue.of(true)
                        }
                    }
                    return BoolValue.of(false)
                }
                SpecialFunction.CHOICE -> {
                    if (args.size != 3) {
                        throw WrongNumberOfArgumentsException(line, 3, args.size)
                    }
                    val (condition, ifTrue, ifFalse) = args
                    return if (isTruthy(evaluateExpression(line, condition, scope, context), line)) {
                        evaluateExpression(line, ifTrue, scope, context)
                    } else {
                        evaluateExpression(line, ifFalse, scope, context)
                    }
                }
            }
        }
        is NativeFunctionValue -> {
            return value.nativeFunction(line, args.map { evaluateExpression(line, it, scope, context) })
        }
        is FunctionValue -> {
            val (parameters, body, outerScope, isStandardLibrary) = value

            val functionScope = Scope("Function($function)@$line", parentScope = outerScope, isFunctionScope = true)
            if (parameters.size != args.size) {
                throw WrongNumberOfArgumentsException(line, parameters.size, args.size)
            }
            args.forEachIndexed { i, arg ->
                val argName = parameters[i]
                functionScope.defineVariable(argName)
                functionScope.setValue(argName, evaluateExpression(line, arg, scope, context))
            }
            return try {
                when (val continuation = runBlock(line, body, functionScope, context, newScope = false)) {
                    is Return -> continuation.returnValue
                    NormalContinuation -> Nil
                    is Break -> throw IllegalStatementException(continuation.line, "break is only allowed in loops")
                    is Continue -> throw IllegalStatementException(continuation.line, "continuation is only allowed in loops")
                }
            } catch (e: VoxRuntimeException) {
                if (isStandardLibrary) {
                    e.line = line
                }
                throw e
            }
        }
        else -> {
            throw FunctionException(line, "\"$function\" is not a function.")
        }
    }
}
