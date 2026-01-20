package com.heartmonitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class HeartRateMonitorActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothLEManager
    private lateinit var heartRateChart: LineChart
    private lateinit var heartRateValue: TextView
    private lateinit var deviceNameText: TextView
    private lateinit var connectionStatusText: TextView
    private lateinit var disconnectButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    private var currentHeartRate = 0
    private var chartXValue = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate_monitor)

        val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("device")
        }

        heartRateChart = findViewById(R.id.heartRateChart)
        heartRateValue = findViewById(R.id.heartRateValue)
        deviceNameText = findViewById(R.id.deviceNameText)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        disconnectButton = findViewById(R.id.disconnectButton)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        setupChart()

        bluetoothManager = BluetoothLEManager(this)

        bluetoothManager.onConnectionStateChange = { connected ->
            runOnUiThread {
                if (connected) {
                    connectionStatusText.text = getString(R.string.connected)
                } else {
                    connectionStatusText.text = getString(R.string.disconnected)
                }
            }
        }

        bluetoothManager.onHeartRateReceived = { heartRate ->
            runOnUiThread {
                currentHeartRate = heartRate
                updateHeartRate(heartRate)
                addChartEntry(heartRate)
            }
        }

        device?.let {
            connectToDevice(it)
        }

        disconnectButton.setOnClickListener {
            bluetoothManager.disconnect()
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        deviceNameText.text = getString(R.string.device_name, device.name ?: "Unknown")
        connectionStatusText.text = getString(R.string.connecting)
        bluetoothManager.connectToDevice(device)
    }

    private fun setupChart() {
        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333333")
                axisMinimum = 40f
                axisMaximum = 200f
            }

            axisRight.isEnabled = false

            val emptyDataSet = LineDataSet(ArrayList(), "Heart Rate")
            emptyDataSet.apply {
                color = Color.parseColor("#00FF00")
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            data = LineData(emptyDataSet as ILineDataSet)
        }
    }

    private fun updateHeartRate(heartRate: Int) {
        heartRateValue.text = heartRate.toString()
    }

    private fun addChartEntry(heartRate: Int) {
        val data = heartRateChart.data

        if (data != null) {
            var set = data.getDataSetByIndex(0) as? LineDataSet

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            // Add the heart rate data point
            data.addEntry(Entry(chartXValue, heartRate.toFloat()), 0)
            chartXValue += 1f

            // Simulate ECG-like waveform by adding intermediate points
            // This creates a more realistic heartbeat pattern
            if (currentHeartRate > 0) {
                val baselineHR = heartRate.toFloat()

                // Add some variation to create ECG-like pattern
                for (i in 1..5) {
                    val variation = Random.nextFloat() * 10 - 5
                    data.addEntry(Entry(chartXValue, baselineHR + variation), 0)
                    chartXValue += 0.2f
                }
            }

            data.notifyDataChanged()
            heartRateChart.notifyDataSetChanged()

            // Limit visible range to last 50 points for scrolling effect
            heartRateChart.setVisibleXRangeMaximum(50f)
            heartRateChart.moveViewToX(data.entryCount.toFloat())

            // Remove old entries to prevent memory issues
            // Use set.removeEntry(0) to remove by index from the first dataset
            if (data.entryCount > 200) {
                set?.removeEntry(0)
            }
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Heart Rate")
        set.apply {
            color = Color.parseColor("#00FF00")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        return set
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
