import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // built-in
    java
    application
    jacoco

    id("org.sonarqube") version "2.8"
    id("com.diffplug.gradle.spotless") version "3.27.1"
    id("net.researchgate.release") version "2.6.0"
    id("com.palantir.graal") version "0.7.1"

    kotlin("jvm") version "1.3.61"
}

graal {
    javaVersion("11")
    outputName("cpg-vis-neo4j")
    graalVersion("20.1.0")
    mainClass("de.fraunhofer.aisec.cpg.ptn4j.Application")
}

application {
    mainClassName = "de.fraunhofer.aisec.cpg.ptn4j.Application"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

group = "de.fraunhofer.aisec"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

val mavenCentralUri: String
    get() {
        val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
        return if (project.extra["isReleaseVersion"] as Boolean) releasesRepoUrl else snapshotsRepoUrl
    }

repositories {
    mavenLocal()
    mavenCentral()

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/9.11/cdt-9.11.1/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.extra["isReleaseVersion"] as Boolean }
}

tasks.named("compileJava") {
    dependsOn(":spotlessApply")
}

tasks.named("sonarqube") {
    dependsOn(":jacocoTestReport")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()
}

val versions = mapOf(
        "neo4j-ogm" to "4.0.0",
        "neo4j-ogm-old" to "3.2.8",
        "junit5" to "5.6.0",
        "cpg" to "2.1.1",
        "docker" to "3.0.14"
)

dependencies {
    // CPG
    api("de.fraunhofer.aisec", "cpg", versions["cpg"])

    implementation("org.eclipse.platform:org.eclipse.core.resources:3.13.700")

    // neo4j
    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm-old"])

    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    // Docker
    testImplementation("com.github.docker-java","docker-java",versions["docker"])
    implementation(kotlin("stdlib-jdk8"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}
