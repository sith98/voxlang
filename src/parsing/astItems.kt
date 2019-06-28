package parsing

sealed class Expr
object Nil : Expr()
data class IntConst(val value: Int) : Expr()
data class FloatConst(val value: Float) : Expr()
data class BoolConst(val value: Boolean) : Expr()
data class StringConst(val value: String) : Expr()
data class Variable(val name: String) : Expr()
data class FunctionExpr(val name: String, val args: List<Expr>) : Expr()
data class FunctionDefinition(val args: List<String>, val body: Statement) : Expr()

sealed class Statement
data class Definition(val name: String) : Statement()
data class Assignment(val name: String, val value: Expr) : Statement()
data class FunctionCall(val function: FunctionExpr) : Statement()
data class Block(val statements: List<Statement>) : Statement()
data class If(val condition: Expr, val thenBody: Statement, val elseBody: Statement?) : Statement()
data class While(val condition: Expr, val body: Statement) : Statement()