package no.nav.klage.oppgave.config

import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.io.File
import java.time.Duration


@Configuration
class KafkaConfiguration(
    @Value("\${KAFKA_BOOTSTRAP_SERVERS}")
    private val bootstrapServers: String,
    @Value("\${KAFKA_GROUP_ID}")
    private val groupId: String,
    @Value("\${SERVICE_USER_USERNAME}")
    private val username: String,
    @Value("\${SERVICE_USER_PASSWORD}")
    private val password: String
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.consumerFactory = consumerFactory()

        factory.setErrorHandler { thrownException, data ->
            logger.error("There was a problem during processing of the record. See secure logs for details.")
            secureLogger.error("There was a problem during processing of the record: $data", thrownException)
        }

        //Retry consumer/listener even if authorization fails
        factory.setContainerCustomizer { container ->
            container.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)
        }

        return factory;
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<Long, String> {
        return DefaultKafkaConsumerFactory(consumerProps());
    }

    private fun consumerProps(): Map<String, Any> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = true
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props["spring.deserializer.key.delegate.class"] = LongDeserializer::class.java
        props["spring.deserializer.value.delegate.class"] = StringDeserializer::class.java
        props[SaslConfigs.SASL_JAAS_CONFIG] =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        props[SaslConfigs.SASL_MECHANISM] = "PLAIN"
        System.getenv("NAV_TRUSTSTORE_PATH")?.let {
            props[SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = File(it).absolutePath
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = System.getenv("NAV_TRUSTSTORE_PASSWORD")
        }
        return props
    }

}
