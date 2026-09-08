package com.johnb.conjugaisonfr

import android.content.Context

/** SharedPreferences: chosen level, voice rate, and which verbs are in rotation. */
object Store {
    private const val P = "conjugaison_fr"

    val CORE = listOf(
        "être", "avoir", "aller", "faire", "dire", "pouvoir", "vouloir", "savoir", "voir",
        "venir", "prendre", "parler", "aimer", "manger", "donner", "trouver", "mettre",
        "partir", "sortir", "devoir", "vivre", "écrire", "lire", "finir", "choisir",
        "attendre", "répondre", "entrer", "rester", "passer", "regarder", "écouter", "jouer",
        "acheter", "payer", "commencer", "appeler", "courir", "dormir", "ouvrir", "boire"
    )

    private fun p(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun level(c: Context) = p(c).getInt("level", 2).coerceIn(1, 4)
    fun setLevel(c: Context, n: Int) = p(c).edit().putInt("level", n).apply()

    fun rate(c: Context) = p(c).getFloat("rate", 0.9f)
    fun setRate(c: Context, r: Float) = p(c).edit().putFloat("rate", r).apply()

    fun activeVerbs(c: Context): MutableSet<String> {
        val s = p(c).getStringSet("verbs", null)
        return if (s.isNullOrEmpty()) CORE.toMutableSet() else s.toMutableSet()
    }
    fun setActiveVerbs(c: Context, v: Set<String>) =
        p(c).edit().putStringSet("verbs", v).apply()
}
