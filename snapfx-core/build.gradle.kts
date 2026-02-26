plugins {
    `java-library`
    alias(libs.plugins.javafx)
}

val javafxModules = listOf("javafx.controls")
val javaVersion = JavaVersion.VERSION_21
val javafxRuntimeVersion = javaVersion.majorVersion

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    modularity.inferModulePath.set(true)
}

javafx {
    version = javafxRuntimeVersion
    modules(*javafxModules.toTypedArray())
}

dependencies {
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testfx.core)
    testImplementation(libs.testfx.junit5)
    testImplementation(libs.hamcrest)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    inputs.property("snapfxVersion", project.version.toString())
    filesMatching("snapfx-version.properties") {
        expand("snapfxVersion" to project.version.toString())
    }
}

tasks.test {
    useJUnitPlatform()

    // Keep project/tests on classpath, but load JavaFX as named modules to avoid
    // "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'".
    val javafxJars = classpath.filter { file ->
        file.name.startsWith("javafx-") && file.name.endsWith(".jar")
    }
    if (!javafxJars.isEmpty) {
        classpath = classpath.minus(javafxJars)
        jvmArgs(
            "--module-path", javafxJars.asPath,
            "--add-modules", javafxModules.joinToString(","),
            // TestFX reflects into JavaFX internals during setup/cleanup.
            "--add-exports", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
            "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
        )
    }
}
