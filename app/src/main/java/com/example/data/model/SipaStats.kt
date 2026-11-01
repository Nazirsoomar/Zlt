package com.example.data.model

data class SipaStats(
    val rxSum: Long = 0L,
    val rxCnt: Long = 0L,
    val rxFail: Long = 0L,
    val rxBusy: Long = 0L,
    val rxEvt: Long = 0L,
    val txSum: Long = 0L,
    val txCnt: Long = 0L,
    val txFail: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val deltaTxFail: Long = 0L,
    val deltaRxFail: Long = 0L
) {
    companion object {
        fun parseFromOutput(output: String, previous: SipaStats? = null): SipaStats {
            var rxSum = 0L
            var rxCnt = 0L
            var rxFail = 0L
            var rxBusy = 0L
            var rxEvt = 0L
            var txSum = 0L
            var txCnt = 0L
            var txFail = 0L

            output.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("rx_sum:") -> rxSum = parseVal(trimmed)
                    trimmed.startsWith("rx_cnt:") -> rxCnt = parseVal(trimmed)
                    trimmed.startsWith("rx_fail:") -> rxFail = parseVal(trimmed)
                    trimmed.startsWith("rx_busy:") -> rxBusy = parseVal(trimmed)
                    trimmed.startsWith("rx_evt:") -> rxEvt = parseVal(trimmed)
                    trimmed.startsWith("tx_sum:") -> txSum = parseVal(trimmed)
                    trimmed.startsWith("tx_cnt:") -> txCnt = parseVal(trimmed)
                    trimmed.startsWith("tx_fail:") -> txFail = parseVal(trimmed)
                }
            }

            val dTx = if (previous != null && txFail >= previous.txFail) txFail - previous.txFail else 0L
            val dRx = if (previous != null && rxFail >= previous.rxFail) rxFail - previous.rxFail else 0L

            return SipaStats(
                rxSum = rxSum,
                rxCnt = rxCnt,
                rxFail = rxFail,
                rxBusy = rxBusy,
                rxEvt = rxEvt,
                txSum = txSum,
                txCnt = txCnt,
                txFail = txFail,
                deltaTxFail = dTx,
                deltaRxFail = dRx
            )
        }

        private fun parseVal(line: String): Long {
            return line.substringAfter(":").trim().toLongOrNull() ?: 0L
        }
    }
}
