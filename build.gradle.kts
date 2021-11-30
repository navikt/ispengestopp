
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

object Versions {
    const val coroutines = "1.5.2"
    const val flyway = "8.1.0"
    const val hikari = "5.0.0"
    const val jackson = "2.13.0"
    const val kafka = "2.7.0"
    const val kafkaEmbeddedEnv = "2.5.0"
    const val kluent = "1.68"
    const val ktor = "1.6.5"
    const val logback = "1.2.7"
    const val logstashEncoder = "7.0.1"
    const val mockk = "1.12.1"
    const val nimbusjosejwt = "9.15.2"
    const val postgres = "42.3.1"
    const val postgresEmbedded = "0.13.4"
    const val prometheus = "0.9.0"
    const val spek = "2.0.17"
}

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repository.mulesoft.org/nexus/content/repositories/public/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutines}")

    implementation("io.ktor:ktor-auth-jwt:${Versions.ktor}")
    implementation("io.ktor:ktor-client-apache:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-client-jackson:${Versions.ktor}")
    implementation("io.ktor:ktor-jackson:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")

    // Logging
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoder}")

    // Metrics and Prometheus
    implementation("io.prometheus:simpleclient_common:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_hotspot:${Versions.prometheus}")

    // Database
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    testImplementation("com.opentable.components:otj-pg-embedded:${Versions.postgresEmbedded}")

    // Kafka
    implementation("org.apache.kafka:kafka_2.12:${Versions.kafka}")
    testImplementation("no.nav:kafka-embedded-env:${Versions.kafkaEmbeddedEnv}")

    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusjosejwt}")
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktor}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin}")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin}")
    }
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
