package util

import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import okhttp3.OkHttpClient
import okhttp3.Request
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.echoosx.mirai.plugin.Anisub
import org.echoosx.mirai.plugin.AnisubConfig.host
import org.echoosx.mirai.plugin.AnisubConfig.port
import org.echoosx.mirai.plugin.AnisubConfig.timeout
import org.echoosx.mirai.plugin.data.SubscribeRecord
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class Bangumi{
    var bangumiTitle:String? = null
    var bangumiDesc:String? = null
    var bangumiLink:String? = null
    var chapterTitle:String? = null
    var chapterDesc:String? = null
    var chapterLink:String? = null
}

/**
 * http get方法
 * @param url 链接
 * @return get返回值字符串
 */
fun connectHttpGet(url: String) :String {
    var result = ""
    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
    val client = OkHttpClient().newBuilder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .proxy(proxy)
        .build()

    val request = Request.Builder()
        .url(url)
        .get()
        .build()
    //同步处理
    val call = client.newCall(request)
    try {
        val response = call.execute()
        result = response.body?.string().toString()
    } catch (e: IOException) {
        Anisub.logger.error(e)
    }
    return result
}


/**
 * 根据链接获取最新话
 * @param rssUrl 番剧rss链接
 * @return 包含最新话所有信息的Bangumi实例
 */
fun getLatestChapter(rssUrl:String):Bangumi{
    val document = getXml(rssUrl,debug = true)
    Anisub.logger.info("getLatestChapter:"+document.text)
    val bangumi = Bangumi()
    bangumi.bangumiTitle = document.selectSingleNode("//channel/title").text
    bangumi.bangumiDesc = document.selectSingleNode("//channel/description").text
    bangumi.bangumiLink = document.selectSingleNode("//channel/link").text
    bangumi.chapterTitle = document.selectSingleNode("//channel/item/title").text
    bangumi.chapterDesc = document.selectSingleNode("//channel/item/description").text
    bangumi.chapterLink = document.selectSingleNode("//channel/item/link").text

    if(bangumi.chapterDesc == bangumi.chapterTitle){
        bangumi.chapterDesc = null
    }
    bangumi.bangumiTitle = bangumi.bangumiTitle?.replace("AGE动漫 - ","")

    return bangumi
}


/**
 * 根据链接获取xml
 * @param rssUrl 番剧rss链接
 * @param debug 是否开启debug输出
 * @return xml document
 */
fun getXml(rssUrl: String,debug:Boolean = false): Document {
    val xml = connectHttpGet(rssUrl)
    if(debug)
        Anisub.logger.info(xml)
    val xmlMap = HashMap<String, String>()
    xmlMap["atom"] = "http://www.w3.org/2005/Atom"

    val reader = SAXReader()
    reader.documentFactory.xPathNamespaceURIs = xmlMap
    return reader.read(InputSource(StringReader(xml)))
}

/**
 * 检查番剧是否更新
 * @param rssUrl 番剧rss链接
 * @param record 番剧更新记录
 * @return true更新 false未更新
 */
fun checkUpdate(rssUrl: String, record:SubscribeRecord):Boolean{
    val document = getXml(rssUrl)
    val newLink = document.selectSingleNode("//channel/item/link").text
    return newLink !in record.chapterList
}


/**
 * 格式化输出更新内容
 * @param bangumi 番剧最新话信息
 * @return 格式化内容
 */
fun buildMessage(bangumi:Bangumi):MessageChain{
    val message = buildMessageChain {
        appendLine("《${bangumi.bangumiTitle}》更新啦！")
        appendLine("标题：${bangumi.chapterTitle}")
        if(bangumi.chapterDesc != null){
            appendLine("简介：${bangumi.chapterDesc}")
        }
        append("链接：${bangumi.chapterLink}")
    }
    return message
}