package runtime

import parsing.*

fun loadStdLib(): List<WithLine<Statement>> {
    val source = object {}.javaClass.getResource("/runtime/stdlib/stdlib.vox").readText()
    return parse(TokenStream(tokenize(source)))
}

fun runAst(statements: List<WithLine<Statement>>) {
    val scope = Scope()
    val stdLib = loadStdLib()

    for ((statement, line) in stdLib) {
        runStatement(line, statement, scope)
    }
    for ((statement, line) in statements) {
        runStatement(line, statement, scope)
    }
}

// returns whether to continue execution in current function scope
fun runStatement(line: Int, statement: Statement, scope: Scope) {
    when (statement) {
        is Definition -> {
            val (name) = statement
            if (scope.isVariableDefinedInThisScope(name)) {
                throw VariableException(line, "Cannot define variable $name because it already exists.")
            }
            if (name in illegalVariableNames) {
                throw VariableException(line, "Cannot define variable $name as this is an illegal variable name")
            }
            scope.defineVariable(name)
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
        }
        is ConstantDefinition -> {
            val (name, expression) = statement

            if (scope.isVariableDefinedInThisScope(name)) {
                throw VariableException(line, "Cannot define variable $name because it already exists.")
            }
            if (name in illegalVariableNames) {
                throw VariableException(line, "Cannot define variable $name as this is an illegal variable name")
            }
            scope.defineConstant(name, evaluateExpression(line, expression, scope))
        }
        is FunctionCall -> {
            evaluateFunctionExpression(line, statement.function, scope)
        }
        is Block -> {
            val blockScope = Scope(parentScope = scope)
            for ((subStatement, subLine) in statement.statements) {
                runStatement(subLine, subStatement, blockScope)
                if (!scope.continueExecution) {
                    return
                }
            }
        }
        is IfElse -> {
            val (condition, thenBody, elseBody) = statement
            val isTrue = isTruthy(evaluateExpression(line, condition, scope))
            if (isTrue) {
                runStatement(line, thenBody, scope)
            } else if (elseBody != null) {
                runStatement(line, elseBody, scope)
            }
        }
        is While -> {
            val (condition, body) = statement
            while (isTruthy(evaluateExpression(line, condition, scope))) {
                runStatement(line, body, scope)
            }
        }
        is Return -> {
            var functionScope = scope
            while (!functionScope.isFunctionScope) {
                functionScope = scope.parentScope ?: throw IllegalStatementException(
                    line,
                    "\"return\" statement is only allowed in functions."
                )
            }
            functionScope.returnValue = evaluateExpression(line, statement.expression, scope)
            scope.terminateFunction()
        }
        is GroupedStatement -> {
            for (subStatement in statement.statements) {
                runStatement(line, subStatement, scope)
                if (!scope.continueExecution) {
                    return
                }
            }
        }
    }
    if (!scope.continueExecution) {
        return
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
            scope.getValue(expression.name) ?:
            throw VariableException(line, "Cannot access variable ${expression.name} because it does not exist.")
        is FunctionExpression -> evaluateFunctionExpression(line, expression, scope)
        is FunctionDefinition -> FunctionValue(expression.args, expression.body, scope)
    }
}

fun evaluateFunctionExpression(line: Int, expression: FunctionExpression, scope: Scope): Value {
    val (name, args) = expression
    val specialFunction = SpecialFunction.byIdentifier(name)
    if (specialFunction != null) {
        when (specialFunction) {
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

    val nativeFunction = nativeFunctions[name]
    if (nativeFunction != null) {
        return nativeFunction(line, args.map { evaluateExpression(line, it, scope) })
    }

    val value = scope.getValue(name) ?: throw FunctionException(line, "Function with name $name does not exist.")
    val function = value as? FunctionValue ?: throw FunctionException(line, "\"$name\" is not a function.")

    val (parameters, body, outerScope) = function

    val functionScope = Scope(parentScope = outerScope, isFunctionScope = true)
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
