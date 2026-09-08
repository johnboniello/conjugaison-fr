package com.johnb.conjugaisonfr

data class Challenge(
    val verb: Verb,
    val inf: String,
    val tense: String,
    val tenseName: String,
    val pIdx: Int,
    val answer: String,
    val personLabel: String,
    val impe: Boolean,
)

object Conj {
    val PERSON = listOf("je", "tu", "il", "nous", "vous", "ils")
    val PERSON_LABEL = listOf("je", "tu", "il / elle", "nous", "vous", "ils / elles")
    private val IMPE_PIDX = listOf(1, 3, 4)
    private val IMPE_LABEL = mapOf(1 to "tu", 3 to "nous", 4 to "vous")

    val LEVEL_TENSES = mapOf(
        1 to listOf("pres", "imp", "fut"),
        2 to listOf("pres", "imp", "fut", "pc"),
        3 to listOf("pres", "imp", "fut", "pc", "cond", "subj"),
        4 to listOf("pres", "imp", "fut", "pc", "cond", "subj", "impe"),
    )
    val LEVEL_DESC = mapOf(
        1 to "Présent · imparfait · futur simple",
        2 to "+ passé composé",
        3 to "+ conditionnel · subjonctif présent",
        4 to "+ impératif",
    )

    fun pick(activeInf: Collection<String>, level: Int): Challenge? {
        val active = activeInf.mapNotNull { ConjugationData.byInf(it) }
        if (active.isEmpty()) return null
        val tenses = LEVEL_TENSES[level] ?: LEVEL_TENSES[2]!!
        repeat(300) {
            val v = active.random()
            val avail = tenses.filter { v.t.containsKey(it) }
            if (avail.isEmpty()) return@repeat
            val tense = avail.random()
            val forms = v.t[tense]!!
            if (tense == "impe") {
                val cand = (0..2).filter { it < forms.size && forms[it] != null }
                if (cand.isEmpty()) return@repeat
                val k = cand.random()
                val pIdx = IMPE_PIDX[k]
                return Challenge(v, v.inf, tense, ConjugationData.TENSE_NAME[tense]!!, pIdx, forms[k]!!, IMPE_LABEL[pIdx]!!, true)
            } else {
                val cand = (0..5).filter { it < forms.size && forms[it] != null }
                if (cand.isEmpty()) return@repeat
                val pIdx = cand.random()
                return Challenge(v, v.inf, tense, ConjugationData.TENSE_NAME[tense]!!, pIdx, forms[pIdx]!!, PERSON_LABEL[pIdx], false)
            }
        }
        return null
    }

    /* ---------------- cloze (mode 2) ---------------- */
    const val BLANK = "_____"

    private val FRAMES = mapOf(
        "pres" to listOf("Maintenant, {s} {b} {t}.", "Tous les jours, {s} {b} {t}.", "En ce moment, {s} {b} {t}.", "D'habitude, {s} {b} {t}."),
        "imp" to listOf("Avant, {s} {b} {t} souvent.", "Quand j'étais petit, {s} {b} {t}.", "Autrefois, {s} {b} {t}.", "À cette époque-là, {s} {b} {t}."),
        "fut" to listOf("Demain, {s} {b} {t}.", "La semaine prochaine, {s} {b} {t}.", "Bientôt, {s} {b} {t}.", "Un jour, {s} {b} {t}."),
        "pc" to listOf("Hier, {s} {b} {t}.", "La semaine dernière, {s} {b} {t}.", "Ce matin, {s} {b} {t}.", "Récemment, {s} {b} {t}."),
        "cond" to listOf("À ta place, {s} {b} {t}.", "Si c'était possible, {s} {b} {t}.", "Avec plus de temps, {s} {b} {t}."),
        "subj" to listOf("Il faut que {s} {b} {t}.", "Je veux que {s} {b} {t}.", "Il est important que {s} {b} {t}.", "Bien que {s} {b} {t}, ça va."),
        "impe" to listOf("{b} {t} maintenant !", "{b} {t}, s'il te plaît !", "{b} {t} tout de suite !", "Allez, {b} {t} !"),
    )
    private val OBJ = mapOf(
        "manger" to "une pomme", "boire" to "de l'eau", "lire" to "un livre", "écrire" to "une lettre",
        "parler" to "à un ami", "regarder" to "la télé", "écouter" to "la radio", "jouer" to "au foot",
        "chanter" to "une chanson", "travailler" to "à la maison", "étudier" to "le français",
        "finir" to "le travail", "choisir" to "un dessert", "ouvrir" to "la porte",
        "prendre" to "le bus", "faire" to "les devoirs", "dire" to "la vérité", "voir" to "un film",
        "aller" to "à l'école", "partir" to "en voyage", "venir" to "chez nous", "sortir" to "le soir",
        "acheter" to "du pain", "donner" to "un cadeau", "aimer" to "le chocolat", "trouver" to "la solution",
        "chercher" to "ses clés", "attendre" to "le bus", "répondre" to "à la question", "gagner" to "le match",
        "dormir" to "tôt", "courir" to "vite", "mettre" to "la table", "perdre" to "ses clés",
        "porter" to "un sac", "montrer" to "le chemin", "rendre" to "le livre",
    )
    private val VOWEL = Regex("^[aàâäeéèêëiîïoôöuùûüyh]", RegexOption.IGNORE_CASE)

    fun cloze(ch: Challenge): String {
        val frame = FRAMES[ch.tense]!!.random()
        val tail = OBJ[ch.inf] ?: ""
        var s: String = if (ch.tense == "impe") {
            frame.replace("{b}", BLANK).replace("{t}", tail)
        } else {
            val subj = if (ch.pIdx == 0) {
                if (VOWEL.containsMatchIn(ch.answer)) "j'" else "je"
            } else PERSON[ch.pIdx]
            frame.replace("{s}", subj).replace("{b}", BLANK).replace("{t}", tail)
                .replace("j' $BLANK", "j'$BLANK")
        }
        return s.replace(Regex("\\s+([.!?,])"), "$1").replace(Regex("\\s{2,}"), " ").trim()
    }

    fun clozeSpoken(sentence: String): String =
        sentence.replace(BLANK, ", … ,").replace(Regex("\\s{2,}"), " ")

    /* ---------------- multiple choice (mode 3) ---------------- */
    fun mcOptions(ch: Challenge, level: Int): Pair<List<String>, Int> {
        val pool = LinkedHashSet<String>()
        ch.verb.t[ch.tense]?.forEach { if (it != null && it != ch.answer) pool.add(it) }
        for (t in LEVEL_TENSES[level] ?: emptyList()) {
            if (t == ch.tense) continue
            val forms = ch.verb.t[t] ?: continue
            val f = if (t == "impe") {
                val k = IMPE_PIDX.indexOf(ch.pIdx)
                if (k in forms.indices) forms[k] else null
            } else if (ch.pIdx in forms.indices) forms[ch.pIdx] else null
            if (f != null && f != ch.answer) pool.add(f)
        }
        val distractors = pool.toMutableList().also { it.shuffle() }.take(3).toMutableList()
        var n = 1
        while (distractors.size < 3) {
            val c = ch.answer + if (n == 1) "s" else "nt"
            if (c != ch.answer && c !in distractors) distractors.add(c)
            n++
        }
        val opts = (distractors + ch.answer).toMutableList().also { it.shuffle() }
        return opts to opts.indexOf(ch.answer)
    }
}
