# 誰のための設計？ D.A.ノーマンのデザイン原理で作るKotlinコントロールブレイクDSL

## はじめに

「このコード、何をやっているのか分からない...」

レガシーシステムのコントロールブレイク処理を見て、こんな感想を持ったことはありませんか？ 階層的な集計処理に必要なロジックが、状態変数とネストしたif文の海に埋もれ、保守する開発者を苦しめています。

本記事では、D.A.ノーマン博士の『誰のためのデザイン？』で提唱されたデザイン原理を、KotlinのDSL設計に適用することで、この問題を解決する方法を紹介します。

## コントロールブレイク処理とは

コントロールブレイク処理は、階層的にグループ化されたデータを処理し、各レベルで集計を行う古典的なプログラミング手法です。例えば、売上データを部門別・月別に集計するような処理です。

```
営業部
  2024年4月
    2024-04-01: 120,000円
    2024-04-15:  85,000円
  月計: 205,000円
  
  2024年5月
    2024-05-02: 156,000円
  月計: 156,000円
部門計: 361,000円
```

## レガシーコードの問題点

典型的なコントロールブレイク処理の実装を見てみましょう：

```kotlin
fun generateSalesReport(salesData: List<SalesRecord>): String {
    val output = StringBuilder()
    var currentDept = ""
    var currentMonth = ""
    var deptTotal = 0.0
    var monthTotal = 0.0
    var monthQuantity = 0
    var grandTotal = 0.0
    var isFirst = true
    
    val sortedData = salesData.sortedWith(
        compareBy({ it.dept }, { it.month }, { it.date })
    )
    
    for (record in sortedData) {
        // 部門ブレイク
        if (currentDept != record.dept) {
            if (!isFirst) {
                output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
                deptTotal = 0.0
            }
            currentDept = record.dept
            output.append("【${currentDept}】\n")
            currentMonth = ""
        }
        
        // 月ブレイク
        if (currentMonth != record.month) {
            if (!isFirst && currentMonth.isNotEmpty()) {
                output.append("    月計: ${formatCurrency(monthTotal)}\n")
                monthTotal = 0.0
                monthQuantity = 0
            }
            currentMonth = record.month
            output.append("  ▼ ${currentMonth}\n")
        }
        
        // 詳細行出力
        output.append("    ${record.date}: ${formatCurrency(record.amount)}\n")
        
        // 累計更新
        monthTotal += record.amount
        monthQuantity += record.quantity
        deptTotal += record.amount
        grandTotal += record.amount
        isFirst = false
    }
    
    // 最後の集計処理（最も忘れやすい部分）
    if (!isFirst) {
        output.append("    月計: ${formatCurrency(monthTotal)}\n")
        output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
    }
    
    output.append("総計: ${formatCurrency(grandTotal)}\n")
    return output.toString()
}
```

### このコードの問題点

1. **高い認知負荷**: 7つの状態変数を追跡する必要がある
2. **エラーの温床**: リセット忘れ、最終集計の漏れが発生しやすい
3. **低い可読性**: ネストが深く、処理の流れが追いにくい
4. **拡張困難**: 新しい階層を追加する際の影響範囲が広い

## D.A.ノーマンのデザイン原理

D.A.ノーマン博士は『誰のためのデザイン？』で、使いやすいデザインの原理を提唱しました。これをソフトウェア設計に適用してみましょう。

### 1. 概念モデル（Conceptual Model）
ユーザーが持つメンタルモデルと、システムの実装モデルを一致させる

### 2. アフォーダンス（Affordance）
オブジェクトが持つ「可能な操作」を明確にする

### 3. シグニファイア（Signifier）
可能な操作を発見可能にする手がかり

### 4. 制約（Constraints）
誤った操作を防ぐ仕組み

### 5. フィードバック（Feedback）
操作の結果を即座に伝える

## 新しいDSLの設計

これらの原理を適用し、コントロールブレイク処理のためのDSLを設計しました。

### 概念モデルの適用

コントロールブレイクの階層構造を、そのままコードで表現：

```kotlin
salesData.applyControlBreak {
    // レベル1: 部門
    breakOn { it.dept }
        .onStart { dept -> println("【 $dept 】") }
        .memo({ doubleAccumulator() }) { record -> 
            this += record.amount 
        }
        .onEnd { dept, total -> 
            println("部門売上合計: ${formatCurrency(total?.value)}") 
        }
        .register()
        
    // レベル2: 月
    breakOn { it.month }
        .onStart { month -> println(" ▼ $month") }
        .memo({ MonthlyStats() }) { record ->
            update(record.amount, record.quantity)
        }
        .onEnd { month, stats ->
            stats?.let {
                println("  売上合計: ${formatCurrency(it.totalAmount)}")
                println("  販売数量: ${it.totalQuantity}個")
            }
        }
        .register()
        
    // 詳細行
    detail { record ->
        println("    ${record.date}: ${formatCurrency(record.amount)}")
    }
    
    // 総計
    grandTotal { data ->
        val total = data.sumOf { it.amount }
        println("\n総売上: ${formatCurrency(total)}")
    }
}
```

### アフォーダンスとシグニファイア

Kotlinの拡張関数により、リストに新しい能力を付与：

```kotlin
fun <T> List<T>.applyControlBreak(block: ControlBreakDSL<T>.() -> Unit) {
    val dsl = ControlBreakDSL<T>()
    dsl.block()
    val processor = dsl.build()
    processor.process(this)
}
```

IDEの補完機能により、利用可能な操作が自然に発見できます：

```kotlin
salesData.applyControlBreak {  // ← IDEが補完候補として表示
    breakOn { }     // ← 次に使えるメソッドが明確
    detail { }      // ← 階層構造が自然に理解できる
    grandTotal { }
}
```

### 制約による安全性

型システムとバリデーションによる多層防御：

```kotlin
// コンパイル時の型チェック
.memo({ doubleAccumulator() }) { record ->
    this += record.amount    // OK: Double型
    this += record.dept      // コンパイルエラー: String型
}

// 実行時のバリデーション
private fun validateSorting(data: List<T>) {
    // データが適切にソートされているかチェック
    // ソートされていない場合は詳細なエラー情報と共に例外を投げる
}
```

### フィードバック

エラー発生時の詳細な情報提供：

```kotlin
throw ControlBreakException(
    "Data is not properly sorted for control break processing",
    mapOf(
        "level" to levelIndex,
        "recordIndex" to i,
        "previousRecord" to data[lastPos],
        "currentRecord" to record,
        "key" to key,
        "message" to "Key '$key' at level $levelIndex appears in non-consecutive positions"
    )
)
```

## 実装の詳細

### DSLビルダー

```kotlin
class ControlBreakDSL<T> {
    inner class BreakBuilder<K>(private val keySelector: (T) -> K) {
        fun onStart(action: (K) -> Unit) = apply { /* ... */ }
        
        fun <M> memo(
            initializer: () -> M,
            updater: M.(T) -> Unit
        ) = apply { /* ... */ }
        
        fun <M> onEnd(action: (K, M?) -> Unit) = apply { /* ... */ }
        
        fun register() { /* ... */ }
    }
    
    fun <K> breakOn(keySelector: (T) -> K): BreakBuilder<K> {
        return BreakBuilder(keySelector)
    }
}
```

### Accumulatorパターン

累積値の更新を自然に表現するためのヘルパークラス：

```kotlin
class Accumulator<T>(var value: T) {
    operator fun plusAssign(other: T) {
        value = when (value) {
            is Double -> (value as Double + other as Double) as T
            is Int -> (value as Int + other as Int) as T
            // ...
        }
    }
}

// 使用例
.memo({ doubleAccumulator() }) { record ->
    this += record.amount  // 自然な累積操作
}
```

### 処理エンジン

階層的なブレイク判定と状態管理：

```kotlin
class ControlBreakProcessor<T>(
    private val definitions: List<BreakDefinition<T, *, *>>,
    private val detailProcessor: ((T) -> Unit)?,
    private val grandTotalProcessor: ((List<T>) -> Unit)?
) {
    fun process(data: List<T>) {
        validateSorting(data)  // 事前条件チェック
        
        val levelStates = definitions.map { LevelState() }
        val memos = definitions.map { it.memoInitializer?.invoke() }
        
        data.forEach { record ->
            // 各レベルでブレイク判定
            definitions.forEachIndexed { level, definition ->
                // キー変更検出、イベント発火、メモ更新
            }
            detailProcessor?.invoke(record)
        }
        
        // 最終集計処理
    }
}
```

## 効果

### Before（100行のレガシーコード）
- 状態変数: 7個
- ネストレベル: 3層
- バグ発生ポイント: 多数

### After（20行のDSL）
- 状態変数: 0個（DSL内部で管理）
- ネストレベル: 1層（フラットな宣言）
- バグ発生ポイント: 型システムとバリデーションで防御

### 保守性の向上

新しい集計レベルの追加が容易：

```kotlin
// 例：地域レベルを追加
breakOn { it.region }
    .onStart { region -> println("◆ $region") }
    .memo({ intAccumulator() }) { record -> this += record.count }
    .onEnd { region, total -> println("地域計: $total") }
    .register()
```

## 応用例

### 複雑な統計情報

```kotlin
data class DetailedStats(
    val amounts: MutableList<Double> = mutableListOf(),
    val quantities: MutableList<Int> = mutableListOf()
) {
    val median: Double
        get() = amounts.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            } else {
                sorted[sorted.size / 2]
            }
        }
    
    val standardDeviation: Double
        get() = // 標準偏差の計算
}

// 使用
breakOn { it.dept }
    .memo({ DetailedStats() }) { record ->
        add(record.amount, record.quantity)
    }
    .onEnd { dept, stats ->
        println("中央値: ${formatCurrency(stats?.median)}")
        println("標準偏差: ${formatCurrency(stats?.standardDeviation)}")
    }
    .register()
```

## まとめ

D.A.ノーマンのデザイン原理をソフトウェア設計に適用することで：

1. **概念モデル**: コントロールブレイクの階層構造が直接コードに反映
2. **アフォーダンス**: 拡張関数により自然な操作感を実現
3. **制約**: 型システムとバリデーションによるミス防止
4. **フィードバック**: 詳細なエラー情報による問題の早期発見

これらの原理により、複雑なコントロールブレイク処理を、宣言的で直感的なDSLとして表現することができました。

「誰のためのデザイン？」という問いに対する答えは明確です。それは、コードを書き、読み、保守する**すべての開発者のため**のデザインです。

## 参考資料

- D.A.ノーマン『誰のためのデザイン？』
- [Kotlin公式ドキュメント - Type-safe builders](https://kotlinlang.org/docs/type-safe-builders.html)
- [ソースコード（GitHub）](https://github.com/example/kotlin-control-break-dsl)

## 著者について

[著者プロフィール]

---

*この記事は、ソフトウェア開発者向けカンファレンスでの発表内容を基に作成されました。*