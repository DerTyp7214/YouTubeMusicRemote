package de.dertyp7214.youtubemusicremote.screens

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import de.dertyp7214.youtubemusicremote.CustomWebSocket
import de.dertyp7214.youtubemusicremote.CustomWebSocketListener
import de.dertyp7214.youtubemusicremote.R
import de.dertyp7214.youtubemusicremote.types.Action
import de.dertyp7214.youtubemusicremote.types.SendAction
import de.dertyp7214.youtubemusicremote.types.SocketResponse
import de.dertyp7214.youtubemusicremote.types.StatusData


class IntroActivity : AppCompatActivity() {

    private lateinit var inputLayout: TextInputLayout

    private val gson = Gson().newBuilder().enableComplexMapKeySerialization().create()

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                inputLayout.editText?.setText(result.contents)
                checkWebSocket(result.contents) {
                    if (it) inputLayout.boxStrokeColor = Color.GREEN
                    else inputLayout.boxStrokeColor = Color.RED
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val url = preferences.getString("url", null)

        if (url != null) {
            checkWebSocket(url) {
                if (it) {
                    MainActivity.URL = url
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    inputLayout.boxStrokeColor = Color.RED
                    inputLayout.editText?.setText(url.removePrefix("ws://"))
                }
            }
        } else {
            val scanQrCode = findViewById<Button>(R.id.scanQrCode)
            val next = findViewById<Button>(R.id.next)

            inputLayout = findViewById(R.id.textInputLayout)

            inputLayout.editText?.doAfterTextChanged {
                inputLayout.boxStrokeColor = MaterialColors.getColor(
                    this,
                    androidx.appcompat.R.attr.colorPrimary,
                    Color.WHITE
                )
            }

            scanQrCode.setOnClickListener {
                barcodeLauncher.launch(ScanOptions())
            }

            next.setOnClickListener {
                inputLayout.editText?.text?.let { editable ->
                    val newUrl =
                        editable.toString().let { if (it.startsWith("ws://")) it else "ws://$it" }
                    checkWebSocket(newUrl) {
                        if (it) {
                            inputLayout.boxStrokeColor = Color.GREEN
                            preferences.edit {
                                putString("url", newUrl)
                                MainActivity.URL = newUrl
                                startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                                finish()
                            }
                        } else inputLayout.boxStrokeColor = Color.RED
                    }
                }
            }
        }
    }

    private fun checkWebSocket(url: String, callback: (connected: Boolean) -> Unit) {
        val customWebSocketListener = CustomWebSocketListener()

        try {
            val webSocket = CustomWebSocket(
                if (url.startsWith("ws://")) url else "ws://$url",
                customWebSocketListener
            )

            customWebSocketListener.onMessage { _, text ->
                try {
                    val socketResponse = gson.fromJson(text, SocketResponse::class.java)

                    when (socketResponse.action) {
                        Action.STATUS -> {
                            val statusData =
                                gson.fromJson(socketResponse.data, StatusData::class.java)

                            if (statusData.name == "ytmd") callback(true)
                            else callback(false)
                            webSocket.close()
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false)
                    webSocket.close()
                }
            }

            customWebSocketListener.onFailure { _, throwable, _ ->
                throwable.printStackTrace()
                callback(false)
            }

            webSocket.send(SendAction(Action.STATUS))
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }
}