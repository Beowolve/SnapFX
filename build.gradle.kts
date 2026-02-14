plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.github.beowolve"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    modularity.inferModulePath.set(true)
}

// JavaFX Configuration
javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    // JSON for Persistence
    implementation("com.google.code.gson:gson:2.10.1")

    // Test Dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // TestFX for UI Testing
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")

    // Hamcrest for TestFX
    testImplementation("org.hamcrest:hamcrest:2.2")
}

// Application Configuration
application {
    mainClass.set("com.github.beowolve.snapfx.demo.MainDemo")
    mainModule.set("com.github.beowolve.snapfx")
}

// Configure test task - tests run on classpath
tasks.test {
    useJUnitPlatform()

    // Only need JavaFX internal access for TestFX
    jvmArgs(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
        "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    )
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
