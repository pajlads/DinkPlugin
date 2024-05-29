plugins {
    java
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

dependencies {
    val lombokVersion = "1.18.30" // supports JDK 21 and is verified by runelite
    // old annotation processor approach due to runelite plugin hub verification restrictions
    compileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testCompileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testAnnotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)

    // this version of annotations is verified by runelite
    compileOnly(group = "org.jetbrains", name = "annotations", version = "23.0.0")

    val runeLiteVersion = "latest.release"
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "jshell", version = runeLiteVersion)

    val junitVersion = "5.5.2" // max version before junit-bom was added to pom files, due to runelite restrictions
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = junitVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
    testRuntimeOnly(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.5.2")

    // mocking and test injection used by runelite client
    testImplementation(group = "org.mockito", name = "mockito-core", version = "4.11.0") // runelite uses 3.1.0
    testImplementation(group = "com.google.inject.extensions", name = "guice-testlib", version = "4.1.0") {
        exclude(group = "com.google.inject", module = "guice") // already provided by runelite client
    }
}

group = "dinkplugin"
version = "1.10.1"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    val version = JavaVersion.VERSION_11.toString()
    sourceCompatibility = version
    targetCompatibility = version
}

tasks.test {
    useJUnitPlatform {
        excludeTags("generator")
    }
}

tasks.register(name = "generateResources", type = Test::class) {
    useJUnitPlatform {
        includeTags("generator")
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register(name = "shadowJar", type = Jar::class) {
    dependsOn(configurations.testRuntimeClasspath)
    manifest {
        attributes(mapOf("Main-Class" to "dinkplugin.DinkTest"))
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from(sourceSets.test.get().output)
    from({
        configurations.testRuntimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("**/module-info.class")

    group = BasePlugin.BUILD_GROUP
    archiveClassifier.set("shadow")
    archiveFileName.set(rootProject.name + "-" + project.version + "-all.jar")
}
