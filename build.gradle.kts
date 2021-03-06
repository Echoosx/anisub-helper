plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.10.0"
}

group = "org.echoosx"
version = "1.0.0"

repositories {
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenCentral()
}

dependencies{
    implementation("dom4j:dom4j:1.6.1")
    implementation("jaxen:jaxen:1.2.0")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    testImplementation(kotlin("test"))
}