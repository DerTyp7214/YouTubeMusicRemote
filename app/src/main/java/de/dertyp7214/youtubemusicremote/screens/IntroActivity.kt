package de.dertyp7214.youtubemusicremote.screens

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.components.CustomWebSocket
import de.dertyp7214.youtubemusicremote.components.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.services.MediaPlayer
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SendAction
import de.dertyp7214.youtubemusicremote.types.SocketResponse
import de.dertyp7214.youtubemusicremote.types.StatusData


class IntroActivity : AppCompatActivity() {

    private lateinit var inputLayout: TextInputLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var scanQrCode: MaterialButton

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
                        inputLayout.boxStrokeColor = Color.GREEN
                    } else {
                        nextButton.isEnabled = false
                        inputLayout.boxStrokeColor = Color.RED
                        reason?.let { inputLayout.editText?.error = it }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val urls = preferences.getStringSet("url", setOf())

        inputLayout = findViewById(R.id.textInputLayout)
        scanQrCode = findViewById(R.id.scanQrCode)
        nextButton = findViewById(R.id.next)

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
                        inputLayout.boxStrokeColor = Color.RED
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
                inputLayout.boxStrokeColor = MaterialColors.getColor(
                    this, androidx.appcompat.R.attr.colorPrimary, Color.WHITE
                )
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
                            inputLayout.boxStrokeColor = Color.GREEN
                            preferences.edit {
                                if (newUrl != "devUrl") putStringSet("url",
                                    arrayListOf(newUrl).apply { if (urls != null) addAll(urls) }
                                        .toSet()
                                )
                                MediaPlayer.URL = newUrl
                                startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            nextButton.isEnabled = false
                            inputLayout.boxStrokeColor = Color.RED
                            reason?.let { inputLayout.editText?.error = it }
                        }
                    }
                }
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
                cb(false, throwable.localizedMessage)
            }

            webSocket.send(SendAction(Action.STATUS))
        } catch (e: Exception) {
            e.printStackTrace()
            cb(false, e.localizedMessage)
        }
    }
}