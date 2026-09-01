import org.gradle.api.tasks.compile.JavaCompile

plugins {
    application
}

description = "Non-published named-module smoke test for the TLSH public API"

dependencies {
    implementation(project(":"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }

    modularity.inferModulePath.set(true)
}

application {
    mainModule = "io.github.camilyed.tlsh.smoke"
    mainClass = "io.github.camilyed.tlsh.smoke.TlshModuleSmokeTest"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.map(String::toInt)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.check {
    dependsOn(tasks.run)
}
