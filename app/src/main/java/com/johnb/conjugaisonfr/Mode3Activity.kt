package com.johnb.conjugaisonfr

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/** Mode 3 — "Choix multiple" : pick the right form out of four. */
class Mode3Activity : AppCompatActivity() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var progressView: TextView
    private lateinit var emptyView: TextView
    private lateinit var bodyBox: View
    private lateinit var promptView: TextView
    private lateinit var feedbackView: TextView
    private lateinit var nextBtn: Button
    private val opts = ArrayList<Button>(4)

    private var ch: Challenge? = null
    private var correctIdx = -1
    private var n = 0
    private var score = 0
    private var aided = 0
    private var solved = false
    private var wrong = false
    private val questions = 15

    private val purple = 0xFF5E35B1.toInt()
    private val green = 0xFF2E7D32.toInt()
    private val red = 0xFFC62828.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode3)
        padForSystemBars()
        supportActionBar?.title = "Choix multiple"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ConjugationData.load(this)

        progressView = findViewById(R.id.progressView)
        emptyView = findViewById(R.id.emptyView)
        bodyBox = findViewById(R.id.bodyBox)
        promptView = findViewById(R.id.promptView)
        feedbackView = findViewById(R.id.feedbackView)
        nextBtn = findViewById(R.id.nextBtn)
        opts.add(findViewById(R.id.opt0))
        opts.add(findViewById(R.id.opt1))
        opts.add(findViewById(R.id.opt2))
        opts.add(findViewById(R.id.opt3))
        opts.forEachIndexed { i, b -> b.setOnClickListener { pick(i) } }
        nextBtn.setOnClickListener { nextRound() }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.FRANCE)
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        startSeries()
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        Feedback.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu); return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_verbs -> { startActivity(Intent(this, VerbPickerActivity::class.java)); true }
        R.id.action_rate -> { Voice.rateDialog(this, tts); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun startSeries() {
        val active = Store.activeVerbs(this).filter { ConjugationData.byInf(it) != null }
        if (active.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            bodyBox.visibility = View.GONE
            progressView.text = ""
            return
        }
        emptyView.visibility = View.GONE
        bodyBox.visibility = View.VISIBLE
        n = 0; score = 0; aided = 0
        round()
    }

    private fun round() {
        val c = Conj.pick(Store.activeVerbs(this), Store.level(this))
        if (c == null) {
            emptyView.visibility = View.VISIBLE
            bodyBox.visibility = View.GONE
            return
        }
        ch = c
        solved = false; wrong = false
        n++
        feedbackView.text = ""
        nextBtn.visibility = View.GONE
        promptView.text = promptSpan(c)
        val (options, idx) = Conj.mcOptions(c, Store.level(this))
        correctIdx = idx
        opts.forEachIndexed { i, b ->
            b.text = options.getOrElse(i) { "" }
            b.isEnabled = true
            b.backgroundTintList = ColorStateList.valueOf(purple)
        }
        progressView.text = progressText()
    }

    private fun progressText() =
        "Question $n     Score : $score" + (if (aided > 0) "   ·   avec aide : $aided" else "")

    private fun pick(i: Int) {
        if (solved || !opts[i].isEnabled) return
        if (i == correctIdx) {
            solved = true
            opts[i].backgroundTintList = ColorStateList.valueOf(green)
            opts.forEach { it.isEnabled = false }
            feedbackView.setTextColor(green)
            feedbackView.text = if (wrong) "Bravo ! (avec aide)" else "Bravo ! 🎉"
            if (wrong) aided++ else score++
            progressView.text = progressText()
            Feedback.correct(this, tts, ttsReady)
            nextBtn.visibility = View.VISIBLE
        } else {
            wrong = true
            opts[i].isEnabled = false
            opts[i].backgroundTintList = ColorStateList.valueOf(red)
            feedbackView.setTextColor(red)
            feedbackView.text = "Essaie encore."
            Feedback.wrong(this, tts, ttsReady)
        }
    }

    private fun nextRound() {
        if (n >= questions) {
            AlertDialog.Builder(this)
                .setTitle("Série terminée !")
                .setMessage("Sans aide : $score / $questions\nAvec aide : $aided")
                .setPositiveButton("Recommencer") { _, _ -> startSeries() }
                .setNegativeButton("Retour") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }
        round()
    }

    private fun promptSpan(c: Challenge): CharSequence {
        val sb = SpannableStringBuilder()
        val vs = sb.length
        sb.append(c.inf)
        sb.setSpan(RelativeSizeSpan(1.7f), vs, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(Typeface.BOLD), vs, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), vs, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (c.verb.en.isNotBlank()) {
            val es = sb.length
            sb.append("\n(").append(c.verb.en).append(")")
            sb.setSpan(ForegroundColorSpan(0xFF5F6368.toInt()), es, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        sb.append("\n\n")
        val ps = sb.length
        sb.append(c.personLabel)
        sb.setSpan(StyleSpan(Typeface.BOLD), ps, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("  ·  ")
        val ts = sb.length
        sb.append(c.tenseName)
        sb.setSpan(StyleSpan(Typeface.ITALIC), ts, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(0xFF4527A0.toInt()), ts, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }
}
