plugins {
    java
}

group = "com.kamismp"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API for Minecraft 26.3. "+" = always use the newest 26.3 build.
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

tasks.withType<JavaCompile>().configureEach {
    // Minecraft 26.x runs on Java 25, so we compile for Java 25.
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.processResources {
    // Puts the version from above into plugin.yml automatically.
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
