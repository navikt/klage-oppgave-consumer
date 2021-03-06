package no.nav.klage.oppgave.services

import org.springframework.stereotype.Service

@Service
class HjemmelParsingService {

    private val hjemmelRegex = """(\d{1,2}-\d{1,2})+""".toRegex()

    fun extractHjemmel(text: String): List<String> = hjemmelRegex.findAll(text).collect()

    private fun Sequence<MatchResult>.collect(): List<String> {
        val list = mutableListOf<String>()
        this.iterator().forEachRemaining {
            val hjemmel = it.value.replace("§", "").trim()
            if (hjemmel.isValidHjemmel()) {
                list.add(hjemmel)
            }
        }
        return list
    }

    private fun String.isValidHjemmel(): Boolean = hjemler.contains(this)

    private val hjemler = listOf(
        "8-1",
        "8-2",
        "8-3",
        "8-4",
        "8-5",
        "8-6",
        "8-7",
        "8-8",
        "8-9",
        "8-10",
        "8-11",
        "8-12",
        "8-13",
        "8-14",
        "8-15",
        "8-16",
        "8-17",
        "8-18",
        "8-19",
        "8-20",
        "8-21",
        "8-22",
        "8-23",
        "8-24",
        "8-25",
        "8-26",
        "8-27",
        "8-28",
        "8-29",
        "8-30",
        "8-31",
        "8-32",
        "8-33",
        "8-34",
        "8-35",
        "8-36",
        "8-37",
        "8-38",
        "8-39",
        "8-40",
        "8-41",
        "8-42",
        "8-43",
        "8-44",
        "8-45",
        "8-46",
        "8-47",
        "8-48",
        "8-49",
        "8-50",
        "8-51",
        "8-52",
        "8-53",
        "8-54",
        "8-55",
        "21-7",
        "21-12",
        "22-3",
        "22-13",
        "22-15"
    )

    /*
    fun getHjemmelFromOppgaveKopi(oppgaveKopi: OppgaveKopi): MutableList<Hjemmel> {
        val metadataHjemmel = oppgaveKopi.metadata.find {
            it.noekkel == MetadataNoekkel.HJEMMEL && it.verdi.matchesHjemmelRegex()
        }
        if (metadataHjemmel != null) {
            return mutableListOf(generateHjemmelFromText(metadataHjemmel.verdi))
        }
        val hjemler = hjemmelRegex.findAll(oppgaveKopi.beskrivelse ?: "").collect()
        if (hjemler.isNotEmpty()) {
            return mutableListOf(generateHjemmelFromText(hjemler[0]))
        }
        return mutableListOf()
    }

    fun getHjemmelFromOppgaveKopiVersjon(oppgaveKopiVersjon: OppgaveKopiVersjon): String? =
        oppgaveKopiVersjon.metadata
            .find { it.noekkel == MetadataNoekkel.HJEMMEL && it.verdi.matchesHjemmelRegex() }
            ?.verdi
            ?: hjemmelRegex.findAll(oppgaveKopiVersjon.beskrivelse ?: "").collect().firstOrNull()


    private fun Sequence<MatchResult>.collect(): List<String> {
        val list = mutableListOf<String>()
        this.iterator().forEachRemaining {
            val hjemmel = it.value.replace("§", "").trim()
            list.add(hjemmel)
        }
        return list
    }

    private fun String.matchesHjemmelRegex() = hjemmelRegex.find(this) != null
*/
}
