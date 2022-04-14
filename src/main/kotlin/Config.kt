package org.echoosx.mirai.plugin

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("Config"){
    @ValueDescription("rss前缀")
    val rssPrefix:String by value("https://rsshub.app/agefans/detail/")

    @ValueDescription("代理host")
    val host:String by value("127.0.0.1")

    @ValueDescription("代理端口")
    val port:Int by value(7890)

    @ValueDescription("请求过期时间(s)")
    val timeout:Long by value(30L)

    @ValueDescription("轮询间隔(min)")
    val interval:Long by value(10L)
}