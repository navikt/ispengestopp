import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os

group = "no.nav.syfo"
version = "1.0.0"

object Versions {
    const val confluent = "7.3.1"
    const val coroutines = "1.5.2"
    const val flyway = "8.5.13"
    const val hikari = "5.0.1"
    const val jackson = "2.13.3"
    const val kafka = "3.2.3"
    const val kafkaEmbeddedEnv = "3.2.1"
    const val kluent = "1.68"
    const val ktor = "2.3.1"
    const val logback = "1.2.11"
    const val logstashEncoder = "7.2"
    const val micrometerRegistry = "1.9.4"
    const val mockk = "1.12.4"
    const val nimbusjosejwt = "9.25.3"
    const val postgres = "42.5.1"
    val postgresEmbedded = if (Os.isFamily(Os.FAMILY_MAC)) "1.0.0" else "0.13.4"
    const val scala = "2.13.9"
    const val spek = "2.0.18"
}

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.4.1"
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

    implementation("io.ktor:ktor-server-auth-jwt:${Versions.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-server-call-id:${Versions.ktor}")
    implementation("io.ktor:ktor-server-status-pages:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-client-apache:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-jackson:${Versions.ktor}")

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")

    // Logging
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoder}")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:${Versions.ktor}")
    implementation("io.micrometer:micrometer-registry-prometheus:${Versions.micrometerRegistry}")

    // Database
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    testImplementation("com.opentable.components:otj-pg-embedded:${Versions.postgresEmbedded}")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:${Versions.kafka}", excludeLog4j)
    constraints {
        implementation("org.scala-lang:scala-library") {
            because("org.apache.kafka:kafka_2.13:${Versions.kafka} -> https://www.cve.org/CVERecord?id=CVE-2022-36944")
            version {
                require(Versions.scala)
            }
        }
    }
    implementation("io.confluent:kafka-avro-serializer:${Versions.confluent}", excludeLog4j)
    implementation("io.confluent:kafka-schema-registry:${Versions.confluent}", excludeLog4j)
    constraints {
        implementation("org.yaml:snakeyaml") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-25857/")
            version {
                require("1.31")
            }
        }
        implementation("org.glassfish:jakarta.el") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2021-28170/")
            version {
                require("3.0.4")
            }
        }
        implementation("com.google.protobuf:protobuf-java") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://www.cve.org/CVERecord?id=CVE-2022-3510")
            version {
                require("3.21.7")
            }
        }
        implementation("com.google.code.gson:gson") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://www.cve.org/CVERecord?id=CVE-2022-25647")
            version {
                require("2.8.9")
            }
        }
        implementation("org.json:json") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://nvd.nist.gov/vuln/detail/CVE-2022-45688")
            version {
                require("20231013")
            }
        }
        implementation("org.apache.zookeeper:zookeeper") {
            because("io.confluent:kafka-schema-registry:${Versions.confluent} -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.9.1")
            }
        }
    }
    testImplementation("no.nav:kafka-embedded-env:${Versions.kafkaEmbeddedEnv}", excludeLog4j)
    constraints {
        implementation("org.yaml:snakeyaml") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbeddedEnv} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-25857/")
            version {
                require("1.31")
            }
        }
        implementation("org.eclipse.jetty.http2:http2-server") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbeddedEnv} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-2048/")
            version {
                require("9.4.48.v20220622")
            }
        }
        implementation("com.google.protobuf:protobuf-java") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbeddedEnv} -> https://cwe.mitre.org/data/definitions/400.html")
            version {
                require("3.21.7")
            }
        }
    }

    testImplementation(kotlin("test"))
    testImplementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusjosejwt}")
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktor}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
    testImplementation("io.ktor:ktor-client-mock:${Versions.ktor}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin}")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin}")
    }
}

kotlin {
    jvmToolchain(17)
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
