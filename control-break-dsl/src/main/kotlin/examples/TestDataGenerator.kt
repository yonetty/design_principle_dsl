package examples

import kotlin.random.Random
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TestDataGenerator {
    private val departments = listOf("営業部", "開発部", "マーケティング部", "管理部", "人事部")
    private val months = listOf("2024年1月", "2024年2月", "2024年3月", "2024年4月", "2024年5月", "2024年6月")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    fun generateLargeSalesData(recordCount: Int = 10000): List<SalesRecord> {
        val random = Random(42)
        val records = mutableListOf<SalesRecord>()
        
        for (i in 0 until recordCount) {
            val dept = departments[random.nextInt(departments.size)]
            val month = months[random.nextInt(months.size)]
            val monthNumber = months.indexOf(month) + 1
            val day = random.nextInt(1, 29)
            val date = LocalDate.of(2024, monthNumber, day).format(dateFormatter)
            
            val amount = when (dept) {
                "営業部" -> random.nextDouble(50000.0, 200000.0)
                "開発部" -> random.nextDouble(60000.0, 150000.0)
                "マーケティング部" -> random.nextDouble(40000.0, 120000.0)
                "管理部" -> random.nextDouble(30000.0, 100000.0)
                "人事部" -> random.nextDouble(20000.0, 80000.0)
                else -> random.nextDouble(10000.0, 50000.0)
            }
            
            val quantity = random.nextInt(1, 50)
            
            records.add(SalesRecord(dept, month, date, amount, quantity))
        }
        
        return records.sortedWith(
            compareBy({ it.dept }, { it.month }, { it.date })
        )
    }
    
    fun generateSalesDataWithVariations(): List<SalesRecord> {
        return listOf(
            SalesRecord("営業部", "2024年1月", "2024-01-05", 120000.0, 10),
            SalesRecord("営業部", "2024年1月", "2024-01-10", 85000.0, 7),
            SalesRecord("営業部", "2024年1月", "2024-01-15", 95000.0, 8),
            SalesRecord("営業部", "2024年2月", "2024-02-01", 156000.0, 12),
            SalesRecord("営業部", "2024年2月", "2024-02-10", 98000.0, 8),
            SalesRecord("営業部", "2024年2月", "2024-02-20", 110000.0, 9),
            
            SalesRecord("開発部", "2024年1月", "2024-01-10", 95000.0, 5),
            SalesRecord("開発部", "2024年1月", "2024-01-20", 78000.0, 6),
            SalesRecord("開発部", "2024年2月", "2024-02-15", 110000.0, 8),
            SalesRecord("開発部", "2024年2月", "2024-02-25", 125000.0, 10),
            
            SalesRecord("マーケティング部", "2024年1月", "2024-01-05", 65000.0, 15),
            SalesRecord("マーケティング部", "2024年1月", "2024-01-25", 72000.0, 18),
            SalesRecord("マーケティング部", "2024年2月", "2024-02-15", 88000.0, 20),
            
            SalesRecord("管理部", "2024年1月", "2024-01-08", 45000.0, 3),
            SalesRecord("管理部", "2024年1月", "2024-01-18", 52000.0, 4),
            SalesRecord("管理部", "2024年2月", "2024-02-12", 58000.0, 5),
            
            SalesRecord("人事部", "2024年1月", "2024-01-12", 35000.0, 2),
            SalesRecord("人事部", "2024年2月", "2024-02-22", 42000.0, 3)
        )
    }
    
    fun generateCornerCaseData(): List<SalesRecord> {
        return listOf(
            SalesRecord("部門A", "2024年1月", "2024-01-01", 0.0, 0),
            SalesRecord("部門A", "2024年1月", "2024-01-02", 100.0, 1),
            SalesRecord("部門B", "2024年1月", "2024-01-01", 200.0, 2),
            SalesRecord("部門B", "2024年1月", "2024-01-02", 0.0, 0),
            SalesRecord("部門C", "2024年1月", "2024-01-01", -100.0, 1),
            SalesRecord("部門C", "2024年1月", "2024-01-02", 300.0, 3)
        )
    }
    
    fun generateEmptyData(): List<SalesRecord> {
        return emptyList()
    }
    
    fun generateSingleRecordData(): List<SalesRecord> {
        return listOf(
            SalesRecord("営業部", "2024年1月", "2024-01-01", 100000.0, 10)
        )
    }
    
    fun generateUnsortedData(): List<SalesRecord> {
        return listOf(
            SalesRecord("開発部", "2024年2月", "2024-02-15", 110000.0, 8),
            SalesRecord("営業部", "2024年1月", "2024-01-05", 120000.0, 10),
            SalesRecord("マーケティング部", "2024年1月", "2024-01-25", 72000.0, 18),
            SalesRecord("営業部", "2024年2月", "2024-02-01", 156000.0, 12),
            SalesRecord("開発部", "2024年1月", "2024-01-10", 95000.0, 5),
            SalesRecord("マーケティング部", "2024年2月", "2024-02-15", 88000.0, 20)
        )
    }
}