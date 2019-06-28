package parsing

class TokenStream(val stream: List<WithLine<Token>>) {
    private var index = 0

    fun next(): WithLine<Token> {
        return peek().also { index += 1 }
    }

    private fun peek() = stream[index]
}