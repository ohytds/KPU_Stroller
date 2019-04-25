package com.app.hongdev.bluetooth.bluetooth

class BluetoothState {
    companion object {
        // 연결 상태
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECT = 3

        // 핸들러 메시지 구분
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5
        const val DEVICE_NAME = "device_name"
        const val DEVICE_ADDRESS = "device_address"
        const val TOAST = "toast"


        const val REQUEST_CONNECT_DEVICE = 100
        const val REQUEST_ENABLE_BT = 101

    }
}