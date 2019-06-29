package parsing

data class WithLine<out T>(val token: T, val line: Int) {
    override fun toString() = token.toString()
}

infix fun <T> T.withLine(line: Int): WithLine<T> = WithLine(this, line)