package org.echoosx.mirai.plugin

import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.info
import org.echoosx.mirai.plugin.AnisubConfig.interval
import org.echoosx.mirai.plugin.AnisubConfig.rssPrefix
import org.echoosx.mirai.plugin.command.SubscribeManage
import org.echoosx.mirai.plugin.data.AnisubSubscribe
import org.echoosx.mirai.plugin.data.AnisubSubscribe.record
import util.buildMessage
import util.checkUpdate
import util.getLatestChapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object Anisub : KotlinPlugin(
    JvmPluginDescription(
        id = "org.echoosx.mirai.plugin.anisub-helper",
        name = "anisub-helper",
        version = "0.1.3"
    ) {
        author("Echoosx")
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
        //配置文件目录 "${dataFolder.absolutePath}/"

        AnisubSubscribe.reload()
        AnisubConfig.reload()
        SubscribeManage.register()

        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeOnce<BotOnlineEvent> {
            val checkUpdateTimer = object : TimerTask() {
                override fun run() {
                    this@Anisub.launch {
                        record.forEach {
                            val rssUrl = rssPrefix + it.key
                            if(checkUpdate(rssUrl, it.value.latest)){
                                val bangumi = getLatestChapter(rssUrl)
                                val message = buildMessage(bangumi)
                                it.value.contacts.forEach{
                                    val contact = bot.getGroup(it)
                                    contact?.sendMessage(message)
                                }
                                it.value.latest = bangumi.chapterLink!!
                                it.value.updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EE yyyy/MM/dd HH:mm",Locale.CHINA))
//                                logger.info(connectHttpGet(rssUrl))
                            }
                        }
                    }
                }
            }
            try{
                Timer().schedule(checkUpdateTimer, Date(), interval * 60 * 1000)
            }catch (e:Throwable){
                logger.error("AGE番剧订阅获取失败\n$e")
            }
        }
    }
}
