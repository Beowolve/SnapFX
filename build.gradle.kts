plugins {
    java
    application
    id("fr.brouillard.oss.gradle.jgitver") version "0.10.0-rc03"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.github.beowolve"

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

// Configure test task - tests run on classpath
tasks.test {
    useJUnitPlatform()
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
