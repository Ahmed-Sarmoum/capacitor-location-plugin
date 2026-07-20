# Capacitor Location Plugin

A Capacitor plugin to check whether location services are enabled, listen for changes in real time, and **detect fake / mock GPS** (spoofing apps such as Lockito, Fake GPS, etc.).

## Features

- Check if location services are enabled (`isEnabled`)
- Listen for location-services changes (`locationStatusChanged` event)
- **Detect mock / spoofed location** (`checkMock`) — on Android reads the mock flag from the **fused** provider (the source most apps use); on iOS 15+ reads `CLLocation.sourceInformation.isSimulatedBySoftware`. Catches a spoofed position even while the device is stationary
- Open the device settings so the user can disable the mock-location app (`openDeveloperSettings`)
- Lightweight: the status check uses system broadcasts (no continuous updates)

> **Mock detection support:** Android, and iOS 15+. On iOS below 15 and on web, `checkMock` resolves `{ isMock: false, available: false }`.

---

## Installation

```bash
npm install capacitor-location-plugin
npx cap sync
```

The mock detection uses `com.google.android.gms:play-services-location`, which the plugin declares as a dependency — no extra setup required.

---

## Usage

```ts
import { LocationPlugin } from 'capacitor-location-plugin';
```

### Check if location services are enabled

```ts
const { isEnabled } = await LocationPlugin.isEnabled();
```

### Listen for location-services changes

```ts
const listener = await LocationPlugin.addListener(
  'locationStatusChanged',
  (status) => {
    console.log('Location enabled:', status.isEnabled);
  },
);

// later
listener.remove();
```

### Detect fake / mock GPS

```ts
const { isMock, available } = await LocationPlugin.checkMock();

if (available && isMock) {
  // A mock-location app is actively feeding the position.
  await LocationPlugin.openDeveloperSettings();
}
```

Poll it on an interval (and re-check on app resume) to react when the user
enables or disables a spoofing app:

```ts
setInterval(async () => {
  const { isMock, available } = await LocationPlugin.checkMock();
  if (available) blocked.value = isMock;
}, 5000);
```

---

## API

### `isEnabled()`

```ts
isEnabled() => Promise<{ isEnabled: boolean }>
```

Returns whether GPS/network location services are enabled.

### `initialize(options?)`

```ts
initialize(options?: any) => Promise<{ isEnabled: boolean }>
```

Registers the internal broadcast receiver and returns the current status. Call once before relying on `locationStatusChanged`.

### `checkMock()`

```ts
checkMock() => Promise<{ isMock: boolean; available: boolean }>
```

Requests a fresh location and reports whether it is mocked.

- **Android**: reads the **fused** provider (`Location.isMock()` on API 31+, `isFromMockProvider()` below), falling back to the `LocationManager` providers if fused is unavailable.
- **iOS 15+**: requests a one-shot location and reads `CLLocation.sourceInformation.isSimulatedBySoftware`.

Fields:

- **`isMock`**: `true` when the current position comes from a mock/simulated source.
- **`available`**: `false` when the check could not run (no location permission, iOS below 15, or web) — treat `isMock` as inconclusive.

Requires location permission granted at runtime (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` on Android, when-in-use on iOS).

### `openDeveloperSettings()`

```ts
openDeveloperSettings() => Promise<void>
```

On Android, opens the developer settings screen (falls back to the main settings) so the user can turn off the selected mock-location app. On iOS, opens the app's settings page.

### `addListener('locationStatusChanged', ...)`

```ts
addListener(
  eventName: 'locationStatusChanged',
  listenerFunc: (status: { isEnabled: boolean }) => void,
) => Promise<PluginListenerHandle>
```

---

## Android setup

Add the location permissions to your app's **`AndroidManifest.xml`**:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## iOS setup

In **`Info.plist`**, add usage descriptions:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app requires access to your location while using the app.</string>
```

---

## Limitations

Mock detection relies on the OS simulated-location flag. It reliably catches spoofing apps on a **non-rooted / non-jailbroken** device. A **rooted** Android device running a mock-hiding module (Xposed/LSPosed) can suppress the flag; defeating that requires attestation (e.g. Play Integrity) and is out of scope for this plugin. iOS detection requires **iOS 15+** (no public API exists below that).

---

## License

MIT
