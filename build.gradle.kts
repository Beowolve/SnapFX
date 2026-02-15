plugins {
    java
    application
    alias(libs.plugins.jgitver)
    alias(libs.plugins.javafx)
}

group = "com.github.beowolve"
val javafxModules = listOf("javafx.controls")
val javaVersion = JavaVersion.VERSION_21
val javafxRuntimeVersion = javaVersion.majorVersion

jgitver {
    strategy("CONFIGURABLE")
    autoIncrementPatch(true)
    useDistance(true)
    useGitCommitID(true)
    gitCommitIDLength(7)
    nonQualifierBranches("main,master")
    regexVersionTag("v(.*)")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    modularity.inferModulePath.set(true)
}

// JavaFX Configuration
javafx {
    version = javafxRuntimeVersion
    modules(*javafxModules.toTypedArray())
}

dependencies {
    // JSON for Persistence
    implementation(libs.gson)

    // Test dependencies grouped by destination configuration.
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

// Application Configuration
application {
    mainClass.set("com.github.beowolve.snapfx.demo.MainDemo")
    mainModule.set("com.github.beowolve.snapfx")
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

tasks.register<JavaExec>("captureMainDemoScreenshot") {
    group = "documentation"
    description = "Launches MainDemo and updates docs/images/main-demo.png"

    mainClass.set("com.github.beowolve.snapfx.demo.MainDemoScreenshotGenerator")
    classpath = sourceSets.main.get().runtimeClasspath

    val outputPath = (project.findProperty("snapfxScreenshotOutput") as String?)
        ?: "docs/images/main-demo.png"
    args(layout.projectDirectory.file(outputPath).asFile.absolutePath)
}

tasks.register<JavaExec>("runSimpleExample") {
    group = "application"
    description = "Runs SimpleExample via module launch (includes JavaFX runtime + snapfx.css)"

    mainModule.set("com.github.beowolve.snapfx")
    mainClass.set("com.github.beowolve.snapfx.demo.SimpleExample")
    classpath = sourceSets.main.get().runtimeClasspath
}
