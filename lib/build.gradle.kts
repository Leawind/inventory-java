import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("me.champeau.jmh") version "0.7.2"
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    mavenCentral()
}

dependencies {
    // Test Framework
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Assertion Library
    testImplementation(libs.hamcrest)

    // JMH for performance testing
    testImplementation(libs.jmh.core)
    testAnnotationProcessor(libs.jmh.generator.annprocess)

    // compileOnly(libs.guava)
    compileOnly(libs.jspecify)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier = "shadow"
    minimize()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(project.properties["org.gradle.java.version"] as String)
    }

    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.locale = "en_US"
    options.encoding = "UTF-8"
    options {
        this as StandardJavadocDocletOptions
        // Suppress missing doclint warnings
        addStringOption("Xdoclint:-missing", "-quiet")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

group = project.properties["lib_group_id"] as String
version = project.properties["lib_version"] as String

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.properties["lib_artifact_id"] as String
            group = project.properties["lib_group_id"] as String
            version = project.properties["lib_version"] as String

            from(components["java"])
            pom {
                url = "https://github.com/Leawind/inventory-java"

                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/license/mit"
                    }
                }

                developers {
                    developer {
                        id = "Leawind"
                        email = "leawind@yeah.net"
                    }
                }
            }
        }
    }
}
