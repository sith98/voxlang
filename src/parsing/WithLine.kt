package parsing

data class WithLine<T>(val token: T, val line: Int) {
    override fun toString() = token.toString()
}