package examples

import controlbreak.*
import kotlin.math.pow
import kotlin.math.sqrt

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

fun main() {
    println("===== コントロールブレイクDSLデモ =====\n")
    
    val salesData = generateSalesData()
    
    println("1. レガシーコードによる売上レポート")
    println("=====================================")
    val legacyReport = generateSalesReportLegacy(salesData)
    println(legacyReport)
    
    println("\n2. 新DSLによる売上レポート")
    println("=============================")
    
    val sortedData = salesData.sortedWith(
        compareBy({ it.dept }, { it.month }, { it.date })
    )
    
    sortedData.applyControlBreak {
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
            
        breakOn { it.month }
            .onStart { month ->
                println(" ▼ $month")
            }
            .memo({ MonthlyStats() }) { record ->
                update(record.amount, record.quantity)
            }
            .onEnd { month, stats: MonthlyStats? ->
                stats?.let {
                    println("  売上合計: ${formatCurrency(it.totalAmount)}")
                    println("  販売数量: ${it.totalQuantity}個")
                    println("  取引件数: ${it.recordCount}件")
                    println("  平均売上: ${formatCurrency(it.averageAmount)}")
                    println("  平均単価: ${formatCurrency(it.averagePrice)}")
                }
            }
            .register()
            
        detail { record ->
            println("    ${record.date}: ${formatCurrency(record.amount)} (${record.quantity}個)")
        }
        
        grandTotal { data ->
            val total = data.sumOf { it.amount }
            val totalQuantity = data.sumOf { it.quantity }
            println("\n===============================")
            println("総売上: ${formatCurrency(total)}")
            println("総販売数: ${totalQuantity}個")
            println("===============================")
        }
    }
    
    println("\n3. 高度な統計情報を含むレポート")
    println("=================================")
    
    sortedData.applyControlBreak {
        breakOn { it.dept }
            .memo({ DetailedStats() }) { record ->
                add(record.amount, record.quantity)
            }
            .onEnd { dept, stats: DetailedStats? ->
                stats?.let {
                    println("\n【$dept 統計情報】")
                    println("  中央値: ${formatCurrency(it.median)}")
                    println("  標準偏差: ${formatCurrency(it.standardDeviation)}")
                }
            }
            .register()
    }
    
    println("\n4. エラーハンドリングのデモ")
    println("=============================")
    demonstrateErrorHandling()
}

fun generateSalesReportLegacy(salesData: List<SalesRecord>): String {
    val output = StringBuilder()
    var currentDept = ""
    var currentMonth = ""
    var deptTotal = 0.0
    var monthTotal = 0.0
    var monthQuantity = 0
    var grandTotal = 0.0
    var isFirst = true
    
    val sortedData = salesData.sortedWith(compareBy({ it.dept }, { it.month }, { it.date }))
    
    for (record in sortedData) {
        if (currentDept != record.dept) {
            if (!isFirst) {
                output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
                deptTotal = 0.0
            }
            currentDept = record.dept
            output.append("【${currentDept}】\n")
            currentMonth = ""
        }
        
        if (currentMonth != record.month) {
            if (!isFirst && currentMonth.isNotEmpty()) {
                output.append("    月計: ${formatCurrency(monthTotal)} (${monthQuantity}個)\n")
                monthTotal = 0.0
                monthQuantity = 0
            }
            currentMonth = record.month
            output.append("  ▼ ${currentMonth}\n")
        }
        
        output.append("    ${record.date}: ${formatCurrency(record.amount)}\n")
        
        monthTotal += record.amount
        monthQuantity += record.quantity
        deptTotal += record.amount
        grandTotal += record.amount
        isFirst = false
    }
    
    if (!isFirst) {
        output.append("    月計: ${formatCurrency(monthTotal)} (${monthQuantity}個)\n")
        output.append("  部門計: ${formatCurrency(deptTotal)}\n\n")
    }
    
    output.append("総計: ${formatCurrency(grandTotal)}\n")
    return output.toString()
}

fun generateSalesData(): List<SalesRecord> {
    return listOf(
        SalesRecord("営業部", "2024年4月", "2024-04-01", 120000.0, 10),
        SalesRecord("営業部", "2024年4月", "2024-04-15", 85000.0, 7),
        SalesRecord("営業部", "2024年5月", "2024-05-02", 156000.0, 12),
        SalesRecord("営業部", "2024年5月", "2024-05-10", 98000.0, 8),
        SalesRecord("開発部", "2024年4月", "2024-04-10", 95000.0, 5),
        SalesRecord("開発部", "2024年4月", "2024-04-20", 78000.0, 6),
        SalesRecord("開発部", "2024年5月", "2024-05-20", 110000.0, 8),
        SalesRecord("開発部", "2024年5月", "2024-05-25", 125000.0, 10),
        SalesRecord("マーケティング部", "2024年4月", "2024-04-05", 65000.0, 15),
        SalesRecord("マーケティング部", "2024年4月", "2024-04-25", 72000.0, 18),
        SalesRecord("マーケティング部", "2024年5月", "2024-05-15", 88000.0, 20)
    )
}

fun demonstrateErrorHandling() {
    val invalidData = listOf<SalesRecord>()
    
    try {
        println("\nケース1: 空のデータセット")
        invalidData.applyControlBreak {
            breakOn { it.dept }
                .onStart { println("部門: $it") }
                .register()
        }
    } catch (e: ControlBreakException) {
        println("エラーが正しく処理されました:")
        println(e)
    }
    
    try {
        println("\nケース2: キーセレクタのエラー")
        val data = listOf(SalesRecord("", "", "", 0.0, 0))
        data.applyControlBreak {
            breakOn { it.dept.substring(10) }
                .register()
        }
    } catch (e: ControlBreakException) {
        println("エラーが正しく処理されました:")
        println(e)
    }
    
    try {
        println("\nケース3: 設定されていないブレイクレベル")
        val data = listOf(SalesRecord("A", "B", "C", 100.0, 1))
        data.applyControlBreak {
            detail { println(it) }
        }
    } catch (e: ControlBreakException) {
        println("エラーが正しく処理されました:")
        println(e)
    }
}