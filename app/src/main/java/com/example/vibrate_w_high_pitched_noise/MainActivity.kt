package com.example.vibrate_w_high_pitched_noise

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.*
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.example.vibrate_w_high_pitched_noise.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        var dispatcher: AudioDispatcher =
            AudioDispatcherFactory.fromDefaultMicrophone(44100, 16384, 0)

        val pdh = PitchDetectionHandler { res, _ ->
            val pitchInHz = res.pitch
            runOnUiThread { processPitch(pitchInHz) }
        }
        val pitchProcessor: AudioProcessor =
            PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 44100F, 16384, pdh)
        dispatcher.addAudioProcessor(pitchProcessor)

        var audioThread = Thread(dispatcher, "Audio Thread")

        val button = binding.button

        // set on-click listener
        button.setOnClickListener {
            // your code to perform when the user clicks on the button
            if (button.text == "start") {
                button.text = ""
                button.setBackgroundColor(Color.TRANSPARENT)
                if (dispatcher.isStopped) {
                    dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100, 16384, 0)
                    dispatcher.addAudioProcessor(pitchProcessor)
                    audioThread = Thread(dispatcher, "Audio Thread")
                }
                audioThread.start()
            } else {
                if (dispatcher != null) {
                    dispatcher.stop()
                }
                audioThread.join()
                button.text = "start"
                button.setBackgroundColor(Color.DKGRAY)
            }
        }
    }

    private fun processPitch(pitchInHz: Float) {

        if (pitchInHz > 1000) {
            vibrate(1000)
            // Toast.makeText(this, pitchInHz.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    fun vibrate(sleepTime: Long){
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(sleepTime, VibrationEffect.DEFAULT_AMPLITUDE) )
        }else{
            @Suppress("DEPRECATION")
            vib.vibrate(sleepTime)
        }
        Thread.sleep(sleepTime)
    }
}