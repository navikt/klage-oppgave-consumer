package no.nav.klage.oppgave.config

import no.nav.slackposter.SlackClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackConfiguration(
    @Value("\${SLACK_URL}")
    val url: String,
    @Value("\${SLACK_CHANNEL_ID}")
    val channelId: String,
    @Value("\${NAIS_CLUSTER_NAME}")
    val cluster: String,
    @Value("\${spring.application.name}")
    val appName: String
) {

    @Bean
    fun slackClient(): SlackClient = SlackClient(url, channelId, appName, cluster)
}
