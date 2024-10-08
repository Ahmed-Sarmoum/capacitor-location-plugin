import { WebPlugin } from '@capacitor/core';

import type { LocationPluginPlugin } from './definitions';

export class LocationPluginWeb extends WebPlugin implements LocationPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
