# Capacitor Location Plugin

A Capacitor plugin that allows you to check whether location services (GPS, network, etc.) are enabled on the device, and listen in real-time for changes to the location services state.

## Features

- Check if location services are enabled (`isEnabled`)
- Listen for changes to location services (`locationStatusChanged` event)
- Lightweight implementation (no continuous location updates, uses system broadcasts)
- Works on **Android** and **iOS** (iOS support requires CLLocation permissions)

---

## Installation

```bash
npm install capacitor-location-plugin
npx cap sync
```

---

## Usage

### Example with Vue 3

```ts
import { Plugins } from '@capacitor/core';
const { LocationPlugin } = Plugins;

async function checkLocationEnabled() {
  const { isEnabled } = await LocationPlugin.isEnabled();
  if (isEnabled) {
    console.log('✅ Location is enabled!');
  } else {
    console.log('❌ Location is disabled!');
  }
}
```

---

### Listen for Location Status Changes

You can subscribe to `locationStatusChanged` to get notified whenever the user enables or disables GPS/location services:

```ts
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { Plugins } from '@capacitor/core';
const { LocationPlugin } = Plugins;

const isLocationEnabled = ref(false);
let listener: any;

async function checkLocation() {
  const { isEnabled } = await LocationPlugin.isEnabled();
  isLocationEnabled.value = isEnabled;
}

const onLocationStatusChanged = (status: { isEnabled: boolean }) => {
  isLocationEnabled.value = status.isEnabled;
};

onMounted(() => {
  // Initial check
  checkLocation();

  // Start listening
  listener = LocationPlugin.addListener('locationStatusChanged', onLocationStatusChanged);
});

onBeforeUnmount(() => {
  // Clean up listener
  if (listener) listener.remove();
});
```

---

## API

### `isEnabled()`

Checks whether location services are enabled.

```ts
isEnabled() => Promise<{ isEnabled: boolean }>
```

#### Returns

- **`isEnabled`**: `boolean` → `true` if location services are enabled, `false` otherwise.

---

## Android Setup

Add the following permission to your **`AndroidManifest.xml`**:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## iOS Setup

In **`Info.plist`**, add usage descriptions:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app requires access to your location while using the app.</string>
<key>NSLocationAlwaysUsageDescription</key>
<string>This app requires access to your location even when running in the background.</string>
```

---

## License

MIT
