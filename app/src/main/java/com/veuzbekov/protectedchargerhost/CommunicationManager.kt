package com.veuzbekov.protectedchargerhost
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

class CommunicationManager(private val bluetoothAdapter: BluetoothAdapter) {
    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private var device: BluetoothDevice? = null
    private val outputFlow = MutableStateFlow("")
    private val cycleContext = SupervisorJob() + Dispatchers.IO

    private fun receiveData() {
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

    private fun init(): Boolean {
        return try {
            val blueAdapter = BluetoothAdapter.getDefaultAdapter()
            if (blueAdapter != null) {
                if (blueAdapter.isEnabled) {
                    val bondedDevices = blueAdapter.bondedDevices
                    if (bondedDevices.size > 0) {
                        val uuids = device!!.uuids
                        val socket = device!!.createRfcommSocketToServiceRecord(uuids[0].uuid)
                        socket.connect()
                        outputStream = socket.outputStream
                        inStream = socket.inputStream
                    }
                    Log.e("error", "No appropriate paired devices.")
                } else {
                    Log.e("error", "Bluetooth is disabled.")
                }
            }
            true
        } catch (e: IOException) {
            // TODO return false; true is returned to emulate that charger is connected
            true
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