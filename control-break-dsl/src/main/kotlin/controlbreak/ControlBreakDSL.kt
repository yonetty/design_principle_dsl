package controlbreak

class ControlBreakDSL<T> {
    private val breakDefinitions = mutableListOf<BreakDefinition<T, *, *>>()
    private var detailProcessor: ((T) -> Unit)? = null
    private var grandTotalProcessor: ((List<T>) -> Unit)? = null
    private var hasDetailProcessor = false
    private val usedKeySelectors = mutableSetOf<Int>()
    
    inner class BreakDefinition<T, K, M>(
        val keySelector: (T) -> K,
        var onStart: ((K) -> Unit)? = null,
        var memoInitializer: (() -> M)? = null,
        var memoUpdater: ((M, T) -> Unit)? = null,
        var onEnd: ((K, M?) -> Unit)? = null
    )
    
    inner class BreakBuilder<K>(private val keySelector: (T) -> K) {
        private val definition = BreakDefinition<T, K, Any?>(keySelector)
        private var memoSet = false
        private var onStartSet = false
        private var onEndSet = false
        
        fun onStart(action: (K) -> Unit) = apply {
            require(!onStartSet) { "onStart() can only be called once per level" }
            onStartSet = true
            definition.onStart = action
        }
        
        fun <M> memo(
            initializer: () -> M,
            updater: M.(T) -> Unit
        ) = apply {
            require(!memoSet) { "memo() can only be called once per level" }
            memoSet = true
            @Suppress("UNCHECKED_CAST")
            definition.memoInitializer = initializer as () -> Any
            definition.memoUpdater = { memo, item ->
                @Suppress("UNCHECKED_CAST")
                (memo as M).updater(item)
            }
        }
        
        fun <M> onEnd(action: (K, M?) -> Unit) = apply {
            require(!onEndSet) { "onEnd() can only be called once per level" }
            onEndSet = true
            @Suppress("UNCHECKED_CAST")
            definition.onEnd = action as (K, Any?) -> Unit
        }
        
        fun register() {
            @Suppress("UNCHECKED_CAST")
            breakDefinitions.add(definition as BreakDefinition<T, *, *>)
        }
    }
    
    fun <K> breakOn(keySelector: (T) -> K): BreakBuilder<K> {
        // 重複チェックは実際の使用では不要な場合が多い
        // val selectorHash = System.identityHashCode(keySelector)
        // require(selectorHash !in usedKeySelectors) {
        //     "Duplicate key selector"
        // }
        // usedKeySelectors.add(selectorHash)
        
        // キーセレクタの検証は実行時に行う
        
        return BreakBuilder(keySelector)
    }
    
    fun detail(processor: (T) -> Unit) {
        require(!hasDetailProcessor) {
            "detail() can only be called once"
        }
        hasDetailProcessor = true
        detailProcessor = processor
    }
    
    fun grandTotal(processor: (List<T>) -> Unit) {
        grandTotalProcessor = processor
    }
    
    internal fun build(): ControlBreakProcessor<T> {
        val validationResults = validateConfiguration()
        
        if (validationResults.errors.isNotEmpty()) {
            throw ControlBreakException(
                "Configuration validation failed",
                mapOf(
                    "errors" to validationResults.errors,
                    "suggestions" to validationResults.suggestions
                )
            )
        }
        
        // 警告はWarningオブジェクトに含めて、呼び出し側で処理する
        
        return ControlBreakProcessor(
            breakDefinitions,
            detailProcessor,
            grandTotalProcessor
        )
    }
    
    private fun validateConfiguration(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<Warning>()
        val suggestions = mutableListOf<String>()
        
        if (breakDefinitions.isEmpty()) {
            errors.add("At least one break level must be defined")
            suggestions.add("Add breakOn { /* key selector */ } to your DSL")
        }
        
        if (breakDefinitions.size > 3) {
            warnings.add(Warning(
                "Deep nesting detected (${breakDefinitions.size} levels)",
                "Consider restructuring data or combining levels"
            ))
        }
        
        breakDefinitions.forEach { definition ->
            if (definition.onEnd != null && definition.memoInitializer == null) {
                warnings.add(Warning(
                    "onEnd defined without memo initialization",
                    "Consider adding memo() to accumulate data for onEnd"
                ))
            }
        }
        
        return ValidationResult(errors, warnings, suggestions)
    }
    
    private fun createDummyRecord(): T? {
        return null
    }
}