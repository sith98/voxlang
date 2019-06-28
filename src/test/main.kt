package test

import parsing.tokenize
import java.io.File

fun main() {
    val text = File("src/test/test.vox").readText()
    println(tokenize(text))
}