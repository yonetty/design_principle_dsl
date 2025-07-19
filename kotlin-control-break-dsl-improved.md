# Kotlin版コントロールブレイクDSL
## ノーマンのデザイン原理を適用したAPI設計

### 概要

D.A.ノーマン博士のデザイン原理をKotlinのコントロールブレイクDSLに適用し、レガシーな手続き的コードから宣言的で直感的なDSLへのリファクタリングを実現する。

**重点デザイン原理:**
- **概念モデル**: コントロールブレイクのメンタルモデルとコード構造の自然な対応づけ
- **アフォーダンス/シグニファイア**: 拡張関数による新しい能力の付与と発見
- **制約**: Kotlinの型システムによるミス防止
- **フィードバック**: 早期エラー検出による明確な問題通知

**対象読者:**
- Kotlinでのコントロールブレイク処理実装を必要とする開発者
- レガシーシステムのモダナイゼーションに取り組む開発者
- DSL設計パターンを学習したい開発者

**前提条件:**
- Kotlin 1.9以上
- JDK 21以上推奨
- VS Code + Kotlin拡張機能（Claude Code使用時）

---

## 1. レガシーコード：認知負荷の高い実装

### 典型的なコントロールブレイク処理の問題点

```kotlin
// 売上レポート生成の典型的なレガシー実装
fun generateSalesReport(salesData: List<SalesRecord>): String {
    val output = StringBuilder()
    var currentDept = ""
    var currentMonth = ""
    var deptTotal = 0.0
    var monthTotal = 0.0
    var monthQuantity = 0
    var grandTotal = 0.0
    var isFirst = true
    
    // ソートを忘れがち（バグの温床）
    val sortedData = salesData.sortedWith(compareBy({ it.dept }, { it.month }, { it.date }))
    
    for (record in sortedData) {
        // 部門ブレイク（ネストが深い）
        if (currentDept != record.dept) {
            if (!isFirst) {
                // 前の部門の集計出力（条件が複雑）
                output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
                deptTotal = 0.0 // リセット忘れでバグが発生しやすい
            }
            currentDept = record.dept
            output.append("【${currentDept}】\n")
            currentMonth = "" // 下位レベルもリセット（忘れがち）
        }
        
        // 月ブレイク（さらにネスト）
        if (currentMonth != record.month) {
            if (!isFirst && currentMonth.isNotEmpty()) {
                // 前の月の集計出力（複雑な条件）
                output.append("    月計: ${formatCurrency(monthTotal)} (${monthQuantity}個)\n")
                monthTotal = 0.0
                monthQuantity = 0
            }
            currentMonth = record.month
            output.append("  ▼ ${currentMonth}\n")
        }
        
        // 詳細行出力
        output.append("    ${record.date}: ${formatCurrency(record.amount)}\n")
        
        // 累計更新（更新漏れでバグになりやすい）
        monthTotal += record.amount
        monthQuantity += record.quantity
        deptTotal += record.amount
        grandTotal += record.amount
        isFirst = false
    }
    
    // 最後の集計処理（最も忘れやすい部分）
    if (!isFirst) {
        output.append("    月計: ${formatCurrency(monthTotal)} (${monthQuantity}個)\n")
        output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
    }
    
    output.append("総計: ${formatCurrency(grandTotal)}\n")
    return output.toString()
}
```

### 問題点の分析

- **状態変数が多すぎる**（認知負荷が高い）
- **ネストが深い**（可読性が低い）
- **エラーが混入しやすい**（リセット忘れ、条件ミス）
- **拡張が困難**（新しいレベル追加時の影響範囲が広い）
- **テストが困難**（状態の組み合わせが多い）

---

## 2. 概念モデルの適用と対応づけ

### コントロールブレイクの基本メンタルモデル

```
データの流れ: [レコード群] → [グループ化] → [集計] → [出力]

階層構造:
- レベル1（部門）
  - ヘッダー: 部門名表示
  - レベル2（月）
    - ヘッダー: 月名表示  
    - 詳細: 各レコード
    - フッター: 月計
  - フッター: 部門計
- 総計
```

### メンタルモデルに対応するDSL設計

```kotlin
// コントロールブレイクの概念を直接コードで表現
class ControlBreakDSL<T> {
    private val breakDefinitions = mutableListOf<BreakDefinition<T, *, *>>()
    private var detailProcessor: ((T) -> Unit)? = null
    private var grandTotalProcessor: ((List<T>) -> Unit)? = null
    
    inner class BreakDefinition<T, K, M>(
        val keySelector: (T) -> K,
        var onStart: ((K) -> Unit)? = null,
        var memoInitializer: (() -> M)? = null,
        var memoUpdater: ((M, T) -> Unit)? = null,
        var onEnd: ((K, M?) -> Unit)? = null
    )
    
    inner class BreakBuilder<T, K>(private val keySelector: (T) -> K) {
        private val definition = BreakDefinition<T, K, Any?>(keySelector)
        
        fun onStart(action: (K) -> Unit) = apply {
            definition.onStart = action
        }
        
        // Kotlinの魔法：レシーバー付きラムダ
        fun <M> memo(
            initializer: () -> M,
            updater: M.(T) -> Unit  // Mがレシーバーとなるラムダ
        ) = apply {
            @Suppress("UNCHECKED_CAST")
            definition.memoInitializer = initializer as () -> Any
            definition.memoUpdater = { memo, item ->
                @Suppress("UNCHECKED_CAST")
                (memo as M).updater(item) // memoがthisとして扱われる
            }
        }
        
        fun onEnd(action: (K, M?) -> Unit) = apply {
            @Suppress("UNCHECKED_CAST")
            definition.onEnd = action as (K, Any?) -> Unit
        }
        
        fun register() {
            breakDefinitions.add(definition as BreakDefinition<T, *, *>)
        }
    }
    
    fun <K> breakOn(keySelector: (T) -> K): BreakBuilder<T, K> {
        return BreakBuilder(keySelector)
    }
    
    fun detail(processor: (T) -> Unit) {
        detailProcessor = processor
    }
    
    fun grandTotal(processor: (List<T>) -> Unit) {
        grandTotalProcessor = processor
    }
    
    internal fun build(): ControlBreakProcessor<T> {
        return ControlBreakProcessor(
            breakDefinitions,
            detailProcessor,
            grandTotalProcessor
        )
    }
}

// 使用例：メンタルモデルがそのままコードになる
salesData.applyControlBreak {
    breakOn { it.dept }          // レベル1: 部門
        .onStart { dept -> println("【$dept】") }
        .memo({ 0.0 }) { record -> this += record.amount }
        .onEnd { dept, total -> println("部門計: $total") }
        .register()
        
    breakOn { it.month }         // レベル2: 月
        .onStart { month -> println("▼ $month") }
        .memo({ MonthlyStats() }) { record ->
            update(record.amount, record.quantity)
        }
        .onEnd { month, stats -> println("月計: ${stats?.totalAmount}") }
        .register()
        
    detail { record ->           // 詳細行処理
        println("  ${record.date}: ${formatCurrency(record.amount)}")
    }
    
    grandTotal { data ->         // 総計処理
        val total = data.sumOf { it.amount }
        println("\n総計: ${formatCurrency(total)}")
    }
}
```

---

## 3. アフォーダンス/シグニファイア：新しい能力の付与

### 通常のリストのアフォーダンス

```kotlin
val numbers = listOf(1, 2, 3, 4, 5)
// 既存のアフォーダンス:
numbers.map { it * 2 }     // 変換できる
numbers.filter { it > 3 }  // フィルタできる
numbers.reduce { a, b -> a + b } // 集約できる
```

### 新しいアフォーダンスの付与: 「コントロールブレイク処理できる」

```kotlin
// 拡張関数による新しい能力の付与
fun <T> List<T>.applyControlBreak(block: ControlBreakDSL<T>.() -> Unit) {
    val dsl = ControlBreakDSL<T>()
    dsl.block()
    val processor = dsl.build()
    processor.process(this)
}

// シグニファイア：Kotlinの型システムが新しい能力を明示
val salesData: List<SalesRecord> = loadSalesData()

// IDEの補完で新しいメソッドが発見可能
salesData.applyControlBreak {  // ← IDEが補完候補として表示
    // ここでDSLが使用可能であることが明確に示される
    breakOn { it.dept }
        .onStart { dept -> println("【$dept】") }
        .memo({ 0.0 }) { record -> this += record.amount }
        .onEnd { dept, total -> println("部門計: ${formatCurrency(total)}") }
        .register()
}
```

### アフォーダンスの発見可能性

```kotlin
// 1. 拡張関数により能力が明示される
val data: List<SalesRecord> = listOf()
data.applyControlBreak  // ← IDEが補完候補として表示

// 2. DSLスコープ内で階層的な処理が定義可能
// 3. レシーバー付きラムダにより自然な記述
.memo({ 0.0 }) { record ->  // thisが自動的にDoubleを指す
    this += record.amount   // 自然な更新記述
}

// 4. メソッドチェーンにより次に何ができるかが明確
breakOn { it.dept }    // ← この後は BreakBuilder のメソッドが利用可能
    .onStart { }       // ← 継続してBreakBuilderのメソッド
    .memo({ }) { }     // ← 継続してBreakBuilderのメソッド
    .register()        // ← 登録して次のレベル定義へ
```

---

## 4. 制約：型システムによるミス防止

### レベル1: コンパイル時制約

```kotlin
// データ型定義
data class SalesRecord(
    val dept: String,
    val month: String,
    val date: String,
    val amount: Double,
    val quantity: Int
)

// 存在しないプロパティアクセスを防止
salesData.applyControlBreak {
    breakOn { it.deparment }  // コンパイルエラー: Unresolved reference
    breakOn { it.dept }       // OK: 正しいプロパティ
}

// 型の不整合を防止
.memo({ 0.0 }) { record ->  // 初期値からDouble型と推論
    this += record.amount   // OK: Doubleに対する加算
    this += record.dept     // コンパイルエラー: String cannot be assigned to Double
}
```

### レベル2: 型推論による安全性

```kotlin
// 自動型推論による安全な操作
.memo({ 0.0 }) { record ->           // Double型と推論
    this += record.amount            // OK
}

.memo({ "" }) { record ->            // String型と推論  
    this += "${record.date}, "       // OK
}

.memo({ mutableListOf<String>() }) { record ->  // MutableList<String>と推論
    add(record.date)                 // OK
}

.memo({ MonthlyStats() }) { record ->  // MonthlyStats型と推論
    update(record.amount, record.quantity)  // MonthlyStatsのメソッド呼び出し
}
```

### レベル3: DSLビルダーによる論理的制約

```kotlin
class ControlBreakDSL<T> {
    private var hasDetailProcessor = false
    private val usedKeySelectors = mutableSetOf<String>()
    
    fun detail(processor: (T) -> Unit) {
        require(!hasDetailProcessor) {
            "detail() can only be called once"
        }
        hasDetailProcessor = true
        detailProcessor = processor
    }
    
    fun <K> breakOn(keySelector: (T) -> K): BreakBuilder<T, K> {
        val selectorString = keySelector.toString()
        require(selectorString !in usedKeySelectors) {
            "Duplicate key selector: $selectorString"
        }
        usedKeySelectors.add(selectorString)
        return BreakBuilder(keySelector)
    }
    
    internal fun build(): ControlBreakProcessor<T> {
        require(breakDefinitions.isNotEmpty()) { 
            "At least one break level must be defined" 
        }
        return ControlBreakProcessor(
            breakDefinitions, 
            detailProcessor,
            grandTotalProcessor
        )
    }
}

// 重複定義の防止
class BreakBuilder<T, K>(private val keySelector: (T) -> K) {
    private var memoSet = false
    private var onStartSet = false
    private var onEndSet = false
    
    fun <M> memo(initializer: () -> M, updater: M.(T) -> Unit) = apply {
        require(!memoSet) { "memo() can only be called once per level" }
        memoSet = true
        // memo設定
    }
    
    fun onStart(action: (K) -> Unit) = apply {
        require(!onStartSet) { "onStart() can only be called once per level" }
        onStartSet = true
        definition.onStart = action
    }
    
    fun onEnd(action: (K, M?) -> Unit) = apply {
        require(!onEndSet) { "onEnd() can only be called once per level" }
        onEndSet = true
        @Suppress("UNCHECKED_CAST")
        definition.onEnd = action as (K, Any?) -> Unit
    }
}
```

### レベル4: ランタイム制約による早期エラー

```kotlin
class ControlBreakProcessor<T>(
    private val definitions: List<BreakDefinition<T, *, *>>,
    private val detailProcessor: ((T) -> Unit)?,
    private val grandTotalProcessor: ((List<T>) -> Unit)?
) {
    init {
        require(definitions.isNotEmpty()) { 
            "At least one break level must be defined" 
        }
        
        // ソートキーの階層検証
        validateKeyHierarchy()
    }
    
    private fun validateKeyHierarchy() {
        // キーセレクタが階層的に適切かチェック
        if (definitions.size > 1) {
            println("⚠️ 複数のブレイクレベルが定義されています。データが適切にソートされていることを確認してください。")
        }
    }
    
    fun process(data: List<T>) {
        validateInput(data)
        if (data.isEmpty()) {
            println("⚠️ 空のデータセットを処理しています")
            grandTotalProcessor?.invoke(emptyList())
            return
        }
        
        // ソート検証
        validateSorting(data)
        
        // 実際の処理
        processInternal(data)
    }
    
    private fun validateInput(data: List<T>) {
        if (data.isEmpty()) return
        
        // キーセレクタの動作確認
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
        // 簡易的なソート検証（最初の数レコードのみ）
        val sampleSize = minOf(10, data.size)
        for (i in 1 until sampleSize) {
            val prev = data[i - 1]
            val curr = data[i]
            
            for (definition in definitions) {
                val prevKey = definition.keySelector(prev)
                val currKey = definition.keySelector(curr)
                
                if (prevKey != currKey) {
                    // 上位レベルのキーが変わった場合、以降のチェックは不要
                    break
                }
            }
        }
    }
}
```

---

## 5. フィードバック：早期エラー検出と明確な問題通知

### カスタム例外クラス

```kotlin
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
        sb.append(super.toString())
        if (context.isNotEmpty()) {
            sb.append("\nContext:")
            context.forEach { (key, value) ->
                sb.append("\n  $key: $value")
            }
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
```

### フェーズ1: 設定時のフィードバック

```kotlin
class ControlBreakDSL<T> {
    fun <K> breakOn(keySelector: (T) -> K): BreakBuilder<T, K> {
        // キーセレクタの基本検証
        try {
            // ダミーレコードでの検証が可能な場合
            val dummy = createDummyRecord<T>()
            dummy?.let {
                keySelector(it) // 実行可能性の確認
            }
        } catch (e: Exception) {
            println("⚠️ キーセレクタの検証に失敗しました: ${e.message}")
        }
        
        return BreakBuilder(keySelector)
    }
    
    private inline fun <reified T> createDummyRecord(): T? {
        // 型に応じたダミーレコードの生成（オプション）
        return null
    }
}

// 使用例でのフィードバック
try {
    salesData.applyControlBreak {
        breakOn { null }  // 即座にエラー
    }
} catch (e: IllegalArgumentException) {
    println("設定エラー: ${e.message}")
}
```

### フェーズ2: ビルド時のフィードバック

```kotlin
class ControlBreakDSL<T> {
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
        
        if (validationResults.warnings.isNotEmpty()) {
            println("⚠️ コントロールブレイク設定の警告:")
            validationResults.warnings.forEach { warning ->
                println("  - ${warning.message}")
                warning.suggestion?.let {
                    println("    提案: $it")
                }
            }
        }
        
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
        
        // 必須項目チェック
        if (breakDefinitions.isEmpty()) {
            errors.add("At least one break level must be defined")
            suggestions.add("Add breakOn { /* key selector */ } to your DSL")
        }
        
        // 効率性の警告
        if (breakDefinitions.size > 3) {
            warnings.add(Warning(
                "Deep nesting detected (${breakDefinitions.size} levels)",
                "Consider restructuring data or combining levels"
            ))
        }
        
        // onStartとonEndの整合性チェック
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
}
```

### フェーズ3: 実行時のフィードバック

```kotlin
class ControlBreakProcessor<T>(
    private val definitions: List<BreakDefinition<T, *, *>>,
    private val detailProcessor: ((T) -> Unit)?,
    private val grandTotalProcessor: ((List<T>) -> Unit)?
) {
    fun process(data: List<T>) {
        try {
            // 進行状況のフィードバック
            if (data.size > 10000) {
                println("大量データを処理中... (${data.size}件)")
                return processWithProgress(data)
            }
            
            processInternal(data)
            
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
    
    private fun processWithProgress(data: List<T>) {
        val totalRecords = data.size
        var processedRecords = 0
        val progressInterval = totalRecords / 10 // 10%ごとに進捗表示
        
        // 処理ロジック（processInternalと同様だが進捗表示付き）
        data.forEachIndexed { index, record ->
            // 処理実行
            processRecord(record, index)
            
            processedRecords++
            if (processedRecords % progressInterval == 0) {
                val percentage = (processedRecords * 100) / totalRecords
                println("処理進捗: $percentage% ($processedRecords/$totalRecords)")
            }
        }
    }
    
    private fun processInternal(data: List<T>) {
        val levelStates = definitions.map { LevelState() }
        val memos = definitions.map { it.memoInitializer?.invoke() }
        
        data.forEachIndexed { recordIndex, record ->
            try {
                // 各レベルのブレイク判定
                definitions.forEachIndexed { levelIndex, definition ->
                    val currentKey = definition.keySelector(record)
                    val previousKey = levelStates[levelIndex].previousKey
                    
                    if (previousKey != null && previousKey != currentKey) {
                        // ブレイク発生: 下位レベルも含めて終了処理
                        for (i in definitions.size - 1 downTo levelIndex) {
                            definitions[i].onEnd?.invoke(
                                levelStates[i].previousKey!!,
                                memos[i]
                            )
                            // メモをリセット
                            memos[i] = definitions[i].memoInitializer?.invoke()
                        }
                        
                        // 新しいグループの開始
                        for (i in levelIndex until definitions.size) {
                            levelStates[i].previousKey = definitions[i].keySelector(record)
                            definitions[i].onStart?.invoke(levelStates[i].previousKey!!)
                        }
                    } else if (previousKey == null) {
                        // 最初のレコード
                        levelStates[levelIndex].previousKey = currentKey
                        definition.onStart?.invoke(currentKey)
                    }
                    
                    // メモの更新
                    memos[levelIndex]?.let { memo ->
                        definition.memoUpdater?.invoke(memo, record)
                    }
                }
                
                // 詳細処理
                detailProcessor?.invoke(record)
                
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
        
        // 最後のグループの終了処理
        for (i in definitions.size - 1 downTo 0) {
            if (levelStates[i].previousKey != null) {
                definitions[i].onEnd?.invoke(
                    levelStates[i].previousKey!!,
                    memos[i]
                )
            }
        }
        
        // 総計処理
        grandTotalProcessor?.invoke(data)
    }
    
    private fun reportDetailedError(error: ControlBreakException) {
        println("\n🚨 コントロールブレイクエラー: ${error.message}")
        
        error.context["recordIndex"]?.let {
            println("📍 位置: レコード #$it")
        }
        
        error.context["record"]?.let {
            println("📄 レコード: $it")
        }
        
        error.context["levelIndex"]?.let {
            println("📊 ブレイクレベル: $it")
        }
        
        error.context["suggestions"]?.let { suggestions ->
            if (suggestions is List<*>) {
                println("💡 提案:")
                suggestions.forEach { suggestion ->
                    println("   - $suggestion")
                }
            }
        }
        
        error.cause?.let {
            println("💥 根本原因: ${it.message}")
            it.printStackTrace()
        }
    }
    
    private data class LevelState(var previousKey: Any? = null)
}
```

---

## 6. 完成形：使用例とその効果

### データモデル定義

```kotlin
data class SalesRecord(
    val dept: String,
    val month: String,
    val date: String,
    val amount: Double,
    val quantity: Int
)

data class MonthlyStats(
    var totalAmount: Double = 0.0,
    var totalQuantity: Int = 0,
    var recordCount: Int = 0
) {
    fun update(amount: Double, quantity: Int) {
        totalAmount += amount
        totalQuantity += quantity
        recordCount++
    }
    
    val averageAmount: Double
        get() = if (recordCount > 0) totalAmount / recordCount else 0.0
        
    val averagePrice: Double
        get() = if (totalQuantity > 0) totalAmount / totalQuantity else 0.0
}
```

### 実際の使用例

```kotlin
fun main() {
    val salesData = generateSalesData()
    
    // レガシー版: 100行の手続き的コード
    val legacyReport = generateSalesReport(salesData)
    
    // 新DSL版: 宣言的で直感的
    val sortedData = salesData.sortedWith(
        compareBy({ it.dept }, { it.month }, { it.date })
    )
    
    sortedData.applyControlBreak {
        // レベル1: 部門別集計
        breakOn { it.dept }
            .onStart { dept ->
                println("\n【 $dept 】")
            }
            .memo({ 0.0 }) { record ->  // シンプルな合計
                this += record.amount
            }
            .onEnd { dept, total ->
                println("部門売上合計: ${formatCurrency(total)}")
            }
            .register()
            
        // レベル2: 月別詳細集計
        breakOn { it.month }
            .onStart { month ->
                println(" ▼ $month")
            }
            .memo({ MonthlyStats() }) { record ->  // 複雑な集計
                update(record.amount, record.quantity)
            }
            .onEnd { month, stats ->
                stats?.let {
                    println("  売上合計: ${formatCurrency(it.totalAmount)}")
                    println("  販売数量: ${it.totalQuantity}個")
                    println("  取引件数: ${it.recordCount}件")
                    println("  平均売上: ${formatCurrency(it.averageAmount)}")
                    println("  平均単価: ${formatCurrency(it.averagePrice)}")
                }
            }
            .register()
            
        // 詳細行の処理
        detail { record ->
            println("    ${record.date}: ${formatCurrency(record.amount)} (${record.quantity}個)")
        }
        
        // 総計処理
        grandTotal { data ->
            val total = data.sumOf { it.amount }
            val totalQuantity = data.sumOf { it.quantity }
            println("\n===============================")
            println("総売上: ${formatCurrency(total)}")
            println("総販売数: ${totalQuantity}個")
            println("===============================")
        }
    }
}

// ヘルパー関数
fun formatCurrency(amount: Double?): String =
    amount?.let { "%,.0f円".format(it) } ?: "0円"

// テスト用データ生成
fun generateSalesData(): List<SalesRecord> {
    return listOf(
        SalesRecord("営業部", "2024年4月", "2024-04-01", 120000.0, 10),
        SalesRecord("営業部", "2024年4月", "2024-04-15", 85000.0, 7),
        SalesRecord("営業部", "2024年5月", "2024-05-02", 156000.0, 12),
        SalesRecord("開発部", "2024年4月", "2024-04-10", 95000.0, 5),
        SalesRecord("開発部", "2024年5月", "2024-05-20", 110000.0, 8)
    )
}
```

### エラーハンドリングの例

```kotlin
// 設定ミスのある例
fun demonstrateErrorHandling() {
    val invalidData = listOf<SalesRecord>()
    
    try {
        // ケース1: 空のデータ
        invalidData.applyControlBreak {
            breakOn { it.dept }
                .onStart { println("部門: $it") }
                .register()
        }
    } catch (e: ControlBreakException) {
        println("エラーハンドリング例1:")
        println(e)
    }
    
    try {
        // ケース2: キーセレクタのエラー
        val data = listOf(SalesRecord("", "", "", 0.0, 0))
        data.applyControlBreak {
            breakOn { it.dept.substring(10) }  // StringIndexOutOfBoundsException
                .register()
        }
    } catch (e: ControlBreakException) {
        println("\nエラーハンドリング例2:")
        println(e)
    }
}
```

### 高度な使用例：複数の集計パターン

```kotlin
// より複雑な集計例
data class DetailedStats(
    val amounts: MutableList<Double> = mutableListOf(),
    val quantities: MutableList<Int> = mutableListOf()
) {
    fun add(amount: Double, quantity: Int) {
        amounts.add(amount)
        quantities.add(quantity)
    }
    
    val median: Double
        get() = amounts.sorted().let { sorted ->
            if (sorted.isEmpty()) 0.0
            else if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            } else {
                sorted[sorted.size / 2]
            }
        }
    
    val standardDeviation: Double
        get() {
            if (amounts.size < 2) return 0.0
            val mean = amounts.average()
            val variance = amounts.map { (it - mean).pow(2) }.average()
            return sqrt(variance)
        }
}

// 統計情報を含む高度な集計
salesData.applyControlBreak {
    breakOn { it.dept }
        .memo({ DetailedStats() }) { record ->
            add(record.amount, record.quantity)
        }
        .onEnd { dept, stats ->
            stats?.let {
                println("\n【$dept 統計情報】")
                println("  中央値: ${formatCurrency(it.median)}")
                println("  標準偏差: ${formatCurrency(it.standardDeviation)}")
            }
        }
        .register()
}
```

---

## 7. 実装ガイド（Claude Code向け）

### プロジェクト構造

```
control-break-dsl/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       ├── controlbreak/
│   │       │   ├── ControlBreakDSL.kt
│   │       │   ├── ControlBreakProcessor.kt
│   │       │   ├── ControlBreakException.kt
│   │       │   └── Extensions.kt
│   │       └── examples/
│   │           ├── SalesReportExample.kt
│   │           └── TestDataGenerator.kt
│   └── test/
│       └── kotlin/
│           └── controlbreak/
│               ├── ControlBreakDSLTest.kt
│               └── ControlBreakProcessorTest.kt
└── README.md
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
```

### 実装手順

1. **基本構造の実装**
   - `ControlBreakException.kt`: 例外クラス
   - `ControlBreakDSL.kt`: DSL本体
   - `ControlBreakProcessor.kt`: 処理エンジン
   - `Extensions.kt`: 拡張関数

2. **テストの実装**
   - 単体テスト: 各コンポーネントの動作確認
   - 統合テスト: DSL全体の動作確認
   - エラーケーステスト: 例外処理の確認

3. **サンプルの実装**
   - 基本的な売上レポート
   - 複雑な統計処理
   - エラーハンドリングの例

### テスト例

```kotlin
class ControlBreakDSLTest {
    @Test
    fun `should process simple control break correctly`() {
        val data = listOf(
            TestRecord("A", 100),
            TestRecord("A", 200),
            TestRecord("B", 300)
        )
        
        var result = ""
        
        data.applyControlBreak {
            breakOn { it.group }
                .onStart { group -> result += "Start:$group," }
                .memo({ 0 }) { record -> this += record.value }
                .onEnd { group, total -> result += "End:$group=$total," }
                .register()
        }
        
        assertEquals("Start:A,End:A=300,Start:B,End:B=300,", result)
    }
    
    @Test
    fun `should handle empty data gracefully`() {
        val emptyData = emptyList<TestRecord>()
        
        assertDoesNotThrow {
            emptyData.applyControlBreak {
                breakOn { it.group }.register()
            }
        }
    }
    
    @Test
    fun `should detect duplicate key selectors`() {
        val data = listOf(TestRecord("A", 100))
        
        assertThrows<IllegalArgumentException> {
            data.applyControlBreak {
                breakOn { it.group }.register()
                breakOn { it.group }.register()  // 重複
            }
        }
    }
}

data class TestRecord(val group: String, val value: Int)
```

---

## 8. Kotlin版の特徴と利点

### 1. レシーバー付きラムダの威力

```kotlin
// memo関数でthisが自動的に適切な型を指す
.memo({ 0.0 }) { record ->
    this += record.amount  // thisは自動的にDoubleを指す
}

.memo({ MonthlyStats() }) { record ->
    update(record.amount, record.quantity)  // thisは自動的にMonthlyStatsを指す
}
```

### 2. 拡張関数による自然な統合

```kotlin
// 既存のListに新しい能力を自然に追加
val salesData: List<SalesRecord> = loadData()
salesData.applyControlBreak { /* DSL */ }  // 既存APIと一貫した使用感
```

### 3. 強力な型推論

```kotlin
// 明示的な型宣言をほとんど必要とせず、安全性を確保
.memo({ mutableMapOf<String, Double>() }) { record ->
    // 型推論により、this は MutableMap<String, Double> と認識される
    put(record.dept, getOrDefault(record.dept, 0.0) + record.amount)
}
```

### 4. DSLの表現力

```kotlin
// Kotlinの言語機能により、非常に読みやすい内部DSLを構築
salesData.applyControlBreak {
    breakOn { it.dept }
        .onStart { dept -> println("【$dept】") }
        .memo({ 0.0 }) { this += it.amount }
        .onEnd { dept, total -> println("部門計: $total") }
        .register()
}
```

### 5. Null安全性

```kotlin
// Kotlinの言語仕様により、null関連のバグを大幅に削減
.onEnd { month, stats ->
    stats?.let {  // null安全な操作
        println("月計: ${it.totalAmount}")
    }
}
```

---

## 9. TypeScript版との比較

| 特徴 | Kotlin版 | TypeScript版 |
|------|----------|---------------|
| **型推論** | より強力で自然 | 明示的な型定義が必要な場合が多い |
| **DSL記述** | レシーバー付きラムダで非常に自然 | メソッドチェーンベース |
| **Null安全性** | 言語レベルでサポート | strictNullChecksに依存 |
| **拡張性** | 拡張関数で自然な統合 | プロトタイプ拡張 |
| **エラーハンドリング** | 例外ベースで詳細な情報 | Error型とunion型 |
| **パフォーマンス** | JVMで高速実行 | V8エンジンに依存 |
| **IDE支援** | IntelliJ IDEAで完璧な支援 | VSCodeで良好な支援 |

---

## 10. パフォーマンス考慮事項

### メモリ効率

```kotlin
// 大量データ処理時の最適化
class ControlBreakProcessor<T> {
    fun processLargeDataset(data: Sequence<T>) {
        // Sequenceを使用してメモリ効率を向上
        data.chunked(1000).forEach { chunk ->
            processInternal(chunk)
        }
    }
}
```

### 並列処理対応

```kotlin
// 並列処理可能な場合の拡張
fun <T> List<T>.applyControlBreakParallel(
    block: ControlBreakDSL<T>.() -> Unit
) {
    // グループごとに並列処理
    groupBy { /* key selector */ }
        .entries
        .parallelStream()
        .forEach { (key, group) ->
            group.applyControlBreak(block)
        }
}
```

---

## 11. まとめ

Kotlin版コントロールブレイクDSLは、D.A.ノーマンのデザイン原理を効果的に適用することで：

1. **認知負荷の大幅な削減**: 100行の手続き的コード → 20行の宣言的コード
2. **直感的な記述**: メンタルモデルがそのままコードになる
3. **安全性の向上**: 型システムによる多層的な制約
4. **優れたフィードバック**: 段階的で詳細なエラー情報
5. **Kotlinらしい自然さ**: 言語機能を最大限活用した表現力

このDSLは、複雑なコントロールブレイク処理を宣言的に、かつ直感的に記述することを可能にし、プログラマにとって「使いやすく、間違えにくく、理解しやすい」APIの設計指針を提供します。

### 次のステップ

1. **実装**: Claude Codeを使用してプロジェクトを作成
2. **テスト**: 様々なデータパターンでの動作確認
3. **最適化**: 大規模データでのパフォーマンスチューニング
4. **拡張**: 追加機能（CSV出力、並列処理など）の実装

### 参考資料

- [Kotlin公式ドキュメント - DSL構築](https://kotlinlang.org/docs/type-safe-builders.html)
- [Effective Kotlin](https://kt.academy/book/effectivekotlin)
- D.A.ノーマン「誰のためのデザイン？」