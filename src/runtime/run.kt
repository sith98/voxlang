package runtime

import parsing.*

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
    for ((statement, line) in stdLib) {
        runStatement(line, statement, scope)
    }
}

fun runAst(statements: List<WithLine<Statement>>) {
    val scope = Scope("Global")
    loadSpecialAndNativeFunctionsIntoScope(scope)
    loadStdLibIntoScope(scope)
    for ((statement, line) in statements) {
        runStatement(line, statement, scope)
    }
}

// returns whether to continue execution in current function scope
fun runStatement(line: Int, statement: Statement, scope: Scope): Boolean {
    return when (statement) {
        is Definition -> {
            val (names) = statement
            for (name in names) {
                if (scope.isVariableDefinedInThisScope(name)) {
                    throw VariableException(line, "Cannot define variable $name because it already exists.")
                }
                scope.defineVariable(name)
            }
            true
        }
        is Assignment -> {
            val (name, expression) = statement
            if (!scope.isVariableDefined(name)) {
                throw VariableException(line, "Cannot assign to variable $name because it does not exist.")
            }
            if (scope.isConstant(name)) {
                throw VariableException(line, "Cannot assign to constant $name")
            }
            scope.setValue(name, evaluateExpression(line, expression, scope))
            true
        }
        is ConstantDefinition -> {
            val (name, expression) = statement

            if (scope.isVariableDefinedInThisScope(name)) {
                throw VariableException(line, "Cannot define variable $name because it already exists.")
            }
            scope.defineConstant(name, evaluateExpression(line, expression, scope))
            true
        }
        is FunctionCall -> {
            evaluateFunctionExpression(line, statement.function, scope)
            true
        }
        is Block -> {
            val blockScope = Scope(label = "Block@$line", parentScope = scope)
            for ((subStatement, subLine) in statement.statements) {
                val continueExecution = runStatement(subLine, subStatement, blockScope)
                if (!continueExecution) {
                    return false
                }
            }
            true
        }
        is IfElse -> {
            val (condition, thenBody, elseBody) = statement
            val isTrue = isTruthy(evaluateExpression(line, condition, scope))
            if (isTrue) {
                return runStatement(line, thenBody, scope)
            } else if (elseBody != null) {
                return runStatement(line, elseBody, scope)
            }
            true
        }
        is While -> {
            val (condition, body) = statement
            while (isTruthy(evaluateExpression(line, condition, scope))) {
                val continueExecution = runStatement(line, body, scope)
                if (!continueExecution) {
                    return false
                }
            }
            true
        }
        is For -> {
            val (identifier, iterableExpr, body) = statement
            if (!scope.isVariableDefined(identifier)) {
                throw ForLoopException(line, "For loop variable $identifier has to be defined beforehand.")
            }
            when (val iterable = evaluateExpression(line, iterableExpr, scope)) {
                is ListValue -> {
                    val list = iterable.value
                    for (element in list) {
                        scope.setValue(identifier, element)
                        val continueExecution = runStatement(line, body, scope)
                        if (!continueExecution) {
                            return false
                        }
                    }
                    true
                }
                is DictValue -> {
                    val dict = iterable.value
                    for (key in dict.keys) {
                        scope.setValue(identifier, key)
                        val continueExecution = runStatement(line, body, scope)
                        if (!continueExecution) {
                            return false
                        }
                    }
                    true
                }
                is RangeValue -> {
                    val (start, end, step) = iterable
                    var i = start

                    while (step > 0 && i <= end || step < 0 && i >= end) {
                        scope.setValue(identifier, IntValue.of(i))
                        val continueExecution = runStatement(line, body, scope)
                        if (!continueExecution) {
                            return false
                        }
                        i += step
                    }
                    true
                }
                else -> {
                    throw ForLoopException(
                        line,
                        "Can only iterate over value of type ${valueTypeName(listZero)} or ${valueTypeName(dictZero)}, found ${valueTypeName(iterable)}"
                    )
                }
            }
        }
        is Return -> {
            var functionScope = scope
            while (!functionScope.isFunctionScope) {
                functionScope = functionScope.parentScope ?: throw IllegalStatementException(
                    line,
                    "\"return\" statement is only allowed in functions."
                )
            }
            functionScope.returnValue = evaluateExpression(line, statement.expression, scope)
            false
        }
        is GroupedStatement -> {
            for (subStatement in statement.statements) {
                val continueExecution = runStatement(line, subStatement, scope)
                if (!continueExecution) {
                    return false
                }
            }
            true
        }
    }
}

fun evaluateExpression(line: Int, expression: Expression, scope: Scope): Value {
    return when (expression) {
        NilExpression -> Nil
        is IntConst -> IntValue.of(expression.value)
        is FloatConst -> FloatValue(expression.value)
        is BoolConst -> BoolValue.of(expression.value)
        is StringConst -> StringValue(expression.value)
        is Variable ->
            scope.getValue(expression.name) ?: throw VariableException(
                line,
                "Cannot access variable ${expression.name} because it does not exist."
            )
        is FunctionExpression -> evaluateFunctionExpression(line, expression, scope)
        is FunctionDefinition -> FunctionValue(expression.args, expression.body, scope)
    }
}

@Suppress("FoldInitializerAndIfToElvis")
fun evaluateFunctionExpression(line: Int, expression: FunctionExpression, scope: Scope): Value {
    val (name, args) = expression

    val value = scope.getValue(name)
    if (value == null) {
        throw FunctionException(line, "Function with name $name does not exist.")
    }
    when (value) {
        is SpecialFunctionValue -> {
            when (value.specialFunction) {
                SpecialFunction.AND -> {
                    for (arg in args) {
                        if (!isTruthy(evaluateExpression(line, arg, scope))) {
                            return BoolValue.of(false)
                        }
                    }
                    return BoolValue.of(true)
                }
                SpecialFunction.OR -> {
                    for (arg in args) {
                        if (isTruthy(evaluateExpression(line, arg, scope))) {
                            return BoolValue.of(true)
                        }
                    }
                    return BoolValue.of(false)
                }
            }
        }
        is NativeFunctionValue -> {
            return value.nativeFunction(line, args.map { evaluateExpression(line, it, scope) })
        }
        is FunctionValue -> {
            val (parameters, body, outerScope) = value

            val functionScope = Scope("Function($name)@$line", parentScope = outerScope, isFunctionScope = true)
            if (parameters.size != args.size) {
                throw WrongNumberOfArgumentsException(line, parameters.size, args.size)
            }
            args.forEachIndexed { i, arg ->
                val argName = parameters[i]
                functionScope.defineVariable(argName)
                functionScope.setValue(argName, evaluateExpression(line, arg, scope))
            }
            runStatement(line, body, functionScope)
            return functionScope.returnValue
        }
        else -> {
            throw FunctionException(line, "\"$name\" is not a function.")
        }
    }
}
