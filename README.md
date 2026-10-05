# Capacitor Location Plugin

A Capacitor plugin to check whether location services are enabled, listen for changes in real time, and **detect fake / mock GPS** (spoofing apps such as Lockito, Fake GPS, etc.).

## Features

- Check if location services are enabled (`isEnabled`)
- Listen for location-services changes (`locationStatusChanged` event)
- **Detect mock / spoofed location** with two complementary checks — both read cached fixes only, so they return **instantly** and never block the UI waiting for a GPS lock:
  - `checkMock` — **current state** from the fused provider. Reflects an _actively_ running mock app and clears quickly once it is switched off (the fused provider drops the flag). Good for a responsive on/off indicator.
  - `checkMockLastKnown` — **deep** check across the raw providers' last-known fix. The mock flag survives there even after the spoofing app is switched off, so this stays positive through the **lingering fake** window — until a genuine fix replaces the planted position. Good for gating access "until the real position returns".
- **Get a fresh position and its mock flag in one call** (`getVerifiedPosition`, Android only) — forces a new fix (no cached position), so it can take a few seconds. Good for stamping a record (visit, order, check-in) with a trusted position.
- Open the device settings so the user can disable the mock-location app (`openDeveloperSettings`)
- Lightweight: the status check uses system broadcasts (no continuous updates)

> **Mock detection support:** Android, and iOS 15+. On iOS below 15 and on web, the mock checks resolve `{ isMock: false, available: false }`. `getVerifiedPosition` is Android only: it resolves `{ isMock: false, available: false }` on web and is not implemented on iOS (the call rejects).

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

Pick the check that matches your intent.

**`checkMock` — responsive indicator.** Reflects whether a mock app is running _right now_; clears soon after it is switched off.

```ts
const { isMock, available } = await LocationPlugin.checkMock();

if (available && isMock) {
  // A mock-location app is actively feeding the position.
  await LocationPlugin.openDeveloperSettings();
}
```

**`checkMockLastKnown` — block until the real position returns.** Because a spoofing app leaves a planted fix behind, a user can switch it off yet still sit on the fake position for a moment. This check keeps reporting `isMock: true` until a genuine fix overwrites it — so you can hold a blocking screen until the device recovers its real location.

```ts
// Poll on an interval (and re-check on app resume) to drive an app-wide block.
setInterval(async () => {
  const { isMock, available } = await LocationPlugin.checkMockLastKnown();
  if (available) blocked.value = isMock;
}, 5000);
```

> **Note:** after the mock app is switched off, the fake fix stays cached and `checkMockLastKnown` / `getVerifiedPosition` keep reporting `isMock: true` until a real GPS fix replaces it. To clear it right away, ask the user to **turn location off and on again**.

### Get a verified position (Android)

Use it when you need the position _and_ proof it is not fake, e.g. when saving a visit or an order.

```ts
const pos = await LocationPlugin.getVerifiedPosition();

if (!pos.available) {
  // No permission, or no fix before the timeout — ask the user to retry.
} else if (pos.isMock) {
  // The fix is fake — refuse it.
  await LocationPlugin.openDeveloperSettings();
} else {
  save({ lat: pos.latitude, lng: pos.longitude, accuracy: pos.accuracy, time: pos.time });
}
```

> It waits for a fresh fix, so show a loader: it can take up to ~8 s with no GPS lock.

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

Instant current-state check. Reads only the cached last location — never forces a fresh acquisition.

- **Android**: reads the **fused** provider's last location (`Location.isMock()` on API 31+, `isFromMockProvider()` below). The fused provider drops the mock flag shortly after the mock app is switched off, so this reflects the _current_ state.
- **iOS 15+**: reads `CLLocation.sourceInformation.isSimulatedBySoftware`.

### `checkMockLastKnown()`

```ts
checkMockLastKnown() => Promise<{ isMock: boolean; available: boolean }>
```

Instant deep check for a "block until corrected" flow. On **Android** it scans the raw providers' `getLastKnownLocation` and reports mocked if any of them is still flagged — that flag survives on the planted fix even after the mock app is switched off, so a lingering fake is still caught. Reads cached fixes only; no fresh acquisition. On **iOS below 15 and web** resolves `{ isMock: false, available: false }`.

**Shared fields (both mock checks):**

- **`isMock`**: `true` when the position comes from a mock/simulated source.
- **`available`**: `false` when the check could not run (no location permission, iOS below 15, or web) — treat `isMock` as inconclusive.

Requires location permission granted at runtime (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` on Android, when-in-use on iOS).

### `getVerifiedPosition()`

```ts
getVerifiedPosition() => Promise<VerifiedPosition>

interface VerifiedPosition {
  available: boolean;
  isMock: boolean;
  latitude?: number;
  longitude?: number;
  accuracy?: number; // meters
  time?: number; // epoch ms
}
```

"Where am I right now, and is it fake?" in one native call. **Android only.**

1. Asks the **fused** provider for a fresh high-accuracy fix (no cached position accepted), waiting up to 4 s.
2. If the fused provider is missing, fails, or returns nothing, it checks the raw providers' last-known fix: a **lingering mocked fix** is returned right away with `isMock: true`.
3. Otherwise it asks GPS / network for a single fresh fix, waiting up to 4 s more.

So the call can take up to ~8 s when there is no GPS lock.

- **`available`**: `false` when there is no location permission or no fix arrived before the timeout. The coordinates are then absent.
- **`isMock`**: `true` when the returned fix is mocked.
- **`latitude`**, **`longitude`**, **`accuracy`**, **`time`**: the fix itself.

On web resolves `{ isMock: false, available: false }`. Not implemented on iOS (the call rejects).

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
