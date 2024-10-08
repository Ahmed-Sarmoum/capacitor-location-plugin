export interface LocationPluginPlugin {
  isLocationEnabled(options?: any): Promise<{ enabled: boolean }>;
}
