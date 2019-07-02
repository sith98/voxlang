package parsing

enum class TokenizerState {
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

    var state = TokenizerState.WHITESPACE
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
            in whitespace -> state = TokenizerState.WHITESPACE
            in commentStart -> state = TokenizerState.COMMENT
            else -> {
                currentToken = StringBuilder().append(char)
                state = TokenizerState.SYMBOL
            }
        }
    }

    for (char in "$text ") {
        if (char == '\n') {
            line += 1
        }
        when (state) {
            TokenizerState.WHITESPACE -> {
                when (char) {
                    in identifierStart -> {
                        currentToken = StringBuilder().append(char)
                        state = if (char in numStart) TokenizerState.IDENTIFIER_OR_NUM else TokenizerState.IDENTIFIER
                    }
                    in numStart -> {
                        currentToken = StringBuilder().append(char)
                        state = TokenizerState.NUM
                    }
                    in whitespace -> {
                        // do nothing
                    }
                    in stringStart -> {
                        currentToken = StringBuilder()
                        state = TokenizerState.STRING
                    }
                    in allowedSymbolCharacters -> {
                        currentToken = StringBuilder().append(char)
                        state = TokenizerState.SYMBOL
                    }
                    in commentStart -> {
                        state = TokenizerState.COMMENT
                    }
                    else -> {
                        throw TokenizeException(line, "Unexpected character: $char")
                    }
                }
            }
            TokenizerState.IDENTIFIER -> {
                when (char) {
                    in identifierMiddle -> {
                        currentToken.append(char)
                    }
                    else -> {
                        endOfIdentifier(char)
                    }
                }
            }
            TokenizerState.IDENTIFIER_OR_NUM -> {
                when (char) {
                    in numMiddle -> {
                        currentToken.append(char)
                        state = TokenizerState.NUM
                    }
                    in identifierMiddle -> {
                        currentToken.append(char)
                        state = TokenizerState.IDENTIFIER
                    }
                    else -> {
                        endOfIdentifier(char)
                    }
                }
            }
            TokenizerState.STRING -> {
                when (char) {
                    in stringEscape -> state = TokenizerState.STRING_ESCAPE
                    in stringEnd -> {
                        addToken(StringLiteral(currentToken.toString()))
                        state = TokenizerState.WHITESPACE
                        currentToken = StringBuilder()
                    }
                    else -> currentToken.append(char)
                }
            }
            TokenizerState.STRING_ESCAPE -> {
                val escapeChar = escapeSequences[char] ?: throw TokenizeException(line, "Illegal escape sequence: $char")
                currentToken.append(escapeChar)
                state = TokenizerState.STRING
            }
            TokenizerState.NUM -> {
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
                        in whitespace -> state = TokenizerState.WHITESPACE
                        in commentStart -> state = TokenizerState.COMMENT
                        else -> {
                            currentToken = StringBuilder().append(char)
                            state = TokenizerState.SYMBOL
                        }
                    }
                }
            }
            TokenizerState.SYMBOL -> {
                val symbolToken = currentToken.toString()
                if (symbolToken in parenMap || char !in allowedSymbolCharacters) {
                    val symbol = symbolMap[symbolToken] ?: throw TokenizeException(line, "Unknown symbol: $symbolToken")
                    addToken(Symbol(symbol))
                    when (char) {
                        in whitespace -> state = TokenizerState.WHITESPACE
                        in stringStart -> state = TokenizerState.STRING
                        in commentStart -> state = TokenizerState.COMMENT
                        else -> {
                            currentToken = StringBuilder().append(char)
                            state = when (char) {
                                in identifierStart -> if (char in numStart) TokenizerState.IDENTIFIER_OR_NUM else TokenizerState.IDENTIFIER
                                in numStart -> TokenizerState.NUM
                                in allowedSymbolCharacters -> TokenizerState.SYMBOL
                                else -> throw TokenizeException(line, "Unexpected character: $char")
                            }
                        }
                    }
                } else {
                    currentToken.append(char)
                }
            }
            TokenizerState.COMMENT -> {
                if (char in commentEnd) {
                    state = TokenizerState.WHITESPACE
                }
            }
        }
    }

    return tokens
}


