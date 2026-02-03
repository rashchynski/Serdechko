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

                    // Use orientation-aware visible range
                    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val visibleRange = if (isLandscape) 200f else 120f
                    heartRateChart.setVisibleXRangeMaximum(visibleRange)
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
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(true)
            setGridBackgroundColor(Color.parseColor("#0A0A0A"))
            legend.isEnabled = false

            // Adjust viewport offsets based on orientation
            if (isLandscape) {
                setViewPortOffsets(40f, 30f, 30f, 30f)
            } else {
                setViewPortOffsets(50f, 15f, 15f, 15f)
            }

            // ECG-style grid - more lines for that classic ECG paper look
            xAxis.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A3300")
                setDrawAxisLine(false)
                setDrawLabels(false)
                gridLineWidth = 0.3f
                setGranularity(5f)
                setGranularityEnabled(true)
            }

            axisLeft.apply {
                textColor = Color.parseColor("#00DD00")
                textSize = if (isLandscape) 12f else 9f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A3300")
                gridLineWidth = 0.3f
                axisMinimum = 40f
                axisMaximum = 200f
                setDrawAxisLine(false)
                setLabelCount(8, true)
            }

            axisRight.isEnabled = false

            val emptyDataSet = LineDataSet(ArrayList(), "Heart Rate")
            emptyDataSet.apply {
                color = Color.parseColor("#00FF00")
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = if (isLandscape) 2f else 1.8f
                mode = LineDataSet.Mode.LINEAR
                setDrawFilled(false)
                // Add subtle highlight for ECG look
                highLightColor = Color.parseColor("#00FF00")
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
                chartXValue += 0.5f  // Slightly wider spacing for cleaner look
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

            // Adjust visible range based on orientation
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val visibleRange = if (isLandscape) 200f else 120f
            heartRateChart.setVisibleXRangeMaximum(visibleRange)
            heartRateChart.moveViewToX(data.entryCount.toFloat())

            // Remove old entries to prevent memory issues (keep more entries for landscape)
            val maxEntries = if (isLandscape) 800 else 500
            if (set != null && set.entryCount > maxEntries) {
                set.removeFirst()
            }
        }
    }

    private fun generateECGWaveform(baseline: Float): List<Float> {
        val waveform = mutableListOf<Float>()
        // Larger amplitude for more dramatic ECG appearance
        val amplitude = baseline * 0.25f

        // Extended flat baseline before P wave (more spacing = less spikes)
        repeat(8) { waveform.add(baseline) }

        // P wave (small atrial depolarization bump)
        waveform.add(baseline + amplitude * 0.08f)
        waveform.add(baseline + amplitude * 0.15f)
        waveform.add(baseline + amplitude * 0.18f)
        waveform.add(baseline + amplitude * 0.15f)
        waveform.add(baseline + amplitude * 0.08f)
        waveform.add(baseline)

        // PR segment (flat - AV node delay)
        repeat(4) { waveform.add(baseline) }

        // Q wave (small dip - septal depolarization)
        waveform.add(baseline - amplitude * 0.08f)
        waveform.add(baseline - amplitude * 0.12f)

        // R wave (tall spike up - ventricular depolarization)
        waveform.add(baseline + amplitude * 0.2f)
        waveform.add(baseline + amplitude * 0.6f)
        waveform.add(baseline + amplitude * 1.0f)  // Peak
        waveform.add(baseline + amplitude * 0.5f)

        // S wave (dip below baseline)
        waveform.add(baseline - amplitude * 0.25f)
        waveform.add(baseline - amplitude * 0.12f)
        waveform.add(baseline)

        // ST segment (isoelectric - early ventricular repolarization)
        repeat(5) { waveform.add(baseline) }

        // T wave (rounded bump - ventricular repolarization)
        waveform.add(baseline + amplitude * 0.05f)
        waveform.add(baseline + amplitude * 0.12f)
        waveform.add(baseline + amplitude * 0.2f)
        waveform.add(baseline + amplitude * 0.25f)
        waveform.add(baseline + amplitude * 0.22f)
        waveform.add(baseline + amplitude * 0.15f)
        waveform.add(baseline + amplitude * 0.08f)
        waveform.add(baseline)

        // Extended flat baseline after T wave (TP segment - more spacing)
        repeat(12) { waveform.add(baseline) }

        return waveform
    }

    private fun updateYAxisRange() {
        if (minHeartRate != Float.MAX_VALUE && maxHeartRate != Float.MIN_VALUE) {
            val range = maxHeartRate - minHeartRate
            // More generous padding for ECG-like appearance (25% of range, min 10)
            val padding = maxOf(range * 0.25f, 10f)

            heartRateChart.axisLeft.apply {
                // Ensure reasonable bounds
                val newMin = maxOf(minHeartRate - padding, 20f)
                val newMax = minOf(maxHeartRate + padding, 250f)

                // Only update if values have changed significantly to avoid jitter
                if (kotlin.math.abs(axisMinimum - newMin) > 2f || kotlin.math.abs(axisMaximum - newMax) > 2f) {
                    axisMinimum = newMin
                    axisMaximum = newMax
                }
            }
        }
    }

    private fun createSet(): LineDataSet {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val set = LineDataSet(null, "Heart Rate")
        set.apply {
            color = Color.parseColor("#00FF00")
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = if (isLandscape) 2f else 1.8f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(false)
            highLightColor = Color.parseColor("#00FF00")
        }
        return set
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
