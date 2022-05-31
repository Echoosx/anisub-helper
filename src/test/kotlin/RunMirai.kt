package org.echoosx.mirai.plugin

import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader

suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()

    Anisub.load()
    Anisub.enable()

    val bot = MiraiConsole.addBot(2090578568, "Zes980437") {
        fileBasedDeviceInfo()
    }.alsoLogin()

    MiraiConsole.job.join()
}