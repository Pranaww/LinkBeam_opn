package com.linkbeam.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothManager

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class ShareReceiverActivity : ComponentActivity() {

    private var sharedUrl: String? = null
    private val prefs by lazy {
        getSharedPreferences("linkbeam", MODE_PRIVATE)
    }

    private val wsClient = OkHttpClient()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                handleBluetooth()
            } else {
                Toast.makeText(this, "Permission denied. Cannot send link.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedUrl = intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }

        handleBluetooth()
    }

    private fun handleBluetooth() {
        val savedMac = prefs.getString("mac", null)
        if (savedMac == null) {
            showBluetoothPicker()
        } else {
            sendViaWebSocket(savedMac) { finish() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothPicker() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter?.let { adapter ->
            if (!adapter.isEnabled) {
                Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val devices = adapter.bondedDevices.toList()
            val names = devices.map { "${it.name} (${it.address})" }.toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("Choose Bluetooth Device")
                .setItems(names) { _, index ->
                    val mac = devices[index].address
                    prefs.edit { putString("mac", mac) }
                    sendViaWebSocket(mac) { finish() }
                }
                .setOnCancelListener { finish() }
                .show()
        } ?: run {
            Toast.makeText(this, "Bluetooth is not available on this device.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendViaWebSocket(btMac: String, onComplete: () -> Unit) {
        val url = sharedUrl ?: run {
            onComplete()
            return
        }

        val request = Request.Builder()
            .url("wss://linkbeam-relay-production.up.railway.app")
            .build()

        wsClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("LinkBeam", "WebSocket OPENED")

                val payload = JSONObject().apply {
                    put("role", "phone")
                    put("url", url)
                    put("mac", btMac)
                }

                android.util.Log.d("LinkBeam", "Sending: $payload")
                webSocket.send(payload.toString())
                webSocket.close(1000, "Message Sent")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("LinkBeam", "WebSocket FAILED", t)
                runOnUiThread(onComplete)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("LinkBeam", "WebSocket CLOSED")
                runOnUiThread(onComplete)
            }
        })

    }
}
