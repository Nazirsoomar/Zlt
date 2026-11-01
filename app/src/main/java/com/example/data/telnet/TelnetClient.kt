package com.example.data.telnet

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TelnetClient {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val _terminalOutput = MutableSharedFlow<String>(replay = 50)
    val terminalOutput: SharedFlow<String> = _terminalOutput.asSharedFlow()

    private var isConnected = false
    private var isSimulation = false

    // Simulated router environment state for UNISOC UDX710 Linux 4.14.98
    private val simulatedState = mutableMapOf(
        "sipa_txqueuelen" to "1000",
        "tcp_congestion" to "cubic",
        "tcp_low_latency" to "0",
        "tcp_autocorking" to "1",
        "irq28_affinity" to "3",
        "irq22_affinity" to "3",
        "netdev_backlog" to "1000",
        "vfs_cache_pressure" to "100",
        "swappiness" to "60",
        "rps_cpus" to "0",
        "cpu0_governor" to "schedutil",
        "cpu1_governor" to "schedutil"
    )

    private var simulatedTxFail = 214L
    private var simulatedRxFail = 0L

    suspend fun connect(host: String = "192.168.0.1", port: Int = 23, timeoutMs: Int = 3000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                val s = Socket()
                s.connect(InetSocketAddress(host, port), timeoutMs)
                s.soTimeout = 15000
                socket = s
                outputStream = s.getOutputStream()
                inputStream = s.getInputStream()
                isConnected = true
                isSimulation = false
                _terminalOutput.emit("\u001B[32m[Connected to $host:$port via Telnet]\u001B[0m\n")
                true
            } catch (e: Exception) {
                Log.w("TelnetClient", "Real connection to $host:$port failed: ${e.message}. Using safety simulation.")
                isConnected = false
                false
            }
        }
    }

    suspend fun enableSimulation() {
        disconnect()
        isSimulation = true
        isConnected = true
        _terminalOutput.emit("\u001B[33m[UNISOC UDX710 Router Emulation Active]\u001B[0m\n")
        _terminalOutput.emit("Linux unisoc-udx710 4.14.98 #1 SMP PREEMPT W22.32.3 armv8l\n")
        _terminalOutput.emit("BusyBox v1.27.2 (2022-08-14) built-in shell (ash)\n")
        _terminalOutput.emit("sh-4.4# ")
    }

    fun isConnected(): Boolean = isConnected
    fun isSimulated(): Boolean = isSimulation

    suspend fun sendCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            val trimmed = command.trim()
            _terminalOutput.emit("\u001B[36msh-4.4# $trimmed\u001B[0m\n")

            if (isSimulation || socket == null || socket?.isConnected != true) {
                val simResult = executeSimulatedCommand(trimmed)
                _terminalOutput.emit(simResult + "\nsh-4.4# ")
                return@withContext simResult
            }

            try {
                val out = outputStream ?: return@withContext "Error: No output stream"
                val inStream = inputStream ?: return@withContext "Error: No input stream"

                val cmdBytes = (trimmed + "\r\n").toByteArray(Charsets.UTF_8)
                out.write(cmdBytes)
                out.flush()

                // Read output up to prompt or short quiet period
                val sb = StringBuilder()
                val buffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < 3500) {
                    if (inStream.available() > 0) {
                        val count = inStream.read(buffer)
                        if (count > 0) {
                            val chunk = filterTelnetIac(buffer, count)
                            val str = String(chunk, Charsets.UTF_8)
                            sb.append(str)
                            _terminalOutput.emit(str)
                            if (str.contains("sh-4.4#") || str.contains("# ") || str.contains("$ ")) {
                                break
                            }
                        }
                    } else {
                        if (sb.isNotEmpty()) {
                            // give a tiny grace for any remaining bytes
                            Thread.sleep(80)
                            if (inStream.available() == 0) break
                        } else {
                            Thread.sleep(50)
                        }
                    }
                }
                sb.toString().trim()
            } catch (e: Exception) {
                Log.e("TelnetClient", "Error running command: $trimmed", e)
                val err = "Command error: ${e.message}"
                _terminalOutput.emit("\u001B[31m$err\u001B[0m\n")
                err
            }
        }
    }

    private fun filterTelnetIac(raw: ByteArray, length: Int): ByteArray {
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < length) {
            val b = raw[i].toInt() and 0xFF
            if (b == 0xFF) { // IAC
                if (i + 1 < length) {
                    val cmd = raw[i + 1].toInt() and 0xFF
                    if (cmd in 251..254) { // WILL, WONT, DO, DONT
                        i += 3
                        continue
                    } else if (cmd == 250) { // SB Subnegotiation
                        i += 2
                        while (i < length && (raw[i].toInt() and 0xFF) != 240) {
                            i++
                        }
                        i++ // skip SE
                        continue
                    }
                }
                i += 2
                continue
            }
            out.add(raw[i])
            i++
        }
        return out.toByteArray()
    }

    private fun executeSimulatedCommand(cmd: String): String {
        return when {
            cmd == "cat /sys/kernel/debug/sipa_eth/sipa_eth0/stats" -> {
                """
                RX statistics:
                rx_sum: 84920421
                rx_cnt: 84920400
                rx_fail: $simulatedRxFail
                rx_busy: 0
                rx_evt: 124809
                TX statistics:
                tx_sum: 62410884
                tx_cnt: 62410670
                tx_fail: $simulatedTxFail
                """.trimIndent()
            }
            cmd.startsWith("ip link show sipa_eth0") || cmd == "ifconfig sipa_eth0" -> {
                val qlen = simulatedState["sipa_txqueuelen"] ?: "1000"
                "3: sipa_eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1420 qdisc pfifo_fast state UP mode DEFAULT group default qlen $qlen\n    link/ether 02:42:0a:00:00:01 brd ff:ff:ff:ff:ff:ff"
            }
            cmd.startsWith("ip link set sipa_eth0 txqueuelen") -> {
                val qlen = cmd.substringAfter("txqueuelen").trim().split(" ").firstOrNull() ?: "500"
                simulatedState["sipa_txqueuelen"] = qlen
                "" // Linux ip link set is silent on success
            }
            cmd == "cat /proc/sys/net/ipv4/tcp_congestion_control" -> {
                simulatedState["tcp_congestion"] ?: "cubic"
            }
            cmd == "cat /proc/sys/net/ipv4/tcp_available_congestion_control" -> {
                "cubic reno"
            }
            cmd.startsWith("sysctl -w net.ipv4.tcp_congestion_control=") -> {
                val algo = cmd.substringAfter("=").trim()
                simulatedState["tcp_congestion"] = algo
                "net.ipv4.tcp_congestion_control = $algo"
            }
            cmd == "cat /proc/sys/net/ipv4/tcp_low_latency" -> {
                simulatedState["tcp_low_latency"] ?: "0"
            }
            cmd.startsWith("sysctl -w net.ipv4.tcp_low_latency=") -> {
                val v = cmd.substringAfter("=").trim()
                simulatedState["tcp_low_latency"] = v
                "net.ipv4.tcp_low_latency = $v"
            }
            cmd == "cat /proc/sys/net/ipv4/tcp_autocorking" -> {
                simulatedState["tcp_autocorking"] ?: "1"
            }
            cmd.startsWith("sysctl -w net.ipv4.tcp_autocorking=") -> {
                val v = cmd.substringAfter("=").trim()
                simulatedState["tcp_autocorking"] = v
                "net.ipv4.tcp_autocorking = $v"
            }
            cmd == "cat /proc/irq/28/smp_affinity" -> {
                simulatedState["irq28_affinity"] ?: "3"
            }
            cmd.startsWith("echo ") && cmd.contains("/proc/irq/28/smp_affinity") -> {
                val aff = cmd.substringAfter("echo ").substringBefore(" >").trim()
                simulatedState["irq28_affinity"] = aff
                ""
            }
            cmd == "cat /proc/irq/22/smp_affinity" -> {
                simulatedState["irq22_affinity"] ?: "3"
            }
            cmd.startsWith("echo ") && cmd.contains("/proc/irq/22/smp_affinity") -> {
                val aff = cmd.substringAfter("echo ").substringBefore(" >").trim()
                simulatedState["irq22_affinity"] = aff
                ""
            }
            cmd == "cat /proc/sys/net/core/netdev_max_backlog" -> {
                simulatedState["netdev_backlog"] ?: "1000"
            }
            cmd.startsWith("sysctl -w net.core.netdev_max_backlog=") -> {
                val v = cmd.substringAfter("=").trim()
                simulatedState["netdev_backlog"] = v
                "net.core.netdev_max_backlog = $v"
            }
            cmd == "cat /proc/sys/vm/vfs_cache_pressure" -> {
                simulatedState["vfs_cache_pressure"] ?: "100"
            }
            cmd.startsWith("sysctl -w vm.vfs_cache_pressure=") -> {
                val v = cmd.substringAfter("=").trim()
                simulatedState["vfs_cache_pressure"] = v
                "vm.vfs_cache_pressure = $v"
            }
            cmd == "cat /proc/sys/vm/swappiness" -> {
                simulatedState["swappiness"] ?: "60"
            }
            cmd.startsWith("sysctl -w vm.swappiness=") -> {
                val v = cmd.substringAfter("=").trim()
                simulatedState["swappiness"] = v
                "vm.swappiness = $v"
            }
            cmd.contains("drop_caches") -> {
                "3"
            }
            cmd == "cat /sys/class/net/sipa_eth0/queues/rx-0/rps_cpus" -> {
                simulatedState["rps_cpus"] ?: "0"
            }
            cmd.startsWith("echo ") && cmd.contains("rps_cpus") -> {
                val v = cmd.substringAfter("echo ").substringBefore(" >").trim()
                simulatedState["rps_cpus"] = v
                ""
            }
            cmd == "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor" -> {
                simulatedState["cpu0_governor"] ?: "schedutil"
            }
            cmd == "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors" -> {
                "performance powersave schedutil userspace ondemand"
            }
            cmd.contains("cpu0/cpufreq/scaling_governor") && cmd.startsWith("echo ") -> {
                val g = cmd.substringAfter("echo ").substringBefore(" >").trim()
                simulatedState["cpu0_governor"] = g
                ""
            }
            cmd == "cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor" -> {
                simulatedState["cpu1_governor"] ?: "schedutil"
            }
            cmd.contains("cpu1/cpufreq/scaling_governor") && cmd.startsWith("echo ") -> {
                val g = cmd.substringAfter("echo ").substringBefore(" >").trim()
                simulatedState["cpu1_governor"] = g
                ""
            }
            cmd == "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq" -> {
                "1820000"
            }
            cmd == "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq" -> {
                "2000000"
            }
            cmd == "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq" -> {
                "800000"
            }
            cmd == "cat /sys/class/thermal/thermal_zone0/temp" -> {
                "48000"
            }
            cmd == "cat /sys/class/thermal/thermal_zone0/type" -> {
                "tsens_tz_sensor0"
            }
            cmd == "cat /proc/cpuinfo" -> {
                """
                processor	: 0
                BogoMIPS	: 52.00
                Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 cpuid
                CPU implementer	: 0x41
                CPU architecture: 8
                CPU variant	: 0x0
                CPU part	: 0xd03
                CPU revision	: 4

                processor	: 1
                BogoMIPS	: 52.00
                Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 cpuid
                CPU implementer	: 0x41
                CPU architecture: 8
                CPU variant	: 0x0
                CPU part	: 0xd03
                CPU revision	: 4
                Hardware	: UNISOC UDX710
                """.trimIndent()
            }
            cmd == "free -m" -> {
                """
                             total        used        free      shared  buff/cache   available
                Mem:           512         264          88          12         160         228
                Swap:            0           0           0
                """.trimIndent()
            }
            cmd == "which tc" || cmd == "tc" -> {
                "sh: which: line 1: tc: not found"
            }
            cmd == "which ethtool" || cmd == "ethtool" -> {
                "sh: which: line 1: ethtool: not found"
            }
            cmd == "uname -a" -> {
                "Linux unisoc-udx710 4.14.98 #1 SMP PREEMPT Sun Aug 14 12:44:32 CST 2022 armv8l GNU/Linux"
            }
            cmd == "cat /proc/version" -> {
                "Linux version 4.14.98 (android-build@unisoc) (gcc version 4.9.x) #1 SMP PREEMPT Sun Aug 14 2022"
            }
            cmd == "brctl show" -> {
                """
                bridge name	bridge id		STP enabled	interfaces
                br0		8000.02420a000001	no		eth0
                """.trimIndent()
            }
            cmd.contains("ls /dev/smd") || cmd.contains("ls /dev/stty") || cmd.contains("ls /dev/chn") -> {
                "/dev/smd1  /dev/smd2  /dev/smd7  /dev/smd11  /dev/stty_lte1"
            }
            cmd.contains("atcmd") || cmd.contains("lte_at") -> {
                "+CSQ: 28,99\nOK"
            }
            cmd == "uptime" -> {
                " 12:44:00 up 2 days,  3:18,  load average: 0.14, 0.09, 0.05"
            }
            else -> {
                "Command executed: $cmd\nOK"
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        isConnected = false
        isSimulation = false
    }
}
