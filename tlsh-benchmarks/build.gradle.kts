import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    alias(libs.plugins.jmh)
    alias(libs.plugins.spotless)
}

description = "Non-published JMH benchmarks for TLSH Hasher"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    implementation(project(":"))
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
        target("*.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.map(String::toInt)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

val jmhLibraryVersion =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("jmh")
        .orElseThrow()
        .requiredVersion

jmh {
    jmhVersion.set(jmhLibraryVersion)
    warmupIterations.set(1)
    iterations.set(3)
    fork.set(1)
    warmup.set("1s")
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
    failOnError.set(true)

    providers.gradleProperty("jmh.includes").orNull?.let { includes.set(it.split(',')) }
    providers.gradleProperty("jmh.profilers").orNull?.let { profilers.set(it.split(',')) }
    providers.gradleProperty("jmh.forks").orNull?.let { fork.set(it.toInt()) }
    providers.gradleProperty("jmh.warmupIterations").orNull?.let {
        warmupIterations.set(it.toInt())
    }
    providers.gradleProperty("jmh.iterations").orNull?.let { iterations.set(it.toInt()) }
    providers.gradleProperty("jmh.warmupTime").orNull?.let { warmup.set(it) }
    providers.gradleProperty("jmh.measurementTime").orNull?.let { timeOnIteration.set(it) }
}

tasks.check {
    dependsOn(tasks.jmhClasses)
    dependsOn(tasks.spotlessCheck)
}
