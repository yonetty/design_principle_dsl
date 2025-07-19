package controlbreak

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlBreakDSLTest {
    
    data class TestRecord(val group: String, val value: Int)
    
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
                .memo({ intAccumulator() }) { record -> this += record.value }
                .onEnd { group, total: Accumulator<Int>? -> result += "End:$group=${total?.value}," }
                .register()
        }
        
        assertEquals("Start:A,End:A=300,Start:B,End:B=300,", result)
    }
    
    @Test
    fun `should handle empty data gracefully`() {
        val emptyData = emptyList<TestRecord>()
        var grandTotalCalled = false
        
        assertDoesNotThrow {
            emptyData.applyControlBreak {
                breakOn { it.group }.register()
                grandTotal { grandTotalCalled = true }
            }
        }
        
        assertTrue(grandTotalCalled)
    }
    
    @Test
    fun `should allow multiple key selectors`() {
        val data = listOf(TestRecord("A", 100))
        
        assertDoesNotThrow {
            data.applyControlBreak {
                breakOn { it.group }.register()
                breakOn { it.value }.register()
            }
        }
    }
    
    @Test
    fun `should enforce single detail processor`() {
        val data = listOf(TestRecord("A", 100))
        
        assertThrows<IllegalArgumentException> {
            data.applyControlBreak {
                breakOn { it.group }.register()
                detail { }
                detail { }
            }
        }
    }
    
    @Test
    fun `should process multiple break levels correctly`() {
        data class ComplexRecord(val dept: String, val month: String, val amount: Double)
        
        val data = listOf(
            ComplexRecord("Sales", "Jan", 100.0),
            ComplexRecord("Sales", "Jan", 200.0),
            ComplexRecord("Sales", "Feb", 300.0),
            ComplexRecord("Dev", "Jan", 400.0)
        )
        
        val results = mutableListOf<String>()
        
        data.applyControlBreak {
            breakOn { it.dept }
                .onStart { dept -> results.add("Dept:$dept") }
                .memo({ doubleAccumulator() }) { record -> this += record.amount }
                .onEnd { dept, total: Accumulator<Double>? -> results.add("DeptTotal:$dept=${total?.value}") }
                .register()
                
            breakOn { it.month }
                .onStart { month -> results.add("Month:$month") }
                .memo({ doubleAccumulator() }) { record -> this += record.amount }
                .onEnd { month, total: Accumulator<Double>? -> results.add("MonthTotal:$month=${total?.value}") }
                .register()
        }
        
        assertTrue(results.contains("Dept:Sales"))
        assertTrue(results.contains("DeptTotal:Sales=600.0"))
        assertTrue(results.contains("Dept:Dev"))
        assertTrue(results.contains("DeptTotal:Dev=400.0"))
    }
    
    @Test
    fun `should handle memos with different types`() {
        val data = listOf(
            TestRecord("A", 100),
            TestRecord("A", 200),
            TestRecord("B", 300)
        )
        
        var stringMemo = ""
        var listMemo = listOf<Int>()
        
        data.applyControlBreak {
            breakOn { it.group }
                .memo({ stringAccumulator() }) { record -> 
                    this += record.value.toString() + ","
                }
                .onEnd { group, memo: Accumulator<String>? -> 
                    if (group == "A") stringMemo = memo?.value ?: ""
                }
                .register()
        }
        
        assertEquals("100,200,", stringMemo)
        
        data.applyControlBreak {
            breakOn { it.group }
                .memo({ mutableListOf<Int>() }) { record -> 
                    add(record.value)
                }
                .onEnd { group, memo: MutableList<Int>? -> 
                    if (group == "B") listMemo = memo ?: emptyList()
                }
                .register()
        }
        
        assertEquals(listOf(300), listMemo)
    }
    
    @Test
    fun `should throw error when no break levels defined`() {
        val data = listOf(TestRecord("A", 100))
        
        assertThrows<ControlBreakException> {
            data.applyControlBreak {
                detail { println(it) }
            }
        }
    }
    
    @Test
    fun `should process detail records correctly`() {
        val data = listOf(
            TestRecord("A", 100),
            TestRecord("A", 200),
            TestRecord("B", 300)
        )
        
        val processedRecords = mutableListOf<TestRecord>()
        
        data.applyControlBreak {
            breakOn { it.group }.register()
            detail { record ->
                processedRecords.add(record)
            }
        }
        
        assertEquals(data, processedRecords)
    }
    
    @Test
    fun `should call grand total with all data`() {
        val data = listOf(
            TestRecord("A", 100),
            TestRecord("B", 200),
            TestRecord("C", 300)
        )
        
        var grandTotalData: List<TestRecord>? = null
        
        data.applyControlBreak {
            breakOn { it.group }.register()
            grandTotal { allData ->
                grandTotalData = allData
            }
        }
        
        assertEquals(data, grandTotalData)
    }
    
    @Test
    fun `should enforce single memo per break level`() {
        val data = listOf(TestRecord("A", 100))
        
        assertThrows<IllegalArgumentException> {
            data.applyControlBreak {
                breakOn { it.group }
                    .memo({ 0 }) { }
                    .memo({ 0 }) { }
                    .register()
            }
        }
    }
}