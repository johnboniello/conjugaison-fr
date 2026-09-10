package com.johnb.conjugaisonfr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/** Shared flow for the two typed modes (Conjugue / En contexte). */
abstract class TypedModeActivity : AppCompatActivity() {

    protected abstract val layoutRes: Int
    protected abstract val titleText: String
    protected abstract fun bindExtraViews()
    protected abstract fun renderChallenge(ch: Challenge)
    protected open fun onListen(ch: Challenge) {}

    protected var tts: TextToSpeech? = null
    protected var ttsReady = false
    protected var ch: Challenge? = null

    private lateinit var progressView: TextView
    private lateinit var emptyView: TextView
    private lateinit var bodyBox: View
    private lateinit var hintBtn: Button
    private lateinit var hintLine: TextView
    private lateinit var guessView: TextView
    private lateinit var keyboardGrid: GridLayout
    private lateinit var backspaceBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var checkBtn: Button
    private lateinit var resultBox: View
    private lateinit var answerRow: View
    private lateinit var targetView: TextView
    private lateinit var yourView: TextView
    private lateinit var summaryView: TextView
    private lateinit var retryBtn: Button
    private lateinit var revealBtn: Button
    private lateinit var nextBtn: Button

    private val guess = StringBuilder()
    private var n = 0
    private var score = 0
    private var aided = 0
    private var scored = false
    private var aidedThis = false
    private var revealCount = 0
    private var attempts = 0
    private var revealed = false

    private val green = 0xFF2E7D32.toInt()
    private val red = 0xFFC62828.toInt()
    private val grey = 0xFF9E9E9E.toInt()
    private val revealAfter = 3
    private val questions = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        padForSystemBars()
        supportActionBar?.title = titleText
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ConjugationData.load(this)

        progressView = findViewById(R.id.progressView)
        emptyView = findViewById(R.id.emptyView)
        bodyBox = findViewById(R.id.bodyBox)
        hintBtn = findViewById(R.id.hintBtn)
        hintLine = findViewById(R.id.hintLine)
        guessView = findViewById(R.id.guessView)
        keyboardGrid = findViewById(R.id.keyboardGrid)
        backspaceBtn = findViewById(R.id.backspaceBtn)
        clearBtn = findViewById(R.id.clearBtn)
        checkBtn = findViewById(R.id.checkBtn)
        resultBox = findViewById(R.id.resultBox)
        answerRow = findViewById(R.id.answerRow)
        targetView = findViewById(R.id.targetView)
        yourView = findViewById(R.id.yourView)
        summaryView = findViewById(R.id.summaryView)
        retryBtn = findViewById(R.id.retryBtn)
        revealBtn = findViewById(R.id.revealBtn)
        nextBtn = findViewById(R.id.nextBtn)

        bindExtraViews()
        buildKeyboard()

        backspaceBtn.setOnClickListener {
            if (guess.isNotEmpty()) { guess.deleteCharAt(guess.length - 1); refreshGuess() }
        }
        clearBtn.setOnClickListener { guess.clear(); refreshGuess() }
        checkBtn.setOnClickListener { onCheck() }
        retryBtn.setOnClickListener { resultBox.visibility = View.GONE }
        revealBtn.setOnClickListener { revealAnswer() }
        nextBtn.setOnClickListener { nextRound() }
        hintBtn.setOnClickListener { revealHint() }
        findViewById<Button?>(R.id.listenBtn)?.setOnClickListener { ch?.let { onListen(it) } }

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
        guess.clear()
        scored = false; aidedThis = false; revealed = false
        revealCount = 0; attempts = 0
        n++
        resultBox.visibility = View.GONE
        hintLine.visibility = View.GONE
        progressView.text = progressText()
        renderChallenge(c)
        refreshGuess()
    }

    private fun progressText() =
        "Question $n     Score : $score" + (if (aided > 0) "   ·   avec aide : $aided" else "")

    private fun refreshGuess() {
        guessView.text = if (guess.isEmpty()) "—" else guess.toString().toCharArray().joinToString(" ")
    }

    private fun revealHint() {
        val a = ch?.answer ?: return
        if (revealCount < a.length) revealCount++
        aidedThis = true
        val sb = StringBuilder("Indice : ")
        a.forEachIndexed { i, c -> sb.append(if (i < revealCount) (if (c == ' ') '␣' else c) else '_').append(' ') }
        hintLine.text = sb.toString().trim()
        hintLine.visibility = View.VISIBLE
    }

    private fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun onCheck() {
        val c = ch ?: return
        if (guess.isBlank()) { toast("Écris d'abord ta réponse."); return }
        val res = SpellingChecker.check(c.answer, guess.toString())
        yourView.text = renderRow(res, targetRow = false)
        if (norm(guess.toString()) == norm(c.answer)) {
            if (!scored) { if (aidedThis || revealed) aided++ else score++; scored = true }
            answerRow.visibility = View.VISIBLE
            targetView.text = renderRow(SpellingChecker.check(c.answer, c.answer), targetRow = true)
            summaryView.setTextColor(green)
            summaryView.text = if (aidedThis || revealed) "Bravo ! 🎉  (avec aide)" else "Bravo ! 🎉"
            retryBtn.visibility = View.GONE
            revealBtn.visibility = View.GONE
            progressView.text = progressText()
            Feedback.correct(this, tts, ttsReady)
        } else {
            attempts++
            if (!revealed) answerRow.visibility = View.GONE
            summaryView.setTextColor(red)
            summaryView.text = "Essaie encore — regarde les lettres en rouge."
            retryBtn.visibility = View.VISIBLE
            revealBtn.visibility =
                if (attempts >= revealAfter && !revealed) View.VISIBLE else View.GONE
            Feedback.wrong(this, tts, ttsReady)
        }
        resultBox.visibility = View.VISIBLE
    }

    private fun revealAnswer() {
        val c = ch ?: return
        revealed = true; aidedThis = true
        answerRow.visibility = View.VISIBLE
        targetView.text = greenWord(c.answer)
        summaryView.setTextColor(0xFF1A1A1A.toInt())
        summaryView.text = "La bonne réponse : ${c.answer}"
        revealBtn.visibility = View.GONE
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

    private fun buildKeyboard() {
        val keys = ('a'..'z').map { it.toString() } +
            listOf("é", "è", "ê", "ë", "à", "â", "î", "ï", "ô", "û", "ù", "ü", "ç", " ")
        keyboardGrid.columnCount = 7
        val m = dp(3)
        for (k in keys) {
            val isSpace = k == " "
            val b = Button(this)
            b.text = if (isSpace) "espace" else k
            b.isAllCaps = false
            b.textSize = if (isSpace) 13f else 18f
            b.setPadding(0, dp(6), 0, dp(6))
            b.minWidth = 0
            b.minimumWidth = 0
            val span = if (isSpace) 2 else 1
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, span, span.toFloat())
            lp.setMargins(m, m, m, m)
            b.layoutParams = lp
            b.setOnClickListener { guess.append(k); refreshGuess() }
            keyboardGrid.addView(b)
        }
    }

    private fun greenWord(w: String): CharSequence {
        val sb = SpannableStringBuilder()
        for (c in w) {
            val s = sb.length
            sb.append(if (c == ' ') "␣" else c.toString())
            sb.append("  ")
            sb.setSpan(ForegroundColorSpan(green), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    @SuppressLint("SetTextI18n")
    private fun renderRow(res: SpellingChecker.Result, targetRow: Boolean): CharSequence {
        val sb = SpannableStringBuilder()
        for (op in res.ops) {
            val ch: String
            val color: Int
            var strike = false
            var underline = false
            when (op.kind) {
                SpellingChecker.Kind.MATCH -> { ch = disp(if (targetRow) op.target else op.guess); color = green }
                SpellingChecker.Kind.SUB -> { ch = disp(if (targetRow) op.target else op.guess); color = red }
                SpellingChecker.Kind.MISSING ->
                    if (targetRow) { ch = disp(op.target); color = red; underline = true }
                    else { ch = "_"; color = grey }
                SpellingChecker.Kind.EXTRA ->
                    if (targetRow) { ch = "·"; color = grey }
                    else { ch = disp(op.guess); color = red; strike = true }
            }
            val start = sb.length
            sb.append(ch); sb.append("  ")
            sb.setSpan(ForegroundColorSpan(color), start, start + ch.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (strike) sb.setSpan(StrikethroughSpan(), start, start + ch.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (underline) sb.setSpan(UnderlineSpan(), start, start + ch.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }
    private fun disp(c: Char?): String = when (c) { null -> ""; ' ' -> "␣"; else -> c.toString() }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
