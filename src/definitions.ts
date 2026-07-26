export interface LocationPluginPlugin {
  initialize(options?: any): Promise<{ isEnabled: boolean }>;

  // Add the method for subscribing to events
  addListener(
    eventName: 'locationStatusChanged',
    listenerFunc: (status: { isEnabled: boolean }) => void,
  ): Promise<PluginListenerHandle>;

  isEnabled(): Promise<{ isEnabled: boolean }>;

  /**
   * Instant "is a mock app active RIGHT NOW?" check.
   *
   * Reads only the fused provider's last-known location — no fresh GPS
   * acquisition, so it returns immediately. The fused provider drops the mock
   * flag within a fix cycle after the spoofing app is switched off, so this
   * reflects the CURRENT state and clears quickly. Good for a responsive on/off
   * indicator.
   *
   * Caveat: it can report `isMock: false` while the coordinates are still a stale
   * fake, because the fused flag clears before the position does. To stay locked
   * until the real position returns, use `checkMockLastKnown` instead.
   *
   * - Android: fused last location, `Location.isMock()` (API 31+) /
   *   `isFromMockProvider()` below.
   * - iOS 15+: `CLLocation.sourceInformation.isSimulatedBySoftware`.
   * - iOS < 15 / web: resolves `{ isMock: false, available: false }`.
   *
   * @returns `isMock` — true when the current position is mocked; `available` —
   * false when the check could not run (no location permission or unsupported
   * platform), in which case `isMock` is inconclusive.
   */
  checkMock(): Promise<{ isMock: boolean; available: boolean }>;

  /**
   * Instant "is the position we're SITTING ON still fake?" check.
   *
   * Reads the RAW providers' last-known fix (GPS / network / passive) and reports
   * mocked if any of them is still flagged. Unlike the fused provider, a raw
   * provider keeps the mock flag on its last-known `Location` even after the
   * spoofing app is switched off — until a genuine fix overwrites it. So this
   * stays `isMock: true` through the "lingering fake" window and only clears once
   * the device recovers a real position.
   *
   * Use it to block access until the real position returns: poll it, and warn the
   * user that unblocking can lag a moment after they disable the mock app. Reads
   * cached fixes only — never forces a fresh acquisition, so it returns
   * immediately.
   *
   * Android only; resolves `{ isMock: false, available: false }` on iOS and web.
   *
   * @returns same shape as `checkMock`.
   */
  checkMockLastKnown(): Promise<{ isMock: boolean; available: boolean }>;

  /**
   * Open the Android developer settings (falls back to the main settings) so the
   * user can turn off the selected mock-location app. No-op on iOS and web.
   */
  openDeveloperSettings(): Promise<void>;
}

// TypeScript interface for handling the event listener removal
export interface PluginListenerHandle {
  remove: () => void;
}
