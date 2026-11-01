package com.example.data.model

enum class ConnectionMode(val label: String) {
    DISCONNECTED("Offline"),
    CONNECTING("Connecting..."),
    CONNECTED_REAL("Connected (Live Telnet)"),
    CONNECTED_SIMULATED("Simulated Mode (Router Offline)")
}

data class RouterSystemInfo(
    val chipset: String = "UNISOC UDX710 5G/LTE",
    val firmware: String = "W22.32.3",
    val kernel: String = "Linux 4.14.98",
    val busybox: String = "1.27.2",
    val shell: String = "sh-4.4",
    val hostIp: String = "192.168.0.1",
    val port: Int = 23,
    val wanInterface: String = "sipa_eth0 (MTU 1420)",
    val lanBridge: String = "br0 (192.168.0.1, eth0)",
    val ethernetInterface: String = "eth0",
    val cpuInfo: String = "CPU0, CPU1 (ARM Cortex)",
    val irqEthernet: String = "IRQ 28 (Affinity: 3)",
    val irqSipa: String = "IRQ 22",
    val qdisc: String = "pfifo_fast, qlen 1000",
    val tcpAlgorithms: String = "cubic, reno (active: cubic)",
    val connectionMode: ConnectionMode = ConnectionMode.DISCONNECTED,
    val uptime: String = "Unknown",
    val loadAverage: String = "Unknown"
)
