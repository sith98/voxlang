package parsing

enum class KeywordE(val word: String) {
    VAR("var"),
    VARAS("varas"),
    AS("as"),
    RETURN("return"),
    EXIT("exit"),
    DO("do"),
    END("end"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    FUNC("func"),
    FUNCTION("function"),
    NIL("nil"),
    TRUE("true"),
    FALSE("false")
}
enum class SymbolE(val symbol: String, val isParen: Boolean = true) {
    OPEN_PAREN("("),
    CLOSE_PAREN(")"),
    OPEN_BRACKET("["),
    CLOSE_BRACKET("]")
}
sealed class Token
data class Identifier(val name: String) : Token()
data class IntLiteral(val num: Int) : Token()
data class FloatLiteral(val num: Double) : Token()
data class StringLiteral(val string: String) : Token()
data class Symbol(val symbol: SymbolE) : Token()
data class Keyword(val keyword: KeywordE) : Token()

val symbolMap = run {
    val map = mutableMapOf<String, SymbolE>()
    for (value in SymbolE.values()) {
        map[value.symbol] = value
    }
    map
}
val keywordMap = run {
    val map = mutableMapOf<String, KeywordE>()
    for (value in KeywordE.values()) {
        map[value.word] = value
    }
    map
}

val paranMap = symbolMap.filterValues { it.isParen }
val identStart = ('a' .. 'z').union('A' .. 'Z') + '_'
val identMiddle = identStart.union('0' .. '9')
val numStart = ('0' .. '9').toSet()
val numMiddle = numStart.union(setOf('.'))
val whitespace = setOf(' ', '\n', '\r', '\t')
val stringStart = setOf('"')
val stringEnd = stringStart
val stringEscape = setOf('\\')
val escapeSequences = mapOf(
    '\\' to '\\',
    '"' to '"',
    'n' to '\n'
)
val allowedSymbolCharacters = SymbolE.values().flatMap { it.symbol.asIterable() }.toSet()

val commentStart = setOf('#')
val commentEnd = setOf('\n')