package parsing

fun expectedException(line: Int, expected: Any, got: Any): Nothing {
    throw ParsingException(line, "Expected $expected, got $got")
}

class TokenStream(private val stream: List<WithLine<Token>>) {
    private var index = 0

    fun next(): WithLine<Token> {
        return peek().also { index += 1 }
    }

    fun expectSymbol(symbol: SymbolE) {
        val (nextToken, line) = next()
        if (nextToken !is Symbol) {
            expectedException(line, symbol.symbol, nextToken)
        }
        if (nextToken.symbol != symbol) {
            expectedException(line, symbol.symbol, nextToken.symbol.symbol)
        }
    }

    fun expectKeyword(keyword: KeywordE) {
        val (nextToken, line) = next()
        if (nextToken !is Keyword) {
            expectedException(line, keyword.word, nextToken)
        }
        if (nextToken.keyword != keyword) {
            expectedException(line, keyword.word, nextToken.keyword.word)
        }
    }

    fun expect(token: Token) {
        val (nextToken, line) = next()
        if (token != nextToken) {
            expectedException(line, token, nextToken)
        }
    }

    inline fun <reified T : Token> expect() {
        val (token, line) = peek()
        if (token !is T) {
            expectedException(line, T::class.simpleName ?: "unknown token", token)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Token> nextAs(): WithLine<T> {
        expect<T>()
        return next() as WithLine<T>
    }

    fun peek() =
        if (index < stream.size) {
            stream[index]
        } else {
            throw ParsingException(stream.last().line, "Unexpected end of file")
        }


    fun isEmpty() = index >= stream.size
}