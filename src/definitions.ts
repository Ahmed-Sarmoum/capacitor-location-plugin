export interface LocationPluginPlugin {
  isLocationEnabled(options?: any): Promise<{ isEnabled: boolean }>;

  // Add the method for subscribing to events
  addListener(
    eventName: 'locationStatusChanged',
    listenerFunc: (status: { isEnabled: boolean }) => void,
  ): Promise<PluginListenerHandle>;
}

// TypeScript interface for handling the event listener removal
export interface PluginListenerHandle {
  remove: () => void;
}
