plugins {
    application
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
    implementation(project(":snapfx-core"))
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testfx.core)
    testImplementation(libs.testfx.junit5)
    testImplementation(libs.hamcrest)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("org.snapfx.demo.MainDemo")
    mainModule.set("org.snapfx.demo")
}

tasks.register<JavaExec>("captureMainDemoScreenshot") {
    group = "documentation"
    description = "Launches MainDemo and updates docs/images/main-demo.png"

    mainClass.set("org.snapfx.demo.MainDemoScreenshotGenerator")
    classpath = sourceSets.main.get().runtimeClasspath

    val outputPath = (project.findProperty("snapfxScreenshotOutput") as String?)
        ?: "docs/images/main-demo.png"
    args(layout.projectDirectory.file(outputPath).asFile.absolutePath)
}

tasks.register<JavaExec>("runSimpleExample") {
    group = "application"
    description = "Runs SimpleExample via module launch (includes JavaFX runtime + snapfx.css)"

    mainModule.set("org.snapfx.demo")
    mainClass.set("org.snapfx.demo.SimpleExample")
    classpath = sourceSets.main.get().runtimeClasspath
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
