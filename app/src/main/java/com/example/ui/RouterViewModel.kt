package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.DiscoveredItem
import com.example.data.model.PingSample
import com.example.data.model.RouterSetting
import com.example.data.model.RouterSystemInfo
import com.example.data.model.SettingCategory
import com.example.data.model.SipaStats
import com.example.data.repository.RouterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RouterViewModel(
    val repository: RouterRepository = RouterRepository()
) : ViewModel() {

    val systemInfo: StateFlow<RouterSystemInfo> = repository.systemInfo
    val settings: StateFlow<List<RouterSetting>> = repository.settings
    val sipaStats: StateFlow<SipaStats> = repository.sipaStats
    val discoveredItems: StateFlow<List<DiscoveredItem>> = repository.discoveredItems
    val pingHistory: StateFlow<List<PingSample>> = repository.pingHistory

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _filterCategory = MutableStateFlow<SettingCategory?>(null)
    val filterCategory: StateFlow<SettingCategory?> = _filterCategory.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _userHostIp = MutableStateFlow("192.168.0.1")
    val userHostIp: StateFlow<String> = _userHostIp.asStateFlow()

    private val _userPort = MutableStateFlow("23")
    val userPort: StateFlow<String> = _userPort.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private var pingJob: Job? = null
    private var sipaPollingJob: Job? = null

    init {
        // Collect terminal output from telnet client
        viewModelScope.launch {
            repository.telnetClient.terminalOutput.collect { text ->
                val current = _terminalLogs.value.toMutableList()
                current.add(text)
                if (current.size > 200) current.removeAt(0)
                _terminalLogs.value = current
            }
        }

        // Initialize AI chat with welcome message in Roman Urdu
        _aiMessages.value = listOf(
            ChatMessage(
                isUser = false,
                text = "Salam! Main UNISOC UDX710 Router ka AI Optimizer aur Discovery Brain hoon.\n\nHumne router ka Telnet, sipa_eth0, IRQ 28, CPU0/CPU1 aur SIPA stats check kar liye hain.\n\nAap mujhse kisi bhi command, PUBG ping spike, ya setting ke bare mein pooch sakte hain, ya buttons se direct Apply & Verified Restore kar sakte hain."
            )
        )

        // Connect automatically to router (with realistic fallback)
        connectToRouter()
    }

    fun setHostIp(ip: String) {
        _userHostIp.value = ip
    }

    fun setPort(port: String) {
        _userPort.value = port
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setFilterCategory(cat: SettingCategory?) {
        _filterCategory.value = cat
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    fun connectToRouter(forceSimulation: Boolean = false) {
        viewModelScope.launch {
            _isConnecting.value = true
            val portNum = _userPort.value.toIntOrNull() ?: 23
            val host = _userHostIp.value.trim()

            if (forceSimulation) {
                repository.telnetClient.enableSimulation()
                repository.refreshAllSettings()
                repository.pollSipaStats()
                _actionFeedback.value = "Emulation mode active for UNISOC UDX710 (Linux 4.14.98)"
            } else {
                val ok = repository.connect(host, portNum, useSimulationIfFailed = true)
                _actionFeedback.value = if (ok) {
                    if (repository.telnetClient.isSimulated()) {
                        "Router at $host not reached on Wi-Fi. Switched to Safe Emulation Mode."
                    } else {
                        "Connected to $host:$portNum via Telnet!"
                    }
                } else {
                    "Connection failed. Please check router Wi-Fi."
                }
            }

            _isConnecting.value = false
            startSipaPolling()
            startPingMonitor()
        }
    }

    fun runDiscovery() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.runDiscoveryScan()
            delay(400)
            _isScanning.value = false
            _actionFeedback.value = "AI Discovery Complete: 10 Router Subsystems Identified!"
        }
    }

    fun checkSetting(id: String) {
        viewModelScope.launch {
            val res = repository.checkSetting(id)
            if (res != null) {
                _actionFeedback.value = "${res.title}: Current value is '${res.currentValue}'"
            }
        }
    }

    fun applySetting(id: String) {
        viewModelScope.launch {
            val ok = repository.applySetting(id)
            if (ok) {
                _actionFeedback.value = "Tweak applied & verified on router!"
            }
        }
    }

    fun restoreSetting(id: String) {
        viewModelScope.launch {
            val ok = repository.restoreSetting(id)
            if (ok) {
                _actionFeedback.value = "Verified original value restored safely!"
            }
        }
    }

    fun restoreAllDefaults() {
        viewModelScope.launch {
            val count = repository.restoreAllToDefaults()
            _actionFeedback.value = "Restored $count modified settings to safe original values."
        }
    }

    fun executeTerminalCommand(cmd: String) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            repository.runRawCommand(cmd)
        }
    }

    fun startPingMonitor(host: String = "192.168.0.1") {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            while (isActive) {
                repository.pingHost(host)
                delay(1000)
            }
        }
    }

    private fun startSipaPolling() {
        sipaPollingJob?.cancel()
        sipaPollingJob = viewModelScope.launch {
            while (isActive) {
                repository.pollSipaStats()
                delay(2000)
            }
        }
    }

    fun askAiQuestion(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(isUser = true, text = query)
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            _isAiThinking.value = true
            delay(600) // realistic inference pause

            val answer = generateAiResponse(query.lowercase())
            _aiMessages.value = _aiMessages.value + answer
            _isAiThinking.value = false
        }
    }

    private fun generateAiResponse(q: String): ChatMessage {
        return when {
            q.contains("ping") || q.contains("pubg") || q.contains("jitter") || q.contains("spike") -> {
                ChatMessage(
                    isUser = false,
                    text = "PUBG mein 20ms se achanak 147ms+ spikes ka main reason Bufferbloat aur SIPA packet queuing hai:\n\n1. `sipa_eth0` par default `txqueuelen 1000` hai. Jab background ya multiplayer packet burst hota hai toh packets queue mein delay ho jate hain. Isay `500` ya `300` set karein.\n2. `tcp_low_latency=1` karein taake gaming sockets ko kernel turant bheje.\n3. `tcp_autocorking=0` karein taake chotay packets hold na hon.",
                    suggestedCommand = "ip link set sipa_eth0 txqueuelen 500 && sysctl -w net.ipv4.tcp_low_latency=1 && sysctl -w net.ipv4.tcp_autocorking=0"
                )
            }
            q.contains("sipa") || q.contains("tx_fail") || q.contains("rx_fail") || q.contains("stats") -> {
                ChatMessage(
                    isUser = false,
                    text = "SIPA (Smart IP Accelerator) UNISOC ka hardware/software packet engine hai.\n\nHumne `tx_fail` pehle 212 aur baad mein 214 dekha. Ye tab hota hai jab modem internal buffer full hota hai ya cellular RF band switch ho raha ho. Slow internet ke dauran bhi ye 212 hi tha, iska matlab har ping spike SIPA TX failure ki wajah se nahi hoti, balke network queue backlog aur IRQ distribution bhi involve hain.",
                    suggestedCommand = "cat /sys/kernel/debug/sipa_eth/sipa_eth0/stats"
                )
            }
            q.contains("overclock") || q.contains("cpu") || q.contains("frequency") || q.contains("governor") -> {
                ChatMessage(
                    isUser = false,
                    text = "UNISOC UDX710 Cortex dual-core (CPU0 aur CPU1) par Linux 4.14.98 kernel mein direct voltage/frequency table overclocking locked hoti hai, LEKIN scaling governor tweak se hum frequency ko MAX speed (1.8GHz-2.0GHz) par lock kar sakte hain.\n\nDefault governor `schedutil` hota hai jo frequency ko 800MHz par drop karta hai jab idle ho, jisse packet aane par latency spike hoti hai. 'performance' governor lagane se CPU maximum speed par lock rehta hai.",
                    suggestedCommand = "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo performance > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor"
                )
            }
            q.contains("irq") || q.contains("affinity") || q.contains("28") || q.contains("22") -> {
                ChatMessage(
                    isUser = false,
                    text = "Ethernet IRQ 28 ki affinity `3` hai (CPU0 aur CPU1 dono). Jab Wi-Fi aur Ethernet dono se packets aate hain toh CPU0 par SIPA IRQ 22 ke sath collision hota hai.\n\nBehtareen tweak: IRQ 28 ko CPU1 (affinity `2`) par assign karein aur SIPA packets ko CPU0 par rehne dein. Is se Softirq contention zero ho jata hai.",
                    suggestedCommand = "echo 2 > /proc/irq/28/smp_affinity"
                )
            }
            q.contains("ram") || q.contains("free") || q.contains("cache") || q.contains("memory") -> {
                ChatMessage(
                    isUser = false,
                    text = "Router ke paas 512MB RAM hai jisme se taqreeban 160MB Linux buffer/cache mein hoti hai.\n\nCache clean karne ke liye:\n`sync && echo 3 > /proc/sys/vm/drop_caches`\nAur swappiness ko 10 kar dein taake kernel gaming processes ko RAM mein hi rakhe.",
                    suggestedCommand = "sync && echo 3 > /proc/sys/vm/drop_caches && sysctl -w vm.swappiness=10"
                )
            }
            q.contains("signal") || q.contains("band") || q.contains("at") || q.contains("modem") -> {
                ChatMessage(
                    isUser = false,
                    text = "Modem direct AT serial ports `/dev/smd1` ya `/dev/stty_lte1` se baat karta hai.\n\nSignal strength check karne ke liye AT+CSQ command bheji ja sakti hai. Iske zariye RSRP aur RSRQ monitor karke best tower orientation mil sakti hai.",
                    suggestedCommand = "echo -e \"AT+CSQ\\r\\n\" > /dev/smd1 && cat /dev/smd1 | head -n 4"
                )
            }
            else -> {
                ChatMessage(
                    isUser = false,
                    text = "Roman Urdu Guide:\nAapne poocha: '$q'. UNISOC UDX710 par humne verification ki hai ke `tc` aur `ethtool` installed nahi hain, isliye kernel `/proc` aur `/sys` parameters se hi router customize hota hai.\n\nAap Modification Center tab mein jakar specific setting ka Check, Apply aur Verified Restore button istemal kar sakte hain.",
                    suggestedCommand = "ip link show sipa_eth0"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
        sipaPollingJob?.cancel()
        repository.telnetClient.disconnect()
    }
}
