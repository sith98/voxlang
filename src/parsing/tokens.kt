package parsing

enum class KeywordE(val word: String) {
    VAR("var"),
    VAR_AS("varas"),
    CONST("const"),
    AS("as"),
    RETURN("return"),
    EXIT("exit"),
    BREAK("break"),
    CONTINUE("continue"),
    DO("do"),
    END("end"),
    IF("if"),
    ELSE("else"),
    ELSE_IF("elif"),
    WHILE("while"),
    FOR("for"),
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
    CLOSE_BRACKET("]"),
    LAMBDA("\\"),
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

val parenMap = symbolMap.filterValues { it.isParen }
val identifierStart: Set<Char> = ('a' .. 'z').union('A' .. 'Z') + "_-+*/%><&|'!?$=~".toSet()
val identifierMiddle = identifierStart.union('0' .. '9')
private val digit = ('0' .. '9').toSet()
val numStart = digit.union(setOf('-', '+'))
val numMiddle = numStart.union(setOf('.'))
val whitespace = setOf(' ', '\n', '\r', '\t')
val stringStart = setOf('"')
val stringEnd = stringStart
val stringEscape = setOf('\\')
val escapeSequences = mapOf(
    '\\' to '\\',
    '"' to '"',
    'n' to '\n',
    'r' to '\r',
    't' to '\t'
)
val allowedSymbolCharacters = SymbolE.values().flatMap { it.symbol.asIterable() }.toSet()

val commentStart = setOf('#')
val commentEnd = setOf('\n')