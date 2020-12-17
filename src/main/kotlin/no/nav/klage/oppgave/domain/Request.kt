package no.nav.klage.oppgave.domain

import java.time.LocalDate

data class BatchUpdateRequest(
    val dryRun: Boolean,
    val oppgaveId: Long?,
    val includeFrom: LocalDate?
)
