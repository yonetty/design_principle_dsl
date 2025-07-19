# Kotlin Control Break DSL

D.A.ノーマンのデザイン原理を適用したコントロールブレイク処理DSL

## 概要

このプロジェクトは、レガシーな手続き的コントロールブレイク処理を、宣言的で直感的なDSLに変換するKotlinライブラリです。

### 特徴

- **概念モデル**: コントロールブレイクのメンタルモデルとコード構造の自然な対応づけ
- **アフォーダンス/シグニファイア**: 拡張関数による新しい能力の付与と発見
- **制約**: Kotlinの型システムによるミス防止
- **フィードバック**: 早期エラー検出による明確な問題通知

## 使用例

### 基本的な使用方法

```kotlin
val salesData = listOf(
    SalesRecord("営業部", "2024年4月", "2024-04-01", 120000.0, 10),
    SalesRecord("営業部", "2024年4月", "2024-04-15", 85000.0, 7),
    SalesRecord("開発部", "2024年4月", "2024-04-10", 95000.0, 5)
)

salesData.applyControlBreak {
    // レベル1: 部門別集計
    breakOn { it.dept }
        .onStart { dept ->
            println("【 $dept 】")
        }
        .memo({ doubleAccumulator() }) { record ->
            this += record.amount
        }
        .onEnd { dept, total ->
            println("部門売上合計: ${formatCurrency(total)}")
        }
        .register()
        
    // 詳細行の処理
    detail { record ->
        println("  ${record.date}: ${formatCurrency(record.amount)}")
    }
    
    // 総計処理
    grandTotal { data ->
        val total = data.sumOf { it.amount }
        println("総売上: ${formatCurrency(total)}")
    }
}
```

### 複数レベルのブレイク

```kotlin
salesData.applyControlBreak {
    // レベル1: 部門
    breakOn { it.dept }
        .onStart { dept -> println("\n【 $dept 】") }
        .memo({ 0.0 }) { record -> this += record.amount }
        .onEnd { dept, total -> println("部門計: $total") }
        .register()
        
    // レベル2: 月
    breakOn { it.month }
        .onStart { month -> println(" ▼ $month") }
        .memo({ MonthlyStats() }) { record ->
            update(record.amount, record.quantity)
        }
        .onEnd { month, stats ->
            stats?.let {
                println("  月計: ${it.totalAmount}")
                println("  平均: ${it.averageAmount}")
            }
        }
        .register()
}
```

## ビルドと実行

### 必要環境

- Kotlin 1.9以上
- JDK 21以上

### ビルド

```bash
cd control-break-dsl
./gradlew build
```

### テスト実行

```bash
./gradlew test
```

### サンプル実行

```bash
./gradlew run -PmainClass=examples.SalesReportExampleKt
```

## プロジェクト構造

```
control-break-dsl/
├── src/
│   ├── main/kotlin/
│   │   ├── controlbreak/
│   │   │   ├── ControlBreakDSL.kt      # DSL本体
│   │   │   ├── ControlBreakProcessor.kt # 処理エンジン
│   │   │   ├── ControlBreakException.kt # 例外クラス
│   │   │   └── Extensions.kt            # 拡張関数
│   │   └── examples/
│   │       ├── SalesReportExample.kt    # 使用例
│   │       └── TestDataGenerator.kt     # テストデータ生成
│   └── test/kotlin/
│       └── controlbreak/
│           ├── ControlBreakDSLTest.kt
│           └── ControlBreakProcessorTest.kt
└── build.gradle.kts
```

## デザイン原理の適用

### 1. 概念モデル

コントロールブレイクの階層構造を直接コードで表現：

```kotlin
breakOn { it.dept }    // レベル1
breakOn { it.month }   // レベル2
detail { }             // 詳細行
grandTotal { }         // 総計
```

### 2. アフォーダンス

拡張関数により、通常のListに新しい能力を付与：

```kotlin
val list: List<SalesRecord> = getData()
list.applyControlBreak { /* DSL */ }  // 新しい能力の発見
```

### 3. 制約

型システムによる安全性：

```kotlin
.memo({ doubleAccumulator() }) { record ->  // Accumulator<Double>型と推論
    this += record.amount    // OK: 累積値に加算
    this += record.dept      // コンパイルエラー: 型の不一致
}
```

### 4. フィードバック

詳細なエラー情報：

```kotlin
ControlBreakException: Configuration validation failed
Context:
  errors: [At least one break level must be defined]
  suggestions: [Add breakOn { /* key selector */ } to your DSL]
```

## ライセンス

このプロジェクトはデモンストレーション用です。