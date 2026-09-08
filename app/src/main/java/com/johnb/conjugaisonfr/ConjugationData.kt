package com.johnb.conjugaisonfr

import android.content.Context
import org.json.JSONObject

data class Verb(
    val inf: String,
    val en: String,
    val group: Int,
    val aux: String,
    val pp: String?,
    val impers: Boolean,
    /** tense key -> forms; length 6 (je..ils) for simple tenses & pc, length 3 (tu/nous/vous) for impe. Nulls = no form. */
    val t: Map<String, List<String?>>,
)

object ConjugationData {
    private var verbs: List<Verb> = emptyList()
    private var map: Map<String, Verb> = emptyMap()
    var loaded = false
        private set

    val TENSE_NAME = mapOf(
        "pres" to "présent", "imp" to "imparfait", "fut" to "futur simple",
        "cond" to "conditionnel présent", "subj" to "subjonctif présent",
        "impe" to "impératif", "pc" to "passé composé",
    )

    fun load(ctx: Context) {
        if (loaded) return
        val txt = ctx.assets.open("verbs.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(txt).getJSONArray("verbs")
        val list = ArrayList<Verb>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tObj = o.getJSONObject("t")
            val t = HashMap<String, List<String?>>()
            val keys = tObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val ja = tObj.getJSONArray(k)
                val forms = ArrayList<String?>(ja.length())
                for (j in 0 until ja.length()) forms.add(if (ja.isNull(j)) null else ja.getString(j))
                t[k] = forms
            }
            list.add(
                Verb(
                    inf = o.getString("inf"),
                    en = o.optString("en", ""),
                    group = o.optInt("group", 3),
                    aux = o.optString("aux", "avoir"),
                    pp = if (o.isNull("pp")) null else o.optString("pp"),
                    impers = o.optBoolean("impers", false),
                    t = t,
                )
            )
        }
        verbs = list
        map = list.associateBy { it.inf }
        loaded = true
    }

    fun all(): List<Verb> = verbs
    fun byInf(inf: String): Verb? = map[inf]
}
