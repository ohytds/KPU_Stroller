package com.app.hongdev.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import com.app.hongdev.bluetooth.bluetooth.BluetoothState
import kotlinx.android.synthetic.main.activity_device_list.*

//블루투스 검색 및 연결
class DeviceList : Activity() {

    private var mBtAdapter: BluetoothAdapter? = null
    private var mPairedDevicesArrayAdapter: ArrayAdapter<String>? = null
    private var pairedDevices: Set<BluetoothDevice>? = null

    private val mDeviceClickListener = AdapterView.OnItemClickListener { _, v, _, _ ->
        if (mBtAdapter!!.isDiscovering)
            mBtAdapter!!.cancelDiscovery()

        if ((v as TextView).text.toString() != "") {
            val info = v.text.toString()
            val address = info.substring(info.length - 17)

            val intent = Intent()
            intent.putExtra(BluetoothState.DEVICE_ADDRESS, address)

            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    mPairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)



        setResult(Activity.RESULT_CANCELED)

        scanButton.setOnClickListener { doDiscovery() }

        Handler().postDelayed({ progressBar.visibility = ProgressBar.GONE; bluetoothListView.visibility = ListView.VISIBLE }, 2000)


        val deviceName = intent.getIntExtra("deviceName", R.layout.device_name)
        mPairedDevicesArrayAdapter = ArrayAdapter(this, deviceName)

        bluetoothListView.adapter = mPairedDevicesArrayAdapter
        bluetoothListView.onItemClickListener = mDeviceClickListener

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        pairedDevices = mBtAdapter!!.bondedDevices

        if (pairedDevices!!.isNotEmpty()) {
            for (device in pairedDevices!!) {
                mPairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBtAdapter != null) {
            mBtAdapter!!.cancelDiscovery()
        }

        this.unregisterReceiver(mReceiver)
        this.finish()
    }

    private fun doDiscovery() {
        Log.d(TAG, "doDiscovery()")

        mPairedDevicesArrayAdapter!!.clear()

        if (pairedDevices!!.isNotEmpty()) {
            for (device in pairedDevices!!) {
                mPairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
            }
        }
        mBtAdapter!!.startDiscovery()
    }

    companion object {
        private const val TAG = "BluetoothList"
    }

}