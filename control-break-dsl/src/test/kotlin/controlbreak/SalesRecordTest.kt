package controlbreak

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalesRecordTest {
    
    data class SalesRecord(
        val dept: String,     // 部門
        val month: String,    // 月
        val amount: Int       // 売上金額
    )
    
    @Test
    fun `should throw error with Japanese message when data is not sorted`() {
        // スライドと同じデータ
        val unsortedData = listOf(
            SalesRecord("営業部", "4月", 100_000),
            SalesRecord("開発部", "4月", 200_000),
            SalesRecord("営業部", "5月", 150_000)  // エラー: 営業部が再び出現
        )
        
        val exception = assertThrows<ControlBreakException> {
            unsortedData.applyControlBreak {
                breakOn { it.dept }.register()
            }
        }
        
        // エラーメッセージの検証
        assertEquals("データが正しくソートされていません", exception.message)
        
        // Context内容の検証
        assertEquals("0 (部門)", exception.context["level"])
        assertEquals(2, exception.context["recordIndex"])
        assertEquals("開発部", exception.context["previousKey"])
        assertEquals("営業部", exception.context["currentKey"])
        
        // レコード情報の検証
        val record = exception.context["record"] as SalesRecord
        assertEquals("営業部", record.dept)
        assertEquals("5月", record.month)
        assertEquals(150_000, record.amount)
        
        // 提案の検証
        val suggestion = exception.context["suggestion"] as String
        assertTrue(suggestion.contains("データを事前にソートしてください"))
        assertTrue(suggestion.contains("someData.sortedBy { it.field }"))
        
        // toString()の形式を検証
        val errorOutput = exception.toString()
        assertTrue(errorOutput.contains("ControlBreakException: データが正しくソートされていません"))
        assertTrue(errorOutput.contains("Context:"))
        assertTrue(errorOutput.contains("level: 0 (部門)"))
        assertTrue(errorOutput.contains("提案: データを事前にソートしてください"))
    }
    
    @Test
    fun `should process correctly when data is properly sorted`() {
        val sortedData = listOf(
            SalesRecord("営業部", "4月", 100_000),
            SalesRecord("営業部", "4月", 85_000),
            SalesRecord("営業部", "5月", 156_000),
            SalesRecord("開発部", "4月", 98_000),
            SalesRecord("開発部", "5月", 134_000)
        )
        
        var result = ""
        var deptTotal = 0
        
        sortedData.applyControlBreak {
            breakOn { it.dept }
                .onStart { dept -> 
                    result += "\n【$dept】\n"
                }
                .memo({ intAccumulator() }) { record -> 
                    this += record.amount
                }
                .onEnd { dept, total: Accumulator<Int>? ->
                    val totalAmount = total?.value ?: 0
                    result += "  部門計: ${String.format("%,d", totalAmount)}円\n"
                    deptTotal += totalAmount
                }
                .register()
                
            breakOn { it.month }
                .onStart { month ->
                    result += "  ▼ $month\n"
                }
                .memo({ intAccumulator() }) { record ->
                    this += record.amount
                }
                .onEnd { month, total: Accumulator<Int>? ->
                    val totalAmount = total?.value ?: 0
                    result += "    月計: ${String.format("%,d", totalAmount)}円\n"
                }
                .register()
                
            detail { record ->
                result += "    ${String.format("%,d", record.amount)}円\n"
            }
        }
        
        // 出力内容の検証
        assertTrue(result.contains("【営業部】"))
        assertTrue(result.contains("【開発部】"))
        assertTrue(result.contains("部門計: 341,000円"))
        assertTrue(result.contains("部門計: 232,000円"))
    }
}