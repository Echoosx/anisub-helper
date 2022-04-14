package org.echoosx.mirai.plugin.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.buildMessageChain
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.data.SubscribeRecord
import util.getLatestChapter
import org.echoosx.mirai.plugin.Config.rssPrefix
import org.echoosx.mirai.plugin.data.Subscribe.record

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
                record.forEach() {
                    if (it.value.contacts.contains(group.id)) {
                        var update = ""
                        if(it.value.updateTime!="")
                            update = it.value.updateTime.substring(0,13)
                        appendLine("${it.value.name}(${it.key}) $update")
                    }
                }
            }
            sendMessage(message)
        }catch (e:Throwable){
            sendMessage("订阅列表获取失败")
            logger.error(e)
        }
    }

//    @Suppress("unused")
//    @SubCommand("latest","最新")
//    suspend fun CommandSender.latest(channelId: String){
//        try{
//            val regex = "^[0-9]{8}$".toRegex()
//            if(!regex.matches(channelId)){
//                sendMessage("番剧编号格式错误")
//                return
//            }
//
//            val bangumi = getLatestChapter(channelId)
//            val message = buildMessageChain {
//                appendLine("${lastVideo.videoName}\n")
//                if (record.contains(channelId) && record[channelId]?.regex!="")
//                    appendLine(simpleDescription(lastVideo.videoDescription!!, record[channelId]!!.regex))
//                else
//                    appendLine(lastVideo.videoDescription)
//                val thumbnailImage = getThumbnail(lastVideo.videoId!!)
//                val image = thumbnailImage.toExternalResource().use { it.uploadAsImage(user!!) }
//                append(image)
//                append(lastVideo.videoLink)
//            }
//            sendMessage(message)
//
//        }catch (e:Throwable){
//            sendMessage("最新番剧获取失败")
//            logger.error("最新番剧获取失败")
//        }
//    }
}