package test

import parsing.*
import runtime.VoxRuntimeException
import runtime.runAst
import java.io.File

fun main() {
    try {
        val text = File("src/test/test.vox").readText()
        val tokens = TokenStream(tokenize(text))

        val ast = parse(tokens)
        runAst(ast)
    } catch (exception: TokenizeException) {
        println(exception.message)
    } catch (exception: ParsingException) {
        println(exception.message)
    } catch (exception: VoxRuntimeException) {
        println(exception.message)
    }
}