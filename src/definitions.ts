export interface LocationPluginPlugin {
  isLocationEnabled(options?: any): Promise<{ isEnabled: boolean }>;
}
