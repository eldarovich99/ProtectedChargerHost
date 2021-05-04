package com.veuzbekov.protectedchargerhost

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class CommunicationManager(private val bluetoothAdapter: BluetoothAdapter) {
    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private val outputFlow = MutableStateFlow("")
    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            "bt_connection",
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        )
    }

    fun receiveData() {
        val bufferSize = 1024
        var buffer = ByteArray(bufferSize)
        val tinyBuffer = ByteArray(bufferSize)
        var bytes = 0
        var lastTime = System.currentTimeMillis()
        val thread = Thread {
            while (true) {
                try {
                    bytes = inStream!!.read(tinyBuffer, bytes, 1)
                    tinyBuffer.copyInto(buffer)
                    if (String(buffer, Charset.forName("UTF-8")) == ".") {
                        if (bytes != 0) {
                            val newTime = System.currentTimeMillis()
                            val result = String(buffer, Charset.forName("UTF-8"))
                            if (newTime - lastTime > 1000) {
                                lastTime = newTime
                                val command = result.substring(0, 1)
                                val uuid = result.substring(1, result.length)
                                buffer = ByteArray(bufferSize)
                                CoroutineScope(Dispatchers.Main).launch {
                                    outputFlow.value = command
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        thread.start()
    }

    fun init(): Boolean {
        return try {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e("bt socket", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    outputStream = socket.outputStream
                    inStream = socket.inputStream
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
            true
        } catch (e: IOException) {
            // TODO return false; true is returned to emulate that charger is connected
            false
            //false
        }
    }

    private fun write(s: String) {
        try {
            outputStream?.write(s.toByteArray())
        } catch (e: IOException) {
            init()
        }
    }

    fun getOutputFlow(): StateFlow<String> = outputFlow

    companion object {
        const val PING_DELAY = 500L
    }

}