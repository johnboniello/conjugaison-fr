package com.johnb.conjugaisonfr

import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

object Voice {
    private val RATE_LABELS = arrayOf("Très lente", "Lente", "Normale", "Assez rapide", "Rapide")
    private val RATE_VALUES = floatArrayOf(0.5f, 0.7f, 0.9f, 1.1f, 1.3f)

    fun rateDialog(activity: AppCompatActivity, tts: TextToSpeech?) {
        val current = Store.rate(activity)
        var sel = RATE_VALUES.indexOfFirst { abs(it - current) < 0.01f }
        if (sel < 0) sel = 2
        AlertDialog.Builder(activity)
            .setTitle("Vitesse de la voix")
            .setSingleChoiceItems(RATE_LABELS, sel) { d, which ->
                Store.setRate(activity, RATE_VALUES[which])
                tts?.setSpeechRate(RATE_VALUES[which])
                tts?.speak("Voici la vitesse de la voix", TextToSpeech.QUEUE_FLUSH, null, "demo")
                d.dismiss()
            }
            .setNegativeButton("Fermer", null)
            .show()
    }
}
