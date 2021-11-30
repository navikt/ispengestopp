package no.nav.syfo.testutils

import no.nav.common.KafkaEnvironment

fun testKafka(
    autoStart: Boolean = false,
    withSchemaRegistry: Boolean = false,
    topicNames: List<String> = listOf(
        "apen-isyfo-stoppautomatikk",
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    withSchemaRegistry = withSchemaRegistry,
    topicNames = topicNames,
)
