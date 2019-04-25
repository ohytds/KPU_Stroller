package com.app.hongdev.bluetooth.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


//블루투스 연결 및 상태, 읽고 쓰기 관리
class BluetoothService(handler: Handler) {

    companion object {
        private const val TAG = "BluetoothService"
        private val UUID_HC06 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val mHandler = handler
    private var mState = BluetoothState.STATE_NONE
    private val mBTAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mOpenSocketThread : OpenSocketThread? = null
    private var mConnectThread : ConnectThread? = null
    private var mConnectedThread : ConnectedThread? = null

    @Synchronized
    private fun setState(state : Int){
        Log.d(TAG, "setState -> $mState -> $state")

        mState = state
        mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
    }

    @Synchronized
    fun getState() : Int = mState

    @Synchronized
    fun startBlueTooth() {
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        setState(BluetoothState.STATE_LISTEN)

        if (mOpenSocketThread == null) {
            mOpenSocketThread = OpenSocketThread()
            mOpenSocketThread?.start()
        }
    }

    @Synchronized
    fun connectBlueTooth(device : BluetoothDevice) {
        if (mState == BluetoothState.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread?.cancel()
                mConnectThread = null
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        mConnectThread = ConnectThread(device)
        mConnectThread?.start()
        setState(BluetoothState.STATE_CONNECTING)
    }

    @Synchronized
    private fun connectedBlueTooth(device : BluetoothDevice, socket: BluetoothSocket) {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mOpenSocketThread != null) {
            mOpenSocketThread!!.cancel()
            mOpenSocketThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(BluetoothState.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(BluetoothState.DEVICE_NAME, device.name)
        bundle.putString(BluetoothState.DEVICE_ADDRESS, device.address)
        msg.data = bundle
        mHandler.sendMessage(msg)

        setState(BluetoothState.STATE_CONNECT)
    }

    @Synchronized
    fun stopBlueTooth() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mOpenSocketThread != null) {
            mOpenSocketThread?.cancel()
            mOpenSocketThread?.stopLoop()
            mOpenSocketThread = null
        }
        setState(BluetoothState.STATE_NONE)
    }

    fun write(string : ByteArray) {
        if (mState != BluetoothState.STATE_CONNECT) return
        if (mConnectedThread != null) {
            mConnectedThread?.write(string)
        }
    }

    private inner class OpenSocketThread : Thread() {
        private var mSocket : BluetoothServerSocket? = null
        private var runFlag = true

        init {
            try {
                mSocket = mBTAdapter.listenUsingRfcommWithServiceRecord(
                    TAG,
                    UUID_HC06
                )
            } catch (e : IOException) {
                Log.e(TAG, "OpenSocketThread -> $e")
            }
        }

        override fun run() {
            name = "OpenSocketThread"

            var socket: BluetoothSocket?

            while (mState != BluetoothState.STATE_CONNECT && runFlag) {
                try {
                    socket = mSocket?.accept()
                } catch (e : IOException) {
                    break
                }

                if (socket != null) {
                     synchronized(BluetoothService) {
                         when (mState) {
                             BluetoothState.STATE_NONE -> {}
                             BluetoothState.STATE_LISTEN -> {}
                             BluetoothState.STATE_CONNECTING -> { connectedBlueTooth(socket.remoteDevice, socket); return }
                             BluetoothState.STATE_CONNECT -> { socket.close(); return }

                         }
                    }
                }
            }

        }

        fun cancel() {
            try {
                mSocket?.close()
                mSocket = null
            } catch (e : IOException) { Log.e(TAG, "OpenSocketThread -> $e") }
        }

        fun stopLoop() {
            runFlag = false
        }
    }
    private inner class ConnectThread(device : BluetoothDevice) : Thread() {

        private var mSocket : BluetoothSocket? = null
        private val mDevice = device

        init {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID_HC06)
            } catch (e : IOException) { Log.e(TAG, "ConnectThread -> $e")}
        }

        override fun run() {
            mBTAdapter.cancelDiscovery()

            if (mSocket != null) {
                try {
                    mSocket!!.connect()
                } catch (e : IOException) {
                    return
                }

                synchronized (BluetoothService) {
                    mConnectThread = null
                }

                connectedBlueTooth(mDevice, mSocket!!)
            }
        }

        fun cancel() {
            try {
                mSocket?.close()
            } catch (e : IOException) { Log.e(TAG, "ConnectThread -> $e")}
        }
    }
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread(){
        private val mSocket = socket
        private lateinit var mInputStream : InputStream
        private lateinit var mOutputStream: OutputStream

        init {
            try {
                mInputStream = socket.inputStream
                mOutputStream = socket.outputStream
            } catch (e : IOException) { Log.e(TAG, "ConnectedThread -> $e") }
        }

        override fun run() {
            var buffer : ByteArray
            var arrayByte = ArrayList<Int>()


            while (true) {
                try {
                    val data = mInputStream.read()
                    if (data == 0x0D) {
                        buffer = ByteArray(arrayByte.size)
                        for (i in arrayByte.indices) {
                            buffer[i] = arrayByte[i].toByte()
                        }
                        mHandler.obtainMessage(BluetoothState.MESSAGE_READ, buffer.size, -1, buffer).sendToTarget()
                        arrayByte = ArrayList()
                    } else {
                        arrayByte.add(data)
                    }
                } catch (e : IOException) {
                    Log.e(TAG, "ConnectedThread -> $e")
                    startBlueTooth()
                    break
                }
            }
        }

        fun write(buffer : ByteArray) {
            mOutputStream.write(buffer)
            mHandler.obtainMessage(BluetoothState.MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
        }

        fun cancel() {
            try {
                mSocket.close()
            } catch (e : IOException) { Log.e(TAG, "ConnectedThread -> $e") }
        }
    }
}