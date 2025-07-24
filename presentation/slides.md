---
marp: true
theme: custom
paginate: true
backgroundColor: white
title: 誰のための設計？
style: |
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
  
  section {
    font-family: 'Inter', sans-serif;
    background-color: white;
    color: #2d3748;
    padding: 60px 80px;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    overflow: visible !important;
  }
  
  h1 {
    color: #1a202c;
    font-size: 1.6em;
    font-weight: 700;
    border-bottom: 3px solid #4299e1;
    padding-bottom: 10px;
    margin-bottom: 30px;
  }
  
  .subtitle {
    font-size: 0.85em;
    font-weight: 600;
    color: #4a5568;
  }
  
  h2 {
    color: #2b6cb0;
    font-size: 1.3em;
    font-weight: 600;
    margin-bottom: 20px;
  }
  
  li {
    margin-bottom: 12px;
    line-height: 1.6;
  }
  
  ul {
    list-style: none;
    padding-left: 0;
  }
  
  ul li {
    position: relative;
    padding-left: 1.5em;
  }
  
  ul li::before {
    content: "▸";
    position: absolute;
    left: 0;
    color: #4299e1;
    font-weight: bold;
  }
  
  ol {
    counter-reset: item;
    list-style: none;
    padding-left: 0;
  }
  
  ol li {
    counter-increment: item;
    position: relative;
    padding-left: 2em;
  }
  
  ol li::before {
    content: counter(item) ".";
    position: absolute;
    left: 0;
    color: #2b6cb0;
    font-weight: bold;
    font-size: 1.1em;
  }
  
  strong {
    color: #2b6cb0;
    font-weight: 600;
  }
  
  code {
    background-color: #f7fafc;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
    color: #e53e3e;
  }
  
  blockquote {
    border-left: 4px solid #4299e1;
    padding-left: 20px;
    margin: 20px 0;
    color: #4a5568;
    font-style: italic;
  }
  
  .ruby {
    font-size: 0.4em;
    position: absolute;
    top: -1em;
    left: 50%;
    transform: translateX(-50%);
    color: #94a3b8;
    font-weight: 400;
  }
  
  img {
    display: block;
    margin: 20px auto;
    max-width: 70%;
    max-height: 400px;
    object-fit: contain;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    border-radius: 8px;
  }
  
  .placeholder {
    background-color: #e2e8f0;
    border: 2px dashed #cbd5e0;
    border-radius: 8px;
    padding: 80px 40px;
    text-align: center;
    color: #a0aec0;
    font-style: italic;
    margin: 20px auto;
    max-width: 600px;
  }
  
  .code-sample {
    background-color: #2d3748;
    color: #e2e8f0;
    padding: 20px;
    border-radius: 8px;
    margin: 20px 0;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 0.85em;
    overflow-x: auto;
  }
  
  pre {
    background-color: #1e293b;
    color: #e2e8f0;
    padding: 20px;
    border-radius: 8px;
    margin: 20px 0;
    font-size: 0.7em;
    overflow-x: auto;
    overflow-y: visible !important;
    max-height: none !important;
    height: auto !important;
    line-height: 1.4;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  }
  
  pre code {
    background-color: transparent;
    color: #e2e8f0;
    padding: 0;
    font-size: inherit;
    overflow-y: visible !important;
    max-height: none !important;
    height: auto !important;
    display: block;
  }
  
  /* Kotlin syntax highlighting */
  pre code .hljs-keyword,
  pre code .hljs-built_in {
    color: #c084fc;
    font-weight: bold;
  }
  
  pre code .hljs-string {
    color: #86efac;
  }
  
  pre code .hljs-number {
    color: #fbbf24;
  }
  
  pre code .hljs-comment {
    color: #94a3b8;
    font-style: italic;
  }
  
  pre code .hljs-function,
  pre code .hljs-title {
    color: #60a5fa;
  }
  
  pre code .hljs-params {
    color: #fda4af;
  }
  
  pre code .hljs-variable,
  pre code .hljs-attribute {
    color: #f472b6;
  }
  
  .highlight-box {
    background-color: #f0f9ff;
    border-left: 4px solid #0ea5e9;
    padding: 20px;
    margin: 20px 0;
    border-radius: 0 8px 8px 0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  }
  
  .highlight-box.warning {
    background-color: #fef3c7;
    border-left-color: #f59e0b;
  }
  
  .highlight-box.danger {
    background-color: #fee2e2;
    border-left-color: #ef4444;
  }
  
  .highlight-box.success {
    background-color: #d1fae5;
    border-left-color: #10b981;
  }
  
  .highlight-box ol,
  .highlight-box ul {
    margin: 0;
  }
  
  .highlight-box p {
    margin: 10px 0;
  }
  
  .highlight-box p:first-child {
    margin-top: 0;
  }
  
  .highlight-box p:last-child {
    margin-bottom: 0;
  }
  
  
  .two-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 30px;
    align-items: start;
  }
  
  .two-column pre {
    margin: 0;
    font-size: 0.65em;
    max-height: none !important;
    overflow-y: visible !important;
    height: auto !important;
  }
  
  section pre,
  section pre[class*="language-"],
  section div pre,
  section .highlight pre,
  section .code pre,
  pre.language-kotlin,
  pre.lang-kotlin {
    overflow-y: visible !important;
    overflow: visible !important;
    max-height: none !important;
    height: auto !important;
  }
  
  /* Marp specific overrides */
  [data-marpit-advanced-background] pre,
  .marpit pre,
  div[data-marpit-fragment] pre,
  .remark-code,
  .remark-code-line {
    overflow-y: visible !important;
    max-height: none !important;
    height: auto !important;
  }
  
  /* Force all containers to not clip content */
  section > *,
  section div {
    overflow: visible !important;
  }
  
  .output-box {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 20px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 0.65em;
    line-height: 1.5;
    color: #2d3748;
    white-space: pre;
    overflow-x: auto;
  }
  
  .image-caption {
    font-size: 0.7em;
    color: #718096;
    margin-top: 5px;
    margin-bottom: 0;
    line-height: 1.3;
  }
  
  .image-container {
    text-align: center;
    margin-bottom: 10px;
  }
  
  .book-gallery {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 30px;
  }
  
  .book-item {
    text-align: center;
  }
  
  .book-image {
    max-width: 100%;
    max-height: 300px;
  }
  
  .book-caption {
    font-size: 0.8em;
    color: #4a5568;
    margin-top: 10px;
  }
  
  .large-image {
    max-width: 91%;
    max-height: 520px;
  }
  
  .medium-image {
    max-width: 60%;
    max-height: 280px;
  }
  
  .half-image {
    max-width: 50%;
    max-height: 280px;
  }
  
  .positioned-image {
    position: absolute;
    bottom: 80px;
    right: 80px;
  }
  
  .positioned-image-small {
    max-width: 45%;
    max-height: 300px;
  }
  
  .positioned-image-medium {
    max-width: 48%;
    max-height: 300px;
  }
  
  .positioned-image-large {
    max-width: 54%;
    max-height: 360px;
  }
  
  .final-message {
    font-size: 6.0em;
    color: #60a5fa;
    font-weight: 700;
  }
  
  .ruby-text {
    position: relative;
  }
  
  .reference-image {
    max-width: 70%;
    max-height: 360px;
  }

  .haiku-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 60vh;
  }

  .haiku-text {
    font-size: 1.8em;
    font-weight: 700;
    color: #2b6cb0;
    line-height: 1.8;
    text-align: center;
    white-space: nowrap;
  }

  .haiku-emoji {
    display: inline-block;
    margin-left: 0.2em;
  }

  .repo-link {
    color: #4299e1;
    text-decoration: none;
    font-size: 0.75em;
    display: block;
    margin-top: 10px;
  }

  .repo-link:hover {
    text-decoration: underline;
  }
---
<!-- _backgroundColor: #1a202c -->
<!-- _color: #e2e8f0 -->
<!-- _paginate: false -->

<style scoped>
section {
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

h1 {
  font-size: 4em;
  border: none;
  margin-bottom: 80px;
  color: #ffffff;
  font-weight: 800;
  letter-spacing: -0.02em;
}

p {
  font-size: 1em;
  color: #94a3b8;
  margin: 5px 0;
}

.bottom-info {
  position: absolute;
  bottom: 60px;
  left: 0;
  right: 0;
  text-align: center;
}

.event-name {
  font-size: 1.2em;
  color: #60a5fa;
  margin-top: 20px;
  font-weight: 600;
}

.ruby {
  font-size: 0.4em;
  position: absolute;
  top: -1em;
  left: 50%;
  transform: translateX(-50%);
  color: #cbd5e0;
  font-weight: 400;
}
</style>

# 誰のための<span class="ruby-text">設計<span class="ruby">design</span></span>？

<div class="bottom-info">
<p>July 25, 2025</p>
<p>Takeshi Yonekubo</p>
<p class="event-name">設計ナイト2025</p>
</div>

---

# 自己紹介

- **米久保 剛** (@tyonekubo)
- 設計大好き

<div class="book-gallery">
  <div class="book-item">
    <img src="images/image_book1.jpg" class="book-image">
    <p class="book-caption">発売一周年！</p>
  </div>
  <div class="book-item">
    <img src="images/image_book4.jpg" class="book-image">
    <p class="book-caption">韓国語版も！</p>
  </div>
  <div class="book-item">
    <img src="images/image_book2.jpg" class="book-image">
    <p class="book-caption">リファクタリング特集に寄稿</p>
  </div>
</div>

---

# タイトルの元ネタ

## 『誰のためのデザイン？ 認知科学者のデザイン言論』
- D.A.ノーマン博士の著書

<img src="images/image_book3.jpg" class="large-image">

---

# HCD（Human-Centered Design）

## 人間中心設計

- 人間のニーズ、能力、行動に合わせてデザインする
- ユーザーの視点から道具を設計する

---

# 7つのデザイン原理

1. **概念モデル**
2. **発見可能性**
3. **アフォーダンス**
4. **シグニファイア**
5. **制約**
6. **対応づけ**
7. **フィードバック**

---

# 概念モデル

- 道具のメカニズムや基本動作原理に対する、ユーザーの心の中にあるモデル（**メンタルモデル**）
- ユーザーが良い概念モデルを形成できるように、道具の設計を行う

<div class="image-container">
  <img src="images/image_conceptual_model.jpg" class="medium-image">
  <p class="image-caption">出典『誰のためのデザイン?』図1・11</p>
</div>

---

# 発見可能性

## 道具の使い方を見出し、理解することが容易であること

- 道具を見ただけで「何ができるか」「どう使うか」が分かる
- 試行錯誤なしに正しい使い方を発見できる
- **統合的な原理**: 他の5つの原理が協調して実現

<div class="highlight-box">

発見可能性 = 
アフォーダンス + シグニファイア + 制約 + 対応づけ + フィードバック

</div>

---

# アフォーダンス

## 環境が提供する行為の可能性

**定義**: モノと主体（人、動物、ロボット）との関係性の中で生まれる、行為の可能性

**例: イスは...**
- 座ることを可能にする（afford sitting）
- 支えを提供する（afford support）
- 持ち上げることを可能にする（afford lifting）

<img src="images/image_affordance.png" class="positioned-image positioned-image-medium">

---

# シグニファイア

## アフォーダンスの存在を示す手がかり
- マークや音など知覚可能なもの

**例: ティーバッグのタグ**

<img src="images/image_signifier.jpg" class="half-image" style="max-height: 300px;">

---

# 制約

## 物理的、論理的、意味的、文化的な制約によって、取りうる行動を制限する

- エラーを防ぐデザイン
- ポカよけ
**例: 乾電池ホルダーのバネ（マイナス端子）**

<img src="images/image_constraint.jpg" class="positioned-image positioned-image-small">

---

# 対応づけ（マッピング）

## 要素同士の関係が明確であること

**例: ファミコンのコントローラー（十字キー）**

<img src="images/image_mapping.jpg" class="half-image">

---

# フィードバック

## ユーザーの意図に対して、システムが働いていること/働いていないことを知らせる

- 行為の結果を伝える
- ユーザーに安心感を与える

<img src="images/image_feedback.png" class="positioned-image positioned-image-large">

---

# デザイン原理の適用

## 物理的な道具から、デジタルな道具へ

- ノーマン博士のデザイン原理は**あらゆる道具**に適用可能
- ソフトウェアという道具の利用者にはプログラマも含まれる
- 使う人（プログラマ）の認知や行動を考慮した設計が重要

---

# プログラマの道具

- プログラミング言語
- IDE
- ライブラリ
- ターミナル、コマンド
- 生成AI
- **ソフトウェアそのもの**: 自分が書いたコンポーネントは、自分や他のプログラマが使う道具

---

# プログラマのための道具をデザインする

## ノーマン博士の7つのデザイン原理を適用可能

![](images/image_programming.jpg)

---

# サンプルコード: 売上レポート集計プログラム

<div class="two-column">

```kotlin
fun generateSalesReportLegacy(salesData: List<SalesRecord>): String {
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
        // 月ブレイク処理、詳細行出力、累計更新...
    }
    // 最終集計処理...
}
```

<pre class="output-box">
【営業部】
  ▼ 2024年4月
    2024-04-01: 120,000円
    2024-04-15:  85,000円
    月計: 205,000円
  ▼ 2024年5月
    2024-05-02: 156,000円
    月計: 156,000円
  部門計: 361,000円

【開発部】
  ▼ 2024年4月
    2024-04-10:  98,000円
    月計:  98,000円
  ...
</pre>

</div>

<p style="margin-top: 20px; text-align: center;">
  <a href="https://github.com/yonetty/design_principle_dsl" class="repo-link">
    📦 https://github.com/yonetty/design_principle_dsl
  </a>
</p>

---

# コントロールブレイク処理

## 特定の条件を満たす間、データに対して反復処理を行う
## 条件が変わったら、集計や出力等の処理を行う

- 例: 月ごと、部門ごとの売上集計
- 古典的な**アルゴリズムのパターン**
- キー項目の変化をトリガーに集計処理を行う

---

# 既存コードの問題点

<div class="highlight-box warning">

**1. 高い認知負荷**: 多くの状態変数を追跡する必要がある

**2. バグの温床**: リセット忘れ、最終集計の漏れが発生しやすい

**3. 低い可読性**: ネストが深く、処理の流れが追いにくい

**4. 拡張困難**: 新しい階層を追加する際の影響範囲が広い

</div>

---

# デザイン原理の適用：<span class="subtitle">概念モデル / 対応づけ</span>

## 「コントロールブレイク」の基本メカニズム

- キー項目のブレイクをきっかけに集計などの処理を行う

 <b>→概念モデルとコードが対応づく形で記述できるようにしたい</b>
 <b>→どうやって？</b>

---

# コードサンプル: <span class="subtitle">DSLによる概念モデルとの対応づけ</span>

```kotlin
salesData.applyControlBreak {
    // レベル1: 部門
    breakOn { it.dept }
        .onStart { dept -> 
            println("\n【 $dept 】")
        }
        .memo({ doubleAccumulator() }) { record -> 
            this += record.amount 
        }
        .onEnd { dept, total: Accumulator<Double>? -> 
            println("部門売上合計: ${formatCurrency(total?.value)}") 
        }
        .register()
    // レベル2: 月
    breakOn { it.month }
        .onStart { month -> 
            println(" ▼ $month") 
        }
        .memo({ MonthlyStats() }) { record ->
            update(record.amount, record.quantity)
        } // (以下略)

```

---

# デザイン原理の適用：<span class="subtitle">アフォーダンス / シグニファイア</span>

## 標準のListのアフォーダンス
- 要素を格納できる、要素にアクセスできる、etc

## 新しいアフォーダンスの追加
- 「コントロールブレイク処理を適用できる」
- Kotlinの拡張関数によりアフォーダンスをListに追加

---

# コードサンプル: <span class="subtitle">拡張関数</span>

```kotlin
// List<T>型に新しいメソッドを追加
fun <T> List<T>.applyControlBreak(block: ControlBreakDSL<T>.() -> Unit) {
    val dsl = ControlBreakDSL<T>()
    dsl.block()
    val processor = dsl.build()
    processor.process(this)
}

// 使用側のファイルでインポート
import com.example.controlbreak.applyControlBreak

fun generateReport(salesData: List<SalesRecord>) {
    // 通常のListメソッドのように自然に呼び出せる
    salesData.applyControlBreak {
        // DSLによる直感的な記述
    }
}
```

---

# デザイン原理の適用：<span class="subtitle">制約によるポカよけ</span>

## 型システムによるガード

- コンパイル時のエラー検出
- 不正な使用を防ぐ

---

# コードサンプル: <span class="subtitle">型システムの活用</span>

```kotlin
// memoの初期化ラムダがAccumulator<Double>を返すので、
// 更新ラムダ内のthisもAccumulator<Double>型となる
.memo({ doubleAccumulator() }) { record ->
    this += record.amount    // OK: Double型の加算
    // this += record.dept   // コンパイルエラー: String型は加算できない
}

// カスタム型でも同様に型安全
// DetailedStats型を返すので、thisもDetailedStats型
.memo({ DetailedStats() }) { record ->
    // DetailedStatsのメソッドが呼び出せる
    add(record.amount, record.quantity)
    updateMedian(record.amount)
}
```

---

# デザイン原理の適用：<span class="subtitle">フィードバック</span>

## 早期エラーによるフィードバック

- エラー詳細情報
- 問題の迅速な特定と修正

---

# コードサンプル: <span class="subtitle">エラー処理</span>

```kotlin
// ミス: データがソートされていない（キーでソートされていることが事前条件）
val unsortedData = listOf(
    SalesRecord("営業部", "4月", 100_000), SalesRecord("開発部", "4月", 200_000),
    SalesRecord("営業部", "5月", 150_000)  // エラー: 営業部が再び出現
)
unsortedData.applyControlBreak { breakOn { it.dept }.register() } // 処理実行
/* 実行時にわかりやすいエラーメッセージでフィードバック
ControlBreakException: データが正しくソートされていません
Context:
  level: 0 (部門)
  recordIndex: 2
  previousKey: "開発部"
  currentKey: "営業部"
  record: SalesRecord(dept="営業部", month="5月", amount=150000)
提案: データを事前にソートしてください
  someData.sortedBy { it.field }
*/
```

<div class="highlight-box success">

**フィードバックの効果**: 
- エラーの原因と発生位置が明確
- 問題のあるデータの詳細を表示
- 具体的な修正方法を提案

</div>

---

# デザイン原理の適用：<span class="subtitle">発見可能性</span>

## テストコードで使い方を具体的に示す

- 実例による学習
- ドキュメントとしての役割

---

# コードサンプル: <span class="subtitle">テストコード</span>

```kotlin
@Test
fun `should process simple control break correctly`() {
    // Given
    val data = listOf(TestRecord("A", 100), TestRecord("A", 200), TestRecord("B", 300))
    var result = ""
    // When
    data.applyControlBreak {
        breakOn { it.group }
            .onStart { group -> result += "Start:$group," }
            .memo({ intAccumulator() }) { record -> 
                this += record.value 
            }
            .onEnd { group, total: Accumulator<Int>? -> 
                result += "End:$group=${total?.value}," 
            }
            .register()
    }
    // Then
    assertEquals("Start:A,End:A=300,Start:B,End:B=300,", result)
}
```

---

# DSL（ドメイン固有言語）

## ユーザーの認知負荷を減らし、本質的な課題に集中することができる

- より表現力豊かなコードを実現
- ただし、DSLの実装難易度や工数が課題
- オーバーエンジニアリングのリスク

---
# AIを活用した開発者体験向上

## これまでは費用対効果で難しかったDSLの実装が、生成AIによって容易に

<div class="highlight-box success">

**富豪プログラミング** - リソースを贅沢に使って理想的な設計を実現

**開発者体験の大幅な向上** - 直感的で使いやすいAPIの提供

</div>

<b>→オーバーエンジニアリングが正当化される時代</b>

---

# AIとの伴走

## コアドメイン（中核）：AIを使いこなすクリエイティビティが重要

<div class="image-container" style="margin-bottom: 0;">
  <img src="images/image_t-wada.png" style="max-height: 300px;">
  <p class="image-caption" style="margin-bottom: 5px;">出典: t-wada『AI時代のソフトウェア開発を考える』</p>
  <a href="https://speakerdeck.com/twada/agentic-software-engineering-findy-2025-07-edition" target="_blank" style="color: #4299e1; text-decoration: none; font-size: 0.75em; display: block;">
    https://speakerdeck.com/twada/agentic-software-engineering-findy-2025-07-edition
  </a>
</div>

---

# まとめ

- **HCD**: 人間のニーズ、能力、行動に合わせてデザインする

- **D.A.ノーマン博士の7つのデザイン原理**はソフトウェア設計にも適用可能

- 「AIに全部作らせる」のではなく、**AIを活用して設計の質を向上させる**

---

# 締めのアレ

<div class="haiku-container">
  <div class="haiku-text">
    誰のため？ 自分のためで いいんです<span class="haiku-emoji">😎</span>
  </div>
</div>

---

<!-- _backgroundColor: #1a202c -->
<!-- _color: #e2e8f0 -->

<style scoped>
section {
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

h1 {
  font-size: 4em;
  border: none;
  margin-bottom: 80px;
  color: #ffffff;
  font-weight: 800;
  letter-spacing: -0.02em;
}

p {
  font-size: 1em;
  color: #94a3b8;
  margin: 5px 0;
}

.bottom-info {
  position: absolute;
  bottom: 60px;
  left: 0;
  right: 0;
  text-align: center;
}

.final-message {
  font-size: 2.0em;
  color: #60a5fa;
  font-weight: 700;
}
</style>

# ご清聴ありがとうございました

<div class="bottom-info">
<p class="final-message">ENJOY DESIGNING!</p>
</div>