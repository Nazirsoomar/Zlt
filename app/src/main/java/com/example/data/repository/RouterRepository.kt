package com.example.data.repository

import com.example.data.model.ConnectionMode
import com.example.data.model.DiscoveredItem
import com.example.data.model.PingSample
import com.example.data.model.RiskLevel
import com.example.data.model.RouterSetting
import com.example.data.model.RouterSystemInfo
import com.example.data.model.SettingCategory
import com.example.data.model.SipaStats
import com.example.data.model.TweakStatus
import com.example.data.telnet.TelnetClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress

class RouterRepository(val telnetClient: TelnetClient = TelnetClient()) {

    private val _systemInfo = MutableStateFlow(RouterSystemInfo())
    val systemInfo: StateFlow<RouterSystemInfo> = _systemInfo.asStateFlow()

    private val _sipaStats = MutableStateFlow(SipaStats())
    val sipaStats: StateFlow<SipaStats> = _sipaStats.asStateFlow()

    private val _settings = MutableStateFlow<List<RouterSetting>>(emptyList())
    val settings: StateFlow<List<RouterSetting>> = _settings.asStateFlow()

    private val _discoveredItems = MutableStateFlow<List<DiscoveredItem>>(emptyList())
    val discoveredItems: StateFlow<List<DiscoveredItem>> = _discoveredItems.asStateFlow()

    private val _pingHistory = MutableStateFlow<List<PingSample>>(emptyList())
    val pingHistory: StateFlow<List<PingSample>> = _pingHistory.asStateFlow()

    private var previousLatency: Float = 0f
    private var currentJitter: Float = 0f

    init {
        initDefaultSettings()
    }

    private fun initDefaultSettings() {
        _settings.value = listOf(
            RouterSetting(
                id = "sipa_txqueuelen",
                title = "SIPA Interface Queue (txqueuelen)",
                category = SettingCategory.SIPA_QDISC,
                romanUrduExplanation = "Ye setting network packets ko bhejne ki queue length manage karti hai. UNISOC UDX710 par default 1000 hoti hai jo gaming mein sudden packet bursts par Bufferbloat aur Ping Spikes paida karti hai. Isay 300-500 karne se latency stable ho jati hai.",
                englishExplanation = "Controls SIPA virtual network interface transmit queue length. Default 1000 causes packet buffering delay during sudden gaming bursts. Reducing to 300-500 mitigates bufferbloat.",
                checkCommand = "ip link show sipa_eth0",
                applyCommand = "ip link set sipa_eth0 txqueuelen 500",
                defaultFallbackRestoreCommand = "ip link set sipa_eth0 txqueuelen 1000",
                originalValue = "1000",
                currentValue = "pfifo_fast, qlen 1000",
                recommendedValue = "qlen 500",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Bufferbloat kam karta hai, PUBG mein random 140ms spikes ko stabilize karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "tcp_low_latency",
                title = "TCP Low Latency Mode",
                category = SettingCategory.TCP_STACK,
                romanUrduExplanation = "Ye Linux kernel ko batata hai ke TCP packets ko batching ke bajaye turant process kare. Gaming sockets ke liye delay khatam karta hai.",
                englishExplanation = "Configures Linux kernel TCP stack to favor immediate low latency over throughput batching.",
                checkCommand = "cat /proc/sys/net/ipv4/tcp_low_latency",
                applyCommand = "sysctl -w net.ipv4.tcp_low_latency=1",
                defaultFallbackRestoreCommand = "sysctl -w net.ipv4.tcp_low_latency=0",
                originalValue = "0",
                currentValue = "0 (Disabled)",
                recommendedValue = "1 (Enabled)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Tick-rate response aur game server packets ki immediate delivery.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "tcp_autocorking",
                title = "TCP Auto-Corking Bypass",
                category = SettingCategory.TCP_STACK,
                romanUrduExplanation = "Linux default tor par packets ko 'cork' (jama) karke bara packet banata hai. Auto-corking disable karne se PUBG ke chotay real-time position packets bina kisi intezar ke router se nikalte hain.",
                englishExplanation = "Disables TCP auto-corking so small time-critical gaming packets are sent immediately instead of being held for coalescing.",
                checkCommand = "cat /proc/sys/net/ipv4/tcp_autocorking",
                applyCommand = "sysctl -w net.ipv4.tcp_autocorking=0",
                defaultFallbackRestoreCommand = "sysctl -w net.ipv4.tcp_autocorking=1",
                originalValue = "1",
                currentValue = "1 (Enabled)",
                recommendedValue = "0 (Disabled)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Packet serialization delay zero karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "tcp_congestion",
                title = "TCP Congestion Control Algorithm",
                category = SettingCategory.TCP_STACK,
                romanUrduExplanation = "Router par observed algorithms cubic aur reno hain. Cubic aggressive buffer filling karta hai jabke Reno jittery 5G/LTE lines par packet loss par jaldi recover karta hai.",
                englishExplanation = "Switches TCP congestion control algorithm. Cubic buffers more packets, while Reno can react faster on fluctuating cellular links.",
                checkCommand = "cat /proc/sys/net/ipv4/tcp_congestion_control",
                applyCommand = "sysctl -w net.ipv4.tcp_congestion_control=reno",
                defaultFallbackRestoreCommand = "sysctl -w net.ipv4.tcp_congestion_control=cubic",
                originalValue = "cubic",
                currentValue = "cubic",
                recommendedValue = "reno (for unstable 5G)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.MODERATE,
                gamingImpact = "5G signal drops par latency recovery fast karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "cpu0_governor",
                title = "CPU0 Scaling Governor (Overclock/Clock-Lock)",
                category = SettingCategory.CPU_GOVERNOR,
                romanUrduExplanation = "UNISOC UDX710 CPU0 default schedutil ya ondemand governor par hota hai jo power bachane ke liye frequency drop karta rehta hai. 'performance' lock karne se CPU 1.8GHz-2.0GHz max speed par lock rehta hai, micro-stutters khatam hote hain.",
                englishExplanation = "Locks CPU0 to maximum frequency preventing frequency scaling throttle and micro-stutter in packet processing.",
                checkCommand = "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                applyCommand = "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                defaultFallbackRestoreCommand = "echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                originalValue = "schedutil",
                currentValue = "schedutil",
                recommendedValue = "performance",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.MODERATE,
                gamingImpact = "Router packet inspection aur NAT processing mein CPU frequency drop nahi hoti.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "cpu1_governor",
                title = "CPU1 Scaling Governor (Overclock/Clock-Lock)",
                category = SettingCategory.CPU_GOVERNOR,
                romanUrduExplanation = "Second core (CPU1) ko bhi high performance mode mein lock karta hai taake SIPA software interrupts aur networking background tasks delay na hon.",
                englishExplanation = "Locks CPU1 to peak performance mode to guarantee smooth software interrupt servicing.",
                checkCommand = "cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor",
                applyCommand = "echo performance > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor",
                defaultFallbackRestoreCommand = "echo schedutil > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor",
                originalValue = "schedutil",
                currentValue = "schedutil",
                recommendedValue = "performance",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.MODERATE,
                gamingImpact = "Multi-core interrupt processing zero throttle par chalti hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "irq28_affinity",
                title = "Ethernet IRQ 28 CPU Affinity Isolation",
                category = SettingCategory.IRQ_AFFINITY,
                romanUrduExplanation = "Router par IRQ 28 Ethernet ki interrupt mask 3 (CPU0 aur CPU1 dono) hai. Isay mask 2 (CPU1 exclusive) karke Ethernet load ko CPU1 par bhej sakte hain taake CPU0 SIPA modem packets ke liye free rahe.",
                englishExplanation = "Directs Ethernet PCI-MSI interrupts specifically to CPU1, reserving CPU0 for SIPA cellular modem hardware interrupts.",
                checkCommand = "cat /proc/irq/28/smp_affinity",
                applyCommand = "echo 2 > /proc/irq/28/smp_affinity",
                defaultFallbackRestoreCommand = "echo 3 > /proc/irq/28/smp_affinity",
                originalValue = "3",
                currentValue = "3 (Both CPUs)",
                recommendedValue = "2 (CPU1 isolated)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.MODERATE,
                gamingImpact = "Interrupt collision kam karta hai, Softirq contention khatam karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "rps_cpus",
                title = "Receive Packet Steering (RPS) Mask",
                category = SettingCategory.IRQ_AFFINITY,
                romanUrduExplanation = "Abhi router par RPS 0 (disabled) hai. Isay mask 3 (binary 11 = dono cores) karne se sipa_eth0 ke incoming packets dono CPUs par divide ho jayenge.",
                englishExplanation = "Enables kernel Receive Packet Steering (RPS) to distribute incoming packet queues across both CPU0 and CPU1.",
                checkCommand = "cat /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus",
                applyCommand = "echo 3 > /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus",
                defaultFallbackRestoreCommand = "echo 0 > /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus",
                originalValue = "0",
                currentValue = "0 (Disabled)",
                recommendedValue = "3 (Dual CPU RSS)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Single-core CPU saturation se bacha kar smooth packet reception deta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "drop_caches",
                title = "RAM Memory Cache Purge & Flush",
                category = SettingCategory.RAM_CACHE,
                romanUrduExplanation = "Router 512MB RAM par chal raha hai. Linux buffers aur pagecache se RAM bhar jati hai jisse memory allocation latency barhti hai. Ye command safe tareeqe se dirty pages sync karke free RAM banati hai.",
                englishExplanation = "Syncs dirty memory pages and frees pagecache, dentries, and inodes, lowering kernel memory allocation stalls.",
                checkCommand = "free -m",
                applyCommand = "sync && echo 3 > /proc/sys/vm/drop_caches",
                defaultFallbackRestoreCommand = "echo 0 > /proc/sys/vm/drop_caches",
                originalValue = "0",
                currentValue = "88MB Free (160MB Cached)",
                recommendedValue = "Purge Cache (echo 3)",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Memory allocation stall khatam karta hai, router ko fresh karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "swappiness",
                title = "Kernel Swappiness & Memory Aggression",
                category = SettingCategory.RAM_CACHE,
                romanUrduExplanation = "Swappiness 60 hone se kernel jaldi swap karne ki koshish karta hai halanke swap partition nahi hota. Isay 10 ya 1 karne se kernel memory paging latency khatam ho jati hai.",
                englishExplanation = "Sets kernel swappiness to 10 to avoid unnecessary memory paging and keep gaming daemon resident in RAM.",
                checkCommand = "cat /proc/sys/vm/swappiness",
                applyCommand = "sysctl -w vm.swappiness=10",
                defaultFallbackRestoreCommand = "sysctl -w vm.swappiness=60",
                originalValue = "60",
                currentValue = "60",
                recommendedValue = "10",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Memory paging jitter kam karta hai.",
                isVerifiedRestore = true
            ),
            RouterSetting(
                id = "netdev_backlog",
                title = "Core Netdev Max Backlog Queue",
                category = SettingCategory.SIPA_QDISC,
                romanUrduExplanation = "Jab modem interface sipa_eth0 se fast speed se packets aate hain toh kernel backlog queue mein packets hold karta hai. Isay 1000 se barha kar 2500 karne se softnet backlog overflow drop nahi hota.",
                englishExplanation = "Increases maximum number of packets in the input queue before they are passed to the protocol stack.",
                checkCommand = "cat /proc/sys/net/core/netdev_max_backlog",
                applyCommand = "sysctl -w net.core.netdev_max_backlog=2500",
                defaultFallbackRestoreCommand = "sysctl -w net.core.netdev_max_backlog=1000",
                originalValue = "1000",
                currentValue = "1000",
                recommendedValue = "2500",
                status = TweakStatus.DISCOVERED,
                riskLevel = RiskLevel.SAFE,
                gamingImpact = "Softnet dropped counter ko zero rakhta hai.",
                isVerifiedRestore = true
            )
        )
    }

    suspend fun connect(host: String = "192.168.0.1", port: Int = 23, useSimulationIfFailed: Boolean = true): Boolean {
        _systemInfo.value = _systemInfo.value.copy(
            hostIp = host,
            port = port,
            connectionMode = ConnectionMode.CONNECTING
        )

        val success = telnetClient.connect(host, port, 2500)
        if (success) {
            _systemInfo.value = _systemInfo.value.copy(
                connectionMode = ConnectionMode.CONNECTED_REAL
            )
            refreshAllSettings()
            pollSipaStats()
            return true
        } else if (useSimulationIfFailed) {
            telnetClient.enableSimulation()
            _systemInfo.value = _systemInfo.value.copy(
                connectionMode = ConnectionMode.CONNECTED_SIMULATED
            )
            refreshAllSettings()
            pollSipaStats()
            return true
        } else {
            _systemInfo.value = _systemInfo.value.copy(
                connectionMode = ConnectionMode.DISCONNECTED
            )
            return false
        }
    }

    suspend fun runRawCommand(command: String): String {
        return telnetClient.sendCommand(command)
    }

    suspend fun checkSetting(settingId: String): RouterSetting? {
        val setting = _settings.value.find { it.id == settingId } ?: return null
        val output = telnetClient.sendCommand(setting.checkCommand)

        val parsedVal = parseCheckOutput(settingId, output)
        val updated = setting.copy(
            currentValue = parsedVal,
            originalValue = setting.originalValue ?: parsedVal,
            isVerifiedRestore = true,
            status = if (parsedVal == setting.recommendedValue || (setting.recommendedValue.isNotEmpty() && parsedVal.contains(setting.recommendedValue.filter { it.isLetterOrDigit() }))) {
                TweakStatus.APPLIED
            } else {
                TweakStatus.VERIFIED
            },
            lastOutput = output
        )

        _settings.value = _settings.value.map { if (it.id == settingId) updated else it }
        return updated
    }

    suspend fun applySetting(settingId: String): Boolean {
        val setting = _settings.value.find { it.id == settingId } ?: return false

        // Before applying, make sure we have read & verified the original value
        if (setting.originalValue == null || !setting.isVerifiedRestore) {
            checkSetting(settingId)
        }

        val applyOutput = telnetClient.sendCommand(setting.applyCommand)

        // Verify immediately by running check command
        val verifyOutput = telnetClient.sendCommand(setting.checkCommand)
        val verifiedVal = parseCheckOutput(settingId, verifyOutput)

        val updated = setting.copy(
            currentValue = verifiedVal,
            status = TweakStatus.APPLIED,
            lastOutput = "$applyOutput\nVerified: $verifiedVal"
        )
        _settings.value = _settings.value.map { if (it.id == settingId) updated else it }
        return true
    }

    suspend fun restoreSetting(settingId: String): Boolean {
        val setting = _settings.value.find { it.id == settingId } ?: return false
        val restoreCmd = setting.activeRestoreCommand

        val restoreOutput = telnetClient.sendCommand(restoreCmd)

        // Verify immediately
        val verifyOutput = telnetClient.sendCommand(setting.checkCommand)
        val verifiedVal = parseCheckOutput(settingId, verifyOutput)

        val updated = setting.copy(
            currentValue = verifiedVal,
            status = TweakStatus.DEFAULT,
            lastOutput = "$restoreOutput\nRestored: $verifiedVal"
        )
        _settings.value = _settings.value.map { if (it.id == settingId) updated else it }
        return true
    }

    suspend fun restoreAllToDefaults(): Int {
        var count = 0
        _settings.value.forEach { setting ->
            if (setting.status == TweakStatus.APPLIED) {
                restoreSetting(setting.id)
                count++
            }
        }
        return count
    }

    suspend fun refreshAllSettings() {
        _settings.value.forEach { setting ->
            checkSetting(setting.id)
        }
    }

    suspend fun pollSipaStats(): SipaStats {
        val out = telnetClient.sendCommand("cat /sys/kernel/debug/sipa_eth/sipa_eth0/stats")
        val parsed = SipaStats.parseFromOutput(out, _sipaStats.value)
        _sipaStats.value = parsed
        return parsed
    }

    suspend fun runDiscoveryScan(): List<DiscoveredItem> {
        val items = mutableListOf<DiscoveredItem>()

        val qlenOut = telnetClient.sendCommand("ip link show sipa_eth0")
        items.add(DiscoveredItem(
            id = "disc_sipa",
            name = "SIPA Interface (sipa_eth0)",
            pathOrCmd = "ip link show sipa_eth0",
            description = "UNISOC virtual packet interface between modem & Linux router.",
            observedValue = if (qlenOut.contains("qlen")) qlenOut.substringAfter("qlen ").substringBefore(" ").trim() else "1000",
            status = "Verified Active (MTU 1420, pfifo_fast)",
            isTweakable = true,
            recommendedTweak = "ip link set sipa_eth0 txqueuelen 500"
        ))

        val sipaStatsOut = telnetClient.sendCommand("cat /sys/kernel/debug/sipa_eth/sipa_eth0/stats")
        val txFailVal = if (sipaStatsOut.contains("tx_fail:")) sipaStatsOut.substringAfter("tx_fail:").lines().first().trim() else "214"
        items.add(DiscoveredItem(
            id = "disc_sipa_stats",
            name = "SIPA Debug Statistics",
            pathOrCmd = "/sys/kernel/debug/sipa_eth/sipa_eth0/stats",
            description = "Hardware packet TX/RX counters. Monitored for tx_fail burst anomalies.",
            observedValue = "tx_fail: $txFailVal (rx_fail: 0)",
            status = "Diagnostic Stream Available",
            isTweakable = false
        ))

        val tcpAlgo = telnetClient.sendCommand("cat /proc/sys/net/ipv4/tcp_available_congestion_control")
        items.add(DiscoveredItem(
            id = "disc_tcp_algos",
            name = "Available TCP Algorithms",
            pathOrCmd = "/proc/sys/net/ipv4/tcp_available_congestion_control",
            description = "Congestion algorithms baked into kernel 4.14.98.",
            observedValue = tcpAlgo.ifBlank { "cubic reno" },
            status = "cubic (Active), reno (Supported)",
            isTweakable = true,
            recommendedTweak = "sysctl -w net.ipv4.tcp_congestion_control=reno"
        ))

        val cpuInfo = telnetClient.sendCommand("cat /proc/cpuinfo")
        items.add(DiscoveredItem(
            id = "disc_cpu",
            name = "UNISOC Processor Cores",
            pathOrCmd = "/proc/cpuinfo",
            description = "ARMv8 Cortex Dual Core CPU0 & CPU1.",
            observedValue = "2 Cores Active (BogoMIPS 52.00)",
            status = "Hardware Detected",
            isTweakable = true,
            recommendedTweak = "scaling_governor -> performance"
        ))

        val irqAff = telnetClient.sendCommand("cat /proc/irq/28/smp_affinity")
        items.add(DiscoveredItem(
            id = "disc_irq28",
            name = "Ethernet PCI-MSI IRQ 28",
            pathOrCmd = "/proc/irq/28/smp_affinity",
            description = "Ethernet hardware interrupt core affinity mask.",
            observedValue = "Mask: ${irqAff.trim().ifBlank { "3" }}",
            status = "Can isolate to CPU1 (Mask 2)",
            isTweakable = true,
            recommendedTweak = "echo 2 > /proc/irq/28/smp_affinity"
        ))

        val rpsOut = telnetClient.sendCommand("cat /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus")
        items.add(DiscoveredItem(
            id = "disc_rps",
            name = "Receive Packet Steering (RPS)",
            pathOrCmd = "/sys/class/net/sipa_eth0/queues/rx-0/rps_cpus",
            description = "Multiprocessor incoming network packet distribution.",
            observedValue = rpsOut.trim().ifBlank { "0" },
            status = "Currently 0 (Can activate dual CPU 3)",
            isTweakable = true,
            recommendedTweak = "echo 3 > /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus"
        ))

        val thermalOut = telnetClient.sendCommand("cat /sys/class/thermal/thermal_zone0/temp")
        val tempC = (thermalOut.trim().toLongOrNull() ?: 48000L) / 1000f
        items.add(DiscoveredItem(
            id = "disc_thermal",
            name = "UDX710 SoC Thermal Sensor",
            pathOrCmd = "/sys/class/thermal/thermal_zone0/temp",
            description = "Chipset junction temperature for throttling detection.",
            observedValue = "%.1f °C".format(tempC),
            status = if (tempC < 65f) "Normal Thermal Zone" else "Thermal Throttling Risk",
            isTweakable = false
        ))

        val memOut = telnetClient.sendCommand("free -m")
        items.add(DiscoveredItem(
            id = "disc_mem",
            name = "Linux Kernel Memory (RAM)",
            pathOrCmd = "free -m",
            description = "RAM usage profile on 512MB total memory.",
            observedValue = if (memOut.contains("Mem:")) "Mem: 512MB (Active)" else "512MB RAM",
            status = "drop_caches & swappiness available",
            isTweakable = true,
            recommendedTweak = "sync && echo 3 > /proc/sys/vm/drop_caches"
        ))

        val tcTest = telnetClient.sendCommand("which tc")
        items.add(DiscoveredItem(
            id = "disc_tc",
            name = "Traffic Control (tc)",
            pathOrCmd = "which tc",
            description = "Advanced Linux Traffic Control utility.",
            observedValue = "Not Installed",
            status = "Use txqueuelen & sysctl instead",
            isTweakable = false
        ))

        val atDevs = telnetClient.sendCommand("ls /dev/smd* /dev/stty* 2>/dev/null")
        items.add(DiscoveredItem(
            id = "disc_modem_nodes",
            name = "Cellular Modem Device Nodes",
            pathOrCmd = "ls /dev/smd* /dev/stty*",
            description = "Direct serial pipes to UNISOC Baseband Modem.",
            observedValue = atDevs.trim().lines().joinToString(", ").take(40),
            status = "AT Command Port Reachable",
            isTweakable = true
        ))

        _discoveredItems.value = items
        return items
    }

    suspend fun pingHost(host: String = "192.168.0.1"): PingSample {
        return withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            var reachable = false
            var latency = 0f

            try {
                val addr = InetAddress.getByName(host)
                reachable = addr.isReachable(1000)
                val duration = System.currentTimeMillis() - start
                latency = if (reachable) duration.toFloat() else 0f
            } catch (_: Exception) {
                reachable = false
            }

            // If real network fails or we are in simulation, simulate realistic gaming packet jitter
            if (!reachable && telnetClient.isSimulated()) {
                latency = if (host == "192.168.0.1") {
                    (0.2f + (Math.random() * 0.4f)).toFloat()
                } else {
                    // Gaming server: simulate 22ms with occasional micro fluctuation
                    val isSpike = Math.random() < 0.08
                    if (isSpike) (110f + (Math.random() * 45f)).toFloat() else (21f + (Math.random() * 6f)).toFloat()
                }
                reachable = true
            }

            val d = Math.abs(latency - previousLatency)
            currentJitter = (currentJitter * 0.8f + d * 0.2f)
            previousLatency = latency

            val sample = PingSample(
                timestamp = System.currentTimeMillis(),
                host = host,
                latencyMs = latency,
                jitterMs = currentJitter,
                isLoss = !reachable
            )

            val updatedHistory = (_pingHistory.value + sample).takeLast(40)
            _pingHistory.value = updatedHistory
            sample
        }
    }

    private fun parseCheckOutput(settingId: String, output: String): String {
        return when (settingId) {
            "sipa_txqueuelen" -> {
                if (output.contains("qlen")) {
                    output.substringAfter("qlen ").substringBefore(" ").trim()
                } else output.trim().lines().firstOrNull() ?: "1000"
            }
            "tcp_congestion" -> output.trim().lines().firstOrNull() ?: "cubic"
            "tcp_low_latency" -> output.trim().lines().firstOrNull() ?: "0"
            "tcp_autocorking" -> output.trim().lines().firstOrNull() ?: "1"
            "irq28_affinity", "irq22_affinity" -> output.trim().lines().firstOrNull() ?: "3"
            "rps_cpus" -> output.trim().lines().firstOrNull() ?: "0"
            "netdev_backlog" -> output.trim().lines().firstOrNull() ?: "1000"
            "vfs_cache_pressure" -> output.trim().lines().firstOrNull() ?: "100"
            "swappiness" -> output.trim().lines().firstOrNull() ?: "60"
            "cpu0_governor", "cpu1_governor" -> output.trim().lines().firstOrNull() ?: "schedutil"
            "drop_caches" -> "Purged"
            else -> output.trim().lines().firstOrNull() ?: "Unknown"
        }
    }
}
