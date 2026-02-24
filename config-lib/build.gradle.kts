import org.gradle.kotlin.dsl.support.KotlinCompilerOptions
import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "org.nwtls"
version = "1.0.0"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.snake.yaml)
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}
