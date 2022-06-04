package org.echoosx.mirai.plugin.command

import io.ktor.client.request.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.Anisub.dataFolder
import org.echoosx.mirai.plugin.AnisubConfig.rssPrefix
import org.echoosx.mirai.plugin.data.AnisubSubscribe.record
import org.echoosx.mirai.plugin.data.SubscribeRecord
import org.echoosx.mirai.plugin.util.StringCompareUtil
import org.echoosx.mirai.plugin.util.searchChannel
import util.*
import java.io.File
import java.math.BigDecimal
import kotlin.collections.arrayListOf
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.mutableListOf
import kotlin.collections.reverse
import kotlin.collections.set
import kotlin.collections.sortByDescending

object SubscribeManage:CompositeCommand(
    Anisub,
    "anisub","age",
) {
    private val logger get() = Anisub.logger

    /**
     * 增加订阅
     * @param channelId 频道id
     * @param group 群号
     * @return
     */
    @Suppress("unused")
    @SubCommand("add","subscribe","订阅")
    suspend fun CommandSender.add(channelId:String, group: Group = subject as Group){
        try {
            val regex = "^[0-9]{8}$".toRegex()
            if(!regex.matches(channelId)){
                sendMessage("番剧编号格式错误")
                return
            }

            val bangumi = getLatestChapter(rssPrefix + channelId)       // 校验channel是否存在
            if(record.containsKey(channelId)){
                record[channelId]!!.contacts.add(group.id)
            }else{
                val subscribe = SubscribeRecord()
                subscribe.name = bangumi.bangumiTitle!!
                subscribe.chapterList.add(bangumi.chapterLink!!)
                subscribe.contacts = arrayListOf(group.id)
                record[channelId] = subscribe
                downloadThumbnail(channelId)
            }

            sendMessage("成功在群【${group.id}】中订阅番剧【${bangumi.bangumiTitle}】")

        }catch (e:Throwable){
            sendMessage("订阅失败")
            logger.error(e)
        }
    }


    /**
     * 取消订阅
     * @param channelId 频道id
     * @param group 群号
     * @return
     */
    @Suppress("unused")
    @SubCommand("remove","rm","退订")
    suspend fun CommandSender.remove(channelId:String,group:Group = subject as Group){
        try {
            val regex = "^[0-9]{8}$".toRegex()
            if(!regex.matches(channelId)){
                sendMessage("番剧编号格式错误")
                return
            }
            record[channelId]!!.contacts.remove(group.id)
            sendMessage("已取消【${record[channelId]!!.name}】的订阅")
        }catch (e:Throwable){
            sendMessage("订阅取消失败")
            logger.error(e)
        }
    }


    /**
     * 获取番剧订阅列表
     * @param group 群号
     * @return
     */
    @Suppress("unused")
    @SubCommand("list","ls","列表")
    suspend fun CommandSender.list(group: Group = subject as Group){
        try {
            val message = buildMessageChain {
                appendLine("番剧订阅列表:")
                val tempList = mutableListOf<String>()
                record.forEach{
                    if (it.value.contacts.contains(group.id)) {
                        var update = ""
                        if(it.value.updateTime!="")
                            update = it.value.updateTime.substring(0,13)
                        tempList.add("${it.value.name}(${it.key}) $update")
                    }
                }
                tempList.reverse()
                tempList.forEach{
                    appendLine(it)
                }
            }
            sendMessage(message)
        }catch (e:Throwable){
            sendMessage("订阅列表获取失败")
            logger.error(e)
        }
    }


    /**
     * 获取最新话
     * @param args 关键字
     * @return
     */
    @Suppress("unused")
    @SubCommand("latest","最新")
    suspend fun CommandSender.latest(vararg args: String){
        try{
            val channelOrKeyword = args.joinToString("")
            val regex = "^[0-9]{8}$".toRegex()
            if(regex.matches(channelOrKeyword)){
                val bangumi = getLatestChapter(rssPrefix + channelOrKeyword)
                sendMessage(latestChapterMessage(bangumi,channelOrKeyword,subject!!))
                return
            }else {

                data class MatchBangumi(val channelId:String,val coincidence:Double)
                val matchList = mutableListOf<MatchBangumi>()

                record.forEach {
                    val coincidence = StringCompareUtil.coincidenceRate(it.value.name.trim(),channelOrKeyword.trim(),
                        it.value.name.trim().length.coerceAtMost(channelOrKeyword.trim().length))
                    if (coincidence > 0.5) {
                        matchList.add(MatchBangumi(it.key,coincidence))
                    }
                }

                if(matchList.size == 0){
                    val result = searchChannel(channelOrKeyword)
                    if(result.size == 1){
                        val channelId = result.first().channelId
                        val bangumi = getLatestChapter(rssPrefix + result.first().channelId)
                        sendMessage(latestChapterMessage(bangumi,channelId!!,subject!!))
                    }else if(result.size > 1){
                        val forward: ForwardMessage = buildForwardMessage(subject!!) {
                            add(bot!!.id,bot!!.nick,PlainText("搜索到以下番剧："))
                            result.forEach { channel ->
                                val resource = httpClient.get<ByteArray>(channel.thumbnailUrl!!).toExternalResource()
                                val thumbnail = subject?.uploadImage(resource)
                                add(bot!!.id,bot!!.nick, buildMessageChain {
                                    append(thumbnail!!)
                                    append("\n名称：${channel.channelTitle}")
                                    append("\n番剧编号：${channel.channelId}")
                                    for(info in channel.channelInfo){
                                        append("\n${info.first}${info.second}")
                                    }
                                })
                            }
                        }

                        val render = ForwardMessage(
                            preview = forward.preview,
                            title = "AGE番剧搜索结果",
                            brief = forward.brief,
                            source = forward.source,
                            summary = forward.summary,
                            nodeList = forward.nodeList
                        )
                        sendMessage(render)
                    }else{
                        sendMessage("没有搜索到对应的番剧")
                    }


                }else if(matchList.size == 1){
                    val channelId = matchList[0].channelId
                    val bangumi = getLatestChapter(rssPrefix + channelId)
                    sendMessage(latestChapterMessage(bangumi,channelId,subject!!))
                }else{
                    matchList.sortByDescending { MatchBangumi->MatchBangumi.coincidence }

                    // 不存在完全匹配项
                    if(BigDecimal(matchList[0].coincidence) != BigDecimal(1.0)){
                        val message = buildMessageChain {
                            appendLine("查询到多个番剧：")
                            matchList.forEach{
                                appendLine("${record[it.channelId]?.name}(${it.channelId})")
                            }
                        }
                        sendMessage(message)
                    }else{
                        // 仅存在一个完全匹配项
                        if(BigDecimal(matchList[1].coincidence) != BigDecimal(1.0)){
                            val channelId = matchList[0].channelId
                            val bangumi = getLatestChapter(rssPrefix + channelId)
                            sendMessage(latestChapterMessage(bangumi,channelId,subject!!))
                        }
                        else{
                            // 存在多个完全匹配项
                            val message = buildMessageChain {
                                appendLine("查询到多个番剧：")
                                matchList.forEach{
                                    if(BigDecimal(it.coincidence) == BigDecimal(1.0))
                                        appendLine("${record[it.channelId]?.name}(${it.channelId})")
                                }
                            }
                            sendMessage(message)
                        }
                    }
                }
            }
        }catch (e:Throwable){
            sendMessage("获取失败")
            logger.error(e)
        }
    }


    /**
     * 通过关键字搜索番剧
     * @param args 关键字
     * @return
     */
    @Suppress("unused")
    @SubCommand("search","搜索")
    suspend fun CommandSender.search(vararg args: String){
        try{
            val keyword = args.joinToString(" ")
            val result = searchChannel(keyword)
            if(result.size > 0){
                val forward: ForwardMessage = buildForwardMessage(subject!!) {
                    add(bot!!.id,bot!!.nick,PlainText("搜索到以下番剧："))
                    result.forEach { channel ->
                        val resource = httpClient.get<ByteArray>(channel.thumbnailUrl!!).toExternalResource()
                        val thumbnail = subject?.uploadImage(resource)
                        add(bot!!.id,bot!!.nick, buildMessageChain {
                            append(thumbnail!!)
                            append("\n名称：${channel.channelTitle}")
                            append("\n番剧编号：${channel.channelId}")
                            for(info in channel.channelInfo){
                                append("\n${info.first}${info.second}")
                            }
                        })
                    }
                }

                val render = ForwardMessage(
                    preview = forward.preview,
                    title = "AGE番剧搜索结果",
                    brief = forward.brief,
                    source = forward.source,
                    summary = forward.summary,
                    nodeList = forward.nodeList
                )
                sendMessage(render)
            }else{
                sendMessage("没有搜索到对应的番剧")
            }

        }catch (e:Throwable){
            sendMessage("搜索失败")
            logger.error(e)
        }
    }


    /**
     * 格式化生成更新内容
     * @param bangumi 频道内容
     * @param channelId 频道id
     * @return 格式化消息
     */
    private suspend fun latestChapterMessage(bangumi: Bangumi, channelId:String, contact:Contact):MessageChain{
        val message = buildMessageChain {
            val thumbnail = File("${dataFolder.absolutePath}/thumbnail/${channelId}.jpg")
            if(!thumbnail.exists()){
                downloadThumbnail(channelId)
            }
            val resource = thumbnail.toExternalResource()
            contact.uploadImage(resource).imageId.apply {
                append(Image(this))
            }
            resource.close()
            appendLine("《${bangumi.bangumiTitle}》")
            appendLine("最新话：${bangumi.chapterTitle}")
            if(bangumi.chapterDesc != null)
                appendLine("简介：${bangumi.chapterDesc}")
            if(record[channelId]?.updateTime != null)
                appendLine("更新时间：${record[channelId]?.updateTime}")
            append("链接：${bangumi.chapterLink}")
        }
        return message
    }
}