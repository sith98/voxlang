package test

import parsing.TokenStream
import parsing.parse
import parsing.tokenize
import runtime.runAst
import java.io.File

fun main() {
    val text = File("src/test/test.vox").readText()
    val tokens = TokenStream(tokenize(text))

    val ast = parse(tokens)
    runAst(ast)
}