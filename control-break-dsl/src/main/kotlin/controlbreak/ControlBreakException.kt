package controlbreak

class ControlBreakException(
    message: String,
    val context: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : Exception(message, cause) {
    
    fun withContext(key: String, value: Any?) = ControlBreakException(
        message ?: "", 
        context + (key to value), 
        cause
    )
    
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("ControlBreakException: ").append(message)
        
        // Context表示（suggestionは別扱い）
        val contextWithoutSuggestion = context.filterKeys { it != "suggestion" }
        if (contextWithoutSuggestion.isNotEmpty()) {
            sb.append("\nContext:")
            contextWithoutSuggestion.forEach { (key, value) ->
                sb.append("\n  $key: $value")
            }
        }
        
        // 提案の表示
        context["suggestion"]?.let { suggestion ->
            sb.append("\n  \n提案: $suggestion")
        }
        
        return sb.toString()
    }
}

data class ValidationResult(
    val errors: List<String>,
    val warnings: List<Warning>,
    val suggestions: List<String>
)

data class Warning(
    val message: String,
    val suggestion: String? = null
)