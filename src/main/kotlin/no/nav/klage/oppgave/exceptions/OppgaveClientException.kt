package no.nav.klage.oppgave.exceptions

class OppgaveClientException : Exception {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
