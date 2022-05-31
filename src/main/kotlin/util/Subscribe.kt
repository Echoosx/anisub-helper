package org.echoosx.mirai.plugin.util

import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.AnisubConfig.rssPrefix
import org.echoosx.mirai.plugin.data.AnisubSubscribe.record
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import util.buildMessage
import util.checkUpdate
import util.getLatestChapter
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class Subscribe : Job {
    private val logger get() = Anisub.logger

    @Throws(JobExecutionException::class)
    override fun execute(jobExecutionContext: JobExecutionContext?) {
        try {
            Bot.instances.filter { it.isOnline }.forEach { bot ->
                bot.subscribe()
            }
        }catch (e: Exception){
            logger.error("番剧更新失败，Error:$e")
        }
    }

    private fun Bot.subscribe() = Anisub.launch {
        record.forEach {
            val rssUrl = rssPrefix + it.key
            if(checkUpdate(rssUrl, it.value.latest)){
                val bangumi = getLatestChapter(rssUrl)
                val message = buildMessage(bangumi)
                it.value.contacts.forEach{ id->
                    val contact = bot.getGroupOrFail(id)
                    contact.sendMessage(message)
                }
                it.value.latest = bangumi.chapterLink!!
                it.value.updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EE yyyy/MM/dd HH:mm",
                    Locale.CHINA))
                logger.info("now:'${it.value.latest}'")
            }
        }
    }
}