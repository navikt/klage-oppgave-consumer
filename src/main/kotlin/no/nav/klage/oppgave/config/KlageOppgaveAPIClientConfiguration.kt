package no.nav.klage.oppgave.config

import no.nav.klage.oppgave.utils.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class KlageOppgaveAPIClientConfiguration(private val webClientBuilder: WebClient.Builder) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${KLAGE-OPPGAVE-API_SERVICE_URL}")
    private lateinit var klageOppgaveAPIServiceURL: String

    @Bean
    fun klageOppgaveAPIWebClient(): WebClient {
        return webClientBuilder
            .baseUrl("$klageOppgaveAPIServiceURL/internal")
            .build()
    }
}