package com.heartmonitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothLEManager
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var scanButton: MaterialButton
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var noDevicesText: TextView

    private var isScanning = false

    private val requestBluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            checkBluetooth()
        } else {
            Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = BluetoothLEManager(this)

        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        scanButton = findViewById(R.id.scanButton)
        noDevicesText = findViewById(R.id.noDevicesText)

        deviceAdapter = DeviceAdapter { device ->
            onDeviceSelected(device)
        }

        devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        bluetoothManager.onDeviceFound = { device, rssi ->
            runOnUiThread {
                deviceAdapter.addDevice(device, rssi)
                noDevicesText.visibility = View.GONE
            }
        }

        scanButton.setOnClickListener {
            if (isScanning) {
                stopScan()
            } else {
                startScan()
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkBluetooth()
        }
    }

    private fun checkBluetooth() {
        if (!bluetoothManager.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            ) {
                requestBluetoothEnableLauncher.launch(enableBtIntent)
            }
        }
    }

    private fun startScan() {
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show()
            checkBluetooth()
            return
        }

        deviceAdapter.clear()
        noDevicesText.visibility = View.VISIBLE
        bluetoothManager.startScan()
        isScanning = true
        scanButton.text = getString(R.string.stop_scan)
    }

    private fun stopScan() {
        bluetoothManager.stopScan()
        isScanning = false
        scanButton.text = getString(R.string.scan_for_devices)
    }

    private fun onDeviceSelected(device: BluetoothDevice) {
        stopScan()
        val intent = Intent(this, HeartRateMonitorActivity::class.java)
        intent.putExtra("device", device)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }
}
