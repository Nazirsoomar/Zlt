package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SipaStats
import com.example.data.repository.RouterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches UNISOC AI Master`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UNISOC AI Master", appName)
    }

    @Test
    fun `parse SipaStats from observed output`() {
        val sampleOutput = """
            RX statistics:
            rx_sum: 84920421
            rx_cnt: 84920400
            rx_fail: 0
            rx_busy: 0
            rx_evt: 124809
            TX statistics:
            tx_sum: 62410884
            tx_cnt: 62410670
            tx_fail: 214
        """.trimIndent()

        val parsed = SipaStats.parseFromOutput(sampleOutput)
        assertEquals(84920421L, parsed.rxSum)
        assertEquals(214L, parsed.txFail)
        assertEquals(0L, parsed.rxFail)
    }

    @Test
    fun `router settings initialize with verified restore commands`() = runTest {
        val repo = RouterRepository()
        val settings = repo.settings.value
        assertTrue(settings.isNotEmpty())

        val txQueue = settings.find { it.id == "sipa_txqueuelen" }
        assertNotNull(txQueue)
        assertEquals("ip link set sipa_eth0 txqueuelen 1000", txQueue?.activeRestoreCommand)
    }
}
