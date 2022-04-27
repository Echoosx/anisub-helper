package org.echoosx.mirai.plugin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@Serializable
data class SubscribeRecord(
    @SerialName("name")
    var name: String = "",
    @SerialName("latest")
    var latest: String = "",
    @SerialName("update")
    var updateTime:String = "",
    @SerialName("contact")
    var contacts: MutableList<Long> = arrayListOf(),
    @SerialName("chapterList")
    var chapterList: MutableSet<Long> = mutableSetOf(),
)

object AnisubSubscribe : AutoSavePluginData("Subscribe") {
    @ValueDescription("订阅记录")
    val record:MutableMap<String,SubscribeRecord> by value()
}