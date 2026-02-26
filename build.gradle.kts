plugins {
    java
    alias(libs.plugins.jgitver)
    alias(libs.plugins.javafx)
}

group = "org.snapfx"
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

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    repositories {
        mavenCentral()
    }
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
