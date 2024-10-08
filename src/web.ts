import { WebPlugin } from '@capacitor/core';

import type { LocationPluginPlugin } from './definitions';

export class LocationPluginWeb extends WebPlugin implements LocationPluginPlugin {
  async isLocationEnabled(options: { enabled: string }): Promise<{ enabled: string }> {
    console.log('ECHO', options);
    return options;
  }
}
