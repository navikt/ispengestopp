import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os

group = "no.nav.syfo"
version = "1.0.0"

val confluent = "7.7.1"
val flyway = "9.22.3"
val hikari = "5.1.0"
val jackson = "2.17.2"
val kafka = "3.6.0"
val kluent = "1.73"
val ktor = "2.3.12"
val logback = "1.5.8"
val logstashEncoder = "7.4"
val micrometerRegistry = "1.13.4"
val mockk = "1.13.12"
val nimbusjosejwt = "9.41.1"
val postgres = "42.7.4"
val postgresEmbedded = if (Os.isFamily(Os.FAMILY_MAC)) "1.0.0" else "0.13.4"
val spek = "2.0.19"

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.4.2"
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

    implementation("io.ktor:ktor-server-auth-jwt:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-call-id:$ktor")
    implementation("io.ktor:ktor-server-status-pages:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-client-apache:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-jackson:$ktor")

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoder")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktor")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistry")

    // Database
    implementation("org.flywaydb:flyway-core:$flyway")
    implementation("org.postgresql:postgresql:$postgres")
    implementation("com.zaxxer:HikariCP:$hikari")
    testImplementation("com.opentable.components:otj-pg-embedded:$postgresEmbedded")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:$kafka", excludeLog4j)
    implementation("io.confluent:kafka-avro-serializer:$confluent", excludeLog4j)
    implementation("io.confluent:kafka-schema-registry:$confluent", excludeLog4j)
    constraints {
        implementation("org.apache.avro:avro") {
            because("io.confluent:kafka-avro-serializer:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-39410")
            version {
                require("1.11.3")
            }
        }
        implementation("org.apache.commons:commons-compress") {
            because("org.apache.commons:commons-compress:1.22 -> https://www.cve.org/CVERecord?id=CVE-2012-2098")
            version {
                require("1.26.0")
            }
        }
        implementation("org.yaml:snakeyaml") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-25857/")
            version {
                require("1.31")
            }
        }
        implementation("org.glassfish:jakarta.el") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2021-28170/")
            version {
                require("3.0.4")
            }
        }
        implementation("com.google.protobuf:protobuf-java") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2022-3510")
            version {
                require("3.21.7")
            }
        }
        implementation("com.google.code.gson:gson") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2022-25647")
            version {
                require("2.8.9")
            }
        }
        implementation("org.json:json") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://nvd.nist.gov/vuln/detail/CVE-2022-45688")
            version {
                require("20231013")
            }
        }
        implementation("org.apache.zookeeper:zookeeper") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.9.1")
            }
        }
    }

    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusjosejwt")
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
    testImplementation("io.mockk:mockk:$mockk")
    testImplementation("org.amshove.kluent:kluent:$kluent")
    testImplementation("io.ktor:ktor-client-mock:$ktor")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek") {
        exclude(group = "org.jetbrains.kotlin}")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek") {
        exclude(group = "org.jetbrains.kotlin}")
    }
}

kotlin {
    jvmToolchain(21)
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
