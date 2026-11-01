package com.example.data.model

enum class SettingCategory(val displayName: String, val iconName: String) {
    NETWORKING("Network & Routing", "wifi"),
    SIPA_QDISC("SIPA & Queues", "alt_route"),
    CPU_GOVERNOR("CPU & Frequency", "speed"),
    IRQ_AFFINITY("IRQ & Softirq", "memory"),
    RAM_CACHE("RAM & Memory", "storage"),
    TCP_STACK("TCP Gaming Stack", "tune"),
    CELLULAR_AT("Cellular Modem AT", "cell_tower")
}

enum class TweakStatus {
    DISCOVERED,
    VERIFIED,
    APPLIED,
    DEFAULT
}

enum class RiskLevel(val label: String) {
    SAFE("Safe / Low Risk"),
    MODERATE("Moderate / Monitor"),
    CAUTION("Caution / Verify Rollback")
}

data class RouterSetting(
    val id: String,
    val title: String,
    val category: SettingCategory,
    val romanUrduExplanation: String,
    val englishExplanation: String,
    val checkCommand: String,
    val applyCommand: String,
    val defaultFallbackRestoreCommand: String,
    var originalValue: String? = null,
    var currentValue: String = "Unknown",
    val recommendedValue: String = "",
    var status: TweakStatus = TweakStatus.DISCOVERED,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val gamingImpact: String = "",
    var isVerifiedRestore: Boolean = false,
    var lastOutput: String = ""
) {
    val activeRestoreCommand: String
        get() = if (!originalValue.isNullOrBlank()) {
            when (id) {
                "sipa_txqueuelen" -> "ip link set sipa_eth0 txqueuelen $originalValue"
                "tcp_congestion" -> "sysctl -w net.ipv4.tcp_congestion_control=$originalValue"
                "tcp_low_latency" -> "sysctl -w net.ipv4.tcp_low_latency=$originalValue"
                "tcp_autocorking" -> "sysctl -w net.ipv4.tcp_autocorking=$originalValue"
                "irq28_affinity" -> "echo $originalValue > /proc/irq/28/smp_affinity"
                "irq22_affinity" -> "echo $originalValue > /proc/irq/22/smp_affinity"
                "netdev_backlog" -> "sysctl -w net.core.netdev_max_backlog=$originalValue"
                "vfs_cache_pressure" -> "sysctl -w vm.vfs_cache_pressure=$originalValue"
                "swappiness" -> "sysctl -w vm.swappiness=$originalValue"
                "rps_cpus" -> "echo $originalValue > /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus"
                else -> defaultFallbackRestoreCommand
            }
        } else {
            defaultFallbackRestoreCommand
        }
}
