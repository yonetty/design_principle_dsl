package controlbreak

import kotlin.math.min

class ControlBreakProcessor<T>(
    private val definitions: List<ControlBreakDSL<T>.BreakDefinition<T, *, *>>,
    private val detailProcessor: ((T) -> Unit)?,
    private val grandTotalProcessor: ((List<T>) -> Unit)?
) {
    init {
        require(definitions.isNotEmpty()) { 
            "At least one break level must be defined" 
        }
        validateKeyHierarchy()
    }
    
    private fun validateKeyHierarchy() {
        // 複数レベルが定義されている場合は、後でvalidateSortingでチェックする
    }
    
    fun process(data: List<T>) {
        validateInput(data)
        if (data.isEmpty()) {
            grandTotalProcessor?.invoke(emptyList())
            return
        }
        
        validateSorting(data)
        
        try {
            if (data.size > 10000) {
                processWithProgress(data)
            } else {
                processInternal(data)
            }
        } catch (e: ControlBreakException) {
            reportDetailedError(e)
            throw e
        } catch (e: Exception) {
            throw ControlBreakException(
                "コントロールブレイク処理中にエラーが発生しました",
                mapOf("originalError" to e.message),
                e
            )
        }
    }
    
    private fun validateInput(data: List<T>) {
        if (data.isEmpty()) return
        
        val firstRecord = data.first()
        definitions.forEachIndexed { index, definition ->
            try {
                val key = definition.keySelector(firstRecord)
                require(key != null) {
                    "Level $index key selector returned null for record: $firstRecord"
                }
            } catch (e: Exception) {
                throw ControlBreakException(
                    "Level $index key selector failed",
                    mapOf(
                        "record" to firstRecord,
                        "error" to e.message
                    ),
                    e
                )
            }
        }
    }
    
    private fun validateSorting(data: List<T>) {
        if (definitions.isEmpty() || data.size < 2) return
        
        // 各レベルごとに、(親キー, 自キー) -> 最後の出現位置 を記録
        val lastPositions = mutableListOf<MutableMap<List<Any?>, Int>>()
        repeat(definitions.size) { lastPositions.add(mutableMapOf()) }
        
        for (i in 0 until data.size) {
            val record = data[i]
            
            // 各レベルのキーを取得
            val keys = definitions.map { it.keySelector(record) }
            
            // 各レベルでチェック
            for (levelIndex in definitions.indices) {
                val key = keys[levelIndex]
                val parentKeys = keys.take(levelIndex)  // このレベルより上の親キー
                val fullKey = parentKeys + key  // 親キー + 自分のキー
                
                val lastPos = lastPositions[levelIndex][fullKey]
                if (lastPos != null && lastPos < i - 1) {
                    // 同じキーの組み合わせが非連続で出現
                    val levelName = when(levelIndex) {
                        0 -> "部門"
                        1 -> "月"
                        else -> "レベル$levelIndex"
                    }
                    
                    // 前のキーを取得（lastPos+1からi-1までの間にあったキー）
                    val previousKey = if (i > 0) {
                        definitions[levelIndex].keySelector(data[i - 1])
                    } else null
                    
                    throw ControlBreakException(
                        "データが正しくソートされていません",
                        mapOf(
                            "level" to "$levelIndex ($levelName)",
                            "recordIndex" to i,
                            "previousKey" to previousKey,
                            "currentKey" to key,
                            "record" to record,
                            "suggestion" to "データを事前にソートしてください\n  someData.sortedBy { it.field }"
                        )
                    )
                }
                
                // 現在の位置を記録
                lastPositions[levelIndex][fullKey] = i
            }
        }
    }
    
    private fun processWithProgress(data: List<T>) {
        var processedRecords = 0
        
        val levelStates = definitions.map { LevelState() }
        val memos = definitions.map { it.memoInitializer?.invoke() }.toMutableList()
        
        data.forEachIndexed { recordIndex, record ->
            try {
                processRecord(record, recordIndex, levelStates, memos)
                
                processedRecords++
                // プログレス表示は削除（必要に応じて別途コールバックなどで実装）
            } catch (e: Exception) {
                throw ControlBreakException(
                    "レコード #$recordIndex の処理中にエラーが発生しました",
                    mapOf(
                        "recordIndex" to recordIndex,
                        "record" to record
                    ),
                    e
                )
            }
        }
        
        for (i in definitions.size - 1 downTo 0) {
            if (levelStates[i].previousKey != null) {
                @Suppress("UNCHECKED_CAST")
                (definitions[i].onEnd as ((Any, Any?) -> Unit)?)?.invoke(
                    levelStates[i].previousKey!!,
                    memos[i]
                )
            }
        }
        
        grandTotalProcessor?.invoke(data)
    }
    
    private fun processInternal(data: List<T>) {
        val levelStates = definitions.map { LevelState() }
        val memos = definitions.map { it.memoInitializer?.invoke() }.toMutableList()
        
        data.forEachIndexed { recordIndex, record ->
            try {
                processRecord(record, recordIndex, levelStates, memos)
            } catch (e: Exception) {
                throw ControlBreakException(
                    "レコード #$recordIndex の処理中にエラーが発生しました",
                    mapOf(
                        "recordIndex" to recordIndex,
                        "record" to record
                    ),
                    e
                )
            }
        }
        
        for (i in definitions.size - 1 downTo 0) {
            if (levelStates[i].previousKey != null) {
                @Suppress("UNCHECKED_CAST")
                (definitions[i].onEnd as ((Any, Any?) -> Unit)?)?.invoke(
                    levelStates[i].previousKey!!,
                    memos[i]
                )
            }
        }
        
        grandTotalProcessor?.invoke(data)
    }
    
    private fun processRecord(
        record: T, 
        @Suppress("UNUSED_PARAMETER") recordIndex: Int,
        levelStates: List<LevelState>,
        memos: MutableList<Any?>
    ) {
        definitions.forEachIndexed { levelIndex, definition ->
            val currentKey = definition.keySelector(record)
            val previousKey = levelStates[levelIndex].previousKey
            
            if (previousKey != null && previousKey != currentKey) {
                for (i in definitions.size - 1 downTo levelIndex) {
                    @Suppress("UNCHECKED_CAST")
                    (definitions[i].onEnd as ((Any, Any?) -> Unit)?)?.invoke(
                        levelStates[i].previousKey!!,
                        memos[i]
                    )
                    memos[i] = definitions[i].memoInitializer?.invoke()
                }
                
                for (i in levelIndex until definitions.size) {
                    levelStates[i].previousKey = definitions[i].keySelector(record)
                    @Suppress("UNCHECKED_CAST")
                    (definitions[i].onStart as ((Any) -> Unit)?)?.invoke(levelStates[i].previousKey!!)
                }
            } else if (previousKey == null) {
                levelStates[levelIndex].previousKey = currentKey
                currentKey?.let { key ->
                    @Suppress("UNCHECKED_CAST")
                    (definition.onStart as ((Any) -> Unit)?)?.invoke(key)
                }
            }
            
            memos[levelIndex]?.let { memo ->
                @Suppress("UNCHECKED_CAST")
                (definition.memoUpdater as ((Any, T) -> Unit)?)?.invoke(memo, record)
            }
        }
        
        detailProcessor?.invoke(record)
    }
    
    private fun reportDetailedError(@Suppress("UNUSED_PARAMETER") error: ControlBreakException) {
        // エラー報告は例外の内容で行う（printlnは使用しない）
    }
    
    private data class LevelState(var previousKey: Any? = null)
}