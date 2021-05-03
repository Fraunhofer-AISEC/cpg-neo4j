import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // built-in
    java
    application
    jacoco

    id("org.sonarqube") version "2.8"
    id("com.diffplug.gradle.spotless") version "3.27.1"
    id("net.researchgate.release") version "2.6.0"
    kotlin("jvm") version "1.4.32"
}

application {
    mainClassName = "de.fraunhofer.aisec.cpg_vis_neo4j.ApplicationKt"
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
    mavenCentral()
    maven { setUrl("https://jitpack.io") }

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.2/cdt-10.2.0/plugins")
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
        "neo4j-ogm-old" to "3.2.22",
        "junit5" to "5.6.0",
        "cpg" to "3.5.1"
)

dependencies {
    // CPG
    //api("de.fraunhofer.aisec", "cpg", versions["cpg"])
    //implementation("com.github.Fraunhofer-AISEC:cpg:0e10b90eb2") {
    implementation("com.github.Fraunhofer-AISEC:cpg:-SNAPSHOT") {
        isChanging = true
    }

    // neo4j
    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm-old"])

    implementation(kotlin("test-junit"))

    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

    // Command line interface support
    api("info.picocli:picocli:4.1.4")
    annotationProcessor("info.picocli:picocli-codegen:4.1.4")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
}