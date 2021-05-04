package com.veuzbekov.protectedchargerhost

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class CommunicationManager(private val bluetoothAdapter: BluetoothAdapter) {
    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null
    private val outputFlow = MutableStateFlow("")
    private var state: Protection = Protection.Waiting()
    private val stringBuilder = StringBuilder()
    private var alarmJob: Job? = null
    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            "bt_connection",
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        )
    }

    fun receiveData() {
        val bufferSize = 1024
        //var buffer = ByteArray(bufferSize)
        //val tinyBuffer = ByteArray(bufferSize)
        var lastTime = System.currentTimeMillis()
        val thread = Thread {
            while (true) {
                try {
                    val result = read()
                    if (stringBuilder.isNotEmpty()) {
                        /*val newTime = System.currentTimeMillis()
                        if (newTime - lastTime > 1000) {
                            lastTime = newTime*/
                        val command =
                            ChargerCommands.checkCommand(command = result, uuid = "0123456789")
                        val isCommandSuccessful = sendBack(
                            result,
                            command == ChargerCommands.Command.PING
                                    || command == ChargerCommands.Command.LOST_ELECTRICITY
                        )
                        if (isCommandSuccessful) processCommand(command)
                        //}
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        thread.start()
    }

    private fun read(): String {
        var charCode: Int = -2
        while (charCode != -1 && charCode.toChar() != '.') {
            charCode = inStream!!.read()
            val char = charCode.toChar()
            stringBuilder.append(char)
        }
        val result = stringBuilder.toString()
        stringBuilder.clear()
        return result
    }

    private fun sendBack(initialCommand: String, shouldAlarm: Boolean): Boolean {
        write(initialCommand)
        if (shouldAlarm) {
            alarmJob = CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                alarm()
            }
        }
        val response = read()
        if (response == initialCommand)
            alarmJob?.cancel()
        return response == initialCommand
    }

    private fun processCommand(command: ChargerCommands.Command) {
        when (command) {
            ChargerCommands.Command.PING -> {
                pingBack()
            }
            ChargerCommands.Command.ALARM -> {
                alarm()
            }
            ChargerCommands.Command.CHILD_ENABLED -> {
                onChildProtectionEnabled()
            }
            ChargerCommands.Command.DISABLED -> {
                onProtectionDisabled()
            }
            ChargerCommands.Command.ENABLED -> {
                onSelfProtectionEnabled()
            }
            ChargerCommands.Command.LOST_ELECTRICITY -> {
                notifyElectricityLost()
            }
            ChargerCommands.Command.UNKNOWN -> {
                alarm()
            }
        }
    }

    private fun alarm() {
        // Here should be actual alarm
        outputFlow.value = "Alarm"
        state = Protection.Alarm()
    }

    private fun notifyElectricityLost() {
        // write, wait for answer for 1 second, alarm if no response is provided
        state = Protection.ElectricityLost()
        outputFlow.value = "Electricity lost"
    }

    private fun pingBack() {

    }

    private fun onSelfProtectionEnabled() {
        state = Protection.ProtectingPhone()
    }

    private fun onChildProtectionEnabled() {
        if (state !is Protection.ProtectingPhone
            && state !is Protection.Alarm
        ) {
            state = Protection.ProtectingChild()
        }
    }

    private fun onProtectionDisabled() {
        state = Protection.Waiting()
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
            //init()
        }
    }

    fun getOutputFlow(): StateFlow<String> = outputFlow

    companion object {
        const val PING_DELAY = 500L
    }

}