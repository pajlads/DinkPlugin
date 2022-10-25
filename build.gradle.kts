import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.freefair.lombok") version "6.5.1"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

lombok {
    version.set("1.18.24")
}

dependencies {
    implementation(group = "com.google.guava", name = "guava", version = "31.1-jre") // runelite has outdated version with CVEs

    val runeLiteVersion = "1.9.1"
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "jshell", version = runeLiteVersion)

    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter")
}

group = "dinkplugin"
version = "1.0.0"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    from(sourceSets.test.get().output)
    configurations += project.configurations.testRuntimeClasspath.get()
    manifest {
        attributes(mapOf("Main-Class" to "dinkplugin.DinkTest"))
    }
}
