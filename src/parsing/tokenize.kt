package parsing

enum class TokenizingState {
    WHITESPACE,
    IDENTIFIER,
    STRING,
    STRING_ESCAPE,
    NUM,
    SYMBOL,
    COMMENT,
    IDENTIFIER_OR_NUM
}

fun tokenize(text: String): List<WithLine<Token>> {
    val tokens = mutableListOf<WithLine<Token>>()

    var state = TokenizingState.WHITESPACE
    var currentToken = StringBuilder()
    var line = 1

    fun addToken(token: Token) {
        tokens.add(WithLine(token, line))
    }

    fun endOfIdentifier(char: Char) {
        val identifier = currentToken.toString()
        val keyword = keywordMap[identifier]
        if (keyword == null) {
            addToken(Identifier(identifier))
        } else {
            addToken(Keyword(keyword))
        }
        when (char) {
            in whitespace -> state = TokenizingState.WHITESPACE
            in commentStart -> state = TokenizingState.COMMENT
            else -> {
                currentToken = StringBuilder().append(char)
                state = TokenizingState.SYMBOL
            }
        }
    }

    for (char in "$text ") {
        if (char == '\n') {
            line += 1
        }
        when (state) {
            TokenizingState.WHITESPACE -> {
                when (char) {
                    in identStart -> {
                        currentToken = StringBuilder().append(char)
                        state = if (char in numStart) TokenizingState.IDENTIFIER_OR_NUM else TokenizingState.IDENTIFIER
                    }
                    in numStart -> {
                        currentToken = StringBuilder().append(char)
                        state = TokenizingState.NUM
                    }
                    in whitespace -> {
                        // do nothing
                    }
                    in stringStart -> {
                        currentToken = StringBuilder()
                        state = TokenizingState.STRING
                    }
                    in allowedSymbolCharacters -> {
                        currentToken = StringBuilder().append(char)
                        state = TokenizingState.SYMBOL
                    }
                    in commentStart -> {
                        state = TokenizingState.COMMENT
                    }
                    else -> {
                        throw TokenizeException(line, "Unexpected character: $char")
                    }
                }
            }
            TokenizingState.IDENTIFIER -> {
                when (char) {
                    in identMiddle -> {
                        currentToken.append(char)
                    }
                    else -> {
                        endOfIdentifier(char)
                    }
                }
            }
            TokenizingState.IDENTIFIER_OR_NUM -> {
                when (char) {
                    in numMiddle -> {
                        currentToken.append(char)
                        state = TokenizingState.NUM
                    }
                    in identMiddle -> {
                        currentToken.append(char)
                        state = TokenizingState.IDENTIFIER
                    }
                    else -> {
                        endOfIdentifier(char)
                    }
                }
            }
            TokenizingState.STRING -> {
                when (char) {
                    in stringEscape -> state = TokenizingState.STRING_ESCAPE
                    in stringEnd -> {
                        addToken(StringLiteral(currentToken.toString()))
                        state = TokenizingState.WHITESPACE
                        currentToken = StringBuilder()
                    }
                    else -> currentToken.append(char)
                }
            }
            TokenizingState.STRING_ESCAPE -> {
                val escapeChar = escapeSequences[char] ?: throw TokenizeException(line, "Illegal escape sequence: $char")
                currentToken.append(escapeChar)
                state = TokenizingState.STRING
            }
            TokenizingState.NUM -> {
                if (char in numMiddle) {
                    currentToken.append(char)
                } else {
                    val literal = currentToken.toString()
                    val int = literal.toIntOrNull()
                    val double = literal.toDoubleOrNull()
                    when {
                        int != null -> addToken(IntLiteral(int))
                        double != null -> addToken(FloatLiteral(double))
                        else -> throw TokenizeException(line, "Illegal number literal: $literal")
                    }
                    when (char) {
                        in whitespace -> state = TokenizingState.WHITESPACE
                        in commentStart -> state = TokenizingState.COMMENT
                        else -> {
                            currentToken = StringBuilder().append(char)
                            state = TokenizingState.SYMBOL
                        }
                    }
                }
            }
            TokenizingState.SYMBOL -> {
                val symbolToken = currentToken.toString()
                if (symbolToken in paranMap || char !in allowedSymbolCharacters) {
                    val symbol = symbolMap[symbolToken] ?: throw TokenizeException(line, "Unknown symbol: $symbolToken")
                    addToken(Symbol(symbol))
                    when (char) {
                        in whitespace -> state = TokenizingState.WHITESPACE
                        in stringStart -> state = TokenizingState.STRING
                        in commentStart -> state = TokenizingState.COMMENT
                        else -> {
                            currentToken = StringBuilder().append(char)
                            state = when (char) {
                                in identStart -> if (char in numStart) TokenizingState.IDENTIFIER_OR_NUM else TokenizingState.IDENTIFIER
                                in numStart -> TokenizingState.NUM
                                in allowedSymbolCharacters -> TokenizingState.SYMBOL
                                else -> throw TokenizeException(line, "Unexpected character: $char")
                            }
                        }
                    }
                } else {
                    currentToken.append(char)
                }
            }
            TokenizingState.COMMENT -> {
                if (char in commentEnd) {
                    state = TokenizingState.WHITESPACE
                }
            }
        }
    }

    return tokens
}


