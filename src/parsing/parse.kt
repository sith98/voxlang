package parsing

fun parse(tokens: TokenStream): List<WithLine<Statement>> {
    val ast = mutableListOf<WithLine<Statement>>()
    while (!tokens.isEmpty()) {
        ast.add(parseStatement(tokens))
    }
    return ast
}

fun parseStatement(tokens: TokenStream): WithLine<Statement> {
    val (token, line) = tokens.next()
    return when (token) {
        is Keyword -> {
            when (token.keyword) {
                KeywordE.VAR -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    Definition(identifier.name)
                }
                KeywordE.AS -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    Assignment(identifier.name, expr)
                }
                KeywordE.VARAS -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    GroupedStatement(
                        listOf(
                            Definition(identifier.name),
                            Assignment(identifier.name, expr)
                        )
                    )
                }
                KeywordE.CONST -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    ConstantDefinition(identifier.name, expr)
                }
                KeywordE.RETURN -> {
                    val (expr) = parseExpression(tokens)
                    Return(expr)
                }
                KeywordE.EXIT -> {
                    Return(NilExpression)
                }
                KeywordE.DO -> {
                    parseBlock(tokens)
                }
                KeywordE.IF -> {
                    parseIfElse(tokens)
                }
                KeywordE.WHILE -> {
                    val (expression) = parseExpression(tokens)
                    val (body) = parseStatement(tokens)
                    While(expression, body)
                }
                KeywordE.FUNCTION -> {
                    val (name) = tokens.nextAs<Identifier>()
                    val functionDefinition = parseFunctionDefinition(tokens)
                    ConstantDefinition(name.name, functionDefinition)
                }
                else -> throw ParsingException(line, "Unexpected keyword ${token.keyword.word}")
            }
        }
        is Symbol -> {
            when (token.symbol) {
                SymbolE.OPEN_PAREN -> {
                    val expression = parseFunctionExpression(tokens)
                    FunctionCall(expression)
                }
                else -> throw ParsingException(line, "Unexpected symbol ${token.symbol.symbol}")
            }
        }
        else -> throw ParsingException(line, "Expected statement, got $token")
    } withLine line
}

fun parseBlock(tokens: TokenStream): Block {
    val statements = mutableListOf<WithLine<Statement>>()
    while (true) {
        val (token) = tokens.peek()
        if (token is Keyword && token.keyword == KeywordE.END) {
            tokens.next()
            break
        }
        val statement = parseStatement(tokens)
        statements.add(statement)
    }
    return Block(statements)
}

fun parseIfElse(tokens: TokenStream): IfElse {
    val (expression) = parseExpression(tokens)
    val (thenBody) = parseStatement(tokens)
    val (nextToken) = tokens.peek()
    if (nextToken is Keyword) {
        if (nextToken.keyword == KeywordE.ELSE) {
            tokens.next()
            val (elseBody) = parseStatement(tokens)
            return IfElse(expression, thenBody, elseBody)
        }
    }
    return IfElse(expression, thenBody, null)
}

fun parseExpression(tokens: TokenStream): WithLine<Expression> {
    val (token, line) = tokens.next()
    return when (token) {
        is Keyword -> {
            when (token.keyword) {
                KeywordE.NIL -> NilExpression
                KeywordE.TRUE -> BoolConst(true)
                KeywordE.FALSE -> BoolConst(false)
                KeywordE.FUNC -> parseFunctionDefinition(tokens)
                else -> throw ParsingException(line, "Unexpected keyword ${token.keyword.word}")
            }
        }
        is Identifier -> Variable(token.name)
        is IntLiteral -> IntConst(token.num)
        is FloatLiteral -> FloatConst(token.num)
        is StringLiteral -> StringConst(token.string)
        is Symbol -> {
            when (token.symbol) {
                SymbolE.OPEN_PAREN -> parseFunctionExpression(tokens)
                else -> throw ParsingException(line, "Unexpected symbol ${token.symbol.symbol}")
            }
        }
    } withLine line
}

fun parseFunctionDefinition(tokens: TokenStream): FunctionDefinition {
    val args = parseIdentifierList(tokens).map { it.name }
    val (body) = parseStatement(tokens)
    return FunctionDefinition(args, body)
}

fun parseIdentifierList(tokens: TokenStream): List<Identifier> {
    val identifiers = mutableListOf<Identifier>()
    tokens.expectSymbol(SymbolE.OPEN_BRACKET)

    while (true) {
        val (token) = tokens.peek()
        if (token is Identifier) {
            identifiers.add(token)
            tokens.next()
        } else {
            tokens.expectSymbol(SymbolE.CLOSE_BRACKET)
            break
        }
    }
    return identifiers
}

fun parseFunctionExpression(tokens: TokenStream): FunctionExpression {
    val (name) = tokens.nextAs<Identifier>()
    val args = mutableListOf<Expression>()

    while (true) {
        val (token) = tokens.peek()
        if (token is Symbol && token.symbol == SymbolE.CLOSE_PAREN) {
            tokens.next()
            break
        }
        val (expression) = parseExpression(tokens)
        args.add(expression)
    }

    return FunctionExpression(name.name, args)
}