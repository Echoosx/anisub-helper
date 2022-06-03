package org.echoosx.mirai.plugin.util

import org.echoosx.mirai.plugin.AnisubConfig
import org.jsoup.Jsoup
import util.CHANNEL_PREFIX
import util.getThumbnail
import java.net.InetSocketAddress
import java.net.Proxy

class Channel{
    var channelId: String? = null
    var channelTitle: String? = null
    var channelInfo = mutableListOf<Pair<String, String>>()
    var thumbnailUrl:String? = null
}

fun searchChannel(key:String):MutableList<Channel>{
    val channelList = mutableListOf<Channel>()
    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(AnisubConfig.host, AnisubConfig.port))
    val document: org.jsoup.nodes.Document = Jsoup.connect("https://www.agemys.com/search")
        .proxy(proxy)
        .timeout(30_000)
        .data("query",key)
        .data("page","1")
        .get()

    val box = document.select("div.blockcontent1>div.cell")
    for(b in box){
        val channel = Channel()

        channel.channelId = b.select("a.cell_poster").attr("href").split("/").last()
        channel.channelTitle = b.select("div.cell_imform>div:first-child>a").text()
        channel.thumbnailUrl = getThumbnail(CHANNEL_PREFIX + channel.channelId)
        val infoList = b.select("div.cell_imform>div.cell_imform_kvs>div.cell_imform_kv")
        for(info in infoList){
            val infoKey = info.select("span.cell_imform_tag").text()
            val infoValue = info.select("span.cell_imform_value,div.cell_imform_desc").text()
            channel.channelInfo.add(Pair(infoKey,infoValue))
        }
        channelList.add(channel)
    }

    return channelList
}