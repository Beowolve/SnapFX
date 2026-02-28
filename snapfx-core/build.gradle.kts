import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.javafx)
}

val javafxModules = listOf("javafx.controls")
val javaVersion = JavaVersion.VERSION_21
val javafxRuntimeVersion = javaVersion.majorVersion
val mavenCentralUsernameProvider = providers.gradleProperty("mavenCentralUsername")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
val mavenCentralPasswordProvider = providers.gradleProperty("mavenCentralPassword")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
val mavenCentralReleaseUrlProvider = providers.gradleProperty("mavenCentralReleaseUrl")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_RELEASE_URL"))
    .orElse("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
val signingKeyProvider = providers.gradleProperty("signingInMemoryKey")
    .orElse(providers.environmentVariable("SIGNING_KEY"))
val signingPasswordProvider = providers.gradleProperty("signingInMemoryKeyPassword")
    .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    modularity.inferModulePath.set(true)
    withSourcesJar()
    withJavadocJar()
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "snapfx-core"

            pom {
                name.set("SnapFX Core")
                description.set("A lightweight JavaFX docking framework with drag-and-drop, floating windows, and layout persistence.")
                url.set("https://snapfx.org")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("beowolve")
                        name.set("Beowolve")
                    }
                }

                scm {
                    url.set("https://github.com/Beowolve/SnapFX")
                    connection.set("scm:git:https://github.com/Beowolve/SnapFX.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Beowolve/SnapFX.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatypeCentral"
            url = uri(mavenCentralReleaseUrlProvider.get())
            credentials {
                username = mavenCentralUsernameProvider.orNull
                password = mavenCentralPasswordProvider.orNull
            }
        }
    }
}

signing {
    // Signing remains optional for local dry runs; CI/real releases can provide keys via env/Gradle properties.
    val signingKey = signingKeyProvider.orNull
    val signingPassword = signingPasswordProvider.orNull

    if (!signingKey.isNullOrBlank()) {
        // Secrets may arrive with CRLF from CI; normalize to avoid parser issues.
        useInMemoryPgpKeys(signingKey.replace("\r\n", "\n"), signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    if (repository.name == "sonatypeCentral") {
        doFirst {
            check(!mavenCentralUsernameProvider.orNull.isNullOrBlank()) {
                "Missing Maven Central username. Set mavenCentralUsername or MAVEN_CENTRAL_USERNAME."
            }
            check(!mavenCentralPasswordProvider.orNull.isNullOrBlank()) {
                "Missing Maven Central password/token. Set mavenCentralPassword or MAVEN_CENTRAL_PASSWORD."
            }
            check(!signingKeyProvider.orNull.isNullOrBlank()) {
                "Missing signing key. Set signingInMemoryKey or SIGNING_KEY."
            }
            check(!signingPasswordProvider.orNull.isNullOrBlank()) {
                "Missing signing password. Set signingInMemoryKeyPassword or SIGNING_PASSWORD."
            }
        }
    }
}
