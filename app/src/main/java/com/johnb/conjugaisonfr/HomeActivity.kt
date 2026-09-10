package com.johnb.conjugaisonfr

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private var tts: TextToSpeech? = null
    private lateinit var levelButtons: List<Button>
    private lateinit var levelDesc: TextView
    private lateinit var verbCount: TextView

    private val purple = 0xFF5E35B1.toInt()
    private val purpleLite = 0xFFEDE7F6.toInt()
    private val purpleDark = 0xFF4527A0.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        padForSystemBars()
        supportActionBar?.title = "Conjugaison FR"
        ConjugationData.load(this)

        levelDesc = findViewById(R.id.levelDesc)
        verbCount = findViewById(R.id.verbCount)
        levelButtons = listOf(
            findViewById(R.id.lvl1), findViewById(R.id.lvl2),
            findViewById(R.id.lvl3), findViewById(R.id.lvl4)
        )
        levelButtons.forEachIndexed { i, b ->
            b.setOnClickListener { Store.setLevel(this, i + 1); syncLevel() }
        }
        findViewById<Button>(R.id.mode1Btn).setOnClickListener { start(Mode1Activity::class.java) }
        findViewById<Button>(R.id.mode2Btn).setOnClickListener { start(Mode2Activity::class.java) }
        findViewById<Button>(R.id.mode3Btn).setOnClickListener { start(Mode3Activity::class.java) }
        findViewById<Button>(R.id.verbsBtn).setOnClickListener { start(VerbPickerActivity::class.java) }

        tts = TextToSpeech(this) { s ->
            if (s == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.FRANCE)
        }
    }

    override fun onResume() {
        super.onResume()
        syncLevel()
        val n = Store.activeVerbs(this).count { ConjugationData.byInf(it) != null }
        verbCount.text = if (n == 1) "1 verbe actif" else "$n verbes actifs"
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu); return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_verbs -> { start(VerbPickerActivity::class.java); true }
        R.id.action_rate -> { Voice.rateDialog(this, tts); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun syncLevel() {
        val lvl = Store.level(this)
        levelButtons.forEachIndexed { i, b ->
            val on = i + 1 == lvl
            b.backgroundTintList = ColorStateList.valueOf(if (on) purple else purpleLite)
            b.setTextColor(if (on) 0xFFFFFFFF.toInt() else purpleDark)
        }
        levelDesc.text = Conj.LEVEL_DESC[lvl]
    }

    private fun start(cls: Class<*>) = startActivity(Intent(this, cls))
}
