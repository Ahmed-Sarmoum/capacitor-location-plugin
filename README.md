# Capacitor Location Plugin

A Capacitor plugin that enables checking if location services are enabled on the user's device.

## Features
- Check if location services (GPS, etc.) are enabled
- Easy integration with Capacitor projects
- Support both Android and IOS

## Getting Started

### 1. Install the Plugin

To install the plugin, use npm or yarn:

```bash
npm install capacitor-location-plugin
npx cap sync
```
### 2. Usage (Example with Vue3)
After installation, you can use the plugin as follows to check if location services are enabled:
```ts
import { Plugins } from '@capacitor/core';
const { LocationPlugin } = Plugins;

async function checkLocationEnabled() {
  const { isEnabled } = await LocationPlugin.isLocationEnabled();
  if (isEnabled) {
    console.log('Location is enabled!');
  } else {
    console.log('Location is disabled!');
  }
}

checkLocationEnabled();
```

#### Listen for GPS Status Changes in Real-Time
You can listen for changes in the GPS status using the locationStatusChanged event:

```ts
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { Plugins } from '@capacitor/core';
const { LocationPlugin } = Plugins;


const isLocationEnabled = ref(false);
let locationListener: any;

async function checkLocationEnabled() {
  const { isEnabled } = await LocationPlugin.isLocationEnabled();
  isLocationEnabled.value = isEnabled;
  if (isEnabled) {
    console.log('Location is enabled!');
  } else {
    console.log('Location is disabled!');
  }
}

const onLocationStatusChanged = (status: { isEnabled: boolean }) => {
  isLocationEnabled.value = status.isEnabled;
};

onMounted(() => {
  // Check initial location status
  checkLocationEnabled();

  // Listen for location status changes and store the listener reference
  locationListener = LocationPlugin.addListener('locationStatusChanged', onLocationStatusChanged);
});

onBeforeUnmount(() => {
  // Clean up event listener when component is destroyed
  LocationPlugin.remove();
});

```

### 3. API
#### isLocationEnabled()
This method checks whether location services are enabled on the device.

```ts
isLocationEnabled(options?: any) => Promise<{ isEnabled: boolean }>
```
#### Returns
**`Promise<{ isEnabled: boolean }>`**: Resolves with an object containing:

- **`isEnabled`**: <code>boolean</code> - `true` if location services are enabled, otherwise `false`.



<!-- ### Breakdown of the README Syntax:
1. **Installation Instructions** are provided under the "Install" section with proper code blocks for terminal commands.
2. **iOS and Android Setup** is detailed with code snippets for `Info.plist` and `AndroidManifest.xml`.
3. **Usage** provides an example of how to use the plugin in a Capacitor app.
4. **API Documentation** for `isLocationEnabled` includes a code block for its signature and a table explaining its parameters and return value.
5. **License** section indicates the type of license used for the plugin. -->
