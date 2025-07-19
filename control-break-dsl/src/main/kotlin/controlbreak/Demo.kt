package controlbreak

data class MySalesRecord(val dept: String, val month: String, val amount: Int)

fun main() {
    println("=== 日本語エラーメッセージのデモ ===\n")
    
    // スライドと同じデータ
    val unsortedData = listOf(
        MySalesRecord("営業部", "4月", 100_000),
        MySalesRecord("開発部", "4月", 200_000),
        MySalesRecord("営業部", "5月", 150_000)  // エラー: 営業部が再び出現
    )
    
    try {
        unsortedData.applyControlBreak {
            breakOn { it.dept }.register()
        }
    } catch (e: ControlBreakException) {
        println("エラーが発生しました:")
        println(e.toString())
    }
}