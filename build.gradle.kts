
group = "no.nav.syfo"
version = "1.0.0"

val confluent = "7.8.0"
val flyway = "11.3.0"
val hikari = "6.2.1"
val jackson = "2.18.2"
val jetty = "9.4.57.v20241219"
val kafka = "3.9.0"
val kluent = "1.73"
val ktor = "3.0.3"
val logback = "1.5.16"
val logstashEncoder = "8.0"
val micrometerRegistry = "1.12.13"
val mockk = "1.13.16"
val nimbusjosejwt = "10.0.1"
val postgres = "42.7.5"
val postgresEmbedded = "2.1.0"
val spek = "2.0.19"

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.5"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
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
    implementation("org.flywaydb:flyway-database-postgresql:$flyway")
    implementation("org.postgresql:postgresql:$postgres")
    implementation("com.zaxxer:HikariCP:$hikari")
    testImplementation("io.zonky.test:embedded-postgres:$postgresEmbedded")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:$kafka", excludeLog4j)
    constraints {
        implementation("org.apache.zookeeper:zookeeper") {
            because("org.apache.kafka:kafka_2.13:$kafka -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.9.3")
            }
        }
        implementation("org.bitbucket.b_c:jose4j") {
            because("org.apache.kafka:kafka_2.13:$kafka -> https://github.com/advisories/GHSA-6qvw-249j-h44c")
            version {
                require("0.9.6")
            }
        }
    }
    implementation("io.confluent:kafka-avro-serializer:$confluent", excludeLog4j)
    constraints {
        implementation("org.apache.avro:avro") {
            because("io.confluent:kafka-avro-serializer:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-39410")
            version {
                require("1.11.4")
            }
        }
        implementation("org.apache.commons:commons-compress") {
            because("org.apache.commons:commons-compress:1.22 -> https://www.cve.org/CVERecord?id=CVE-2012-2098")
            version {
                require("1.27.1")
            }
        }
    }
    implementation("io.confluent:kafka-schema-registry:$confluent", excludeLog4j)
    constraints {
        implementation("io.github.classgraph:classgraph") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2021-47621")
            version {
                require("4.8.179")
            }
        }
        implementation("org.json:json") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-5072")
            version {
                require("20250107")
            }
        }
        implementation("org.apache.mina:mina-core") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2024-52046")
            version {
                require("2.2.4")
            }
        }
        implementation("org.eclipse.jetty:jetty-server") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-36478")
            version {
                require(jetty)
            }
        }
        implementation("org.eclipse.jetty:jetty-xml") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-36478")
            version {
                require(jetty)
            }
        }
        implementation("org.eclipse.jetty:jetty-servlets") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-36478")
            version {
                require(jetty)
            }
        }
        implementation("org.eclipse.jetty.http2:http2-server") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-36478")
            version {
                require(jetty)
            }
        }
    }
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
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
    jar {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    test {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
    }
}
