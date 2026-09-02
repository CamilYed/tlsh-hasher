import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

plugins {
    application
    jacoco
}

group = rootProject.group
version = rootProject.version
description = "Command-line application for calculating and comparing TLSH digests"

dependencies {
    implementation(project(":"))
    implementation(libs.jline.builtins)
    implementation(libs.jline.reader)
    implementation(libs.jline.terminal)
    implementation(libs.picocli)
    runtimeOnly(libs.jline.terminal.ffm)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = libs.versions.jacoco.get()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }

    modularity.inferModulePath.set(true)
}

application {
    applicationName = "tlsh"
    mainModule = "io.github.camilyed.tlsh.cli"
    mainClass = "io.github.camilyed.tlsh.cli.TlshCli"
    applicationDefaultJvmArgs = listOf("--enable-native-access=org.jline.terminal.ffm")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.map(String::toInt)
    options.javaModuleVersion.set(provider { project.version.toString() })
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",
            "-Werror",
            "-Aproject=tlsh-cli",
        ),
    )
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }

    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to (project.description ?: "TLSH command-line application"),
                "Implementation-Version" to project.version.toString(),
            ),
        )
    }
}

distributions {
    main {
        contents {
            from(rootProject.file("LICENSE"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}
