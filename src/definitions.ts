export interface LocationPlugin {
  isLocationEnabled(options?: any): Promise<{ isEnabled: boolean }>;
}
