/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("commons-codec:commons-codec:1.16.0")
    api("org.jline:jline-terminal-jna:3.25.1")
    api("org.jline:jline-reader:3.25.1")
}

group = "com.github.oliveiradd.javazcrypt"
version = "1.0.0"
description = "javazcrypt"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.oliveiradd.javazcrypt.Main"
    }
}

tasks.withType<Jar>() {
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}
