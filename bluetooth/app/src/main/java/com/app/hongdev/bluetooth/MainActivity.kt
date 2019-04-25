package com.app.hongdev.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.app.hongdev.bluetooth.bluetooth.BluetoothState
import com.app.hongdev.bluetooth.bluetooth.HBluetooth
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*

// 메인
class MainActivity : AppCompatActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mContext: Context
    }

    lateinit var mHBT: HBluetooth
    private lateinit var mMenu: Menu
    private var mLastState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            supportActionBar?.title = Html.fromHtml("<font color=\"#4B89DC\">" + getString(R.string.app_name) + "</font>", Html.FROM_HTML_MODE_LEGACY)
        } else {
            supportActionBar?.title = Html.fromHtml("<font color=\"#4B89DC\">" + getString(R.string.app_name) + "</font>")
        }

        TedPermission.with(this).setPermissionListener(object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(applicationContext, "감사합니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            }
        }).setPermissions(android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION).check()


        mHBT = HBluetooth(this)
        mContext = this

        if (!mHBT.isBluetoothEnabled()) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0)
        }

        mHBT.setOnDataReceivedListener(object : HBluetooth.OnDataReceivedListener {
            override fun onDataReceived(data: ByteArray?, message: String) {
                when(message) {
                    "20" -> { lightTB.isChecked = false }
                    "21" -> { lightTB.isChecked = true }
                    "10" -> { breakTB.isChecked = false }
                    "11" -> { breakTB.isChecked = true }
                    "30" -> { ledTB.isChecked = false }
                    "31" -> { ledTB.isChecked = true }
                }
            }
        })

        mHBT.setBluetoothConnectionListener(object : HBluetooth.BluetoothConnectionListener {
            @SuppressLint("SetTextI18n")
            override fun onDeviceConnected(name: String?, address: String?) {
                mMenu.clear()
                menuInflater.inflate(R.menu.menu_disconnection, mMenu)
            }

            override fun onDeviceDisconnected() {
                mMenu.clear()
                menuInflater.inflate(R.menu.menu_connect, mMenu)
            }

            override fun onDeviceConnectionFailed() {
                Toast.makeText(this@MainActivity, "연결 실패", Toast.LENGTH_SHORT).show()
            }
        })

        mHBT.setBluetoothStateListener(object : HBluetooth.BluetoothStateListener {
            override fun onServiceStateChanged(state: Int) {
                if (state == BluetoothState.STATE_LISTEN && mLastState == 3) {
                    val dialog = AlertDialog.Builder(this@MainActivity)
                    dialog.setTitle("연결 끊김")
                    dialog.setMessage("블루투스 연결이 끊겼습니다.")
                    dialog.setPositiveButton("OK") { _, _ -> }
                    dialog.show()
                }
                mLastState = state
            }
        })

        lightTB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sendBluetoothRequest("21")
            else sendBluetoothRequest("20")
        }
        breakTB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sendBluetoothRequest("11")
            else sendBluetoothRequest("10")
        }
        ledTB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sendBluetoothRequest("31")
            else sendBluetoothRequest("30")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        menuInflater.inflate(R.menu.menu_connect, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_device_connect -> {
                mHBT.setDeviceTarget()
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE)
            }
            R.id.menu_disconnect -> {
                if (mHBT.getServiceState() == BluetoothState.STATE_CONNECT) {
                    mHBT.disconnect()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHBT.stopService()
    }

    override fun onStart() {
        super.onStart()
        if (mHBT.isBluetoothEnabled()) {
            if (!mHBT.isServiceAvailable()) {
                mHBT.setupService()
                mHBT.startService()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BluetoothState.REQUEST_CONNECT_DEVICE -> {
                if (data != null)
                    if (resultCode == Activity.RESULT_OK) mHBT.connect(data)
            }
            BluetoothState.REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    mHBT.setupService()
                    mHBT.startService()
                } else {
                    Toast.makeText(applicationContext, "올바른 블루투스 아님", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    fun sendBluetoothRequest(data: String) {
        mHBT.send(data, false)
    }
}
