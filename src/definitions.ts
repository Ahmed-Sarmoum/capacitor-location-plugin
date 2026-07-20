export interface LocationPluginPlugin {
  initialize(options?: any): Promise<{ isEnabled: boolean }>;

  // Add the method for subscribing to events
  addListener(
    eventName: 'locationStatusChanged',
    listenerFunc: (status: { isEnabled: boolean }) => void,
  ): Promise<PluginListenerHandle>;

  isEnabled(): Promise<{ isEnabled: boolean }>;

  /**
   * Detect whether the current device position comes from a mock / fake GPS
   * provider (spoofing apps such as Lockito).
   *
   * On Android reads a fresh fix from the fused provider (the source most apps
   * use); on iOS 15+ reads `CLLocation.sourceInformation.isSimulatedBySoftware`.
   * Catches a spoofed position even while the device is stationary. On iOS below
   * 15 and on web it resolves `{ isMock: false, available: false }`.
   *
   * @returns `isMock` — true when the position is mocked; `available` — false
   * when the check could not run (no location permission or unsupported platform),
   * in which case `isMock` is inconclusive.
   */
  checkMock(): Promise<{ isMock: boolean; available: boolean }>;

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
