package com.custom.musicvolumeeq

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null

    private val bandSeekBars = mutableListOf<SeekBar>()
    private lateinit var ledVuMeter: LedVuMeterView
    private var minEqLevel: Short = -1500
    private var maxEqLevel: Short = 1500

    private var currentBalance: Int = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ledVuMeter = findViewById(R.id.ledVuMeter)

        setupRotaryKnobs()
        setupAudioEffects()
        checkAudioPermissionForVisualizer()
    }

    private fun setupRotaryKnobs() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val knobVolume = findViewById<RotaryKnobView>(R.id.knobVolume).apply {
            label = "SES (VOL)"
            activeColor = Color.parseColor("#00E676")
            max = maxVol
            progress = curVol
            onProgressChanged = { vol ->
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
            }
        }

        val knobPreamp = findViewById<RotaryKnobView>(R.id.knobPreamp).apply {
            label = "PREAMP (+dB)"
            activeColor = Color.parseColor("#FF1744")
            max = 3000
            progress = 0
            onProgressChanged = { gain ->
                loudnessEnhancer?.setTargetGain(gain)
            }
        }

        val knobBass = findViewById<RotaryKnobView>(R.id.knobBass).apply {
            label = "BASS BOOST"
            activeColor = Color.parseColor("#FF9100")
            max = 1000
            progress = 0
            onProgressChanged = { strength ->
                bassBoost?.setStrength(strength.toShort())
            }
        }

        val knobVirtualizer = findViewById<RotaryKnobView>(R.id.knobVirtualizer).apply {
            label = "3D SURROUND"
            activeColor = Color.parseColor("#00B0FF")
            max = 1000
            progress = 0
            onProgressChanged = { strength ->
                virtualizer?.setStrength(strength.toShort())
            }
        }

        val knobBalance = findViewById<RotaryKnobView>(R.id.knobBalance).apply {
            label = "BALANS (L / R)"
            activeColor = Color.parseColor("#E040FB")
            max = 100
            progress = 50
            isCentered = true
            onProgressChanged = { bal ->
                currentBalance = bal
            }
        }
    }

    private fun setupAudioEffects() {
        val sessionId = 0

        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Toast.makeText(this, "DSP Motoru Başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        val eq = equalizer ?: return
        val container = findViewById<LinearLayout>(R.id.eqBandsContainer)
        val numBands = eq.numberOfBands
        val levelRange = eq.bandLevelRange
        minEqLevel = levelRange[0]
        maxEqLevel = levelRange[1]

        for (i in 0 until numBands) {
            val band = i.toShort()
            val centerFreqHz = eq.getCenterFreq(band) / 1000
            val freqLabel = if (centerFreqHz >= 1000) "${centerFreqHz / 1000f} kHz" else "$centerFreqHz Hz"

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 10)
            }

            val labelRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val freqText = TextView(this).apply {
                text = freqLabel
                setTextColor(Color.WHITE)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val dbText = TextView(this).apply {
                val curDb = eq.getBandLevel(band) / 100
                text = "${if (curDb >= 0) "+$curDb" else curDb} dB"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 13f
                gravity = Gravity.END
            }

            labelRow.addView(freqText)
            labelRow.addView(dbText)

            val seekBar = SeekBar(this).apply {
                max = maxEqLevel - minEqLevel
                progress = eq.getBandLevel(band) - minEqLevel
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                        val newLevel = (progress + minEqLevel).toShort()
                        eq.setBandLevel(band, newLevel)
                        val db = newLevel / 100
                        dbText.text = "${if (db >= 0) "+$db" else db} dB"
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }

            bandSeekBars.add(seekBar)
            row.addView(labelRow)
            row.addView(seekBar)
            container.addView(row)
        }

        val presetsList = mutableListOf<String>()
        for (i in 0 until eq.numberOfPresets) {
            presetsList.add(eq.getPresetName(i.toShort()))
        }
        val spinner = findViewById<Spinner>(R.id.spinnerPresets)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presetsList)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                (parent?.getChildAt(0) as? TextView)?.setTextColor(Color.WHITE)
                eq.usePreset(position.toShort())
                syncSeekBarsWithEq()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnFlat).setOnClickListener {
            for (i in 0 until numBands) {
                eq.setBandLevel(i.toShort(), 0)
            }
            syncSeekBarsWithEq()
        }

        findViewById<Button>(R.id.btnVoiceFilter).setOnClickListener {
            for (i in 0 until numBands) {
                val band = i.toShort()
                val freqHz = eq.getCenterFreq(band) / 1000
                val targetLevel = when {
                    freqHz < 250 -> minEqLevel
                    freqHz in 300..3200 -> (maxEqLevel * 0.6).toInt().toShort()
                    else -> (minEqLevel * 0.8).toInt().toShort()
                }
                eq.setBandLevel(band, targetLevel)
            }
            syncSeekBarsWithEq()
            Toast.makeText(this, "SSB / Vokal Filtresi Aktif", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSubBassCut).setOnClickListener {
            for (i in 0 until numBands) {
                val band = i.toShort()
                val freqHz = eq.getCenterFreq(band) / 1000
                val targetLevel = if (freqHz <= 150) minEqLevel else 0.toShort()
                eq.setBandLevel(band, targetLevel)
            }
            syncSeekBarsWithEq()
            Toast.makeText(this, "150Hz Altı Uğultu Kesici Aktif", Toast.LENGTH_SHORT).show()
        }

        findViewById<Switch>(R.id.switchEqEnable).setOnCheckedChangeListener { _, isChecked ->
            equalizer?.enabled = isChecked
            bassBoost?.enabled = isChecked
            virtualizer?.enabled = isChecked
            loudnessEnhancer?.enabled = isChecked
        }
    }

    private fun syncSeekBarsWithEq() {
        val eq = equalizer ?: return
        for (i in 0 until eq.numberOfBands) {
            bandSeekBars[i].progress = eq.getBandLevel(i.toShort()) - minEqLevel
        }
    }

    private fun checkAudioPermissionForVisualizer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startVisualizer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVisualizer()
        }
    }

    private fun startVisualizer() {
        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform ?: return
                        var sumL = 0f
                        for (i in 0 until waveform.size / 2) {
                            sumL += abs((waveform[i].toInt() and 0xFF) - 128)
                        }
                        var rawL = ((sumL / (waveform.size / 2)) * 2.0f).toInt()

                        val balanceMultiplier = if (currentBalance < 50) {
                            currentBalance / 50f
                        } else {
                            1.0f + ((currentBalance - 50) / 50f) * 0.5f
                        }
                        val levelL = (rawL * balanceMultiplier).toInt().coerceIn(5, 100)
                        ledVuMeter.leftLevel = levelL
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        fft ?: return
                        var sumR = 0f
                        for (i in 2 until fft.size step 2) {
                            sumR += hypot(fft[i].toDouble(), fft[i + 1].toDouble()).toFloat()
                        }
                        var rawR = ((sumR / (fft.size / 2)) * 2.8f).toInt()

                        val balanceMultiplierR = if (currentBalance > 50) {
                            (100 - currentBalance) / 50f
                        } else {
                            1.0f + ((50 - currentBalance) / 50f) * 0.5f
                        }
                        val levelR = (rawR * balanceMultiplierR).toInt().coerceIn(5, 100)
                        ledVuMeter.rightLevel = levelR
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer?.release()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
    }
}
