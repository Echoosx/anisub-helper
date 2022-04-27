package org.echoosx.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.data.SubscribeRecord
import util.getLatestChapter
import org.echoosx.mirai.plugin.AnisubConfig.rssPrefix
import org.echoosx.mirai.plugin.data.AnisubSubscribe.record
import org.echoosx.mirai.plugin.util.StringCompareUtil
import util.Bangumi
import java.math.BigDecimal

object SubscribeManage:CompositeCommand(
    Anisub,
    "anisub","age",
) {
    private val logger get() = Anisub.logger
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
                subscribe.latest = bangumi.chapterLink!!
                subscribe.contacts = arrayListOf(group.id)
                record[channelId] = subscribe
            }

            sendMessage("成功在群【${group.id}】中订阅番剧【${bangumi.bangumiTitle}】")

        }catch (e:Throwable){
            sendMessage("订阅失败")
            logger.error(e)
        }
    }

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

    @Suppress("unused")
    @SubCommand("list","ls","列表")
    suspend fun CommandSender.list(group: Group = subject as Group){
        try {
            val message = buildMessageChain {
                appendLine("番剧订阅列表:")
                val tempList = mutableListOf<String>()
                record.forEach() {
                    if (it.value.contacts.contains(group.id)) {
                        var update = ""
                        if(it.value.updateTime!="")
                            update = it.value.updateTime.substring(0,13)
                        tempList.add("${it.value.name}(${it.key}) $update")
                    }
                }
                tempList.reverse()
                tempList.forEach() {
                    appendLine(it)
                }
            }
            sendMessage(message)
        }catch (e:Throwable){
            sendMessage("订阅列表获取失败")
            logger.error(e)
        }
    }

    @Suppress("unused")
    @SubCommand("latest","最新")
    suspend fun CommandSender.latest(vararg args: String){
        try{
            val channelOrKeyword = args.joinToString("")
            val regex = "^[0-9]{8}$".toRegex()
            if(regex.matches(channelOrKeyword)){
                val bangumi = getLatestChapter(rssPrefix + channelOrKeyword)
                sendMessage(latestChapterMessage(bangumi,channelOrKeyword))
                return
            }else {

                data class MatchBangumi(val channelId:String,val coincidence:Double)
                val matchList = mutableListOf<MatchBangumi>()

                record.forEach {
                    val coincidence = StringCompareUtil.coincidenceRate(it.value.name.trim(),channelOrKeyword.trim(),Math.min(it.value.name.trim().length,channelOrKeyword.trim().length))
                    if (coincidence > 0.5) {
                        matchList.add(MatchBangumi(it.key,coincidence))
                    }
                }

                if(matchList.size == 0){
                    sendMessage("没有查询到关键字【${channelOrKeyword}】")
                }else if(matchList.size == 1){
                    val channelId = matchList[0].channelId
                    val bangumi = getLatestChapter(rssPrefix + channelId)
                    sendMessage(latestChapterMessage(bangumi,channelId))
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
                            sendMessage(latestChapterMessage(bangumi,channelId))
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

    private fun latestChapterMessage(bangumi: Bangumi,channelId:String):MessageChain{
        val message = buildMessageChain {
            appendLine("《${bangumi.bangumiTitle}》:")
            appendLine("最新话：${bangumi.chapterTitle}")
            if(bangumi.chapterDesc != null)
                appendLine("简介：${bangumi.chapterDesc}")
            if(record[channelId]?.updateTime != "")
                appendLine("更新时间：${record[channelId]?.updateTime}")
            append("链接：${bangumi.chapterLink}")
        }
        return message
    }
}