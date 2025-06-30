plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.1"
}

group = "ratger"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.kyori:adventure-api:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava {
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveFileName.set("${project.name}.jar")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    val copyPluginJar by registering(Copy::class) {
        from("$buildDir/libs/${project.name}.jar")
        into("C:/Users/Home/AppData/Roaming/Fork/servers/1.21.1/plugins")
        rename { "${project.name}.jar" }
        doLast {
            println("Plugin moved to server plugins.")
        }
    }
    build {
        finalizedBy(copyPluginJar)
    }
}