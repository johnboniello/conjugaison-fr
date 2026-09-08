package com.johnb.conjugaisonfr

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.TextView

/** Mode 1 — "Conjugue" : the verb, tense and person are given; type the form. */
class Mode1Activity : TypedModeActivity() {
    override val layoutRes = R.layout.activity_mode1
    override val titleText = "Conjugue"

    private lateinit var promptView: TextView

    override fun bindExtraViews() {
        promptView = findViewById(R.id.promptView)
    }

    override fun renderChallenge(ch: Challenge) {
        val sb = SpannableStringBuilder()
        val vStart = sb.length
        sb.append(ch.inf)
        sb.setSpan(RelativeSizeSpan(1.7f), vStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), vStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), vStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (ch.verb.en.isNotBlank()) {
            val eStart = sb.length
            sb.append("\n(").append(ch.verb.en).append(")")
            sb.setSpan(ForegroundColorSpan(0xFF5F6368.toInt()), eStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        sb.append("\n\n")
        val pStart = sb.length
        sb.append(ch.personLabel)
        sb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), pStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("  ·  ")
        val tStart = sb.length
        sb.append(ch.tenseName)
        sb.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), tStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), tStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        promptView.text = sb
    }
}
