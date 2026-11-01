package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RouterViewModel
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DiagnosticsScreen(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    val sipaStats by viewModel.sipaStats.collectAsState()
    val pingHistory by viewModel.pingHistory.collectAsState()

    val avgPing = if (pingHistory.isNotEmpty()) pingHistory.map { it.latencyMs }.average().toFloat() else 0.25f
    val maxPing = if (pingHistory.isNotEmpty()) pingHistory.maxOf { it.latencyMs } else 0.58f
    val minPing = if (pingHistory.isNotEmpty()) pingHistory.minOf { it.latencyMs } else 0.18f
    val latestJitter = pingHistory.lastOrNull()?.jitterMs ?: 0.12f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Real-Time Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "SIPA Subsystem, Kernel Queues & PUBG Jitter Analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live Latency Graph Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diagnostic_ping_graph_card")
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Latency Waveform & Jitter Tracking",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "RFC 3550 Standard",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }

                    // Canvas Waveform
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090E17))
                            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(0f, h * 0.25f),
                                end = Offset(w, h * 0.25f)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(0f, h * 0.5f),
                                end = Offset(w, h * 0.5f)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(0f, h * 0.75f),
                                end = Offset(w, h * 0.75f)
                            )

                            if (pingHistory.size >= 2) {
                                val samples = pingHistory.takeLast(30)
                                val maxVal = (samples.maxOfOrNull { it.latencyMs } ?: 10f).coerceAtLeast(1.0f)
                                val stepX = w / (samples.size - 1)

                                val path = Path()
                                samples.forEachIndexed { idx, item ->
                                    val x = idx * stepX
                                    val y = h - ((item.latencyMs / maxVal) * (h * 0.85f)) - (h * 0.05f)
                                    if (idx == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = NeonCyan,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Draw point dots for spikes
                                samples.forEachIndexed { idx, item ->
                                    val x = idx * stepX
                                    val y = h - ((item.latencyMs / maxVal) * (h * 0.85f)) - (h * 0.05f)
                                    val isSpike = item.latencyMs > avgPing * 2.0f
                                    drawCircle(
                                        color = if (isSpike) StatusDanger else StatusSuccess,
                                        radius = if (isSpike) 4.dp.toPx() else 2.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                    }

                    // Stat Readout Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(label = "Min Latency", value = "%.2f ms".format(minPing), color = StatusSuccess)
                        StatTile(label = "Avg Latency", value = "%.2f ms".format(avgPing), color = NeonCyan)
                        StatTile(label = "Max Spike", value = "%.2f ms".format(maxPing), color = if (maxPing > 50f) StatusDanger else StatusWarning)
                        StatTile(label = "Jitter", value = "%.2f ms".format(latestJitter), color = if (latestJitter < 2f) StatusSuccess else StatusWarning)
                    }
                }
            }
        }

        // SIPA Deep Diagnostic Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sipa_detailed_card")
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIPA Engine Internal Statistics (/sys/.../sipa_eth0/stats)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "RECEIVE (RX)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                            SipaRow(name = "rx_sum", value = sipaStats.rxSum.toString())
                            SipaRow(name = "rx_cnt", value = sipaStats.rxCnt.toString())
                            SipaRow(name = "rx_fail", value = sipaStats.rxFail.toString(), isHighlight = sipaStats.rxFail > 0)
                            SipaRow(name = "rx_busy", value = sipaStats.rxBusy.toString())
                            SipaRow(name = "rx_evt", value = sipaStats.rxEvt.toString())
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "TRANSMIT (TX)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ElectricBlue)
                            SipaRow(name = "tx_sum", value = sipaStats.txSum.toString())
                            SipaRow(name = "tx_cnt", value = sipaStats.txCnt.toString())
                            SipaRow(name = "tx_fail", value = sipaStats.txFail.toString(), isHighlight = true)
                        }
                    }

                    HorizontalDivider(color = CyberSurfaceBorder)

                    // Roman Urdu Deep Technical Insight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131F33))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Tahqeeq / Research Findings:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "1. TX Fail counter humne 212 aur 214 dekha, phir baad mein 1 dekha. Ye modem transmit ring buffer exhaust hone par barhta hai.\n2. Slow internet ke waqt bhi 212 constant tha, isliye gaming ping spikes ka bada sabab txqueuelen (bufferbloat) aur softnet backlog hai, na ke har dafa SIPA failure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // CPU & Interrupt Health
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CPU & Interrupt Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CPU Cores", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("CPU0 & CPU1 (Dual Core)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("SIPA IRQ 22", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("Modem Path (Affinity 3)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Column {
                            Text("Ethernet IRQ 28", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("PCI-MSI (Affinity 3)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Interrupt Affinity Tweak: IRQ 28 ko CPU1 par lock karne se CPU0 modem ke packets smoothly process karta hai.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatTile(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SipaRow(name: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHighlight) StatusDanger.copy(alpha = 0.15f) else Color(0xFF090D14))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) StatusWarning else TextPrimary
        )
    }
}
