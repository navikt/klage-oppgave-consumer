package no.nav.klage.oppgave.domain

data class BatchUpdateResponse(
    val finished: String,
    val status: ResponseStatus,
    val message: String
)

enum class ResponseStatus {
    OK, PARTIAL, ERROR
}
