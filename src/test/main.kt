package test

import parsing.*
import runtime.VoxRuntimeException
import runtime.runAst
import java.io.File

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Interpreter expects exactly one argument: the source file to interpret")
        return
    }
    try {
        val text = File(args[0]).readText()
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