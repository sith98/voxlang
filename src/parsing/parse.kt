package parsing

import java.lang.IllegalStateException

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
                    val identifiers = parseIdentifierList(tokens)
                    Definition(identifiers)
                }
                KeywordE.AS -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    Assignment(identifier.name, expr)
                }
                KeywordE.VAR_AS -> {
                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    GroupedStatement(
                        listOf(
                            Definition(listOf(identifier.name)),
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
                    ReturnStatement(expr)
                }
                KeywordE.EXIT -> {
                    ReturnStatement(NilExpression)
                }
                KeywordE.DO -> {
                    parseBlock(tokens)
                }
                KeywordE.IF -> {
                    parseIfElse(tokens)
                }
                KeywordE.WHILE -> {
                    val (expression) = parseExpression(tokens)
                    val body = parseBlock(tokens)
                    While(expression, body)
                }
                KeywordE.FOR -> {
                    val (next) = tokens.peek()
                    val variableDeclaration = if (next == Keyword(KeywordE.VAR)) {
                        tokens.next()
                        true
                    } else {
                        false
                    }

                    val (identifier) = tokens.nextAs<Identifier>()
                    val (expr) = parseExpression(tokens)
                    val body = parseBlock(tokens)
                    if (variableDeclaration) {
                        Block(
                            listOf(
                                Definition(listOf(identifier.name)) withLine line,
                                For(identifier.name, expr, body) withLine line
                            )
                        )
                    } else {
                        For(identifier.name, expr, body)
                    }
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

    val thenBody = mutableListOf<WithLine<Statement>>()

    var nextTokenWithLine: WithLine<Token>
    var nextToken: Token
    while (true) {
        nextTokenWithLine = tokens.peek()
        nextToken = nextTokenWithLine.token
        if (nextToken is Keyword && (nextToken.keyword == KeywordE.END || nextToken.keyword == KeywordE.ELSE || nextToken.keyword == KeywordE.ELSE_IF)) {
            break
        }
        thenBody.add(parseStatement(tokens))
    }

    // consume keyword
    tokens.next()
    val keyword = nextToken as Keyword
    if (keyword.keyword == KeywordE.END) {
        return IfElse(expression, Block(thenBody), null)
    }
    if (keyword.keyword == KeywordE.ELSE) {
        return IfElse(expression, Block(thenBody), parseBlock(tokens))
    }
    if (keyword.keyword == KeywordE.ELSE_IF) {
        return IfElse(expression, Block(thenBody), Block(listOf(parseIfElse(tokens) withLine nextTokenWithLine.line)))
    }
    throw IllegalStateException("Should be unreachable")
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
                SymbolE.LAMBDA -> {
                    val args = parseIdentifierList(tokens)
                    val (body) = parseExpression(tokens)
                    FunctionDefinition(args, Block(listOf(ReturnStatement(body) withLine line)))
                }
                else -> throw ParsingException(line, "Unexpected symbol ${token.symbol.symbol}")
            }
        }
    } withLine line
}

fun parseFunctionDefinition(tokens: TokenStream): FunctionDefinition {
    val args = parseIdentifierList(tokens)
    val body = parseBlock(tokens)
    return FunctionDefinition(args, body)
}

fun parseIdentifierList(tokens: TokenStream): List<String> {
    val identifiers = mutableListOf<String>()

    val (next) = tokens.peek()
    when (next) {
        is Symbol -> {
            tokens.expectSymbol(SymbolE.OPEN_BRACKET)
        }
        is Identifier -> {
            identifiers.add(next.name)
            tokens.next()
            return identifiers
        }
    }

    while (true) {
        val (token) = tokens.peek()
        if (token is Identifier) {
            identifiers.add(token.name)
            tokens.next()
        } else {
            tokens.expectSymbol(SymbolE.CLOSE_BRACKET)
            break
        }
    }
    return identifiers
}

fun parseFunctionExpression(tokens: TokenStream): FunctionExpression {
    val (function) = parseExpression(tokens)
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

    return FunctionExpression(function, args)
}