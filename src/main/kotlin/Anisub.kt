package org.echoosx.mirai.plugin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import org.echoosx.mirai.plugin.AnisubConfig.interval
import org.echoosx.mirai.plugin.command.SubscribeManage
import org.echoosx.mirai.plugin.data.AnisubSubscribe
import org.echoosx.mirai.plugin.util.Subscribe
import org.quartz.JobBuilder
import org.quartz.SimpleScheduleBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

object Anisub : KotlinPlugin(
    JvmPluginDescription(
        id = "org.echoosx.mirai.plugin.anisub-helper",
        name = "anisub-helper",
        version = "1.0.0"
    ) {
        author("Echoosx")
    }
) {
    override fun onEnable() {
        logger.info { "Anisub-Helper loaded" }
        //配置文件目录 "${dataFolder.absolutePath}/"

        AnisubSubscribe.reload()
        AnisubConfig.reload()
        SubscribeManage.register()

        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        val jobDetail = JobBuilder.newJob(Subscribe::class.java)
            .build()
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(interval * 60)
                    .repeatForever()
            )
            .startNow()
            .build()

        scheduler.scheduleJob(jobDetail,trigger)
        scheduler.start()
    }
}
