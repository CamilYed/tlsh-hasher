import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.spotless)
}

group = "io.github.camilyed"
version = providers.gradleProperty("releaseVersion").orElse("0.1.0-SNAPSHOT").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }

    withSourcesJar()
    withJavadocJar()
    modularity.inferModulePath.set(true)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "tlsh-hasher"

            pom {
                name.set("TLSH Hasher")
                description.set("A readable Java implementation of TLSH similarity hashing")
                url.set("https://github.com/CamilYed/tlsh-hasher")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("CamilYed")
                        name.set("CamilYed")
                        url.set("https://github.com/CamilYed")
                    }
                }

                scm {
                    url.set("https://github.com/CamilYed/tlsh-hasher")
                    connection.set("scm:git:git://github.com/CamilYed/tlsh-hasher.git")
                    developerConnection.set("scm:git:ssh://git@github.com:CamilYed/tlsh-hasher.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "localBuild"
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    val signingKey =
        providers
            .gradleProperty("signingKey")
            .orElse(providers.environmentVariable("SIGNING_KEY"))
            .orNull
    val signingPassword =
        providers
            .gradleProperty("signingPassword")
            .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
            .orNull

    isRequired = project.isRemotePublishingRequested()

    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

configurations.configureEach {
    resolutionStrategy {
        failOnDynamicVersions()
        failOnChangingVersions()
    }
}

extensions.configure<SpotlessExtension> {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("*.md", ".editorconfig", ".gitignore", "gradle.properties", "gradle/**/*.toml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.map(String::toInt)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = true
    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PACKAGE
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all", "-quiet")
}

tasks.test {
    useJUnitPlatform()

    val officialFixtureDirectory = providers.systemProperty("tlsh.officialFixtureDirectory")
    if (officialFixtureDirectory.isPresent) {
        val fixtureDirectory = officialFixtureDirectory.get()
        inputs
            .dir(fixtureDirectory)
            .withPropertyName("officialTlshFixtureDirectory")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        systemProperty("tlsh.officialFixtureDirectory", fixtureDirectory)
    }

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.check {
    dependsOn(":tlsh-benchmarks:jmhClasses")
    dependsOn(tasks.javadoc)
    dependsOn(tasks.spotlessCheck)
}

fun Project.isRemotePublishingRequested(): Boolean =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("publish", ignoreCase = true) &&
            !taskName.contains("MavenLocal", ignoreCase = true) &&
            !taskName.contains("LocalBuild", ignoreCase = true)
    }
