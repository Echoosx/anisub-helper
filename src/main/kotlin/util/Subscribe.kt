package org.echoosx.mirai.plugin.util

import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.Anisub.dataFolder
import org.echoosx.mirai.plugin.AnisubConfig.rssPrefix
import org.echoosx.mirai.plugin.data.AnisubSubscribe.record
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import util.buildMessage
import util.checkUpdate
import util.downloadThumbnail
import util.getLatestChapter
import java.io.File
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
        }catch (e: Throwable){
            logger.error("番剧更新失败，Error:$e")
        }
    }

    private fun Bot.subscribe() = Anisub.launch {
        record.forEach {
            val rssUrl = rssPrefix + it.key
            if(checkUpdate(rssUrl, it.value)){
                val bangumi = getLatestChapter(rssUrl)
                val thumbnail = File("${dataFolder.absolutePath}/thumbnail/${it.key}.jpg")
                if(!thumbnail.exists()){
                    downloadThumbnail(it.key)
                }

                it.value.contacts.forEach{ id->
                    val contact = bot.getGroupOrFail(id)
                    val thumbnailId = contact.uploadImage(thumbnail.toExternalResource()).imageId
                    val message = buildMessage(bangumi,thumbnailId)

                    contact.sendMessage(message)
                }
                it.value.chapterList.add(bangumi.chapterLink!!)
                it.value.updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EE yyyy/MM/dd HH:mm",
                    Locale.CHINA))
            }
        }
    }
}