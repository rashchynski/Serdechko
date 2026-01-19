# Heart Monitor - Bluetooth Heart Rate Monitor App

An Android application for reading real-time heart rate data from Bluetooth Low Energy (BLE) heart rate monitors like the Wahoo TICKR and displaying it with an ECG-style chart.

## Features

- 📡 **Bluetooth LE Scanning**: Automatically discovers nearby heart rate monitors that support the standard Heart Rate Service (HRS)
- 💓 **Real-time Heart Rate Display**: Shows current BPM in a visually appealing circular display
- 📊 **ECG-Style Chart**: Live visualization of heart rate data with a scrolling waveform chart
- 🔄 **Automatic Connection**: Easy one-tap connection to discovered devices
- 📱 **Modern Material Design**: Clean, intuitive interface following Material Design guidelines

## Supported Devices

This app works with any Bluetooth Low Energy heart rate monitor that implements the standard Bluetooth Heart Rate Service (UUID: 0x180D), including:

- Wahoo TICKR
- Polar H10
- Garmin HRM-Dual
- And many other standard BLE heart rate monitors

## Requirements

- Android 6.0 (API 23) or higher
- Bluetooth Low Energy (BLE) support
- Location permission (required for BLE scanning on Android < 12)
- Bluetooth permissions

## Permissions

The app requires the following permissions:

### Android 12+ (API 31+)
- `BLUETOOTH_SCAN`: To scan for nearby BLE devices
- `BLUETOOTH_CONNECT`: To connect to BLE devices

### Android 11 and below
- `BLUETOOTH`: Standard Bluetooth permission
- `BLUETOOTH_ADMIN`: To discover devices
- `ACCESS_FINE_LOCATION`: Required for BLE scanning

## How to Use

1. **Enable Bluetooth**: Make sure Bluetooth is enabled on your device
2. **Grant Permissions**: Allow the app to access Bluetooth when prompted
3. **Scan for Devices**: Tap "Scan for Devices" to search for nearby heart rate monitors
4. **Connect**: Select your heart rate monitor from the list
5. **View Data**: Watch real-time heart rate data and ECG-style visualization

## Technical Details

### Architecture

- **Language**: Kotlin
- **Minimum SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Chart Library**: MPAndroidChart v3.1.0

### Key Components

- `BluetoothLEManager`: Handles BLE scanning, connection, and data reading
- `MainActivity`: Device discovery and selection
- `HeartRateMonitorActivity`: Real-time data display and visualization
- `DeviceAdapter`: RecyclerView adapter for displaying found devices

### Bluetooth Implementation

The app uses the standard Bluetooth Heart Rate Service (HRS):
- **Service UUID**: `0000180D-0000-1000-8000-00805f9b34fb`
- **Characteristic UUID**: `00002A37-0000-1000-8000-00805f9b34fb`

## Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run on an Android device (BLE is not available on emulators)

```bash
./gradlew assembleDebug
```

## License

This project is open source and available for educational and personal use.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## Known Limitations

- ECG waveform is simulated based on heart rate data (actual ECG requires specialized sensors)
- Requires physical BLE heart rate monitor for testing
- Not tested on all BLE heart rate monitor brands

## Future Enhancements

- [ ] Save and export heart rate data
- [ ] Statistics and analytics
- [ ] Multiple device support
- [ ] Workout tracking
- [ ] Heart rate zones
- [ ] Historical data visualization
