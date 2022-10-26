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
    // old version and annotation processor approach due to runelite plugin hub verification restrictions
    val lombokVersion = "1.18.20"
    compileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testCompileOnly(group = "org.projectlombok", name = "lombok", version = lombokVersion)
    testAnnotationProcessor(group = "org.projectlombok", name = "lombok", version = lombokVersion)

    // this version of annotations is verified by runelite
    compileOnly(group = "org.jetbrains", name = "annotations", version = "23.0.0")

    val runeLiteVersion = "1.9.3"
    compileOnly(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "client", version = runeLiteVersion)
    testImplementation(group = "net.runelite", name = "jshell", version = runeLiteVersion)

    val junitVersion = "5.5.2" // max version before junit-bom was added to pom files, due to runelite restrictions
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = junitVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

group = "dinkplugin"
version = "1.0.2"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.test {
    useJUnitPlatform()
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

    archiveClassifier.set("shadow")
    archiveFileName.set(rootProject.name + "-" + project.version + "-all.jar")
}
