package runtime

import java.lang.IllegalStateException


class Scope(val parentScope: Scope? = null, val isFunctionScope: Boolean = false) {
    private val scope = mutableMapOf<String, Value>()

    var returnValue: Value = Nil
    var continueExecution = true
        private set

    fun terminateFunction() {
        if (isFunctionScope) {
            continueExecution = false
        } else {
            parentScope?.terminateFunction()
        }
    }

    fun isVariableDefinedInThisScope(identifier: String): Boolean {
        return identifier in scope
    }
    fun isVariableDefined(identifier: String): Boolean {
        return isVariableDefinedInThisScope(identifier) || (parentScope?.isVariableDefined(identifier) ?: false)
    }

    fun defineVariable(identifier: String) {
        if (isVariableDefinedInThisScope(identifier)) {
            throw IllegalStateException("Runtime tried to create variable $identifier, but it already exists")
        }
        scope[identifier] = Nil
    }

    fun getValue(identifier: String): Value? {
        val value = scope[identifier]
        if (value == null) {
            if (parentScope == null) {
                return null
            }
            return parentScope.getValue(identifier)
        }
        return value
    }

    // returns true if value exists, otherwise false
    fun setValue(identifier: String, newValue: Value): Boolean {
        return if (identifier in scope) {
            scope[identifier] = newValue
            true
        } else {
            parentScope?.setValue(identifier, newValue) ?: false
        }
    }
}
