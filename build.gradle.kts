plugins {
    java
}

group = "nz.ninjagoldfinch"
version = "0.1.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.249-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    jar {
        archiveBaseName = "NinjaMinecraft"
    }
}
