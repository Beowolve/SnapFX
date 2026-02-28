plugins {
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.beryx.jlink)
}

val javafxModules = listOf("javafx.controls")
val javaVersion = JavaVersion.VERSION_21
val javafxRuntimeVersion = javaVersion.majorVersion
val normalizedJPackageVersion = Regex("""^v?(\d+)\.(\d+)\.(\d+)""")
    .find(project.version.toString())
    ?.destructured
    ?.let { (major, minor, patch) ->
        // macOS jpackage requires appVersion major >= 1.
        // TODO(v1): Remove this major-floor workaround after the first real v1.x release,
        // so demo appVersion matches the core/project version again.
        val appMajor = major.toIntOrNull()?.coerceAtLeast(1) ?: 1
        "$appMajor.$minor.$patch"
    }
    ?: "1.0.0"
val jpackageIconFileName = when {
    System.getProperty("os.name").lowercase().contains("win") -> "snapfx.ico"
    System.getProperty("os.name").lowercase().contains("mac") -> "snapfx.icns"
    else -> "snapfx.png"
}
val jpackageIconFile = layout.projectDirectory.file("src/main/resources/images/$jpackageIconFileName").asFile

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

tasks.register<Zip>("packageJPackageImageZip") {
    group = "distribution"
    description = "Creates a ZIP archive from the jpackage app image for demo distribution testing."
    dependsOn("jpackageImage")

    val appImageDir = layout.buildDirectory.dir("jpackage")
    from(appImageDir)
    archiveBaseName.set("snapfx-demo-jpackage-image")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

jlink {
    options = listOf("--strip-debug", "--compress=zip-6", "--no-header-files", "--no-man-pages")
    launcher {
        name = "snapfx-demo"
    }
    jpackage {
        appVersion = normalizedJPackageVersion
        imageName = "SnapFX-Demo"
        installerName = "SnapFX-Demo"
        skipInstaller = true
        if (jpackageIconFile.exists()) {
            icon = jpackageIconFile.absolutePath
        }
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
