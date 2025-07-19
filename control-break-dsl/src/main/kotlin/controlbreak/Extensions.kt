package controlbreak

fun <T> List<T>.applyControlBreak(block: ControlBreakDSL<T>.() -> Unit) {
    val dsl = ControlBreakDSL<T>()
    dsl.block()
    val processor = dsl.build()
    processor.process(this)
}

fun <T> Sequence<T>.applyControlBreak(block: ControlBreakDSL<T>.() -> Unit) {
    this.toList().applyControlBreak(block)
}

fun <T> List<T>.applyControlBreakParallel(
    parallelism: Int = Runtime.getRuntime().availableProcessors(),
    block: ControlBreakDSL<T>.() -> Unit
) {
    if (size <= 1000) {
        applyControlBreak(block)
        return
    }
    
    val chunkSize = (size + parallelism - 1) / parallelism
    chunked(chunkSize)
        .parallelStream()
        .forEach { chunk ->
            chunk.applyControlBreak(block)
        }
}

fun formatCurrency(amount: Double?): String =
    amount?.let { "%,.0f円".format(it) } ?: "0円"

fun formatNumber(number: Number?): String =
    number?.let { "%,d".format(it.toLong()) } ?: "0"