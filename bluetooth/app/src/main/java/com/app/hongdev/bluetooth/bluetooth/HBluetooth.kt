package com.app.hongdev.bluetooth.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import java.util.ArrayList


//블루투스 리스너 관리
class HBluetooth(context: Context) {

    private val mContext = context
    private var mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var mBluetoothStateListener: BluetoothStateListener? = null
    private var mDataReceivedListener: OnDataReceivedListener? = null
    private var mBluetoothConnectionListener: BluetoothConnectionListener? = null
    private var mAutoConnectionListener: AutoConnectionListener? = null

    private var mBluetoothService: BluetoothService? = null

    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null

    private var isAutoConnecting = false
    private var isAutoConnectionEnabled = false
    private var isConnected = false
    private var isConnecting = false
    private var isServiceRunning = false

    private var keyword = ""
    private var bluetoothCL: BluetoothConnectionListener? = null
    private var c = 0


    interface BluetoothStateListener {
        fun onServiceStateChanged(state: Int)
    }

    interface OnDataReceivedListener {
        fun onDataReceived(data: ByteArray?, message: String)
    }

    interface BluetoothConnectionListener {
        fun onDeviceConnected(name: String?, address: String?)
        fun onDeviceDisconnected()
        fun onDeviceConnectionFailed()
    }

    interface AutoConnectionListener {
        fun onAutoConnectionStarted()
        fun onNewConnection(name: String, address: String)
    }

    fun isBluetoothEnabled(): Boolean {
        return mBluetoothAdapter.isEnabled
    }

    fun isServiceAvailable(): Boolean {
        return mBluetoothService != null
    }

    fun isAutoConnecting(): Boolean {
        return isAutoConnecting
    }

    fun startDiscovery(): Boolean {
        return mBluetoothAdapter.startDiscovery()
    }

    fun isDiscovery(): Boolean {
        return mBluetoothAdapter.isDiscovering
    }

    fun cancelDiscovery(): Boolean {
        return mBluetoothAdapter.cancelDiscovery()
    }

    fun setupService() {
        mBluetoothService = BluetoothService(mHandler)
    }

    fun getBluetoothAdapter(): BluetoothAdapter? {
        return mBluetoothAdapter
    }

    fun getServiceState(): Int {
        return if (mBluetoothService != null)
            mBluetoothService!!.getState()
        else
            -1
    }

    fun startService() {
        if (mBluetoothService != null) {
            isServiceRunning = true
            mBluetoothService!!.startBlueTooth()
        }
    }

    fun stopService() {
        if (mBluetoothService != null) {
            isServiceRunning = false
            mBluetoothService!!.stopBlueTooth()
        }
        Handler().postDelayed({
            if (mBluetoothService != null) {
                isServiceRunning = false
                mBluetoothService!!.stopBlueTooth()
            }
        }, 1000)
    }

    fun setDeviceTarget() {
        stopService()
        startService()
    }

    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BluetoothState.MESSAGE_WRITE -> {
                }
                BluetoothState.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf)
                    if (readBuf.isNotEmpty()) {
                        if (mDataReceivedListener != null)
                            mDataReceivedListener!!.onDataReceived(readBuf, readMessage)
                    }
                }
                BluetoothState.MESSAGE_DEVICE_NAME -> {
                    mDeviceName = msg.data.getString(BluetoothState.DEVICE_NAME)
                    mDeviceAddress = msg.data.getString(BluetoothState.DEVICE_ADDRESS)
                    if (mBluetoothConnectionListener != null)
                        mBluetoothConnectionListener!!.onDeviceConnected(mDeviceName, mDeviceAddress)
                    isConnected = true
                }
                BluetoothState.MESSAGE_TOAST -> Toast.makeText(
                    mContext,
                    msg.data.getString(BluetoothState.TOAST),
                    Toast.LENGTH_SHORT
                ).show()
                BluetoothState.MESSAGE_STATE_CHANGE -> {
                    if (mBluetoothStateListener != null)
                        mBluetoothStateListener!!.onServiceStateChanged(msg.arg1)
                    if (isConnected && msg.arg1 != BluetoothState.STATE_CONNECT) {
                        if (mBluetoothConnectionListener != null)
                            mBluetoothConnectionListener!!.onDeviceDisconnected()
                        if (isAutoConnectionEnabled) {
                            isAutoConnectionEnabled = false
                            autoConnect(keyword)
                        }
                        isConnected = false
                        mDeviceName = null
                        mDeviceAddress = null
                    }

                    if (!isConnecting && msg.arg1 == BluetoothState.STATE_CONNECTING) {
                        isConnecting = true
                    } else if (isConnecting) {
                        if (msg.arg1 != BluetoothState.STATE_CONNECT) {
                            if (mBluetoothConnectionListener != null)
                                mBluetoothConnectionListener!!.onDeviceConnectionFailed()
                        }
                        isConnecting = false
                    }
                }
            }
        }
    }

    fun stopAutoConnect() {
        isAutoConnectionEnabled = false
    }

    fun connect(data: Intent) {
        val address = data.extras!!.getString(BluetoothState.DEVICE_ADDRESS)
        val device = mBluetoothAdapter.getRemoteDevice(address)
        mBluetoothService!!.connectBlueTooth(device)
    }

    fun connect(address: String) {
        val device = mBluetoothAdapter.getRemoteDevice(address)
        mBluetoothService!!.connectBlueTooth(device)
    }

    fun disconnect() {
        if (mBluetoothService != null) {
            isServiceRunning = false
            mBluetoothService!!.stopBlueTooth()
            if (mBluetoothService!!.getState() == BluetoothState.STATE_NONE) {
                isServiceRunning = true
                mBluetoothService!!.startBlueTooth()
            }
        }
    }

    fun setBluetoothStateListener(listener: BluetoothStateListener) {
        mBluetoothStateListener = listener
    }

    fun setOnDataReceivedListener(listener: OnDataReceivedListener) {
        mDataReceivedListener = listener
    }

    fun setBluetoothConnectionListener(listener: BluetoothConnectionListener) {
        mBluetoothConnectionListener = listener
    }

    fun setAutoConnectionListener(listener: AutoConnectionListener) {
        mAutoConnectionListener = listener
    }

    fun enable() {
        mBluetoothAdapter.enable()
    }

    fun send(data: ByteArray, CRLF: Boolean) {
        if (mBluetoothService!!.getState() == BluetoothState.STATE_CONNECT) {
            if (CRLF) {
                val data2 = ByteArray(data.size + 2)
                for (i in data.indices)
                    data2[i] = data[i]
                data2[data2.size - 2] = 0x0A
                data2[data2.size - 1] = 0x0D
                mBluetoothService!!.write(data2)
            } else {
                mBluetoothService!!.write(data)
            }
        }
    }

    fun send(data: String, CRLF: Boolean) {
        var sendData = data
        if (mBluetoothService!!.getState() == BluetoothState.STATE_CONNECT) {
            if (CRLF)
                sendData += "\r\n"
            mBluetoothService!!.write(data.toByteArray())
        }
    }

    fun getConnectedDeviceName(): String? {
        return mDeviceName
    }

    fun getConnectedDeviceAddress(): String? {
        return mDeviceAddress
    }

    private fun getPairedDeviceName(): ArrayList<String> {
        val devices = mBluetoothAdapter.bondedDevices
        val nameList = ArrayList<String>(devices.size)
        for ((c, device) in devices.withIndex()) {
            nameList[c] = device.name
        }
        return nameList
    }

    private fun getPairedDeviceAddress(): ArrayList<String> {
        val devices = mBluetoothAdapter.bondedDevices
        val addressList = ArrayList<String>(devices.size)
        for ((c, device) in devices.withIndex()) {
            addressList[c] = device.address
        }
        return addressList
    }


    fun autoConnect(keywordName: String) {
        if (!isAutoConnectionEnabled) {
            keyword = keywordName
            isAutoConnectionEnabled = true
            isAutoConnecting = true
            if (mAutoConnectionListener != null)
                mAutoConnectionListener!!.onAutoConnectionStarted()
            val arrayFilterAddress = ArrayList<String>()
            val arrayFilterName = ArrayList<String>()
            val arrayName = getPairedDeviceName()
            val arrayAddress = getPairedDeviceAddress()
            for (i in arrayName.indices) {
                if (arrayName[i].contains(keywordName)) {
                    arrayFilterAddress.add(arrayAddress[i])
                    arrayFilterName.add(arrayName[i])
                }
            }

            bluetoothCL = object : BluetoothConnectionListener {
                override fun onDeviceConnected(name: String?, address: String?) {
                    bluetoothCL = null
                    isAutoConnecting = false
                }

                override fun onDeviceDisconnected() {}
                override fun onDeviceConnectionFailed() {
                    Log.e("CHeck", "Failed")
                    if (isServiceRunning) {
                        if (isAutoConnectionEnabled) {
                            c++
                            if (c >= arrayFilterAddress.size)
                                c = 0
                            connect(arrayFilterAddress[c])
                            Log.e("CHeck", "Connect")
                            if (mAutoConnectionListener != null)
                                mAutoConnectionListener!!.onNewConnection(arrayFilterName[c], arrayFilterAddress[c])
                        } else {
                            bluetoothCL = null
                            isAutoConnecting = false
                        }
                    }
                }
            }

            setBluetoothConnectionListener(bluetoothCL as BluetoothConnectionListener)
            c = 0
            if (mAutoConnectionListener != null)
                mAutoConnectionListener!!.onNewConnection(arrayName[c], arrayAddress[c])
            if (arrayFilterAddress.size > 0)
                connect(arrayFilterAddress[c])
            else
                Toast.makeText(mContext, "Device name mismatch", Toast.LENGTH_SHORT).show()
        }
    }
}