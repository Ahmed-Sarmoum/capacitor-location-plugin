export interface LocationPluginPlugin {
  isLocationEnabled(options: { enabled: string }): Promise<{ enabled: string }>;
}
