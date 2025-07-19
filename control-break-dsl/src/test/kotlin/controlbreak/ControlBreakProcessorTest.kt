package controlbreak

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlBreakProcessorTest {
    
    data class TestRecord(val group: String, val subgroup: String, val value: Int)
    
    @Test
    fun `should handle nested breaks correctly`() {
        val data = listOf(
            TestRecord("A", "1", 100),
            TestRecord("A", "1", 200),
            TestRecord("A", "2", 300),
            TestRecord("B", "1", 400),
            TestRecord("B", "2", 500)
        )
        
        val events = mutableListOf<String>()
        
        data.applyControlBreak {
            breakOn { it.group }
                .onStart { group -> events.add("GroupStart:$group") }
                .onEnd { group, _: Any? -> events.add("GroupEnd:$group") }
                .register()
                
            breakOn { it.subgroup }
                .onStart { subgroup -> events.add("SubgroupStart:$subgroup") }
                .onEnd { subgroup, _: Any? -> events.add("SubgroupEnd:$subgroup") }
                .register()
        }
        
        val expected = listOf(
            "GroupStart:A",
            "SubgroupStart:1",
            "SubgroupEnd:1",
            "SubgroupStart:2",
            "SubgroupEnd:2",
            "GroupEnd:A",
            "GroupStart:B",
            "SubgroupStart:1",
            "SubgroupEnd:1",
            "SubgroupStart:2",
            "SubgroupEnd:2",
            "GroupEnd:B"
        )
        
        assertEquals(expected, events)
    }
    
    @Test
    fun `should accumulate values correctly across breaks`() {
        val data = listOf(
            TestRecord("A", "1", 100),
            TestRecord("A", "1", 200),
            TestRecord("A", "2", 300),
            TestRecord("B", "1", 400)
        )
        
        val totals = mutableMapOf<String, Int>()
        val subtotals = mutableMapOf<String, Int>()
        
        data.applyControlBreak {
            breakOn { it.group }
                .memo({ intAccumulator() }) { record -> this += record.value }
                .onEnd { group, total: Accumulator<Int>? -> totals[group] = total?.value ?: 0 }
                .register()
                
            breakOn { it.subgroup }
                .memo({ intAccumulator() }) { record -> this += record.value }
                .onEnd { subgroup, total: Accumulator<Int>? -> subtotals["$subgroup-${subtotals.size}"] = total?.value ?: 0 }
                .register()
        }
        
        assertEquals(600, totals["A"])
        assertEquals(400, totals["B"])
        assertEquals(300, subtotals["1-0"])
        assertEquals(300, subtotals["2-1"])
        assertEquals(400, subtotals["1-2"])
    }
    
    @Test
    fun `should handle single record correctly`() {
        val data = listOf(TestRecord("A", "1", 100))
        var onStartCalled = false
        var onEndCalled = false
        var detailCalled = false
        
        data.applyControlBreak {
            breakOn { it.group }
                .onStart { onStartCalled = true }
                .onEnd { _, _: Any? -> onEndCalled = true }
                .register()
            detail { detailCalled = true }
        }
        
        assertTrue(onStartCalled)
        assertTrue(onEndCalled)
        assertTrue(detailCalled)
    }
    
    @Test
    fun `should throw error for null key selector result`() {
        data class NullableRecord(val group: String?, val value: Int)
        val data = listOf(NullableRecord(null, 100))
        
        assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.group!! }
                    .register()
            }
        }
    }
    
    @Test
    fun `should handle large datasets efficiently`() {
        val largeData = (1..15000).map { 
            TestRecord("Group${it / 1000}", "Sub${it / 100}", it) 
        }
        
        var processed = false
        
        // Should process large dataset without issues
        largeData.applyControlBreak {
            breakOn { it.group }
                .onEnd { _, _: Any? -> processed = true }
                .register()
        }
        
        assertTrue(processed)
    }
    
    @Test
    fun `should process multiple levels without warning`() {
        val data = listOf(
            TestRecord("A", "1", 100),
            TestRecord("A", "2", 200),
            TestRecord("B", "1", 300)
        )
        
        // Should process without any issues
        data.applyControlBreak {
            breakOn { it.group }.register()
            breakOn { it.subgroup }.register()
        }
    }
    
    @Test
    fun `should handle exceptions in key selector`() {
        val data = listOf(TestRecord("", "1", 100))
        
        assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.group.substring(10) }
                    .register()
            }
        }
    }
    
    @Test
    fun `should handle exceptions in processors`() {
        val data = listOf(TestRecord("A", "1", 100))
        
        assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.group }
                    .onStart { throw RuntimeException("Test error") }
                    .register()
            }
        }
    }
    
    @Test
    fun `should process empty memo correctly`() {
        val data = listOf(
            TestRecord("A", "1", 100),
            TestRecord("A", "2", 200)
        )
        
        var endCalledWithNullMemo = false
        
        data.applyControlBreak {
            breakOn { it.group }
                .onEnd { _, memo: Any? -> 
                    endCalledWithNullMemo = memo == null
                }
                .register()
        }
        
        assertTrue(endCalledWithNullMemo)
    }
}