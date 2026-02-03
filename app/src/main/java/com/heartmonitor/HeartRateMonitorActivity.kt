package com.heartmonitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.res.Configuration
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

class HeartRateMonitorActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothLEManager
    private lateinit var heartRateChart: LineChart
    private lateinit var heartRateValue: TextView
    private var deviceNameText: TextView? = null
    private var connectionStatusText: TextView? = null
    private var disconnectButton: MaterialButton? = null
    private var toolbar: MaterialToolbar? = null

    private var currentHeartRate = 0
    private var chartXValue = 0f
    private var minHeartRate = Float.MAX_VALUE
    private var maxHeartRate = Float.MIN_VALUE
    private val yAxisPadding = 5f
    private var chartEntries = ArrayList<Entry>()

    @Volatile
    private var isViewInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate_monitor)

        val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("device")
        }

        initializeViews()
        setupChart()
        isViewInitialized = true

        bluetoothManager = BluetoothLEManager(this)

        bluetoothManager.onConnectionStateChange = { connected ->
            runOnUiThread {
                if (isViewInitialized) {
                    if (connected) {
                        connectionStatusText?.text = getString(R.string.connected)
                    } else {
                        connectionStatusText?.text = getString(R.string.disconnected)
                    }
                }
            }
        }

        bluetoothManager.onHeartRateReceived = { heartRate ->
            runOnUiThread {
                currentHeartRate = heartRate
                if (isViewInitialized) {
                    updateHeartRate(heartRate)
                    addChartEntry(heartRate)
                }
            }
        }

        device?.let {
            connectToDevice(it)
        }
    }

    private fun initializeViews() {
        heartRateChart = findViewById(R.id.heartRateChart)
        heartRateValue = findViewById(R.id.heartRateValue)
        deviceNameText = findViewById(R.id.deviceNameText)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        disconnectButton = findViewById(R.id.disconnectButton)
        toolbar = findViewById(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            finish()
        }

        disconnectButton?.setOnClickListener {
            bluetoothManager.disconnect()
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Prevent callbacks from accessing views during re-initialization
        isViewInitialized = false

        // Save current chart entries before layout change
        saveChartEntries()

        // Re-inflate the layout for new orientation
        setContentView(R.layout.activity_heart_rate_monitor)

        // Re-bind views
        initializeViews()

        // Re-setup chart and restore data
        setupChart()
        restoreChartEntries()

        // Update BPM display with current value
        if (currentHeartRate > 0) {
            heartRateValue.text = currentHeartRate.toString()
        }

        // Views are ready, allow callbacks to access them
        isViewInitialized = true
    }

    private fun saveChartEntries() {
        if (!::heartRateChart.isInitialized) return

        val data = heartRateChart.data ?: return
        val set = data.getDataSetByIndex(0) ?: return

        chartEntries.clear()
        for (i in 0 until set.entryCount) {
            val entry = set.getEntryForIndex(i)
            chartEntries.add(Entry(entry.x, entry.y))
        }
    }

    private fun restoreChartEntries() {
        if (chartEntries.isNotEmpty()) {
            val data = heartRateChart.data
            if (data != null) {
                val set = data.getDataSetByIndex(0) as? LineDataSet
                set?.let {
                    for (entry in chartEntries) {
                        data.addEntry(entry, 0)
                    }
                    data.notifyDataChanged()
                    heartRateChart.notifyDataSetChanged()
                    heartRateChart.setVisibleXRangeMaximum(50f)
                    heartRateChart.moveViewToX(data.entryCount.toFloat())
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        deviceNameText?.text = getString(R.string.device_name, device.name ?: "Unknown")
        connectionStatusText?.text = getString(R.string.connecting)
        bluetoothManager.connectToDevice(device)
    }

    private fun setupChart() {
        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(true)
            setGridBackgroundColor(Color.parseColor("#0D0D0D"))
            legend.isEnabled = false
            setViewPortOffsets(60f, 20f, 20f, 20f)

            xAxis.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A3300")
                setDrawAxisLine(false)
                setDrawLabels(false)
                gridLineWidth = 0.5f
            }

            axisLeft.apply {
                textColor = Color.parseColor("#00FF00")
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A3300")
                gridLineWidth = 0.5f
                axisMinimum = 40f
                axisMaximum = 200f
                setDrawAxisLine(false)
                setLabelCount(6, true)
            }

            axisRight.isEnabled = false

            val emptyDataSet = LineDataSet(ArrayList(), "Heart Rate")
            emptyDataSet.apply {
                color = Color.parseColor("#00FF00")
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 1.5f
                mode = LineDataSet.Mode.LINEAR
                setDrawFilled(false)
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

            val baselineHR = heartRate.toFloat()

            // Generate ECG-like PQRST waveform pattern
            val ecgWaveform = generateECGWaveform(baselineHR)

            // Track min/max for dynamic Y-axis
            var waveformMin = baselineHR
            var waveformMax = baselineHR

            for (point in ecgWaveform) {
                data.addEntry(Entry(chartXValue, point), 0)
                chartXValue += 0.4f
                if (point < waveformMin) waveformMin = point
                if (point > waveformMax) waveformMax = point
            }

            // Update global min/max tracking
            if (waveformMin < minHeartRate) minHeartRate = waveformMin
            if (waveformMax > maxHeartRate) maxHeartRate = waveformMax

            // Dynamically adjust Y-axis based on observed values
            updateYAxisRange()

            data.notifyDataChanged()
            heartRateChart.notifyDataSetChanged()

            // Limit visible range for scrolling effect
            heartRateChart.setVisibleXRangeMaximum(100f)
            heartRateChart.moveViewToX(data.entryCount.toFloat())

            // Remove old entries to prevent memory issues
            if (set != null && set.entryCount > 500) {
                set.removeFirst()
            }
        }
    }

    private fun generateECGWaveform(baseline: Float): List<Float> {
        val waveform = mutableListOf<Float>()
        val amplitude = baseline * 0.15f

        // Flat baseline before P wave
        repeat(3) { waveform.add(baseline) }

        // P wave (small bump)
        waveform.add(baseline + amplitude * 0.15f)
        waveform.add(baseline + amplitude * 0.25f)
        waveform.add(baseline + amplitude * 0.2f)
        waveform.add(baseline + amplitude * 0.1f)
        waveform.add(baseline)

        // PR segment (flat)
        repeat(2) { waveform.add(baseline) }

        // Q wave (small dip)
        waveform.add(baseline - amplitude * 0.1f)

        // R wave (tall spike up)
        waveform.add(baseline + amplitude * 0.3f)
        waveform.add(baseline + amplitude * 0.7f)
        waveform.add(baseline + amplitude * 1.0f)
        waveform.add(baseline + amplitude * 0.6f)

        // S wave (dip below baseline)
        waveform.add(baseline - amplitude * 0.3f)
        waveform.add(baseline - amplitude * 0.15f)

        // ST segment (return to baseline)
        waveform.add(baseline)
        repeat(2) { waveform.add(baseline) }

        // T wave (rounded bump)
        waveform.add(baseline + amplitude * 0.1f)
        waveform.add(baseline + amplitude * 0.25f)
        waveform.add(baseline + amplitude * 0.35f)
        waveform.add(baseline + amplitude * 0.3f)
        waveform.add(baseline + amplitude * 0.15f)
        waveform.add(baseline)

        // Flat baseline after T wave
        repeat(5) { waveform.add(baseline) }

        return waveform
    }

    private fun updateYAxisRange() {
        if (minHeartRate != Float.MAX_VALUE && maxHeartRate != Float.MIN_VALUE) {
            val range = maxHeartRate - minHeartRate
            val padding = maxOf(range * 0.2f, yAxisPadding)

            heartRateChart.axisLeft.apply {
                axisMinimum = minHeartRate - padding
                axisMaximum = maxHeartRate + padding
            }
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Heart Rate")
        set.apply {
            color = Color.parseColor("#00FF00")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 1.5f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(false)
        }
        return set
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
