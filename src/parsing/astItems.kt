package parsing

sealed class Expression
object NilExpression : Expression()
data class IntConst(val value: Int) : Expression()
data class FloatConst(val value: Double) : Expression()
data class BoolConst(val value: Boolean) : Expression()
data class StringConst(val value: String) : Expression()
data class Variable(val name: String) : Expression()
data class FunctionExpression(val name: String, val args: List<Expression>) : Expression()
data class FunctionDefinition(val args: List<String>, val body: Statement) : Expression()

sealed class Statement
data class Definition(val name: String) : Statement()
data class Assignment(val name: String, val value: Expression) : Statement()
data class FunctionCall(val function: FunctionExpression) : Statement()
data class Block(val statements: List<WithLine<Statement>>) : Statement()
data class IfElse(val condition: Expression, val thenBody: Statement, val elseBody: Statement?) : Statement()
data class While(val condition: Expression, val body: Statement) : Statement()
data class Return(val expression: Expression) : Statement()
data class GroupedStatement(val statements: List<Statement>) : Statement()