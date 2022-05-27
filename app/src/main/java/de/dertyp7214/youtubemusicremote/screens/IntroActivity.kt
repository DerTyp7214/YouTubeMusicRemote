package de.dertyp7214.youtubemusicremote.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.core.blur
import de.dertyp7214.youtubemusicremote.core.fitToScreen
import de.dertyp7214.youtubemusicremote.core.getFallBackColor
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.*
import dev.chrisbanes.insetter.applyInsetter
import kotlin.math.roundToInt


class IntroActivity : AppCompatActivity() {

    private val code = (Math.random() * 100).roundToInt()

    private lateinit var inputLayout: TextInputLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var scanQrCode: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val coverLiveData = MutableLiveData<CoverData>()

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                inputLayout.editText?.setText(result.contents)
                scanQrCode.isEnabled = false
                checkWebSocket(result.contents) { connected, reason ->
                    scanQrCode.isEnabled = true
                    if (connected) {
                        nextButton.isEnabled = true
                        setInputColor(inputLayout, Color.GREEN)
                    } else {
                        nextButton.isEnabled = false
                        setInputColor(inputLayout, Color.RED)
                        reason?.let { inputLayout.editText?.error = it }
                    }
                }
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == code && grantResults.first() == PackageManager.PERMISSION_GRANTED)
            setInputColor(inputLayout)
    }

    private fun setInputColor(inputLayout: TextInputLayout, color: Int? = null) {
        if (color == null) {
            fun fetchCoverData() {
                if (coverLiveData.value == null) WallpaperManager.getInstance(this).drawable?.apply {
                    val vibrant = Palette.Builder(toBitmap()).maximumColorCount(32).generate().let {
                        it.getVibrantColor(it.getMutedColor(it.getDominantColor(Color.CYAN)))
                    }
                    blur(this@IntroActivity) {
                        coverLiveData.postValue(
                            CoverData(
                                it,
                                vibrant = vibrant
                            )
                        )
                    }
                } else coverLiveData.postValue(coverLiveData.value)
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                code
            ) else fetchCoverData()

            coverLiveData.observe(this) { coverData ->
                window.decorView.background = coverData.background?.fitToScreen(this)?.apply {
                    colorFilter = LightingColorFilter(0xFF7B7B7B.toInt(), 0x00000000)
                }

                val coverColor = getFallBackColor(coverData.vibrant, coverData.muted)

                if (inputLayout.boxStrokeColor != Color.GREEN) setInputColor(
                    inputLayout,
                    coverColor
                )

                scanQrCode.rippleColor =
                    ColorStateList.valueOf(ColorUtils.blendARGB(coverColor, Color.BLACK, .3f))
                scanQrCode.backgroundTintList = ColorStateList.valueOf(coverColor)

                nextButton.rippleColor = ColorStateList.valueOf(coverColor)
                nextButton.strokeColor = ColorStateList.valueOf(coverColor)
                nextButton.setTextColor(coverColor)

                progressBar.indeterminateTintList = ColorStateList.valueOf(coverColor)
            }
        } else {
            inputLayout.boxStrokeColor = color
            inputLayout.setHelperTextColor(ColorStateList.valueOf(color))
            inputLayout.hintTextColor = ColorStateList.valueOf(color)
            inputLayout.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        var initialized = false

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val urls = preferences.getStringSet("url", setOf())

        inputLayout = findViewById(R.id.textInputLayout)
        scanQrCode = findViewById(R.id.scanQrCode)
        nextButton = findViewById(R.id.next)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ViewGroup>(R.id.bottomBar).applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }

        MainActivity.currentSongInfo.observe(this) {
            if (it.coverData != null && coverLiveData.value != it.coverData)
                coverLiveData.postValue(it.coverData!!)
        }

        window.decorView.rootView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialized) {
                setInputColor(inputLayout)
                MainActivity.currentSongInfo.value?.coverData?.let { coverLiveData.postValue(it) }

                if (!urls.isNullOrEmpty() && !intent.getBooleanExtra("newUrl", false)) {
                    scanQrCode.isEnabled = false
                    fun checkUrls(urls: List<String>, index: Int) {
                        checkWebSocket(urls[index]) { connected, reason ->
                            scanQrCode.isEnabled = true
                            if (connected) {
                                MediaPlayer.URL = urls[index]
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                nextButton.isEnabled = false
                                setInputColor(inputLayout, Color.RED)
                                inputLayout.editText?.setText(urls[index].removePrefix("ws://"))
                                reason?.let { inputLayout.editText?.error = it }
                                if (index < urls.lastIndex) checkUrls(urls, index + 1)
                            }
                        }
                    }
                    checkUrls(urls.toList(), 0)
                } else {
                    inputLayout.editText?.doAfterTextChanged {
                        nextButton.isEnabled = true
                        setInputColor(inputLayout)
                    }

                    scanQrCode.setOnClickListener {
                        barcodeLauncher.launch(ScanOptions())
                    }

                    nextButton.setOnClickListener {
                        inputLayout.editText?.text?.let { editable ->
                            val newUrl =
                                editable.toString()
                                    .let { if (it.startsWith("ws://") || it == "devUrl") it else "ws://$it" }
                            scanQrCode.isEnabled = false
                            nextButton.isEnabled = false
                            checkWebSocket(newUrl) { connected, reason ->
                                scanQrCode.isEnabled = true
                                if (connected) {
                                    setInputColor(inputLayout, Color.GREEN)
                                    preferences.edit {
                                        if (newUrl != "devUrl") putStringSet("url",
                                            arrayListOf(newUrl).apply {
                                                if (urls != null) addAll(
                                                    urls
                                                )
                                            }
                                                .toSet()
                                        )
                                        MediaPlayer.URL = newUrl
                                        startActivity(
                                            Intent(
                                                this@IntroActivity,
                                                MainActivity::class.java
                                            )
                                        )
                                        finish()
                                    }
                                } else {
                                    nextButton.isEnabled = false
                                    setInputColor(inputLayout, Color.RED)
                                    reason?.let { inputLayout.editText?.error = it }
                                }
                            }
                        }
                    }
                }

                initialized = true
            }
        }
    }

    private fun checkWebSocket(
        url: String, callback: (connected: Boolean, reason: String?) -> Unit
    ) {
        if (url == "devUrl") {
            callback(true, null)
            return
        }

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = VISIBLE
        inputLayout.isEnabled = false

        val cb: (Boolean, String?) -> Unit = { p0, p1 ->
            runOnUiThread {
                progressBar.visibility = GONE
                inputLayout.isEnabled = true
                inputLayout.requestFocus()
                callback(p0, p1)
            }
        }

        val customWebSocketListener = CustomWebSocketListener()

        try {
            val webSocket = CustomWebSocket(
                if (url.startsWith("ws://")) url else "ws://$url", customWebSocketListener
            )

            customWebSocketListener.onMessage { _, text ->
                try {
                    val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                    when (socketResponse.action) {
                        Action.STATUS -> {
                            val statusData =
                                gson.fromJson(socketResponse.data, StatusData::class.java)

                            if (statusData.name == "ytmd") cb(true, null)
                            else cb(false, "Invalid name")
                            webSocket.close()
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    cb(false, e.localizedMessage)
                    webSocket.close()
                }
            }

            customWebSocketListener.onFailure { _, throwable, _ ->
                throwable.printStackTrace()
                throwable.localizedMessage.let { message ->
                    if (message.equals("socket closed", true)) cb(true, "")
                    else cb(false, message)
                }
            }

            webSocket.setUp()
            webSocket.send(SendAction(Action.STATUS))
        } catch (e: Exception) {
            e.printStackTrace()
            cb(false, e.localizedMessage)
        }
    }
}