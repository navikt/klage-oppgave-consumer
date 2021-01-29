package no.nav.klage.oppgave.domain

import java.time.LocalDate

data class BatchUpdateRequest(
    val dryRun: Boolean,
    val oppgaveId: Long?,
    val includeFrom: LocalDate?
)

data class BatchStoreRequest(
    val dryRun: Boolean,
    val includeFrom: LocalDate?,
    val tema: String?,
    val behandlingstype: String?,
    val tildeltEnhetsnr: String?
)
