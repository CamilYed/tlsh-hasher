import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
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
        target("*.md", ".gitignore", "gradle.properties", "gradle/**/*.toml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.map(String::toInt)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = true
    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PACKAGE
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all", "-quiet")

    // The learning project deliberately starts with package documentation and no API types.
    onlyIf("there are API types to document") {
        source.files.any { it.name != "package-info.java" }
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.check {
    dependsOn(tasks.javadoc)
    dependsOn(tasks.spotlessCheck)
}
