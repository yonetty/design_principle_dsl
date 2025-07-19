package controlbreak

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SortValidationTest {
    
    data class TestRecord(val dept: String, val month: String, val value: Int)
    
    @Test
    fun `should accept properly sorted data`() {
        val data = listOf(
            TestRecord("A", "Jan", 100),
            TestRecord("A", "Jan", 200),
            TestRecord("A", "Feb", 300),
            TestRecord("B", "Jan", 400),
            TestRecord("B", "Feb", 500)
        )
        
        // Should not throw exception
        data.applyControlBreak {
            breakOn { it.dept }
                .onEnd { dept, _: Any? -> }
                .register()
            breakOn { it.month }
                .onEnd { month, _: Any? -> }
                .register()
        }
    }
    
    @Test
    fun `should detect unsorted data at first level`() {
        val data = listOf(
            TestRecord("A", "Jan", 100),
            TestRecord("B", "Jan", 200),
            TestRecord("A", "Jan", 300)  // A appears again - wrong
        )
        
        val exception = assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.dept }
                    .register()
            }
        }
        
        assertTrue(exception.message?.contains("データが正しくソートされていません") == true)
        assertEquals("A", exception.context["currentKey"])
        assertEquals("0 (部門)", exception.context["level"])
    }
    
    @Test
    fun `should detect unsorted data at second level`() {
        val data = listOf(
            TestRecord("A", "Jan", 100),
            TestRecord("A", "Feb", 200),
            TestRecord("A", "Jan", 300)   // Jan appears again after Feb - wrong
        )
        
        val exception = assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.dept }
                    .register()
                breakOn { it.month }
                    .register()
            }
        }
        
        assertTrue(exception.message?.contains("データが正しくソートされていません") == true)
        assertEquals("1 (月)", exception.context["level"])
        assertEquals("Jan", exception.context["currentKey"])
    }
    
    @Test
    fun `should accept data when first level changes`() {
        val data = listOf(
            TestRecord("A", "Feb", 100),
            TestRecord("B", "Jan", 200)   // Jan < Feb but it's OK because dept changed
        )
        
        // Should not throw exception
        data.applyControlBreak {
            breakOn { it.dept }
                .register()
            breakOn { it.month }
                .register()
        }
    }
    
    @Test
    fun `should work with non-comparable keys`() {
        data class CustomKey(val value: String)  // Not Comparable
        data class Record(val key: CustomKey, val value: Int)
        
        val data = listOf(
            Record(CustomKey("A"), 100),
            Record(CustomKey("A"), 200),
            Record(CustomKey("B"), 300)
        )
        
        // Should not throw exception (non-comparable keys are not checked for order)
        data.applyControlBreak {
            breakOn { it.key }
                .register()
        }
    }
    
    @Test
    fun `should handle complex sorting scenario`() {
        val data = listOf(
            TestRecord("Sales", "2024-01", 100),
            TestRecord("Sales", "2024-01", 200),
            TestRecord("Sales", "2024-02", 300),
            TestRecord("Sales", "2024-01", 400)  // Going back to Jan - error
        )
        
        val exception = assertThrows<ControlBreakException> {
            data.applyControlBreak {
                breakOn { it.dept }
                    .register()
                breakOn { it.month }
                    .register()
            }
        }
        
        assertTrue(exception.message?.contains("データが正しくソートされていません") == true)
        assertEquals("2024-01", exception.context["currentKey"])
    }
}