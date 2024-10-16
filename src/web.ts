import { WebPlugin } from '@capacitor/core';

import type { LocationPluginPlugin } from './definitions';

export class LocationPluginWeb extends WebPlugin implements LocationPluginPlugin {
  async isLocationEnabled(): Promise<{ isEnabled: boolean }> {
    console.log('Checking location enabled status...');
    // For the web, you can default to false, or implement actual checks if needed.
    return { isEnabled: false }; // Assume false for web as location services vary
  }
}
