package com.johnb.conjugaisonfr

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Pick which verbs are in rotation for the three games. */
class VerbPickerActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private lateinit var countView: TextView
    private val boxes = HashMap<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verbpicker)
        padForSystemBars()
        supportActionBar?.title = "Choisir les verbes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ConjugationData.load(this)

        grid = findViewById(R.id.verbGrid)
        countView = findViewById(R.id.pickCount)

        findViewById<Button>(R.id.pickAll).setOnClickListener { setAll(ConjugationData.all().map { it.inf }) }
        findViewById<Button>(R.id.pickNone).setOnClickListener { setAll(emptyList()) }
        findViewById<Button>(R.id.pickCore).setOnClickListener { setAll(Store.CORE.filter { ConjugationData.byInf(it) != null }) }
        findViewById<Button>(R.id.pickG1).setOnClickListener { setAll(ConjugationData.all().filter { it.group == 1 }.map { it.inf }) }
        findViewById<Button>(R.id.pickG2).setOnClickListener { setAll(ConjugationData.all().filter { it.group == 2 }.map { it.inf }) }
        findViewById<Button>(R.id.pickG3).setOnClickListener { setAll(ConjugationData.all().filter { it.group == 3 }.map { it.inf }) }

        buildGrid()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun buildGrid() {
        val active = Store.activeVerbs(this)
        grid.removeAllViews()
        grid.columnCount = 2
        for (v in ConjugationData.all()) {
            val cb = CheckBox(this)
            cb.text = v.inf
            cb.textSize = 16f
            cb.isChecked = active.contains(v.inf)
            cb.setOnCheckedChangeListener { _, checked ->
                val cur = Store.activeVerbs(this)
                if (checked) cur.add(v.inf) else cur.remove(v.inf)
                Store.setActiveVerbs(this, cur)
                updateCount()
            }
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            lp.setMargins(0, dp(2), dp(8), dp(2))
            cb.layoutParams = lp
            boxes[v.inf] = cb
            grid.addView(cb)
        }
        updateCount()
    }

    private fun setAll(list: List<String>) {
        val set = list.toMutableSet()
        Store.setActiveVerbs(this, set)
        for ((inf, cb) in boxes) {
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = set.contains(inf)
            cb.setOnCheckedChangeListener { _, checked ->
                val cur = Store.activeVerbs(this)
                if (checked) cur.add(inf) else cur.remove(inf)
                Store.setActiveVerbs(this, cur)
                updateCount()
            }
        }
        updateCount()
    }

    private fun updateCount() {
        val n = Store.activeVerbs(this).count { ConjugationData.byInf(it) != null }
        countView.text = if (n == 1) "1 verbe coché" else "$n verbes cochés"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
