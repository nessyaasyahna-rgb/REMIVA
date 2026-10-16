package com.example.ui.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SoundManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var toneGen: ToneGenerator? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        } catch (_: Exception) {}

        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeId = Locale("id", "ID")
            val result = textToSpeech?.setLanguage(localeId)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to device's default locale
                textToSpeech?.setLanguage(Locale.getDefault())
            }
            textToSpeech?.setPitch(1.0f)
            textToSpeech?.setSpeechRate(0.95f)
            isTtsReady = true
        } else {
            Log.w("SoundManager", "TextToSpeech init failed with status: $status")
        }
    }

    /**
     * Konversi angka nominal uang ke kalimat terbilang bahasa Indonesia secara akurat dan alami.
     * Contoh: 45000 -> "empat puluh lima ribu"
     *         250000 -> "dua ratus lima puluh ribu"
     */
    fun numberToIndonesianWords(number: Long): String {
        if (number <= 0L) return "nol"
        return convertToWords(number).trim()
    }

    private fun convertToWords(number: Long): String {
        val units = arrayOf(
            "", "satu", "dua", "tiga", "empat", "lima",
            "enam", "tujuh", "delapan", "sembilan", "sepuluh", "sebelas"
        )
        return when {
            number == 0L -> ""
            number < 12L -> units[number.toInt()]
            number < 20L -> "${convertToWords(number - 10L)} belas"
            number < 100L -> {
                val rem = number % 10L
                val tens = "${convertToWords(number / 10L)} puluh"
                if (rem > 0) "$tens ${convertToWords(rem)}" else tens
            }
            number < 200L -> {
                val rem = number - 100L
                if (rem > 0) "seratus ${convertToWords(rem)}" else "seratus"
            }
            number < 1000L -> {
                val rem = number % 100L
                val hundredsPrefix = if (number / 100L == 1L) "seratus" else "${convertToWords(number / 100L)} ratus"
                if (rem > 0) "$hundredsPrefix ${convertToWords(rem)}" else hundredsPrefix
            }
            number < 2000L -> {
                val rem = number - 1000L
                if (rem > 0) "seribu ${convertToWords(rem)}" else "seribu"
            }
            number < 1000000L -> {
                val rem = number % 1000L
                val thousandsPrefix = if (number / 1000L == 1L) "seribu" else "${convertToWords(number / 1000L)} ribu"
                if (rem > 0) "$thousandsPrefix ${convertToWords(rem)}" else thousandsPrefix
            }
            number < 1000000000L -> {
                val rem = number % 1000000L
                val millionsPrefix = "${convertToWords(number / 1000000L)} juta"
                if (rem > 0) "$millionsPrefix ${convertToWords(rem)}" else millionsPrefix
            }
            else -> {
                val rem = number % 1000000000L
                val billionsPrefix = "${convertToWords(number / 1000000000L)} milyar"
                if (rem > 0) "$billionsPrefix ${convertToWords(rem)}" else billionsPrefix
            }
        }
    }

    /**
     * Suara bukti pembayaran yang mengeluarkan kalimat harga yang diterima secara lisan.
     * Contoh: "Pembayaran berhasil diterima sebesar lima puluh ribu rupiah. Terima kasih telah berbelanja di Apotek Farmiva."
     */
    fun speakPaymentSentence(amount: Double, enabled: Boolean = true) {
        if (!enabled) return

        // 1. Play Cash register chime & trigger haptic
        playCashierChime()

        // 2. Format nominal ke kalimat terbilang Indonesia
        val amountLong = amount.toLong()
        val terbilang = if (amountLong > 0) numberToIndonesianWords(amountLong) else "nol"
        val sentence = "Pembayaran berhasil diterima sebesar $terbilang rupiah. Terima kasih telah berbelanja di Apotek Farmiva."

        // 3. Suarakan melalui TextToSpeech
        CoroutineScope(Dispatchers.Main).launch {
            delay(260) // Beri jeda sedikit setelah nada Ka-Ching
            try {
                if (isTtsReady && textToSpeech != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textToSpeech?.speak(
                            sentence,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "FARMIVA_PAYMENT_${System.currentTimeMillis()}"
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null)
                    }
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "Error speaking payment sentence: ${e.message}")
            }
        }
    }

    fun playCashierSuccess(enabled: Boolean = true) {
        speakPaymentSentence(50000.0, enabled)
    }

    private fun playCashierChime() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Cash register "Ka-Ching / Double Beep"
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(130)
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
            } catch (_: Exception) {}
        }
        triggerHaptic(longHaptic = false)
    }

    fun playAlertWarning(enabled: Boolean = true) {
        if (!enabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
            } catch (_: Exception) {}
        }
        triggerHaptic(longHaptic = true)
    }

    private fun triggerHaptic(longHaptic: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (longHaptic) 200L else 80L
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (longHaptic) 200L else 80L)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        toneGen?.release()
        toneGen = null
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
        } catch (_: Exception) {}
    }
}
