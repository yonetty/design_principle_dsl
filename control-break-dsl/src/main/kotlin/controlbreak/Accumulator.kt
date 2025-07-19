package controlbreak

/**
 * 累積値を保持するためのラッパークラス
 */
class Accumulator<T>(var value: T) {
    operator fun plusAssign(other: T) {
        @Suppress("UNCHECKED_CAST")
        value = when (value) {
            is Double -> (value as Double + other as Double) as T
            is Float -> (value as Float + other as Float) as T
            is Long -> (value as Long + other as Long) as T
            is Int -> (value as Int + other as Int) as T
            is String -> (value as String + other as String) as T
            else -> throw UnsupportedOperationException("Cannot add values of type ${value!!::class}")
        }
    }
    
    operator fun minusAssign(other: T) {
        @Suppress("UNCHECKED_CAST")
        value = when (value) {
            is Double -> (value as Double - other as Double) as T
            is Float -> (value as Float - other as Float) as T
            is Long -> (value as Long - other as Long) as T
            is Int -> (value as Int - other as Int) as T
            else -> throw UnsupportedOperationException("Cannot subtract values of type ${value!!::class}")
        }
    }
    
    override fun toString() = value.toString()
}

// 便利なファクトリ関数
fun doubleAccumulator(initial: Double = 0.0) = Accumulator(initial)
fun intAccumulator(initial: Int = 0) = Accumulator(initial)
fun longAccumulator(initial: Long = 0L) = Accumulator(initial)
fun stringAccumulator(initial: String = "") = Accumulator(initial)