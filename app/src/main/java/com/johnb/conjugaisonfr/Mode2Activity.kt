package com.johnb.conjugaisonfr

import android.graphics.Typeface
import android.speech.tts.TextToSpeech
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView

/** Mode 2 — "En contexte" : fill the blank in a sentence whose time cue signals the tense. */
class Mode2Activity : TypedModeActivity() {
    override val layoutRes = R.layout.activity_mode2
    override val titleText = "En contexte"

    private lateinit var clozeView: TextView
    private var sentence = ""

    override fun bindExtraViews() {
        clozeView = findViewById(R.id.clozeView)
    }

    override fun renderChallenge(ch: Challenge) {
        sentence = Conj.cloze(ch)
        val sb = SpannableStringBuilder(sentence)
        val bi = sentence.indexOf(Conj.BLANK)
        if (bi >= 0) {
            sb.setSpan(StyleSpan(Typeface.BOLD), bi, bi + Conj.BLANK.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), bi, bi + Conj.BLANK.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val iStart = sb.length
        sb.append("   (").append(ch.inf).append(")")
        sb.setSpan(StyleSpan(Typeface.BOLD), iStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), iStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val tStart = sb.length
        sb.append("\n→ ").append(ch.tenseName).append(" · ").append(ch.personLabel)
        sb.setSpan(ForegroundColorSpan(0xFF5F6368.toInt()), tStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        clozeView.text = sb
    }

    override fun onListen(ch: Challenge) {
        if (!ttsReady || sentence.isEmpty()) return
        tts?.setSpeechRate(Store.rate(this))
        tts?.speak(Conj.clozeSpoken(sentence), TextToSpeech.QUEUE_FLUSH, null, "sentence")
    }
}
