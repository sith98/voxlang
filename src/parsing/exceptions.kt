package parsing

class TokenizeException(val line: Int, cause: String) : Exception("Parsing error at line $line: $cause")
class ParsingException(val line: Int, cause: String) : Exception("Parsing error at line $line: $cause")