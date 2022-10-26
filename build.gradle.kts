import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val dinkDebug: Boolean = "true" == System.getenv("DINK_DEBUG")

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    if ("true" == System.getenv("DINK_DEBUG")) {
        id("org.jetbrains.kotlin.jvm") version "1.7.20" apply false
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

dependencies {
    // manual lombok since using the plugin requires extra work for runelite plugin hub
    val lombokVersion = "1.18.24"
    compileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testCompileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testAnnotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)

    // runelite has outdated version with CVEs
    implementation(group = "com.google.guava", name = "guava", version = "31.1-jre")

    // avoid excluding jetbrains annotations
    compileOnly(group = "org.jetbrains", name = "annotations", version = "23.0.0")

    val runeLiteVersion = "1.9.1"
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "jshell", version = runeLiteVersion)

    if (dinkDebug) {
        testImplementation(platform("org.junit:junit-bom:5.9.0"))
        testImplementation(group = "org.junit.jupiter", name = "junit-jupiter")
        testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib")
    }
}

group = "dinkplugin"
version = "1.0.0"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<Test> {
    if (dinkDebug) apply(plugin = "org.jetbrains.kotlin.jvm")
}

tasks.test {
    if (dinkDebug) {
        useJUnitPlatform()
    } else {
        enabled = false
    }
}

tasks.named<ShadowJar>("shadowJar") {
    from(sourceSets.test.get().output)
    configurations += project.configurations.testRuntimeClasspath.get()
    manifest {
        attributes(mapOf("Main-Class" to "dinkplugin.DinkTest"))
    }
}
